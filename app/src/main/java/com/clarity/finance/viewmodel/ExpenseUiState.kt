package com.clarity.finance.viewmodel

import com.clarity.finance.model.CategoryItem
import com.clarity.finance.model.Expense
import com.clarity.finance.model.PaymentMethod
import com.clarity.finance.model.SpendingType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * State of the Add Expense Form (Wireframe 1: Standard & Wireframe 2: Quick Log).
 */
data class ExpenseFormState(
    val amountInput: String = "",
    val selectedCategory: CategoryItem? = null,
    val spendingType: SpendingType = SpendingType.DISCRETIONARY,
    val paymentMethod: PaymentMethod = PaymentMethod.CARD,
    val date: String = LocalDate.now().toString(), // YYYY-MM-DD
    val formattedDateDisplay: String = "Today, ${LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))}",
    val note: String = "",
    val isQuickMode: Boolean = false,
    val amountError: String? = null,
    val dateError: String? = null,
    val isSubmitting: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Monthly aggregates for the top card in Wireframe 3 (Expenses History).
 */
data class ExpenseMonthlyMetrics(
    val totalExpense: Double = 0.0,
    val committedTotal: Double = 0.0,
    val discretionaryTotal: Double = 0.0,
    val committedPercentage: Int = 0,
    val discretionaryPercentage: Int = 0
)

/**
 * State for the Expenses History Screen (Wireframe 3).
 */
data class ExpenseHistoryUiState(
    val isLoading: Boolean = false,
    val expenses: List<Expense> = emptyList(),
    val filteredExpenses: List<Expense> = emptyList(),
    val searchQuery: String = "",
    val selectedTypeFilter: SpendingType? = null, // null represents 'All'
    val currentMonth: String = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM")),
    val metrics: ExpenseMonthlyMetrics = ExpenseMonthlyMetrics(),
    val errorMessage: String? = null
)
