package com.clarity.finance.data.repository

import com.clarity.finance.model.Goal
import com.clarity.finance.model.GoalTransaction
import kotlinx.coroutines.flow.Flow

interface GoalRepository {

    /**
     * Observe the user's primary savings goal in real time.
     */
    fun observeGoal(): Flow<Goal?>

    /**
     * Observe all savings deposits and withdrawals.
     */
    fun observeTransactions(): Flow<List<GoalTransaction>>

    /**
     * Update editable goal details.
     */
    suspend fun updateGoal(
        name: String,
        targetAmount: Double,
        deadline: String
    ): Result<Unit>

    /**
     * Add money to the goal.
     */
    suspend fun deposit(
        amount: Double,
        note: String = ""
    ): Result<Unit>

    /**
     * Remove money from the goal.
     */
    suspend fun withdraw(
        amount: Double,
        note: String = ""
    ): Result<Unit>
}