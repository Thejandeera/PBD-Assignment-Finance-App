package com.clarity.finance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clarity.finance.data.repository.GoalRepository
import com.clarity.finance.model.Goal
import com.clarity.finance.model.GoalTransaction
import com.clarity.finance.model.GoalTransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.math.ceil

data class GoalUiState(
    val goal: Goal? = null,
    val transactions: List<GoalTransaction> = emptyList(),

    val currentSavings: Double = 0.0,
    val remainingAmount: Double = 0.0,
    val progress: Float = 0f,

    val monthsRemaining: Long = 0L,
    val requiredMonthlySaving: Double = 0.0,

    val monthlyDepositAverage: Double = 0.0,
    val projectedFinishDate: LocalDate? = null,

    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class GoalViewModel @Inject constructor(
    private val repository: GoalRepository
) : ViewModel() {

    private val errorState = MutableStateFlow<String?>(null)

    val uiState: StateFlow<GoalUiState> =
        combine(
            repository.observeGoal(),
            repository.observeTransactions(),
            errorState
        ) { goal, transactions, error ->

            if (goal == null) {
                return@combine GoalUiState(
                    isLoading = false,
                    error = error
                )
            }

            val deposits = transactions
                .filter { it.type == GoalTransactionType.DEPOSIT }
                .sumOf { it.amount }

            val withdrawals = transactions
                .filter { it.type == GoalTransactionType.WITHDRAWAL }
                .sumOf { it.amount }

            val currentSavings =
                (goal.startingAmount + deposits - withdrawals)
                    .coerceAtLeast(0.0)

            val remaining =
                (goal.targetAmount - currentSavings)
                    .coerceAtLeast(0.0)

            val progress =
                if (goal.targetAmount > 0) {
                    (currentSavings / goal.targetAmount)
                        .toFloat()
                        .coerceIn(0f, 1f)
                } else {
                    0f
                }

            val deadlineDate = goal.deadline
                .takeIf { it.isNotBlank() }
                ?.let {
                    runCatching {
                        LocalDate.parse(it)
                    }.getOrNull()
                }

            val monthsRemaining =
                deadlineDate?.let {
                    monthsBetween(
                        LocalDate.now(),
                        it
                    )
                } ?: 0L

            val requiredMonthlySaving =
                when {
                    remaining <= 0.0 -> 0.0

                    monthsRemaining > 0 ->
                        remaining / monthsRemaining.toDouble()

                    else -> remaining
                }

            val averageMonthlyDeposit =
                calculateAverageMonthlyDeposit(transactions)

            val projectedFinishDate =
                calculateProjectedFinishDate(
                    remainingAmount = remaining,
                    monthlySavingRate = averageMonthlyDeposit
                )

            GoalUiState(
                goal = goal,
                transactions = transactions,

                currentSavings = currentSavings,
                remainingAmount = remaining,
                progress = progress,

                monthsRemaining = monthsRemaining,
                requiredMonthlySaving = requiredMonthlySaving,

                monthlyDepositAverage = averageMonthlyDeposit,
                projectedFinishDate = projectedFinishDate,

                isLoading = false,
                error = error
            )
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = GoalUiState()
            )

    fun deposit(
        amount: Double,
        note: String = ""
    ) {
        viewModelScope.launch {

            errorState.value = null

            repository.deposit(
                amount = amount,
                note = note
            ).onFailure {
                errorState.value =
                    it.message ?: "Could not add savings"
            }
        }
    }

    fun withdraw(
        amount: Double,
        note: String = ""
    ) {
        val currentSavings = uiState.value.currentSavings

        if (amount > currentSavings) {
            errorState.value =
                "Withdrawal cannot exceed current savings"
            return
        }

        viewModelScope.launch {

            errorState.value = null

            repository.withdraw(
                amount = amount,
                note = note
            ).onFailure {
                errorState.value =
                    it.message ?: "Could not withdraw savings"
            }
        }
    }

    fun updateGoal(
        name: String,
        targetAmount: Double,
        deadline: String
    ) {
        viewModelScope.launch {

            errorState.value = null

            repository.updateGoal(
                name = name,
                targetAmount = targetAmount,
                deadline = deadline
            ).onFailure {
                errorState.value =
                    it.message ?: "Could not update goal"
            }
        }
    }

    fun clearError() {
        errorState.value = null
    }

    private fun monthsBetween(
        start: LocalDate,
        end: LocalDate
    ): Long {

        if (!end.isAfter(start)) {
            return 0L
        }

        val startMonth = YearMonth.from(start)
        val endMonth = YearMonth.from(end)

        return ChronoUnit.MONTHS
            .between(startMonth, endMonth)
            .coerceAtLeast(1L)
    }

    private fun calculateAverageMonthlyDeposit(
        transactions: List<GoalTransaction>
    ): Double {

        val deposits = transactions.filter {
            it.type == GoalTransactionType.DEPOSIT
        }

        if (deposits.isEmpty()) {
            return 0.0
        }

        val total = deposits.sumOf { it.amount }

        val months = deposits
            .map {
                val date = java.time.Instant
                    .ofEpochMilli(it.createdAt)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate()

                YearMonth.from(date)
            }
            .distinct()
            .size
            .coerceAtLeast(1)

        return total / months.toDouble()
    }

    private fun calculateProjectedFinishDate(
        remainingAmount: Double,
        monthlySavingRate: Double
    ): LocalDate? {

        if (
            remainingAmount <= 0.0 ||
            monthlySavingRate <= 0.0
        ) {
            return null
        }

        val monthsNeeded =
            ceil(
                remainingAmount / monthlySavingRate
            ).toLong()

        return LocalDate
            .now()
            .plusMonths(monthsNeeded)
    }
}