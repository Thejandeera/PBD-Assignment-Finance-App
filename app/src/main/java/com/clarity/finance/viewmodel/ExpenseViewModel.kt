package com.clarity.finance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clarity.finance.data.repository.ExpenseRepository
import com.clarity.finance.model.CategoryItem
import com.clarity.finance.model.Expense
import com.clarity.finance.model.PaymentMethod
import com.clarity.finance.model.SpendingType
import com.clarity.finance.model.getDefaultCategories
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
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import kotlin.math.roundToInt

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExpenseViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    // =========================================================================
    // Form State & Actions (Wireframes 1 & 2)
    // =========================================================================

    private val _formState = MutableStateFlow(
        getDefaultCategories().firstOrNull().let { defaultCategory ->
            ExpenseFormState(
                selectedCategory = defaultCategory,
                spendingType = defaultCategory?.defaultType ?: SpendingType.DISCRETIONARY
            )
        }
    )
    val formState: StateFlow<ExpenseFormState> = _formState.asStateFlow()

    fun onAmountChanged(input: String) {
        // Strip out non-numeric characters except decimals
        val sanitized = input.filter { it.isDigit() || it == '.' }
        _formState.update { it.copy(amountInput = sanitized, amountError = null) }
    }

    /**
     * Quick amount increment chips ([+500], [+1,000], [+5,000]) shown in Wireframe 1.
     */
    fun onQuickAmountIncrement(increment: Double) {
        val currentAmount = _formState.value.amountInput.toDoubleOrNull() ?: 0.0
        val newAmount = currentAmount + increment
        val formatted = if (newAmount % 1.0 == 0.0) {
            newAmount.toLong().toString()
        } else {
            String.format("%.2f", newAmount)
        }
        _formState.update { it.copy(amountInput = formatted, amountError = null) }
    }

    /**
     * Helper for quick amount increments ([+500], [+1,000], [+5,000]).
     */
    fun applyQuickIncrement(increment: Int) {
        onQuickAmountIncrement(increment.toDouble())
    }

    fun onCategorySelected(category: CategoryItem) {
        _formState.update {
            it.copy(
                selectedCategory = category,
                // Smart auto-mapping: Committed for Rent/Bills, Discretionary for Food/Transport
                spendingType = category.defaultType
            )
        }
    }

    fun onSpendingTypeSelected(type: SpendingType) {
        _formState.update { it.copy(spendingType = type) }
    }

    fun onPaymentMethodSelected(method: PaymentMethod) {
        _formState.update { it.copy(paymentMethod = method) }
    }

    fun onDateChanged(dateString: String) {
        try {
            val parsed = LocalDate.parse(dateString)
            if (parsed.isAfter(LocalDate.now())) {
                _formState.update { it.copy(date = dateString, dateError = "Date cannot be in the future") }
                return
            }
            val display = if (parsed == LocalDate.now()) {
                "Today, ${parsed.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))}"
            } else {
                parsed.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
            }
            _formState.update {
                it.copy(
                    date = dateString,
                    formattedDateDisplay = display,
                    dateError = null
                )
            }
        } catch (e: Exception) {
            _formState.update { it.copy(date = dateString, dateError = "Invalid date format (YYYY-MM-DD)") }
        }
    }

    fun onNoteChanged(note: String) {
        _formState.update { it.copy(note = note) }
    }

    fun setQuickMode(isQuick: Boolean) {
        _formState.update { it.copy(isQuickMode = isQuick) }
    }

    fun resetFormState() {
        val today = LocalDate.now()
        val defaultCategory = categories.value.firstOrNull() ?: getDefaultCategories().firstOrNull()
        _formState.value = ExpenseFormState(
            selectedCategory = defaultCategory,
            spendingType = defaultCategory?.defaultType ?: SpendingType.DISCRETIONARY,
            date = today.toString(),
            formattedDateDisplay = "Today, ${today.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))}"
        )
    }

    fun submitExpense(onSuccess: (() -> Unit)? = null) {
        val state = _formState.value
        val amount = state.amountInput.toDoubleOrNull()

        // Validation 1: Amount must be greater than 0
        if (amount == null || amount <= 0.0) {
            _formState.update { it.copy(amountError = "Enter an amount greater than 0") }
            return
        }

        // Validation 2: Date cannot be in future
        val dateParsed = runCatching { LocalDate.parse(state.date) }.getOrNull()
        if (state.dateError != null || dateParsed == null || dateParsed.isAfter(LocalDate.now())) {
            _formState.update { it.copy(dateError = "Date cannot be in the future") }
            return
        }

        val category = state.selectedCategory ?: CategoryItem(
            id = "OTHER",
            name = "Other",
            icon = "📦",
            defaultType = SpendingType.DISCRETIONARY
        )

        val resolvedMonth = state.date.take(7) // "YYYY-MM"
        val optionalNote = state.note.trim().ifEmpty { null }

        val expense = Expense(
            id = UUID.randomUUID().toString(),
            amount = amount,
            categoryId = category.id,
            categoryName = category.name,
            categoryIcon = category.icon,
            type = state.spendingType,
            paymentMethod = state.paymentMethod,
            date = state.date,
            month = resolvedMonth,
            note = optionalNote,
            createdAt = System.currentTimeMillis()
        )

        viewModelScope.launch {
            _formState.update { it.copy(isSubmitting = true, errorMessage = null) }
            val result = expenseRepository.addExpense(expense)
            if (result.isSuccess) {
                _formState.update { it.copy(isSubmitting = false, isSuccess = true) }
                onSuccess?.invoke()
                resetFormState()
            } else {
                _formState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Failed to save expense"
                    )
                }
            }
        }
    }

    // =========================================================================
    // Expenses History & Metrics (Wireframe 3)
    // =========================================================================

    private val _currentMonth = MutableStateFlow(
        LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
    )
    val currentMonth: StateFlow<String> = _currentMonth.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTypeFilter = MutableStateFlow<SpendingType?>(null) // null = 'All'
    val selectedTypeFilter: StateFlow<SpendingType?> = _selectedTypeFilter.asStateFlow()

    fun onMonthChanged(newMonth: String) {
        _currentMonth.value = newMonth
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onFilterSelected(type: SpendingType?) {
        _selectedTypeFilter.value = type
    }

    val historyState: StateFlow<ExpenseHistoryUiState> = _currentMonth
        .flatMapLatest { month ->
            expenseRepository.observeExpensesForMonth(month)
        }
        .combine(_searchQuery) { expenses, query ->
            Pair(expenses, query)
        }
        .combine(_selectedTypeFilter) { (expenses, query), filter ->
            val total = expenses.sumOf { it.amount }
            val committed = expenses.filter { it.type == SpendingType.COMMITTED }.sumOf { it.amount }
            val discretionary = expenses.filter { it.type == SpendingType.DISCRETIONARY }.sumOf { it.amount }

            val committedPct = if (total > 0) ((committed / total) * 100).roundToInt() else 0
            val discretionaryPct = if (total > 0) ((discretionary / total) * 100).roundToInt() else 0

            val metrics = ExpenseMonthlyMetrics(
                totalExpense = total,
                committedTotal = committed,
                discretionaryTotal = discretionary,
                committedPercentage = committedPct,
                discretionaryPercentage = discretionaryPct
            )

            val filtered = expenses.filter { expense ->
                val matchesFilter = (filter == null || expense.type == filter)
                val matchesQuery = query.isBlank() ||
                    expense.categoryName.contains(query, ignoreCase = true) ||
                    (expense.note?.contains(query, ignoreCase = true) == true)
                matchesFilter && matchesQuery
            }

            ExpenseHistoryUiState(
                isLoading = false,
                expenses = expenses,
                filteredExpenses = filtered,
                searchQuery = query,
                selectedTypeFilter = filter,
                currentMonth = _currentMonth.value,
                metrics = metrics
            )
        }
        .catch { error ->
            emit(ExpenseHistoryUiState(isLoading = false, errorMessage = error.message))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ExpenseHistoryUiState(isLoading = true)
        )

    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            expenseRepository.deleteExpense(expenseId)
        }
    }

    // =========================================================================
    // Category Stream & Custom Categories
    // =========================================================================

    val categories: StateFlow<List<CategoryItem>> = expenseRepository.observeCategories()
        .catch { emit(getDefaultCategories()) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = getDefaultCategories()
        )

    fun addCustomCategory(
        name: String,
        icon: String = "🏷️",
        defaultType: SpendingType = SpendingType.DISCRETIONARY,
        onSuccess: (() -> Unit)? = null
    ) {
        if (name.isBlank()) return
        val category = CategoryItem(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            icon = icon,
            defaultType = defaultType,
            isCustom = true
        )
        viewModelScope.launch {
            val result = expenseRepository.addCustomCategory(category)
            if (result.isSuccess) {
                onSuccess?.invoke()
            }
        }
    }
}
