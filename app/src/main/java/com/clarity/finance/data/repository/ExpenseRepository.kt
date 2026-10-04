package com.clarity.finance.data.repository

import com.clarity.finance.model.Expense
import com.clarity.finance.model.RecurringExpense
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    fun observeExpensesForMonth(month: String): Flow<List<Expense>>
    suspend fun addExpense(expense: Expense): Result<Unit>
    suspend fun updateExpense(expense: Expense): Result<Unit>
    suspend fun deleteExpense(expenseId: String): Result<Unit>
    suspend fun observeRecurringExpenses(): Flow<List<RecurringExpense>>
    suspend fun checkAndApplyRecurringExpenses(currentMonth: String): Result<Unit>
}