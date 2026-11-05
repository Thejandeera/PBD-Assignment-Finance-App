package com.clarity.finance.data.repository

import com.clarity.finance.model.Goal
import com.clarity.finance.model.GoalTransaction
import com.clarity.finance.model.GoalTransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.util.UUID

class FakeGoalRepository(
    initialGoal: Goal? =
        Goal(
            id = "primary_goal",
            name = "MacBook Pro M4",
            targetAmount = 490000.0,
            startingAmount = 0.0,
            deadline =
                LocalDate.now()
                    .plusMonths(12)
                    .toString()
        ),

    initialTransactions:
    List<GoalTransaction> =
        emptyList()
) : GoalRepository {

    private val goalFlow =
        MutableStateFlow(
            initialGoal
        )

    private val transactionsFlow =
        MutableStateFlow(
            initialTransactions
        )

    var depositCallCount:
            Int = 0
        private set

    var withdrawCallCount:
            Int = 0
        private set

    var updateGoalCallCount:
            Int = 0
        private set

    var shouldFailDeposit:
            Boolean = false

    var shouldFailWithdrawal:
            Boolean = false

    var shouldFailUpdate:
            Boolean = false

    override fun observeGoal():
            Flow<Goal?> =
        goalFlow.asStateFlow()

    override fun observeTransactions():
            Flow<List<GoalTransaction>> =
        transactionsFlow.asStateFlow()

    override suspend fun updateGoal(
        name: String,
        targetAmount: Double,
        deadline: String
    ): Result<Unit> {

        updateGoalCallCount++

        if (shouldFailUpdate) {
            return Result.failure(
                IllegalStateException(
                    "Goal update failed"
                )
            )
        }

        val current =
            goalFlow.value

        goalFlow.value =
            if (current != null) {

                current.copy(
                    name = name,
                    targetAmount =
                        targetAmount,
                    deadline =
                        deadline,
                    updatedAt =
                        System.currentTimeMillis()
                )

            } else {

                Goal(
                    id = "primary_goal",
                    name = name,
                    targetAmount =
                        targetAmount,
                    startingAmount =
                        0.0,
                    deadline =
                        deadline,
                    createdAt =
                        System.currentTimeMillis(),
                    updatedAt =
                        System.currentTimeMillis()
                )
            }

        return Result.success(
            Unit
        )
    }

    override suspend fun deposit(
        amount: Double,
        note: String
    ): Result<Unit> {

        depositCallCount++

        if (shouldFailDeposit) {
            return Result.failure(
                IllegalStateException(
                    "Deposit failed"
                )
            )
        }

        val transaction =
            GoalTransaction(
                id =
                    UUID.randomUUID()
                        .toString(),
                type =
                    GoalTransactionType
                        .DEPOSIT,
                amount =
                    amount,
                note =
                    note,
                createdAt =
                    System.currentTimeMillis()
            )

        transactionsFlow.value =
            transactionsFlow.value +
                    transaction

        return Result.success(
            Unit
        )
    }

    override suspend fun withdraw(
        amount: Double,
        note: String
    ): Result<Unit> {

        withdrawCallCount++

        if (shouldFailWithdrawal) {
            return Result.failure(
                IllegalStateException(
                    "Withdrawal failed"
                )
            )
        }

        val transaction =
            GoalTransaction(
                id =
                    UUID.randomUUID()
                        .toString(),
                type =
                    GoalTransactionType
                        .WITHDRAWAL,
                amount =
                    amount,
                note =
                    note,
                createdAt =
                    System.currentTimeMillis()
            )

        transactionsFlow.value =
            transactionsFlow.value +
                    transaction

        return Result.success(
            Unit
        )
    }

    fun setGoal(
        goal: Goal?
    ) {
        goalFlow.value =
            goal
    }

    fun setTransactions(
        transactions:
        List<GoalTransaction>
    ) {
        transactionsFlow.value =
            transactions
    }
}