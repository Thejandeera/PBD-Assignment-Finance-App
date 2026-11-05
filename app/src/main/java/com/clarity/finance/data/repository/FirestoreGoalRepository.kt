package com.clarity.finance.data.repository

import android.util.Log
import com.clarity.finance.data.remote.FirestorePaths
import com.clarity.finance.model.Goal
import com.clarity.finance.model.GoalTransaction
import com.clarity.finance.model.GoalTransactionType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreGoalRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : GoalRepository {

    private fun requireUid(): String =
        auth.currentUser?.uid
            ?: error("No authenticated user")

    private fun goalsCollection() =
        firestore
            .collection(FirestorePaths.USERS)
            .document(requireUid())
            .collection(FirestorePaths.GOALS)

    private fun goalDocument(
        goalId: String
    ) =
        goalsCollection()
            .document(goalId)

    private fun primaryGoalDocument() =
        goalDocument(
            FirestorePaths.PRIMARY_GOAL_ID
        )

    private fun transactionsCollection(
        goalId: String
    ) =
        goalDocument(goalId)
            .collection("transactions")

    // ============================================================
    // Primary goal - backward compatibility
    // ============================================================

    override fun observeGoal(): Flow<Goal?> =
        observeGoals().map { goals ->
            goals.firstOrNull { it.isPrimary } ?: goals.firstOrNull()
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeTransactions():
            Flow<List<GoalTransaction>> =
        observeGoals().flatMapLatest { goals ->
            val primaryId = (goals.firstOrNull { it.isPrimary } ?: goals.firstOrNull())?.id
            if (primaryId != null) {
                observeTransactions(primaryId)
            } else {
                flowOf(emptyList())
            }
        }

    override suspend fun updateGoal(
        name: String,
        targetAmount: Double,
        deadline: String
    ): Result<Unit> {
        val firstId = goalsCollection().get().await().documents.firstOrNull()?.id
            ?: return Result.failure(IllegalStateException("No goal found to update"))
        return updateGoal(
            goalId = firstId,
            name = name,
            targetAmount = targetAmount,
            deadline = deadline
        )
    }

    override suspend fun deposit(
        amount: Double,
        note: String
    ): Result<Unit> {
        val firstId = goalsCollection().get().await().documents.firstOrNull()?.id
            ?: return Result.failure(IllegalStateException("No goal found for deposit"))
        return deposit(
            goalId = firstId,
            amount = amount,
            note = note
        )
    }

    override suspend fun withdraw(
        amount: Double,
        note: String
    ): Result<Unit> {
        val firstId = goalsCollection().get().await().documents.firstOrNull()?.id
            ?: return Result.failure(IllegalStateException("No goal found for withdrawal"))
        return withdraw(
            goalId = firstId,
            amount = amount,
            note = note
        )
    }

    // ============================================================
    // Multi-goal support
    // ============================================================

    override fun observeGoals():
            Flow<List<Goal>> =
        callbackFlow {

            val uid =
                auth.currentUser?.uid

            if (uid == null) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }

            val listener =
                firestore
                    .collection(
                        FirestorePaths.USERS
                    )
                    .document(uid)
                    .collection(
                        FirestorePaths.GOALS
                    )
                    .addSnapshotListener {
                            snapshot,
                            error ->

                        if (error != null) {

                            Log.e(
                                "FirestoreGoalRepo",
                                "Goals stream error: ${error.message}",
                                error
                            )

                            trySend(emptyList())
                            return@addSnapshotListener
                        }

                        val goals =
                            snapshot
                                ?.documents
                                ?.mapNotNull {
                                    it.toGoal()
                                }
                                ?.sortedWith(
                                    compareByDescending<Goal> {
                                        it.isPrimary
                                    }.thenBy {
                                        it.createdAt
                                    }
                                )
                                .orEmpty()

                        trySend(goals)
                    }

            awaitClose {
                listener.remove()
            }
        }

    override fun observeTransactions(
        goalId: String
    ): Flow<List<GoalTransaction>> =
        callbackFlow {

            val uid =
                auth.currentUser?.uid

            if (uid == null) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }

            val listener =
                firestore
                    .collection(
                        FirestorePaths.USERS
                    )
                    .document(uid)
                    .collection(
                        FirestorePaths.GOALS
                    )
                    .document(goalId)
                    .collection("transactions")
                    .orderBy(
                        "createdAt",
                        Query.Direction.DESCENDING
                    )
                    .addSnapshotListener {
                            snapshot,
                            error ->

                        if (error != null) {

                            Log.e(
                                "FirestoreGoalRepo",
                                "Transactions stream error: ${error.message}",
                                error
                            )

                            trySend(emptyList())
                            return@addSnapshotListener
                        }

                        val transactions =
                            snapshot
                                ?.documents
                                ?.mapNotNull {
                                        document ->

                                    document
                                        .toGoalTransaction()
                                }
                                .orEmpty()

                        trySend(
                            transactions
                        )
                    }

            awaitClose {
                listener.remove()
            }
        }

    override suspend fun addGoal(
        name: String,
        targetAmount: Double,
        startingAmount: Double,
        deadline: String
    ): Result<String> =
        runCatching {

            require(
                name.isNotBlank()
            ) {
                "Goal name cannot be empty"
            }

            require(
                targetAmount > 0
            ) {
                "Target amount must be greater than zero"
            }

            require(
                startingAmount >= 0
            ) {
                "Starting amount cannot be negative"
            }

            require(
                startingAmount <=
                        targetAmount
            ) {
                "Starting amount cannot exceed the target amount"
            }

            val isFirstGoal =
                goalsCollection().get().await().isEmpty

            val document =
                goalsCollection()
                    .document()

            document
                .set(
                    mapOf(
                        "name" to
                                name.trim(),

                        "targetAmount" to
                                targetAmount,

                        "startingAmount" to
                                startingAmount,

                        "deadline" to
                                deadline,

                        "isPrimary" to
                                isFirstGoal,

                        "createdAt" to
                                FieldValue
                                    .serverTimestamp(),

                        "updatedAt" to
                                FieldValue
                                    .serverTimestamp()
                    )
                )
                .await()

            document.id
        }

    override suspend fun updateGoal(
        goalId: String,
        name: String,
        targetAmount: Double,
        deadline: String
    ): Result<Unit> =
        runCatching {

            require(
                goalId.isNotBlank()
            ) {
                "Goal ID cannot be empty"
            }

            require(
                name.isNotBlank()
            ) {
                "Goal name cannot be empty"
            }

            require(
                targetAmount > 0
            ) {
                "Target amount must be greater than zero"
            }

            goalDocument(goalId)
                .update(
                    mapOf(
                        "name" to
                                name.trim(),

                        "targetAmount" to
                                targetAmount,

                        "deadline" to
                                deadline,

                        "updatedAt" to
                                FieldValue
                                    .serverTimestamp()
                    )
                )
                .await()

            Unit
        }

    override suspend fun deleteGoal(
        goalId: String
    ): Result<Unit> =
        runCatching {
            require(goalId.isNotBlank()) { "Goal ID cannot be empty" }
            val doc = goalDocument(goalId)
            val wasPrimary = doc.get().await().getBoolean("isPrimary") ?: false
            doc.delete().await()
            if (wasPrimary) {
                val remaining = goalsCollection().get().await().documents
                remaining.firstOrNull()?.reference?.update("isPrimary", true)?.await()
            }
            Unit
        }

    override suspend fun setPrimaryGoal(
        goalId: String
    ): Result<Unit> =
        runCatching {
            require(goalId.isNotBlank()) { "Goal ID cannot be empty" }
            val snapshot = goalsCollection().get().await()
            val batch = firestore.batch()
            for (doc in snapshot.documents) {
                batch.update(doc.reference, "isPrimary", doc.id == goalId)
            }
            batch.commit().await()
            Unit
        }

    override suspend fun deposit(
        goalId: String,
        amount: Double,
        note: String
    ): Result<Unit> =
        runCatching {

            require(
                goalId.isNotBlank()
            ) {
                "Goal ID cannot be empty"
            }

            require(
                amount > 0
            ) {
                "Deposit amount must be greater than zero"
            }

            transactionsCollection(
                goalId
            )
                .add(
                    mapOf(
                        "type" to
                                GoalTransactionType
                                    .DEPOSIT
                                    .name,

                        "amount" to
                                amount,

                        "note" to
                                note.trim(),

                        "createdAt" to
                                FieldValue
                                    .serverTimestamp()
                    )
                )
                .await()

            Unit
        }

    override suspend fun withdraw(
        goalId: String,
        amount: Double,
        note: String
    ): Result<Unit> =
        runCatching {

            require(
                goalId.isNotBlank()
            ) {
                "Goal ID cannot be empty"
            }

            require(
                amount > 0
            ) {
                "Withdrawal amount must be greater than zero"
            }

            transactionsCollection(
                goalId
            )
                .add(
                    mapOf(
                        "type" to
                                GoalTransactionType
                                    .WITHDRAWAL
                                    .name,

                        "amount" to
                                amount,

                        "note" to
                                note.trim(),

                        "createdAt" to
                                FieldValue
                                    .serverTimestamp()
                    )
                )
                .await()

            Unit
        }

    // ============================================================
    // Mapping
    // ============================================================

    private fun DocumentSnapshot.toGoal():
            Goal? {

        if (!exists()) {
            return null
        }

        return Goal(
            id =
                id,

            name =
                getString("name")
                    .orEmpty(),

            targetAmount =
                getDouble(
                    "targetAmount"
                )
                    ?: 0.0,

            startingAmount =
                getDouble(
                    "startingAmount"
                )
                    ?: 0.0,

            deadline =
                getString(
                    "deadline"
                )
                    .orEmpty(),

            isPrimary =
                getBoolean(
                    "isPrimary"
                )
                    ?: false,

            createdAt =
                getTimestamp(
                    "createdAt"
                )
                    ?.toDate()
                    ?.time
                    ?: 0L,

            updatedAt =
                getTimestamp(
                    "updatedAt"
                )
                    ?.toDate()
                    ?.time
                    ?: 0L
        )
    }

    private fun DocumentSnapshot
            .toGoalTransaction():
            GoalTransaction? {

        if (!exists()) {
            return null
        }

        val typeString =
            getString("type")
                ?: return null

        val type =
            runCatching {

                GoalTransactionType
                    .valueOf(
                        typeString
                    )

            }.getOrDefault(
                GoalTransactionType
                    .DEPOSIT
            )

        return GoalTransaction(
            id =
                id,

            type =
                type,

            amount =
                getDouble(
                    "amount"
                )
                    ?: 0.0,

            note =
                getString(
                    "note"
                )
                    .orEmpty(),

            createdAt =
                getTimestamp(
                    "createdAt"
                )
                    ?.toDate()
                    ?.time
                    ?: 0L
        )
    }
}