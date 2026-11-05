package com.clarity.finance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clarity.finance.data.repository.ExpenseRepository
import com.clarity.finance.data.repository.GoalRepository
import com.clarity.finance.data.repository.IncomeRepository
import com.clarity.finance.model.Expense
import com.clarity.finance.model.Goal
import com.clarity.finance.model.GoalTransaction
import com.clarity.finance.model.GoalTransactionType
import com.clarity.finance.model.Income
import com.clarity.finance.model.IncomeStatus
import com.clarity.finance.model.SpendingType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class DashboardViewModel @Inject constructor(
    incomeRepository: IncomeRepository,
    expenseRepository: ExpenseRepository,
    goalRepository: GoalRepository
) : ViewModel() {

    private val currentMonth =
        YearMonth.now().toString()

    val uiState: StateFlow<DashboardUiState> =
        combine(
            incomeRepository.observeIncomesForMonth(
                currentMonth
            ),
            incomeRepository.observePendingIncomes(),
            expenseRepository.observeExpensesForMonth(
                currentMonth
            ),
            goalRepository.observeGoal(),
            goalRepository.observeTransactions()
        ) { monthIncomes,
            pendingIncomes,
            expenses,
            goal,
            goalTransactions ->

            buildDashboardState(
                monthIncomes = monthIncomes,
                pendingIncomes = pendingIncomes,
                expenses = expenses,
                goal = goal,
                goalTransactions = goalTransactions
            )
        }
            .catch { error ->
                emit(
                    DashboardUiState(
                        isLoading = false,
                        errorMessage =
                            error.message
                                ?: "Dashboard couldn't be loaded."
                    )
                )
            }
            .stateIn(
                scope = viewModelScope,
                started =
                    SharingStarted
                        .WhileSubscribed(5000),
                initialValue =
                    DashboardUiState(
                        isLoading = true
                    )
            )

    private fun buildDashboardState(
        monthIncomes: List<Income>,
        pendingIncomes: List<Income>,
        expenses: List<Expense>,
        goal: Goal?,
        goalTransactions: List<GoalTransaction>
    ): DashboardUiState {

        // ---------------------------------------------------------
        // Income
        // ---------------------------------------------------------

        val receivedIncomes =
            monthIncomes.filter {
                it.status ==
                        IncomeStatus.RECEIVED
            }

        val totalIncome =
            receivedIncomes.sumOf {
                it.amountLKR
            }

        val pending =
            pendingIncomes.sortedBy {
                it.date
            }

        val pendingTotal =
            pending.sumOf {
                it.amountLKR
            }

        // ---------------------------------------------------------
        // Expenses
        // ---------------------------------------------------------

        val totalExpenses =
            expenses.sumOf {
                it.amount
            }

        val committedExpenses =
            expenses
                .filter {
                    it.type ==
                            SpendingType.COMMITTED
                }
                .sumOf {
                    it.amount
                }

        val discretionaryExpenses =
            expenses
                .filter {
                    it.type ==
                            SpendingType.DISCRETIONARY
                }
                .sumOf {
                    it.amount
                }

        val committedPercentage =
            if (totalExpenses > 0.0) {
                (
                        committedExpenses /
                                totalExpenses *
                                100
                        )
                    .roundToInt()
            } else {
                0
            }

        val discretionaryPercentage =
            if (totalExpenses > 0.0) {
                (
                        discretionaryExpenses /
                                totalExpenses *
                                100
                        )
                    .roundToInt()
            } else {
                0
            }

        val expenseMetrics =
            ExpenseMonthlyMetrics(
                totalExpense =
                    totalExpenses,
                committedTotal =
                    committedExpenses,
                discretionaryTotal =
                    discretionaryExpenses,
                committedPercentage =
                    committedPercentage,
                discretionaryPercentage =
                    discretionaryPercentage
            )

        // ---------------------------------------------------------
        // Available Balance
        // ---------------------------------------------------------

        val availableBalance =
            (
                    totalIncome -
                            totalExpenses
                    )
                .coerceAtLeast(0.0)

        // ---------------------------------------------------------
        // Goal
        // ---------------------------------------------------------

        val depositedAmount =
            goalTransactions
                .filter {
                    it.type ==
                            GoalTransactionType.DEPOSIT
                }
                .sumOf {
                    it.amount
                }

        val withdrawnAmount =
            goalTransactions
                .filter {
                    it.type ==
                            GoalTransactionType.WITHDRAWAL
                }
                .sumOf {
                    it.amount
                }

        val startingAmount =
            goal?.startingAmount
                ?: 0.0

        val currentSavings =
            (
                    startingAmount +
                            depositedAmount -
                            withdrawnAmount
                    )
                .coerceAtLeast(0.0)

        val targetAmount =
            goal?.targetAmount
                ?: 0.0

        val remainingAmount =
            (
                    targetAmount -
                            currentSavings
                    )
                .coerceAtLeast(0.0)

        val goalProgress =
            if (targetAmount > 0.0) {
                (
                        currentSavings /
                                targetAmount
                        )
                    .toFloat()
                    .coerceIn(
                        0f,
                        1f
                    )
            } else {
                0f
            }

        val monthsRemaining =
            calculateMonthsRemaining(
                goal?.deadline
            )

        val requiredMonthlySaving =
            when {
                remainingAmount <= 0.0 ->
                    0.0

                monthsRemaining > 0 ->
                    remainingAmount /
                            monthsRemaining

                else ->
                    remainingAmount
            }

        val goalStatus =
            when {
                goal == null ->
                    "No goal"

                targetAmount <= 0.0 ->
                    "Set your target"

                currentSavings >= targetAmount ->
                    "Goal completed"

                monthsRemaining <= 0 ->
                    "Deadline reached"

                currentSavings <= 0.0 ->
                    "Start saving"

                else ->
                    "In progress"
            }

        return DashboardUiState(
            isLoading = false,

            totalIncomeLkr =
                totalIncome,

            totalExpensesLkr =
                totalExpenses,

            availableBalanceLkr =
                availableBalance,

            receivedIncomes =
                receivedIncomes,

            pendingIncomes =
                pending,

            pendingTotalLkr =
                pendingTotal,

            expenses =
                expenses.sortedWith(
                    compareByDescending<Expense> {
                        it.date
                    }.thenByDescending {
                        it.createdAt
                    }
                ),

            expenseMetrics =
                expenseMetrics,

            goal =
                goal,

            currentSavingsLkr =
                currentSavings,

            remainingGoalAmountLkr =
                remainingAmount,

            goalProgress =
                goalProgress,

            monthsRemaining =
                monthsRemaining,

            requiredMonthlySavingLkr =
                requiredMonthlySaving,

            goalStatus =
                goalStatus,

            errorMessage =
                null
        )
    }

    /**
     * Returns the number of saving months available,
     * including the current month.
     *
     * Example:
     * Nov 2026 -> Oct 2027 = 12 saving months.
     */
    private fun calculateMonthsRemaining(
        deadline: String?
    ): Int {

        if (deadline.isNullOrBlank()) {
            return 0
        }

        val deadlineDate =
            runCatching {
                LocalDate.parse(
                    deadline
                )
            }.getOrNull()
                ?: return 0

        val currentYearMonth =
            YearMonth.now()

        val deadlineYearMonth =
            YearMonth.from(
                deadlineDate
            )

        if (
            deadlineYearMonth <
            currentYearMonth
        ) {
            return 0
        }

        val difference =
            ChronoUnit.MONTHS
                .between(
                    currentYearMonth,
                    deadlineYearMonth
                )
                .toInt()

        return difference + 1
    }
}

data class DashboardUiState(
    val isLoading: Boolean = false,

    // Income
    val totalIncomeLkr: Double = 0.0,
    val receivedIncomes: List<Income> =
        emptyList(),

    // Expenses
    val totalExpensesLkr: Double = 0.0,
    val availableBalanceLkr: Double = 0.0,
    val expenses: List<Expense> =
        emptyList(),

    val expenseMetrics: ExpenseMonthlyMetrics =
        ExpenseMonthlyMetrics(),

    // Pending income
    val pendingIncomes: List<Income> =
        emptyList(),

    val pendingTotalLkr: Double = 0.0,

    // Goal
    val goal: Goal? = null,
    val currentSavingsLkr: Double = 0.0,
    val remainingGoalAmountLkr: Double = 0.0,
    val goalProgress: Float = 0f,
    val monthsRemaining: Int = 0,
    val requiredMonthlySavingLkr: Double = 0.0,
    val goalStatus: String = "No goal",

    val errorMessage: String? = null
)