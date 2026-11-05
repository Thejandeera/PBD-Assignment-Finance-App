package com.clarity.finance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clarity.finance.data.local.PrimaryAccountProvider
import com.clarity.finance.data.repository.AccountRepository
import com.clarity.finance.data.repository.ExchangeRateProvider
import com.clarity.finance.data.repository.IncomeRepository
import com.clarity.finance.model.Account
import com.clarity.finance.model.AccountBalanceCalculator
import com.clarity.finance.model.CurrencyType
import com.clarity.finance.model.Income
import com.clarity.finance.model.IncomeSourceItem
import com.clarity.finance.model.IncomeStatus
import com.clarity.finance.model.RecurringIncome
import com.clarity.finance.model.getDefaultIncomeSources
import com.clarity.finance.util.CurrencyFormatter
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
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Income tracking (FR-03 – FR-06, FR-12): records multi-source, multi-currency income from
 * built-in or user-defined sources, keeps an LKR equivalent for monthly totals, and follows
 * pending payments until paid. Saved entries can be edited in full.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class IncomeViewModel @Inject constructor(
    private val incomeRepository: IncomeRepository,
    private val exchangeRateProvider: ExchangeRateProvider,
    private val accountRepository: AccountRepository,
    private val primaryAccountProvider: PrimaryAccountProvider
) : ViewModel() {

    val primaryAccountId: StateFlow<String?> = primaryAccountProvider.primaryAccountIdFlow

    val accounts: StateFlow<List<Account>> = accountRepository.observeAccounts()
        .catch { emit(emptyList()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    // Declared before init, which reads it.
    private val _formState = MutableStateFlow(IncomeFormState())
    val formState: StateFlow<IncomeFormState> = _formState.asStateFlow()

    /** Latest LKR-per-USD rate, used to pre-fill the rate for USD and USDT entries. */
    private val _liveRate = MutableStateFlow<Double?>(null)
    val liveRate: StateFlow<Double?> = _liveRate.asStateFlow()

    /** One-off confirmations and failures for a snackbar. */
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    private fun defaultInputCurrency(): CurrencyType =
        if (primaryAccountProvider.defaultCurrencyFlow.value.equals("USD", ignoreCase = true)) {
            CurrencyType.USD
        } else {
            CurrencyType.LKR
        }

    init {
        viewModelScope.launch {
            incomeRepository.processAutoCommitRecurringIncomes()
        }
        viewModelScope.launch {
            exchangeRateProvider.observeLkrPerUsdRate().collect { rate ->
                _liveRate.value = rate
                _formState.update { state ->
                    if (state.isForeignCurrency && state.rateInput.isBlank()) {
                        state.copy(rateInput = defaultRateInput())
                    } else {
                        state
                    }
                }
            }
        }
    }

    fun syncDefaultCurrency() {
        val defaultCurr = defaultInputCurrency()
        _formState.update { state ->
            if (!state.isEditing && state.amountInput.isEmpty()) {
                state.copy(
                    currency = defaultCurr,
                    rateInput = rateInputFor(defaultCurr, state)
                )
            } else {
                state
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
        onCurrencySelected(defaultCurrency)
        viewModelScope.launch {
            incomeRepository.addCustomIncomeSource(source).onFailure {
                _formState.update { it.copy(errorMessage = "Couldn't save the source \"${source.name}\". Try again.") }
            }
        }
        return null
    }

    // =========================================================================
    // Add / Edit Income form
    // =========================================================================

    /** Loads a saved entry into the form so every field can be changed. */
    fun startEditing(income: Income) {
        val source = sources.value.firstOrNull { it.id == income.sourceId } ?: IncomeSourceItem(
            id = income.sourceId,
            name = income.sourceName,
            icon = income.sourceIcon,
            defaultCurrency = income.currency,
            isCustom = income.isCustomSource
        )
        val matchedAccount = income.accountId?.let { id -> accounts.value.firstOrNull { it.id == id } }
        _formState.value = IncomeFormState(
            source = source,
            currency = income.currency,
            amountInput = formatInput(abs(income.amount)),
            isLoss = income.amount < 0 && source.allowsLoss,
            rateInput = if (income.isForeignCurrency) formatInput(income.exchangeRate) else "",
            status = if (source.canBePending) income.status else IncomeStatus.RECEIVED,
            date = income.date,
            projectName = income.projectName,
            note = income.note,
            selectedAccount = matchedAccount,
            hasPickedAccount = true,
            editingIncomeId = income.id,
            editingCreatedAt = income.createdAt,
            editingOriginalIncome = income
        )
    }

    fun onAccountSelected(account: Account?) {
        _formState.update { it.copy(selectedAccount = account, hasPickedAccount = true) }
    }

    /** Picking a source keeps the user's selected/default currency and clears unsupported options. */
    fun onSourceSelected(source: IncomeSourceItem) {
        _formState.update { state ->
            state.copy(
                source = source,
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
        val defaultCurr = defaultInputCurrency()
        _formState.value = IncomeFormState(
            currency = defaultCurr,
            rateInput = if (defaultCurr != CurrencyType.LKR) defaultRateInput() else "",
            selectedAccount = AccountBalanceCalculator.resolveDefaultAccount(
                accounts.value,
                primaryAccountId.value
            ),
            hasPickedAccount = false
        )
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

        val resolvedAccount = state.selectedAccount
            ?: if (!state.hasPickedAccount && !state.isEditing) {
                AccountBalanceCalculator.resolveDefaultAccount(accounts.value, primaryAccountId.value)
            } else null
        val signedAmount = if (state.isLoss && state.source.allowsLoss) -amount else amount
        val income = Income(
            id = state.editingIncomeId ?: UUID.randomUUID().toString(),
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
            accountId = resolvedAccount?.id ?: if (state.isEditing && state.hasPickedAccount && state.selectedAccount == null) null else state.editingOriginalIncome?.accountId,
            accountName = resolvedAccount?.name ?: if (state.isEditing && state.hasPickedAccount && state.selectedAccount == null) null else state.editingOriginalIncome?.accountName,
            createdAt = state.editingCreatedAt ?: System.currentTimeMillis()
        )

        viewModelScope.launch {
            _formState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val result = if (state.isEditing) {
                incomeRepository.updateIncome(income)
            } else {
                incomeRepository.addIncome(income)
            }
            result
                .onSuccess {
                    val liveLkrPerUsd = _liveRate.value ?: CurrencyFormatter.FALLBACK_LKR_PER_USD
                    val oldIncome = state.editingOriginalIncome
                    if (state.isEditing && oldIncome != null && oldIncome.status == IncomeStatus.RECEIVED && !oldIncome.accountId.isNullOrBlank()) {
                        val oldAcc = accounts.value.firstOrNull { it.id == oldIncome.accountId }
                            ?: accountRepository.getAccount(oldIncome.accountId)
                        if (oldAcc != null) {
                            val oldDelta = AccountBalanceCalculator.incomeDelta(oldIncome, oldAcc.currency, liveLkrPerUsd)
                            accountRepository.adjustBalance(oldAcc.id, -oldDelta)
                        }
                    }
                    if (income.status == IncomeStatus.RECEIVED && !income.accountId.isNullOrBlank()) {
                        val newAcc = resolvedAccount
                            ?: accounts.value.firstOrNull { it.id == income.accountId }
                            ?: accountRepository.getAccount(income.accountId)
                        if (newAcc != null) {
                            val newDelta = AccountBalanceCalculator.incomeDelta(income, newAcc.currency, liveLkrPerUsd)
                            accountRepository.adjustBalance(newAcc.id, newDelta)
                        }
                    }
                    if (state.isEditing) _userMessage.value = "${income.title} updated"
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

    /** "1500" for whole numbers, otherwise up to two decimals without trailing zeros. */
    private fun formatInput(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString()
        else String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')

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
                incomeRepository.observePendingIncomes(),
                incomeRepository.observeRecurringIncomes()
            ) { monthIncomes, pending, recurring ->
                buildUiState(yearMonth, monthKey, monthIncomes, pending, recurring)
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
        pending: List<Income>,
        recurring: List<RecurringIncome>
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
            recurringIncomes = recurring,
            errorMessage = null
        )
    }

    fun addRecurringIncome(
        name: String,
        amount: Double,
        sourceId: String,
        sourceName: String,
        sourceIcon: String,
        dayOfMonth: Int,
        currency: CurrencyType = CurrencyType.LKR,
        accountId: String? = null,
        accountName: String? = null,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            val recurring = RecurringIncome(
                name = name.trim(),
                amount = amount,
                sourceId = sourceId,
                sourceName = sourceName,
                sourceIcon = sourceIcon,
                dayOfMonth = dayOfMonth.coerceIn(1, 31),
                currency = currency,
                accountId = accountId,
                accountName = accountName
            )
            incomeRepository.addRecurringIncome(recurring)
                .onSuccess {
                    incomeRepository.processAutoCommitRecurringIncomes()
                    _userMessage.value = "Recurring income added"
                    onSuccess?.invoke()
                }
                .onFailure { error ->
                    _userMessage.value = error.message ?: "Failed to add recurring income"
                }
        }
    }

    fun updateRecurringIncome(
        recurring: RecurringIncome,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            incomeRepository.updateRecurringIncome(recurring)
                .onSuccess {
                    incomeRepository.processAutoCommitRecurringIncomes()
                    _userMessage.value = "Recurring income updated"
                    onSuccess?.invoke()
                }
                .onFailure { error ->
                    _userMessage.value = error.message ?: "Failed to update recurring income"
                }
        }
    }

    fun deleteRecurringIncome(
        recurringId: String,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            incomeRepository.deleteRecurringIncome(recurringId)
                .onSuccess {
                    _userMessage.value = "Recurring income deleted"
                    onSuccess?.invoke()
                }
                .onFailure { error ->
                    _userMessage.value = error.message ?: "Failed to delete recurring income"
                }
        }
    }

    /**
     * A pending invoice has been paid: it becomes received income dated today, so it counts
     * toward this month's actual income.
     */
    fun markAsReceived(income: Income) {
        if (!income.isPending) return
        val today = LocalDate.now().toString()
        val updated = income.copy(status = IncomeStatus.RECEIVED, date = today, month = today.take(7))
        viewModelScope.launch {
            incomeRepository.updateIncome(updated)
                .onSuccess {
                    if (!updated.accountId.isNullOrBlank()) {
                        val acc = accounts.value.firstOrNull { it.id == updated.accountId }
                            ?: accountRepository.getAccount(updated.accountId)
                        if (acc != null) {
                            val rate = _liveRate.value ?: CurrencyFormatter.FALLBACK_LKR_PER_USD
                            val delta = AccountBalanceCalculator.incomeDelta(updated, acc.currency, rate)
                            accountRepository.adjustBalance(acc.id, delta)
                        }
                    }
                    _userMessage.value = "${income.title} marked as received"
                }
                .onFailure { _userMessage.value = "Couldn't update ${income.title}. Try again." }
        }
    }

    fun deleteIncome(income: Income) {
        viewModelScope.launch {
            incomeRepository.deleteIncome(income.id)
                .onSuccess {
                    if (income.status == IncomeStatus.RECEIVED && !income.accountId.isNullOrBlank()) {
                        val acc = accounts.value.firstOrNull { it.id == income.accountId }
                            ?: accountRepository.getAccount(income.accountId)
                        if (acc != null) {
                            val rate = _liveRate.value ?: CurrencyFormatter.FALLBACK_LKR_PER_USD
                            val delta = AccountBalanceCalculator.incomeDelta(income, acc.currency, rate)
                            accountRepository.adjustBalance(acc.id, -delta)
                        }
                    }
                }
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
