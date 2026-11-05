package com.clarity.finance.viewmodel

import com.clarity.finance.MainDispatcherRule
import com.clarity.finance.model.Goal
import com.clarity.finance.model.GoalTransaction
import com.clarity.finance.model.GoalTransactionType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import com.clarity.finance.data.repository.FakeGoalRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

@OptIn(
    ExperimentalCoroutinesApi::class
)
class GoalViewModelTest {

    @get:Rule
    val mainDispatcherRule =
        MainDispatcherRule()

    private lateinit var repository:
            FakeGoalRepository

    private lateinit var viewModel:
            GoalViewModel

    @Before
    fun setup() {

        repository =
            FakeGoalRepository(
                initialGoal =
                    defaultGoal()
            )

        viewModel =
            GoalViewModel(
                repository
            )
    }

    private fun defaultGoal(
        targetAmount:
        Double = 490000.0,

        startingAmount:
        Double = 0.0,

        monthsAhead:
        Long = 12
    ): Goal {

        return Goal(
            id =
                "primary_goal",
            name =
                "MacBook Pro M4",
            targetAmount =
                targetAmount,
            startingAmount =
                startingAmount,
            deadline =
                LocalDate
                    .now()
                    .plusMonths(
                        monthsAhead
                    )
                    .toString()
        )
    }

    private fun transaction(
        type:
        GoalTransactionType,

        amount:
        Double,

        monthsAgo:
        Long = 0,

        note:
        String = ""
    ): GoalTransaction {

        val timestamp =
            LocalDate
                .now()
                .minusMonths(
                    monthsAgo
                )
                .atStartOfDay(
                    ZoneId.systemDefault()
                )
                .toInstant()
                .toEpochMilli()

        return GoalTransaction(
            id =
                "${type.name}-$amount-$monthsAgo",

            type =
                type,

            amount =
                amount,

            note =
                note,

            createdAt =
                timestamp
        )
    }

    private suspend fun startCollecting() =
        kotlinx.coroutines.coroutineScope {

            launch(
                UnconfinedTestDispatcher(
                    mainDispatcherRule
                        .testDispatcher
                        .scheduler
                )
            ) {
                viewModel
                    .uiState
                    .collect()
            }
        }

    @Test
    fun `starting savings are included in current savings`() =
        runTest {

            repository =
                FakeGoalRepository(
                    initialGoal =
                        defaultGoal(
                            startingAmount =
                                11200.0
                        )
                )

            viewModel =
                GoalViewModel(
                    repository
                )

            val job =
                backgroundScope.launch(
                    UnconfinedTestDispatcher(
                        testScheduler
                    )
                ) {
                    viewModel
                        .uiState
                        .collect()
                }

            advanceUntilIdle()

            assertEquals(
                11200.0,
                viewModel
                    .uiState
                    .value
                    .currentSavings,
                0.01
            )

            job.cancel()
        }

    @Test
    fun `deposit increases current savings`() =
        runTest {

            val job =
                backgroundScope.launch(
                    UnconfinedTestDispatcher(
                        testScheduler
                    )
                ) {
                    viewModel
                        .uiState
                        .collect()
                }

            viewModel.deposit(
                amount =
                    50000.0,
                note =
                    "November savings"
            )

            advanceUntilIdle()

            assertEquals(
                50000.0,
                viewModel
                    .uiState
                    .value
                    .currentSavings,
                0.01
            )

            assertEquals(
                1,
                repository
                    .depositCallCount
            )

            job.cancel()
        }

    @Test
    fun `withdrawal reduces current savings`() =
        runTest {

            repository =
                FakeGoalRepository(
                    initialGoal =
                        defaultGoal(
                            startingAmount =
                                50000.0
                        )
                )

            viewModel =
                GoalViewModel(
                    repository
                )

            val job =
                backgroundScope.launch(
                    UnconfinedTestDispatcher(
                        testScheduler
                    )
                ) {
                    viewModel
                        .uiState
                        .collect()
                }

            advanceUntilIdle()

            val accepted =
                viewModel.withdraw(
                    amount =
                        10000.0,
                    note =
                        "Emergency"
                )

            advanceUntilIdle()

            assertTrue(
                accepted
            )

            assertEquals(
                40000.0,
                viewModel
                    .uiState
                    .value
                    .currentSavings,
                0.01
            )

            assertEquals(
                1,
                repository
                    .withdrawCallCount
            )

            job.cancel()
        }

    @Test
    fun `withdrawal above available savings is rejected`() =
        runTest {

            repository =
                FakeGoalRepository(
                    initialGoal =
                        defaultGoal(
                            startingAmount =
                                20000.0
                        )
                )

            viewModel =
                GoalViewModel(
                    repository
                )

            val job =
                backgroundScope.launch(
                    UnconfinedTestDispatcher(
                        testScheduler
                    )
                ) {
                    viewModel
                        .uiState
                        .collect()
                }

            advanceUntilIdle()

            val accepted =
                viewModel.withdraw(
                    amount =
                        25000.0
                )

            advanceUntilIdle()

            assertFalse(
                accepted
            )

            assertEquals(
                0,
                repository
                    .withdrawCallCount
            )

            assertEquals(
                20000.0,
                viewModel
                    .uiState
                    .value
                    .currentSavings,
                0.01
            )

            assertNotNull(
                viewModel
                    .actionError
                    .value
            )

            assertTrue(
                viewModel
                    .actionError
                    .value
                    .orEmpty()
                    .contains(
                        "withdraw",
                        ignoreCase = true
                    )
            )

            job.cancel()
        }

