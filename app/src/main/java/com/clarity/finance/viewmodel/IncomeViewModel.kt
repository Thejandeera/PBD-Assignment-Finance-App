package com.clarity.finance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clarity.finance.data.repository.ExchangeRateProvider
import com.clarity.finance.data.repository.IncomeRepository
import com.clarity.finance.model.CurrencyType
import com.clarity.finance.model.Income
import com.clarity.finance.model.IncomeSourceItem
import com.clarity.finance.model.IncomeStatus
import com.clarity.finance.model.getDefaultIncomeSources
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * Income tracking (FR-03 – FR-06, FR-12): records multi-source, multi-currency income from
 * built-in or user-defined sources, keeps an LKR equivalent for monthly totals, and follows
 * pending payments until paid.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class IncomeViewModel @Inject constructor(
    private val incomeRepository: IncomeRepository,
    private val exchangeRateProvider: ExchangeRateProvider
) : ViewModel() {

    // Declared before init, which reads it.
    private val _formState = MutableStateFlow(IncomeFormState())
    val formState: StateFlow<IncomeFormState> = _formState.asStateFlow()

    /** Latest LKR-per-USD rate, used to pre-fill the rate for USD and USDT entries. */
    private val _liveRate = MutableStateFlow<Double?>(null)
    val liveRate: StateFlow<Double?> = _liveRate.asStateFlow()

    /** One-off confirmations and failures for a snackbar. */
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    init {
        viewModelScope.launch {
            _liveRate.value = exchangeRateProvider.getLkrPerUsdRate()
            // Fill the rate if a foreign currency was chosen before the rate arrived.
            _formState.update { state ->
                if (state.isForeignCurrency && state.rateInput.isBlank()) {
                    state.copy(rateInput = defaultRateInput())
                } else {
                    state
                }
            }
        }
    }

    // =========================================================================
    // Income sources (built-in + user-defined)
    // =========================================================================

    val sources: StateFlow<List<IncomeSourceItem>> = incomeRepository.observeIncomeSources()
        .catch { emit(getDefaultIncomeSources()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = getDefaultIncomeSources()
        )

    /** Validation message for a new source name, or null when it can be created. */
    fun validateNewSourceName(name: String): String? {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> "Enter a name"
            trimmed.length > MAX_SOURCE_NAME_LENGTH -> "Use $MAX_SOURCE_NAME_LENGTH characters or fewer"
            sources.value.any { it.name.equals(trimmed, ignoreCase = true) } -> "\"$trimmed\" already exists"
            else -> null
        }
    }

    /**
     * Creates a custom source and selects it in the form straight away. Returns a validation
     * message instead when the name can't be used.
     */
    fun addCustomSource(name: String, defaultCurrency: CurrencyType): String? {
        validateNewSourceName(name)?.let { return it }
        val source = IncomeSourceItem(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            defaultCurrency = defaultCurrency,
            isCustom = true
        )
        onSourceSelected(source)
        viewModelScope.launch {
            incomeRepository.addCustomIncomeSource(source).onFailure {
                _formState.update { it.copy(errorMessage = "Couldn't save the source \"${source.name}\". Try again.") }
            }
        }
        return null
    }

    // =========================================================================
    // Add Income form
    // =========================================================================

    /** Picking a source applies its usual currency and clears options it doesn't support. */
    fun onSourceSelected(source: IncomeSourceItem) {
        _formState.update { state ->
            state.copy(
                source = source,
                currency = source.defaultCurrency,
                rateInput = rateInputFor(source.defaultCurrency, state),
                status = if (source.canBePending) state.status else IncomeStatus.RECEIVED,
                isLoss = if (source.allowsLoss) state.isLoss else false,
                rateError = null,
                projectError = null
            )
        }
    }

    fun onCurrencySelected(currency: CurrencyType) {
        _formState.update { state ->
            state.copy(currency = currency, rateInput = rateInputFor(currency, state), rateError = null)
        }
    }

    fun onAmountChanged(input: String) {
        _formState.update { it.copy(amountInput = sanitizeDecimal(input), amountError = null) }
    }

    fun onLossSelected(isLoss: Boolean) {
        if (!_formState.value.source.allowsLoss) return
        _formState.update { it.copy(isLoss = isLoss) }
    }

    fun onRateChanged(input: String) {
        _formState.update { it.copy(rateInput = sanitizeDecimal(input), rateError = null) }
    }

    fun onStatusSelected(status: IncomeStatus) {
        if (status == IncomeStatus.PENDING && !_formState.value.source.canBePending) return
        _formState.update { it.copy(status = status, projectError = null) }
    }

    fun onDateSelected(date: LocalDate) {
        _formState.update {
            it.copy(
                date = date.toString(),
                dateError = if (date.isAfter(LocalDate.now())) FUTURE_DATE_ERROR else null
            )
        }
    }

    fun onProjectNameChanged(name: String) {
        _formState.update { it.copy(projectName = name, projectError = null) }
    }

    fun onNoteChanged(note: String) {
        _formState.update { it.copy(note = note) }
    }

    fun resetFormState() {
        _formState.value = IncomeFormState()
    }

    fun submitIncome(onSuccess: (() -> Unit)? = null) {
        val state = _formState.value
        if (state.isSubmitting) return

        val amount = state.amountInput.toDoubleOrNull()?.takeIf { it > 0.0 }
        val rate = if (state.isForeignCurrency) state.rateInput.toDoubleOrNull()?.takeIf { it > 0.0 } else 1.0
        val date = runCatching { LocalDate.parse(state.date) }.getOrNull()
        // The client/project field is only shown for sources that can be invoiced.
        val projectName = if (state.source.canBePending) state.projectName.trim() else ""
        val isPending = state.status == IncomeStatus.PENDING && state.source.canBePending

        val amountError = if (amount == null) "Enter an amount greater than 0" else null
        val rateError = if (rate == null) "Enter how many LKR one ${state.currency.name} is worth" else null
        val dateError = when {
            date == null -> "Choose a valid date"
            date.isAfter(LocalDate.now()) -> FUTURE_DATE_ERROR
            else -> null
        }
        val projectError = if (isPending && projectName.isEmpty()) {
            "Add the client or project so you can follow up"
        } else {
            null
        }

        if (amount == null || rate == null || dateError != null || projectError != null) {
            _formState.update {
                it.copy(
                    amountError = amountError,
                    rateError = rateError,
                    dateError = dateError,
                    projectError = projectError
                )
            }
            return
        }

        val signedAmount = if (state.isLoss && state.source.allowsLoss) -amount else amount
        val income = Income(
            id = UUID.randomUUID().toString(),
            sourceId = state.source.id,
            sourceName = state.source.name,
            sourceIcon = state.source.icon,
            amount = signedAmount,
            currency = state.currency,
            exchangeRate = rate,
            amountLKR = signedAmount * rate,
            status = if (isPending) IncomeStatus.PENDING else IncomeStatus.RECEIVED,
            date = state.date,
            month = state.date.take(7),
            projectName = projectName,
            note = state.note.trim(),
            createdAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            _formState.update { it.copy(isSubmitting = true, errorMessage = null) }
            incomeRepository.addIncome(income)
                .onSuccess {
                    onSuccess?.invoke()
                    resetFormState()
                }
                .onFailure { error ->
                    _formState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = error.message ?: "Couldn't save this income. Try again."
                        )
                    }
                }
        }
    }

    private fun rateInputFor(currency: CurrencyType, state: IncomeFormState): String = when {
        currency == CurrencyType.LKR -> ""
        state.isForeignCurrency && state.rateInput.isNotBlank() -> state.rateInput // USD <-> USDT
        else -> defaultRateInput()
    }

    private fun defaultRateInput(): String =
        _liveRate.value?.let { String.format(Locale.US, "%.2f", it) }.orEmpty()

    private fun sanitizeDecimal(input: String): String {
        val filtered = input.filter { it.isDigit() || it == '.' }
        val firstDot = filtered.indexOf('.')
        return if (firstDot == -1) filtered else filtered.substring(0, firstDot + 1) + filtered.substring(firstDot + 1).replace(".", "")
    }

    // =========================================================================
    // Monthly overview & pending payments
    // =========================================================================

    private val _selectedMonth = MutableStateFlow(YearMonth.now())

    fun selectPreviousMonth() {
        _selectedMonth.value = _selectedMonth.value.minusMonths(1)
    }

    fun selectNextMonth() {
        if (_selectedMonth.value.isBefore(YearMonth.now())) {
            _selectedMonth.value = _selectedMonth.value.plusMonths(1)
        }
    }

    val uiState: StateFlow<IncomeUiState> = _selectedMonth
        .flatMapLatest { yearMonth ->
            val monthKey = yearMonth.format(MONTH_KEY)
            combine(
                incomeRepository.observeIncomesForMonth(monthKey),
                incomeRepository.observePendingIncomes()
            ) { monthIncomes, pending ->
                buildUiState(yearMonth, monthKey, monthIncomes, pending)
            }
        }
        .catch { error ->
            emit(
                IncomeUiState(
                    isLoading = false,
                    currentMonth = _selectedMonth.value.format(MONTH_KEY),
                    canGoForward = _selectedMonth.value.isBefore(YearMonth.now()),
                    errorMessage = error.message ?: "Income couldn't be loaded."
                )
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = IncomeUiState(isLoading = true)
        )

    private fun buildUiState(
        yearMonth: YearMonth,
        monthKey: String,
        monthIncomes: List<Income>,
        pending: List<Income>
    ): IncomeUiState {
        val received = monthIncomes
            .filter { it.status == IncomeStatus.RECEIVED }
            .sortedWith(compareByDescending<Income> { it.date }.thenByDescending { it.createdAt })

        val bySource = received.groupBy { it.sourceId }
        val positiveTotal = bySource.values.sumOf { group -> group.sumOf { it.amountLKR }.coerceAtLeast(0.0) }
        val sourceTotals = bySource.values.map { group ->
            val sample = group.first()
            val total = group.sumOf { it.amountLKR }
            IncomeSourceTotal(
                source = IncomeSourceItem(
                    id = sample.sourceId,
                    name = sample.sourceName,
                    icon = sample.sourceIcon,
                    defaultCurrency = sample.currency,
                    isCustom = sample.isCustomSource
                ),
                amountLkr = total,
                entryCount = group.size,
                percentage = if (positiveTotal > 0 && total > 0) ((total / positiveTotal) * 100).roundToInt() else 0
            )
        }.sortedByDescending { it.amountLkr }

        return IncomeUiState(
            isLoading = false,
            currentMonth = monthKey,
            canGoForward = yearMonth.isBefore(YearMonth.now()),
            receivedIncomes = received,
            receivedTotalLkr = received.sumOf { it.amountLKR },
            sourceTotals = sourceTotals,
            pendingIncomes = pending.sortedBy { it.date },
            pendingTotalLkr = pending.sumOf { it.amountLKR },
            errorMessage = null
        )
    }

    /**
     * A pending invoice has been paid: it becomes received income dated today, so it counts
     * toward this month's actual income.
     */
    fun markAsReceived(income: Income) {
        if (!income.isPending) return
        val today = LocalDate.now().toString()
        viewModelScope.launch {
            incomeRepository.updateIncome(
                income.copy(status = IncomeStatus.RECEIVED, date = today, month = today.take(7))
            )
                .onSuccess { _userMessage.value = "${income.title} marked as received" }
                .onFailure { _userMessage.value = "Couldn't update ${income.title}. Try again." }
        }
    }

    fun deleteIncome(income: Income) {
        viewModelScope.launch {
            incomeRepository.deleteIncome(income.id)
                .onFailure { _userMessage.value = "Couldn't delete ${income.title}. Try again." }
        }
    }

    fun onUserMessageShown() {
        _userMessage.value = null
    }

    private companion object {
        val MONTH_KEY: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM")
        const val FUTURE_DATE_ERROR = "Date can't be in the future"
        const val MAX_SOURCE_NAME_LENGTH = 40
    }
}
