package com.clarity.finance.data.repository

import com.clarity.finance.model.CategoryItem
import com.clarity.finance.model.Expense
import com.clarity.finance.model.RecurringExpense
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    // Expense CRUD
    fun observeExpensesForMonth(month: String): Flow<List<Expense>>
    suspend fun addExpense(expense: Expense): Result<Unit>
    suspend fun updateExpense(expense: Expense): Result<Unit>
    suspend fun deleteExpense(expenseId: String): Result<Unit>

    // Recurring expenses
    fun observeRecurringExpenses(): Flow<List<RecurringExpense>>
    suspend fun saveRecurringExpense(recurring: RecurringExpense): Result<Unit>
    suspend fun deleteRecurringExpense(recurringId: String): Result<Unit>
    suspend fun checkAndApplyRecurringExpenses(currentMonth: String): Result<Unit>

    // Categories (Default + User-defined Custom Categories)
    fun observeCategories(): Flow<List<CategoryItem>>
    suspend fun addCustomCategory(category: CategoryItem): Result<Unit>
    suspend fun deleteCustomCategory(categoryId: String): Result<Unit>
}