    @Test
    fun `zero withdrawal is rejected`() =
        runTest {

            repository =
                FakeGoalRepository(
                    initialGoal =
                        defaultGoal(
                            startingAmount =
                                20000.0
                        )
                )

            viewModel =
                GoalViewModel(
                    repository
                )

            val job =
                backgroundScope.launch(
                    UnconfinedTestDispatcher(
                        testScheduler
                    )
                ) {
                    viewModel
                        .uiState
                        .collect()
                }

            advanceUntilIdle()

            val accepted =
                viewModel.withdraw(
                    amount =
                        0.0
                )

            assertFalse(
                accepted
            )

            assertEquals(
                0,
                repository
                    .withdrawCallCount
            )

            assertEquals(
                "Enter an amount greater than 0.",
                viewModel
                    .actionError
                    .value
            )

            job.cancel()
        }

    @Test
    fun `negative deposit is rejected`() =
        runTest {

            val job =
                backgroundScope.launch(
                    UnconfinedTestDispatcher(
                        testScheduler
                    )
                ) {
                    viewModel
                        .uiState
                        .collect()
                }

            advanceUntilIdle()

            viewModel.deposit(
                amount =
                    -5000.0
            )

            advanceUntilIdle()

            assertEquals(
                0,
                repository
                    .depositCallCount
            )

            assertEquals(
                "Enter an amount greater than 0.",
                viewModel
                    .actionError
                    .value
            )

            job.cancel()
        }

    @Test
    fun `progress is calculated correctly`() =
        runTest {

            repository =
                FakeGoalRepository(
                    initialGoal =
                        defaultGoal(
                            targetAmount =
                                400000.0,
                            startingAmount =
                                100000.0
                        )
                )

            viewModel =
                GoalViewModel(
                    repository
                )

            val job =
                backgroundScope.launch(
                    UnconfinedTestDispatcher(
                        testScheduler
                    )
                ) {
                    viewModel
                        .uiState
                        .collect()
                }

            advanceUntilIdle()

            assertEquals(
                0.25f,
                viewModel
                    .uiState
                    .value
                    .progress,
                0.001f
            )

            job.cancel()
        }

    @Test
    fun `remaining amount is calculated correctly`() =
        runTest {

            repository =
                FakeGoalRepository(
                    initialGoal =
                        defaultGoal(
                            targetAmount =
                                490000.0,
                            startingAmount =
                                122500.0
                        )
                )

            viewModel =
                GoalViewModel(
                    repository
                )

            val job =
                backgroundScope.launch(
                    UnconfinedTestDispatcher(
                        testScheduler
                    )
                ) {
                    viewModel
                        .uiState
                        .collect()
                }

            advanceUntilIdle()

            assertEquals(
                367500.0,
                viewModel
                    .uiState
                    .value
                    .remainingAmount,
                0.01
            )

            job.cancel()
        }

    @Test
    fun `months remaining uses actual month difference`() =
        runTest {

            val deadline =
                LocalDate
                    .now()
                    .plusMonths(
                        12
                    )

            repository =
                FakeGoalRepository(
                    initialGoal =
                        Goal(
                            id =
                                "primary_goal",
                            name =
                                "MacBook Pro M4",
                            targetAmount =
                                490000.0,
                            startingAmount =
                                0.0,
                            deadline =
                                deadline
                                    .toString()
                        )
                )

            viewModel =
                GoalViewModel(
                    repository
                )

            val job =
                backgroundScope.launch(
                    UnconfinedTestDispatcher(
                        testScheduler
                    )
                ) {
                    viewModel
                        .uiState
                        .collect()
                }

            advanceUntilIdle()

            assertEquals(
                12L,
                viewModel
                    .uiState
                    .value
                    .monthsRemaining
            )

            job.cancel()
        }

    @Test
    fun `required monthly saving is remaining divided by months left`() =
        runTest {

            val deadlineMonth =
                YearMonth
                    .now()
                    .plusMonths(
                        10
                    )

            repository =
                FakeGoalRepository(
                    initialGoal =
                        Goal(
                            id =
                                "primary_goal",

                            name =
                                "MacBook Pro M4",

                            targetAmount =
                                120000.0,

                            startingAmount =
                                20000.0,

                            deadline =
                                deadlineMonth
                                    .atDay(
                                        1
                                    )
                                    .toString()
                        )
                )

            viewModel =
                GoalViewModel(
                    repository
                )

            val job =
                backgroundScope.launch(
                    UnconfinedTestDispatcher(
                        testScheduler
                    )
                ) {
                    viewModel
                        .uiState
                        .collect()
                }

            advanceUntilIdle()

            assertEquals(
                10L,
                viewModel
                    .uiState
                    .value
                    .monthsRemaining
            )

            assertEquals(
                10000.0,
                viewModel
                    .uiState
                    .value
                    .requiredMonthlySaving,
                0.01
            )

            job.cancel()
        }

