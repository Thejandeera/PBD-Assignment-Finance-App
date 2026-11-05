package com.clarity.finance.data.repository

import com.clarity.finance.model.Goal
import com.clarity.finance.model.GoalTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface GoalRepository {

    /**
     * Backward-compatible primary goal observer.
     * Dashboard can continue using this.
     */
    fun observeGoal(): Flow<Goal?>

    /**
     * Backward-compatible primary goal transactions.
     */
    fun observeTransactions(): Flow<List<GoalTransaction>>

    suspend fun updateGoal(
        name: String,
        targetAmount: Double,
        deadline: String
    ): Result<Unit>

    suspend fun deposit(
        amount: Double,
        note: String = ""
    ): Result<Unit>

    suspend fun withdraw(
        amount: Double,
        note: String = ""
    ): Result<Unit>

    // ============================================================
    // Multi-goal support
    // ============================================================

    fun observeGoals(): Flow<List<Goal>> =
        observeGoal().map { goal ->
            listOfNotNull(goal)
        }

    fun observeTransactions(
        goalId: String
    ): Flow<List<GoalTransaction>> =
        observeTransactions()

    suspend fun addGoal(
        name: String,
        targetAmount: Double,
        startingAmount: Double,
        deadline: String
    ): Result<String> =
        Result.failure(
            UnsupportedOperationException(
                "Multiple goals are not supported by this repository."
            )
        )

    suspend fun updateGoal(
        goalId: String,
        name: String,
        targetAmount: Double,
        deadline: String
    ): Result<Unit> =
        updateGoal(
            name = name,
            targetAmount = targetAmount,
            deadline = deadline
        )

    suspend fun deposit(
        goalId: String,
        amount: Double,
        note: String = ""
    ): Result<Unit> =
        deposit(
            amount = amount,
            note = note
        )

    suspend fun withdraw(
        goalId: String,
        amount: Double,
        note: String = ""
    ): Result<Unit> =
        withdraw(
            amount = amount,
            note = note
        )

    suspend fun deleteGoal(
        goalId: String
    ): Result<Unit> =
        Result.success(Unit)
}