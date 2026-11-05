package com.clarity.finance.data.repository

import com.clarity.finance.model.Income
import com.clarity.finance.model.IncomeSourceItem
import com.clarity.finance.model.IncomeStatus
import com.clarity.finance.model.getDefaultIncomeSources
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

    private val _customSources = MutableStateFlow<List<IncomeSourceItem>>(emptyList())
    val customSourcesState = _customSources.asStateFlow()

    override fun observeIncomeSources(): Flow<List<IncomeSourceItem>> =
        _customSources.map { getDefaultIncomeSources() + it }

    override suspend fun addCustomIncomeSource(source: IncomeSourceItem): Result<Unit> {
        _customSources.value = _customSources.value + source
        return Result.success(Unit)
    }

    private val _recurringIncomes = MutableStateFlow<List<com.clarity.finance.model.RecurringIncome>>(emptyList())
    val recurringIncomesState = _recurringIncomes.asStateFlow()

    override fun observeRecurringIncomes(): Flow<List<com.clarity.finance.model.RecurringIncome>> =
        _recurringIncomes.asStateFlow()

    override suspend fun addRecurringIncome(recurring: com.clarity.finance.model.RecurringIncome): Result<Unit> {
        val id = recurring.id.ifEmpty { java.util.UUID.randomUUID().toString() }
        _recurringIncomes.value = _recurringIncomes.value + recurring.copy(id = id)
        processAutoCommitRecurringIncomes()
        return Result.success(Unit)
    }

    override suspend fun updateRecurringIncome(recurring: com.clarity.finance.model.RecurringIncome): Result<Unit> {
        _recurringIncomes.value = _recurringIncomes.value.map { if (it.id == recurring.id) recurring else it }
        processAutoCommitRecurringIncomes()
        return Result.success(Unit)
    }

    override suspend fun deleteRecurringIncome(recurringId: String): Result<Unit> {
        _recurringIncomes.value = _recurringIncomes.value.filterNot { it.id == recurringId }
        return Result.success(Unit)
    }

    override suspend fun processAutoCommitRecurringIncomes(): Result<Unit> {
        val today = java.time.LocalDate.now()
        val currentMonth = java.time.YearMonth.now().toString()
        val daysInMonth = today.lengthOfMonth()
        _recurringIncomes.value = _recurringIncomes.value.map { rec ->
            if (rec.lastCommittedMonth != currentMonth) {
                val targetDay = rec.dayOfMonth.coerceIn(1, daysInMonth)
                if (today.dayOfMonth >= targetDay) {
                    val incomeDate = java.time.LocalDate.of(today.year, today.month, targetDay).toString()
                    addIncome(
                        Income(
                            id = "rec_${rec.id}_$currentMonth",
                            sourceId = rec.sourceId,
                            sourceName = rec.sourceName,
                            sourceIcon = rec.sourceIcon,
                            amount = rec.amount,
                            currency = rec.currency,
                            exchangeRate = 1.0,
                            amountLKR = rec.amount,
                            status = IncomeStatus.RECEIVED,
                            date = incomeDate,
                            month = currentMonth,
                            projectName = rec.name.ifBlank { rec.sourceName },
                            note = "Auto-committed monthly recurring income"
                        )
                    )
                    rec.copy(lastCommittedMonth = currentMonth)
                } else rec
            } else rec
        }
        return Result.success(Unit)
    }
}

/** Fixed rate so tests never touch the network. */
class FakeExchangeRateProvider(private val rate: Double = 300.0) : ExchangeRateProvider {
    override suspend fun getLkrPerUsdRate(): Double = rate
}