    @Test
    fun `completed goal has zero remaining and full progress`() =
        runTest {

            repository =
                FakeGoalRepository(
                    initialGoal =
                        defaultGoal(
                            targetAmount =
                                490000.0,
                            startingAmount =
                                490000.0
                        )
                )

            viewModel =
                GoalViewModel(
                    repository
                )

            val job =
                backgroundScope.launch(
                    UnconfinedTestDispatcher(
                        testScheduler
                    )
                ) {
                    viewModel
                        .uiState
                        .collect()
                }

            advanceUntilIdle()

            val state =
                viewModel
                    .uiState
                    .value

            assertEquals(
                490000.0,
                state.currentSavings,
                0.01
            )

            assertEquals(
                0.0,
                state.remainingAmount,
                0.01
            )

            assertEquals(
                1f,
                state.progress,
                0.001f
            )

            assertEquals(
                0.0,
                state.requiredMonthlySaving,
                0.01
            )

            assertNotNull(
                state.predictDeadline
            )

            job.cancel()
        }

    @Test
    fun `deposit and withdrawal transactions are combined into savings`() =
        runTest {

            repository =
                FakeGoalRepository(
                    initialGoal =
                        defaultGoal(
                            startingAmount =
                                10000.0
                        ),

                    initialTransactions =
                        listOf(
                            transaction(
                                type =
                                    GoalTransactionType.DEPOSIT,
                                amount =
                                    50000.0
                            ),

                            transaction(
                                type =
                                    GoalTransactionType.WITHDRAWAL,
                                amount =
                                    15000.0
                            )
                        )
                )

            viewModel =
                GoalViewModel(
                    repository
                )

            val job =
                backgroundScope.launch(
                    UnconfinedTestDispatcher(
                        testScheduler
                    )
                ) {
                    viewModel
                        .uiState
                        .collect()
                }

            advanceUntilIdle()

            assertEquals(
                45000.0,
                viewModel
                    .uiState
                    .value
                    .currentSavings,
                0.01
            )

            job.cancel()
        }

    @Test
    fun `twenty five percent milestone is reached`() =
        runTest {

            repository =
                FakeGoalRepository(
                    initialGoal =
                        defaultGoal(
                            targetAmount =
                                490000.0,
                            startingAmount =
                                122500.0
                        )
                )

            viewModel =
                GoalViewModel(
                    repository
                )

            val job =
                backgroundScope.launch(
                    UnconfinedTestDispatcher(
                        testScheduler
                    )
                ) {
                    viewModel
                        .uiState
                        .collect()
                }

            advanceUntilIdle()

            val milestones =
                viewModel
                    .uiState
                    .value
                    .milestones

            assertEquals(
                4,
                milestones.size
            )

            assertTrue(
                milestones[0]
                    .isReached
            )

            assertEquals(
                25,
                milestones[0]
                    .percentage
            )

            assertFalse(
                milestones[1]
                    .isReached
            )

            assertEquals(
                50,
                viewModel
                    .uiState
                    .value
                    .nextMilestone
                    ?.percentage
            )

            job.cancel()
        }

    @Test
    fun `all milestones are reached when goal is complete`() =
        runTest {

            repository =
                FakeGoalRepository(
                    initialGoal =
                        defaultGoal(
                            targetAmount =
                                490000.0,
                            startingAmount =
                                490000.0
                        )
                )

            viewModel =
                GoalViewModel(
                    repository
                )

            val job =
                backgroundScope.launch(
                    UnconfinedTestDispatcher(
                        testScheduler
                    )
                ) {
                    viewModel
                        .uiState
                        .collect()
                }

            advanceUntilIdle()

            assertTrue(
                viewModel
                    .uiState
                    .value
                    .milestones
                    .all {
                        it.isReached
                    }
            )

            assertNull(
                viewModel
                    .uiState
                    .value
                    .nextMilestone
            )

            job.cancel()
        }

    @Test
    fun `clear action error removes validation message`() =
        runTest {

            viewModel.withdraw(
                amount =
                    0.0
            )

            assertNotNull(
                viewModel
                    .actionError
                    .value
            )

            viewModel
                .clearActionError()

            assertNull(
                viewModel
                    .actionError
                    .value
            )
        }

    @Test
    fun `selectGoal updates selectedGoalId`() =
        runTest {
            viewModel.selectGoal("goal_2")
            assertEquals("goal_2", viewModel.selectedGoalId.value)
        }

    @Test
    fun `setPrimaryGoal updates repository primary goal`() =
        runTest {
            var callbackInvoked = false
            viewModel.setPrimaryGoal("primary_goal") {
                callbackInvoked = true
            }
            advanceUntilIdle()
            assertTrue(callbackInvoked)
        }
}