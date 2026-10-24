package com.clarity.finance.data.repository

import com.clarity.finance.model.CategoryItem
import com.clarity.finance.model.Expense
import com.clarity.finance.model.RecurringExpense
import com.clarity.finance.model.getDefaultCategories
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory test double implementing [ExpenseRepository] for fast, deterministic unit testing.
 */
class FakeExpenseRepository : ExpenseRepository {

    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    val expensesState = _expenses.asStateFlow()

    private val _recurringExpenses = MutableStateFlow<List<RecurringExpense>>(emptyList())
    val recurringState = _recurringExpenses.asStateFlow()

    private val _categories = MutableStateFlow<List<CategoryItem>>(getDefaultCategories())
    val categoriesState = _categories.asStateFlow()

    // Test seed helper
    fun setExpenses(list: List<Expense>) {
        _expenses.value = list
    }

    override fun observeExpensesForMonth(month: String): Flow<List<Expense>> {
        return _expenses.map { all ->
            all.filter { it.month == month || it.date.startsWith(month) }
        }
    }

    override suspend fun addExpense(expense: Expense): Result<Unit> {
        val current = _expenses.value.toMutableList()
        val toAdd = if (expense.id.isBlank()) expense.copy(id = "exp_${System.currentTimeMillis()}") else expense
        current.add(toAdd)
        _expenses.value = current
        return Result.success(Unit)
    }

    override suspend fun updateExpense(expense: Expense): Result<Unit> {
        val current = _expenses.value.toMutableList()
        val index = current.indexOfFirst { it.id == expense.id }
        if (index != -1) {
            current[index] = expense
            _expenses.value = current
        }
        return Result.success(Unit)
    }

    override suspend fun deleteExpense(expenseId: String): Result<Unit> {
        _expenses.value = _expenses.value.filterNot { it.id == expenseId }
        return Result.success(Unit)
    }

    override fun observeRecurringExpenses(): Flow<List<RecurringExpense>> {
        return _recurringExpenses.asStateFlow()
    }

    override suspend fun saveRecurringExpense(recurring: RecurringExpense): Result<Unit> {
        val current = _recurringExpenses.value.toMutableList()
        val index = current.indexOfFirst { it.id == recurring.id }
        if (index != -1) {
            current[index] = recurring
        } else {
            current.add(recurring)
        }
        _recurringExpenses.value = current
        return Result.success(Unit)
    }

    override suspend fun deleteRecurringExpense(recurringId: String): Result<Unit> {
        _recurringExpenses.value = _recurringExpenses.value.filterNot { it.id == recurringId }
        return Result.success(Unit)
    }

    override suspend fun checkAndApplyRecurringExpenses(currentMonth: String): Result<Unit> {
        return Result.success(Unit)
    }

    override fun observeCategories(): Flow<List<CategoryItem>> {
        return _categories.asStateFlow()
    }

    override suspend fun addCustomCategory(category: CategoryItem): Result<Unit> {
        val current = _categories.value.toMutableList()
        current.add(category)
        _categories.value = current
        return Result.success(Unit)
    }

    override suspend fun deleteCustomCategory(categoryId: String): Result<Unit> {
        _categories.value = _categories.value.filterNot { it.id == categoryId }
        return Result.success(Unit)
    }
}
