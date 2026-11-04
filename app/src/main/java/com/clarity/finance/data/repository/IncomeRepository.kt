package com.clarity.finance.data.repository

import com.clarity.finance.model.Income
import kotlinx.coroutines.flow.Flow

interface IncomeRepository {
    /** Every income entry (received and pending) dated in [month] ("YYYY-MM"), newest first. */
    fun observeIncomesForMonth(month: String): Flow<List<Income>>

    /** All outstanding freelance payments across every month, oldest invoice first. */
    fun observePendingIncomes(): Flow<List<Income>>

    suspend fun addIncome(income: Income): Result<Unit>
    suspend fun updateIncome(income: Income): Result<Unit>
    suspend fun deleteIncome(incomeId: String): Result<Unit>
}
