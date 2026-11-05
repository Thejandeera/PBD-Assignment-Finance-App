package com.clarity.finance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clarity.finance.data.repository.GoalRepository
import com.clarity.finance.model.Goal
import com.clarity.finance.model.GoalTransaction
import com.clarity.finance.model.GoalTransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.math.ceil

@OptIn(
    ExperimentalCoroutinesApi::class
)
@HiltViewModel
class GoalViewModel @Inject constructor(
    private val goalRepository:
    GoalRepository
) : ViewModel() {

    private val _selectedGoalId =
        MutableStateFlow<String?>(
            null
        )

    val selectedGoalId:
            StateFlow<String?> =
        _selectedGoalId
            .asStateFlow()

    private val _actionError =
        MutableStateFlow<String?>(
            null
        )

    val actionError:
            StateFlow<String?> =
        _actionError
            .asStateFlow()

    private val goalsFlow =
        goalRepository
            .observeGoals()

    private val selectedTransactionsFlow:
            Flow<List<GoalTransaction>> =
        _selectedGoalId
            .flatMapLatest {
                    goalId ->

                if (
                    goalId.isNullOrBlank()
                ) {

                    flowOf(
                        emptyList()
                    )

                } else {

                    goalRepository
                        .observeTransactions(
                            goalId
                        )
                }
            }

    val uiState:
            StateFlow<GoalUiState> =
        combine(
            goalsFlow,
            _selectedGoalId,
            selectedTransactionsFlow
        ) {
                goals,
                selectedId,
                transactions ->

            val resolvedSelectedId =
                when {

                    goals.isEmpty() ->
                        null

                    selectedId != null &&
                            goals.any {
                                it.id ==
                                        selectedId
                            } ->
                        selectedId

                    else ->
                        (goals.firstOrNull { it.isPrimary } ?: goals.first()).id
                }

            if (
                resolvedSelectedId !=
                selectedId
            ) {

                _selectedGoalId.value =
                    resolvedSelectedId
            }

            val selectedGoal =
                goals.firstOrNull {
                    it.id ==
                            resolvedSelectedId
                }

            buildUiState(
                goals =
                    goals,

                selectedGoal =
                    selectedGoal,

                transactions =
                    transactions
            )
        }
            .stateIn(
                scope =
                    viewModelScope,

                started =
                    SharingStarted
                        .WhileSubscribed(
                            5000
                        ),

                initialValue =
                    GoalUiState(
                        isLoading =
                            true
                    )
            )

    private fun buildUiState(
        goals: List<Goal>,
        selectedGoal: Goal?,
        transactions:
        List<GoalTransaction>
    ): GoalUiState {

        if (
            selectedGoal == null
        ) {

            return GoalUiState(
                isLoading =
                    false,

                goals =
                    goals,

                goal =
                    null,

                transactions =
                    emptyList()
            )
        }

        val sortedTransactions =
            transactions
                .sortedByDescending {
                    it.createdAt
                }

        val totalDeposits =
            transactions
                .filter {
                    it.type ==
                            GoalTransactionType
                                .DEPOSIT
                }
                .sumOf {
                    it.amount
                }

        val totalWithdrawals =
            transactions
                .filter {
                    it.type ==
                            GoalTransactionType
                                .WITHDRAWAL
                }
                .sumOf {
                    it.amount
                }

        val currentSavings =
            (
                    selectedGoal
                        .startingAmount +
                            totalDeposits -
                            totalWithdrawals
                    )
                .coerceAtLeast(
                    0.0
                )

        val targetAmount =
            selectedGoal
                .targetAmount
                .coerceAtLeast(
                    0.0
                )

        val remainingAmount =
            (
                    targetAmount -
                            currentSavings
                    )
                .coerceAtLeast(
                    0.0
                )

        val progress =
            if (
                targetAmount > 0.0
            ) {

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
                selectedGoal
                    .deadline
            )

        val requiredMonthlySaving =
            when {

                remainingAmount <=
                        0.0 ->
                    0.0

                monthsRemaining > 0 ->
                    remainingAmount /
                            monthsRemaining

                else ->
                    remainingAmount
            }

        val monthlyDepositAverage =
            calculateMonthlyDepositAverage(
                transactions
            )

        val predictDeadline =
            calculatePredictDeadline(
                remainingAmount =
                    remainingAmount,

                monthlyDepositAverage =
                    monthlyDepositAverage
            )

        val milestones =
            buildMilestones(
                targetAmount =
                    targetAmount,

                currentSavings =
                    currentSavings
            )

        val nextMilestone =
            milestones
                .firstOrNull {
                    !it.isReached
                }

        val highestReached =
            milestones
                .lastOrNull {
                    it.isReached
                }

        val selectedIndex =
            goals.indexOfFirst {
                it.id ==
                        selectedGoal.id
            }
                .coerceAtLeast(
                    0
                )

        val achievementMessage =
            when {

                progress >= 1f ->

                    "🎉 Goal Completed! You reached your ${selectedGoal.name} savings target."

                highestReached != null ->

                    "🏆 ${highestReached.percentage}% milestone achieved · ${highestReached.title}"

                else ->
                    null
            }

        return GoalUiState(
            isLoading =
                false,

            goals =
                goals,

            selectedGoalIndex =
                selectedIndex,

            goal =
                selectedGoal,

            transactions =
                sortedTransactions,

            currentSavings =
                currentSavings,

            remainingAmount =
                remainingAmount,

            progress =
                progress,

            monthsRemaining =
                monthsRemaining,

            requiredMonthlySaving =
                requiredMonthlySaving,

            monthlyDepositAverage =
                monthlyDepositAverage,

            predictDeadline =
                predictDeadline,

            milestones =
                milestones,

            nextMilestone =
                nextMilestone,

            achievementMessage =
                achievementMessage
        )
    }

    fun selectGoal(goalId: String) {
        _selectedGoalId.value = goalId
    }

    fun setPrimaryGoal(goalId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            goalRepository.setPrimaryGoal(goalId)
                .onSuccess {
                    onSuccess()
                }
                .onFailure { error ->
                    _actionError.value =
                        error.message ?: "Failed to set primary goal"
                }
        }
    }

    fun selectPreviousGoal() {

        val state =
            uiState.value

        if (
            state.goals.size <= 1
        ) {
            return
        }

        val currentIndex =
            state.selectedGoalIndex

        val previousIndex =
            if (
                currentIndex <= 0
            ) {
                state.goals.lastIndex
            } else {
                currentIndex - 1
            }

        _selectedGoalId.value =
            state.goals[
                previousIndex
            ].id
    }

    fun selectNextGoal() {

        val state =
            uiState.value

        if (
            state.goals.size <= 1
        ) {
            return
        }

        val currentIndex =
            state.selectedGoalIndex

        val nextIndex =
            if (
                currentIndex >=
                state.goals.lastIndex
            ) {
                0
            } else {
                currentIndex + 1
            }

        _selectedGoalId.value =
            state.goals[
                nextIndex
            ].id
    }

    fun addGoal(
        name: String,
        targetAmount: Double,
        startingAmount: Double,
        deadline: String,
        onSuccess: () -> Unit = {}
    ) {

        if (
            name.isBlank()
        ) {

            _actionError.value =
                "Goal name cannot be empty."

            return
        }

        if (
            targetAmount <= 0.0
        ) {

            _actionError.value =
                "Target amount must be greater than 0."

            return
        }

        if (
            startingAmount < 0.0
        ) {

            _actionError.value =
                "Starting amount cannot be negative."

            return
        }

        if (
            startingAmount >
            targetAmount
        ) {

            _actionError.value =
                "Starting amount cannot exceed the target."
            return
        }

        val parsedDeadline =
            runCatching {
                LocalDate
                    .parse(
                        deadline
                    )
            }.getOrNull()

        if (
            parsedDeadline == null
        ) {

            _actionError.value =
                "Use date format YYYY-MM-DD."

            return
        }

        if (
            parsedDeadline
                .isBefore(
                    LocalDate.now()
                )
        ) {

            _actionError.value =
                "Target date must be today or later."

            return
        }

        _actionError.value =
            null

        viewModelScope.launch {

            goalRepository
                .addGoal(
                    name =
                        name.trim(),

                    targetAmount =
                        targetAmount,

                    startingAmount =
                        startingAmount,

                    deadline =
                        deadline
                )
                .onSuccess {
                        newGoalId ->

                    _selectedGoalId.value =
                        newGoalId

                    onSuccess()
                }
                .onFailure {
                        error ->

                    _actionError.value =
                        error.message
                            ?: "Goal could not be added."
                }
        }
    }

    fun updateGoal(
        name: String,
        targetAmount: Double,
        deadline: String,
        onSuccess: () -> Unit = {}
    ) {
        val goalId =
            uiState.value
                .goal
                ?.id

        if (goalId.isNullOrBlank()) {
            _actionError.value = "Select a goal first."
            return
        }

        if (name.isBlank()) {
            _actionError.value = "Goal name cannot be empty."
            return
        }

        if (targetAmount <= 0.0) {
            _actionError.value = "Target amount must be greater than 0."
            return
        }

        val parsedDeadline =
            runCatching {
                LocalDate.parse(deadline)
            }.getOrNull()

        if (parsedDeadline == null) {
            _actionError.value = "Use date format YYYY-MM-DD."
            return
        }

        if (parsedDeadline.isBefore(LocalDate.now())) {
            _actionError.value = "Target date must be today or later."
            return
        }

        _actionError.value = null

        viewModelScope.launch {
            goalRepository
                .updateGoal(
                    goalId = goalId,
                    name = name.trim(),
                    targetAmount = targetAmount,
                    deadline = deadline
                )
                .onSuccess {
                    onSuccess()
                }
                .onFailure { error ->
                    _actionError.value =
                        error.message ?: "Goal could not be updated."
                }
        }
    }

    fun deleteGoal(
        goalId: String? = null,
        onSuccess: () -> Unit = {}
    ) {
        val targetId = goalId ?: uiState.value.goal?.id
        if (targetId.isNullOrBlank()) {
            _actionError.value = "Select a goal first."
            return
        }
        _actionError.value = null
        viewModelScope.launch {
            goalRepository.deleteGoal(targetId)
                .onSuccess {
                    if (_selectedGoalId.value == targetId) {
                        _selectedGoalId.value = null
                    }
                    onSuccess()
                }
                .onFailure { error ->
                    _actionError.value =
                        error.message ?: "Goal could not be deleted."
                }
        }
    }

    fun deposit(
        amount: Double,
        note: String = ""
    ) {

        val goalId =
            uiState.value
                .goal
                ?.id

        if (
            goalId.isNullOrBlank()
        ) {

            _actionError.value =
                "Select a goal first."

            return
        }

        if (
            amount <= 0.0
        ) {

            _actionError.value =
                "Enter an amount greater than 0."

            return
        }

        _actionError.value =
            null

        viewModelScope.launch {

            goalRepository
                .deposit(
                    goalId =
                        goalId,

                    amount =
                        amount,

                    note =
                        note.trim()
                )
                .onFailure {
                        error ->

                    _actionError.value =
                        error.message
                            ?: "Savings could not be added."
                }
        }
    }

    fun withdraw(
        amount: Double,
        note: String = ""
    ): Boolean {

        val state =
            uiState.value

        val goalId =
            state.goal?.id

        if (
            goalId.isNullOrBlank()
        ) {

            _actionError.value =
                "Select a goal first."

            return false
        }

        if (
            amount <= 0.0
        ) {

            _actionError.value =
                "Enter an amount greater than 0."

            return false
        }

        if (
            state.currentSavings <=
            0.0
        ) {

            _actionError.value =
                "You don't have any savings to withdraw."

            return false
        }

        if (
            amount >
            state.currentSavings
        ) {

            _actionError.value =
                "You can only withdraw up to ${
                    formatLkrForError(
                        state.currentSavings
                    )
                }."

            return false
        }

        _actionError.value =
            null

        viewModelScope.launch {

            goalRepository
                .withdraw(
                    goalId =
                        goalId,

                    amount =
                        amount,

                    note =
                        note.trim()
                )
                .onFailure {
                        error ->

                    _actionError.value =
                        error.message
                            ?: "Withdrawal failed."
                }
        }

        return true
    }

    fun clearActionError() {

        _actionError.value =
            null
    }

    private fun buildMilestones(
        targetAmount: Double,
        currentSavings: Double
    ): List<GoalMilestone> {

        if (
            targetAmount <= 0.0
        ) {
            return emptyList()
        }

        return listOf(
            GoalMilestone(
                percentage = 25,
                title = "Getting Started",
                targetAmount =
                    targetAmount *
                            0.25,
                isReached =
                    currentSavings >=
                            targetAmount *
                            0.25
            ),

            GoalMilestone(
                percentage = 50,
                title = "Halfway There",
                targetAmount =
                    targetAmount *
                            0.50,
                isReached =
                    currentSavings >=
                            targetAmount *
                            0.50
            ),

            GoalMilestone(
                percentage = 75,
                title = "Almost There",
                targetAmount =
                    targetAmount *
                            0.75,
                isReached =
                    currentSavings >=
                            targetAmount *
                            0.75
            ),

            GoalMilestone(
                percentage = 100,
                title = "Goal Reached",
                targetAmount =
                    targetAmount,
                isReached =
                    currentSavings >=
                            targetAmount
            )
        )
    }

    private fun calculateMonthsRemaining(
        deadline: String
    ): Long {

        val deadlineDate =
            runCatching {

                LocalDate.parse(
                    deadline
                )

            }.getOrNull()
                ?: return 0L

        val today =
            LocalDate.now()

        if (
            deadlineDate
                .isBefore(
                    today
                )
        ) {
            return 0L
        }

        return ChronoUnit
            .MONTHS
            .between(
                YearMonth.from(
                    today
                ),
                YearMonth.from(
                    deadlineDate
                )
            )
    }

    private fun calculateMonthlyDepositAverage(
        transactions:
        List<GoalTransaction>
    ): Double {

        val deposits =
            transactions
                .filter {

                    it.type ==
                            GoalTransactionType
                                .DEPOSIT &&
                            it.amount >
                            0.0
                }

        if (
            deposits.isEmpty()
        ) {
            return 0.0
        }

        val byMonth =
            deposits
                .groupBy {
                        transaction ->

                    YearMonth.from(
                        Instant
                            .ofEpochMilli(
                                transaction
                                    .createdAt
                            )
                            .atZone(
                                ZoneId
                                    .systemDefault()
                            )
                            .toLocalDate()
                    )
                }

        if (
            byMonth.isEmpty()
        ) {
            return 0.0
        }

        return byMonth
            .values
            .map {
                    monthTransactions ->

                monthTransactions
                    .sumOf {
                        it.amount
                    }
            }
            .average()
    }

    private fun calculatePredictDeadline(
        remainingAmount: Double,
        monthlyDepositAverage: Double
    ): LocalDate? {

        if (
            remainingAmount <=
            0.0
        ) {

            return LocalDate.now()
        }

        if (
            monthlyDepositAverage <=
            0.0
        ) {

            return null
        }

        val monthsNeeded =
            ceil(
                remainingAmount /
                        monthlyDepositAverage
            )
                .toLong()
                .coerceAtLeast(
                    1L
                )

        return LocalDate
            .now()
            .plusMonths(
                monthsNeeded
            )
    }

    private fun formatLkrForError(
        amount: Double
    ): String =
        "LKR ${
            String.format(
                "%,.0f",
                amount
            )
        }"
}

data class GoalUiState(
    val isLoading:
    Boolean = false,

    val goals:
    List<Goal> =
        emptyList(),

    val selectedGoalIndex:
    Int = 0,

    val goal:
    Goal? = null,

    val transactions:
    List<GoalTransaction> =
        emptyList(),

    val currentSavings:
    Double = 0.0,

    val remainingAmount:
    Double = 0.0,

    val progress:
    Float = 0f,

    val monthsRemaining:
    Long = 0L,

    val requiredMonthlySaving:
    Double = 0.0,

    val monthlyDepositAverage:
    Double = 0.0,

    val predictDeadline:
    LocalDate? = null,

    val milestones:
    List<GoalMilestone> =
        emptyList(),

    val nextMilestone:
    GoalMilestone? = null,

    val achievementMessage:
    String? = null
)

data class GoalMilestone(
    val percentage:
    Int,

    val title:
    String,

    val targetAmount:
    Double,

    val isReached:
    Boolean
)