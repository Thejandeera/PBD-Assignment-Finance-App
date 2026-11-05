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
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.math.ceil

@HiltViewModel
class GoalViewModel @Inject constructor(
    private val goalRepository: GoalRepository
) : ViewModel() {

    private val _actionError =
        MutableStateFlow<String?>(null)

    val actionError: StateFlow<String?> =
        _actionError.asStateFlow()

    val uiState: StateFlow<GoalUiState> =
        combine(
            goalRepository.observeGoal(),
            goalRepository.observeTransactions()
        ) { goal, transactions ->

            buildUiState(
                goal = goal,
                transactions = transactions
            )
        }
            .catch { error ->
                emit(
                    GoalUiState(
                        isLoading = false,
                        error = error.message
                            ?: "Your savings goal could not be loaded."
                    )
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = GoalUiState(
                    isLoading = true
                )
            )

    private fun buildUiState(
        goal: Goal?,
        transactions: List<GoalTransaction>
    ): GoalUiState {

        if (goal == null) {
            return GoalUiState(
                isLoading = false,
                transactions = transactions,
                error = "Savings goal not found."
            )
        }

        val sortedTransactions =
            transactions.sortedByDescending {
                it.createdAt
            }

        val totalDeposits =
            transactions
                .filter {
                    it.type ==
                            GoalTransactionType.DEPOSIT
                }
                .sumOf {
                    it.amount
                }

        val totalWithdrawals =
            transactions
                .filter {
                    it.type ==
                            GoalTransactionType.WITHDRAWAL
                }
                .sumOf {
                    it.amount
                }

        val currentSavings =
            (
                    goal.startingAmount +
                            totalDeposits -
                            totalWithdrawals
                    )
                .coerceAtLeast(0.0)

        val targetAmount =
            goal.targetAmount
                .coerceAtLeast(0.0)

        val remainingAmount =
            (
                    targetAmount -
                            currentSavings
                    )
                .coerceAtLeast(0.0)

        val progress =
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
                deadline = goal.deadline
            )

        val requiredMonthlySaving =
            when {

                remainingAmount <= 0.0 ->
                    0.0

                monthsRemaining > 0 ->
                    remainingAmount /
                            monthsRemaining.toDouble()

                else ->
                    remainingAmount
            }

        val monthlyDepositAverage =
            calculateMonthlyDepositAverage(
                transactions = transactions
            )

        val projectedFinishDate =
            calculateProjectedFinishDate(
                remainingAmount =
                    remainingAmount,
                monthlyDepositAverage =
                    monthlyDepositAverage
            )

        val milestones =
            buildMilestones(
                targetAmount = targetAmount,
                currentSavings = currentSavings
            )

        val nextMilestone =
            milestones.firstOrNull {
                !it.isReached
            }

        return GoalUiState(
            isLoading = false,
            goal = goal,
            transactions = sortedTransactions,
            currentSavings = currentSavings,
            remainingAmount = remainingAmount,
            progress = progress,
            monthsRemaining = monthsRemaining,
            requiredMonthlySaving =
                requiredMonthlySaving,
            monthlyDepositAverage =
                monthlyDepositAverage,
            projectedFinishDate =
                projectedFinishDate,
            milestones = milestones,
            nextMilestone = nextMilestone,
            error = null
        )
    }

    private fun buildMilestones(
        targetAmount: Double,
        currentSavings: Double
    ): List<GoalMilestone> {

        if (targetAmount <= 0.0) {
            return emptyList()
        }

        val milestoneDefinitions =
            listOf(
                Triple(
                    25,
                    "Getting Started",
                    "A strong start toward your goal."
                ),
                Triple(
                    50,
                    "Halfway There",
                    "You're halfway to your target."
                ),
                Triple(
                    75,
                    "Almost There",
                    "Only one quarter left to save."
                ),
                Triple(
                    100,
                    "Goal Reached",
                    "You reached your savings target."
                )
            )

        return milestoneDefinitions.map {
                definition ->

            val percentage =
                definition.first

            val amount =
                targetAmount *
                        percentage /
                        100.0

            GoalMilestone(
                percentage = percentage,
                title = definition.second,
                description = definition.third,
                targetAmount = amount,
                isReached =
                    currentSavings >= amount
            )
        }
    }

    private fun calculateMonthsRemaining(
        deadline: String
    ): Long {

        val deadlineDate =
            runCatching {
                LocalDate.parse(deadline)
            }.getOrNull()
                ?: return 0L

        val today =
            LocalDate.now()

        if (deadlineDate.isBefore(today)) {
            return 0L
        }

        return ChronoUnit.MONTHS.between(
            YearMonth.from(today),
            YearMonth.from(deadlineDate)
        )
    }

    private fun calculateMonthlyDepositAverage(
        transactions: List<GoalTransaction>
    ): Double {

        val deposits =
            transactions.filter {

                it.type ==
                        GoalTransactionType.DEPOSIT &&
                        it.amount > 0.0
            }

        if (deposits.isEmpty()) {
            return 0.0
        }

        val depositsByMonth =
            deposits.groupBy {
                    transaction ->

                val date =
                    Instant
                        .ofEpochMilli(
                            transaction.createdAt
                        )
                        .atZone(
                            ZoneId.systemDefault()
                        )
                        .toLocalDate()

                YearMonth.from(
                    date
                )
            }

        if (depositsByMonth.isEmpty()) {
            return 0.0
        }

        val monthlyTotals =
            depositsByMonth
                .values
                .map { group ->

                    group.sumOf {
                        it.amount
                    }
                }

        return monthlyTotals.average()
    }

    private fun calculateProjectedFinishDate(
        remainingAmount: Double,
        monthlyDepositAverage: Double
    ): LocalDate? {

        if (
            remainingAmount <= 0.0
        ) {
            return LocalDate.now()
        }

        if (
            monthlyDepositAverage <= 0.0
        ) {
            return null
        }

        val monthsNeeded =
            ceil(
                remainingAmount /
                        monthlyDepositAverage
            )
                .toLong()
                .coerceAtLeast(1L)

        return LocalDate
            .now()
            .plusMonths(
                monthsNeeded
            )
    }

    fun deposit(
        amount: Double,
        note: String = ""
    ) {

        if (amount <= 0.0) {

            _actionError.value =
                "Enter an amount greater than 0."

            return
        }

        _actionError.value = null

        viewModelScope.launch {

            goalRepository
                .deposit(
                    amount = amount,
                    note = note.trim()
                )
                .onFailure { error ->

                    _actionError.value =
                        error.message
                            ?: "Savings could not be added. Try again."
                }
        }
    }

    fun withdraw(
        amount: Double,
        note: String = ""
    ): Boolean {

        if (amount <= 0.0) {

            _actionError.value =
                "Enter an amount greater than 0."

            return false
        }

        val availableSavings =
            uiState.value.currentSavings

        if (availableSavings <= 0.0) {

            _actionError.value =
                "You don't have any savings to withdraw."

            return false
        }

        if (
            amount >
            availableSavings
        ) {

            _actionError.value =
                "You can only withdraw up to ${
                    formatLkrForError(
                        availableSavings
                    )
                }."

            return false
        }

        _actionError.value = null

        viewModelScope.launch {

            goalRepository
                .withdraw(
                    amount = amount,
                    note = note.trim()
                )
                .onFailure { error ->

                    _actionError.value =
                        error.message
                            ?: "Withdrawal failed. Try again."
                }
        }

        return true
    }

    fun updateGoal(
        name: String,
        targetAmount: Double,
        deadline: String
    ) {

        if (name.isBlank()) {

            _actionError.value =
                "Goal name cannot be empty."

            return
        }

        if (targetAmount <= 0.0) {

            _actionError.value =
                "Target amount must be greater than 0."

            return
        }

        val parsedDeadline =
            runCatching {
                LocalDate.parse(
                    deadline
                )
            }.getOrNull()

        if (parsedDeadline == null) {

            _actionError.value =
                "Enter a valid target date."

            return
        }

        _actionError.value = null

        viewModelScope.launch {

            goalRepository
                .updateGoal(
                    name = name.trim(),
                    targetAmount = targetAmount,
                    deadline = deadline
                )
                .onFailure { error ->

                    _actionError.value =
                        error.message
                            ?: "Goal could not be updated."
                }
        }
    }

    fun clearActionError() {
        _actionError.value = null
    }

    private fun formatLkrForError(
        amount: Double
    ): String {

        return "LKR ${
            String.format(
                "%,.0f",
                amount
            )
        }"
    }
}

data class GoalUiState(
    val isLoading: Boolean = false,
    val goal: Goal? = null,

    val transactions: List<GoalTransaction> =
        emptyList(),

    val currentSavings: Double = 0.0,
    val remainingAmount: Double = 0.0,
    val progress: Float = 0f,

    val monthsRemaining: Long = 0L,

    val requiredMonthlySaving: Double = 0.0,

    val monthlyDepositAverage: Double = 0.0,

    val projectedFinishDate: LocalDate? = null,

    val milestones: List<GoalMilestone> =
        emptyList(),

    val nextMilestone: GoalMilestone? = null,

    val error: String? = null
)

data class GoalMilestone(
    val percentage: Int,
    val title: String,
    val description: String,
    val targetAmount: Double,
    val isReached: Boolean
)