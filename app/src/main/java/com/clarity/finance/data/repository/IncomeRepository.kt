package com.clarity.finance.data.repository

import com.clarity.finance.model.Income
import com.clarity.finance.model.IncomeSourceItem
import com.clarity.finance.model.RecurringIncome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface IncomeRepository {
    /** Every income entry (received and pending) dated in [month] ("YYYY-MM"), newest first. */
    fun observeIncomesForMonth(month: String): Flow<List<Income>>

    /** All outstanding freelance payments across every month, oldest invoice first. */
    fun observePendingIncomes(): Flow<List<Income>>

    suspend fun addIncome(income: Income): Result<Unit>
    suspend fun updateIncome(income: Income): Result<Unit>
    suspend fun deleteIncome(incomeId: String): Result<Unit>

    /** Built-in sources followed by the user's own, oldest first. */
    fun observeIncomeSources(): Flow<List<IncomeSourceItem>>
    suspend fun addCustomIncomeSource(source: IncomeSourceItem): Result<Unit>

    /** Recurring monthly incomes. */
    fun observeRecurringIncomes(): Flow<List<RecurringIncome>> = flowOf(emptyList())
    suspend fun addRecurringIncome(recurring: RecurringIncome): Result<Unit> = Result.success(Unit)
    suspend fun updateRecurringIncome(recurring: RecurringIncome): Result<Unit> = Result.success(Unit)
    suspend fun deleteRecurringIncome(recurringId: String): Result<Unit> = Result.success(Unit)
    suspend fun processAutoCommitRecurringIncomes(): Result<Unit> = Result.success(Unit)
}
