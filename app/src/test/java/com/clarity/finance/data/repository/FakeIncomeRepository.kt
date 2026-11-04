package com.clarity.finance.data.repository

import com.clarity.finance.model.Income
import com.clarity.finance.model.IncomeStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/** In-memory [IncomeRepository] for fast, deterministic unit tests. */
class FakeIncomeRepository : IncomeRepository {

    private val _incomes = MutableStateFlow<List<Income>>(emptyList())
    val incomesState = _incomes.asStateFlow()

    fun setIncomes(list: List<Income>) {
        _incomes.value = list
    }

    override fun observeIncomesForMonth(month: String): Flow<List<Income>> =
        _incomes.map { all -> all.filter { it.month == month }.sortedByDescending { it.date } }

    override fun observePendingIncomes(): Flow<List<Income>> =
        _incomes.map { all -> all.filter { it.status == IncomeStatus.PENDING }.sortedBy { it.date } }

    override suspend fun addIncome(income: Income): Result<Unit> {
        _incomes.value = _incomes.value + income
        return Result.success(Unit)
    }

    override suspend fun updateIncome(income: Income): Result<Unit> {
        _incomes.value = _incomes.value.map { if (it.id == income.id) income else it }
        return Result.success(Unit)
    }

    override suspend fun deleteIncome(incomeId: String): Result<Unit> {
        _incomes.value = _incomes.value.filterNot { it.id == incomeId }
        return Result.success(Unit)
    }
}

/** Fixed rate so tests never touch the network. */
class FakeExchangeRateProvider(private val rate: Double = 300.0) : ExchangeRateProvider {
    override suspend fun getLkrPerUsdRate(): Double = rate
}
