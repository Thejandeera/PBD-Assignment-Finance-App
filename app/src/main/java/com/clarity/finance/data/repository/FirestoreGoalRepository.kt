package com.clarity.finance.data.repository

import com.clarity.finance.data.remote.FirestorePaths
import com.clarity.finance.model.Goal
import com.clarity.finance.model.GoalTransaction
import com.clarity.finance.model.GoalTransactionType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreGoalRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : GoalRepository {

    private fun requireUid(): String {
        return auth.currentUser?.uid
            ?: error("No authenticated user")
    }

    private fun goalDocument() =
        firestore
            .collection(FirestorePaths.USERS)
            .document(requireUid())
            .collection(FirestorePaths.GOALS)
            .document(FirestorePaths.PRIMARY_GOAL_ID)

    private fun transactionsCollection() =
        goalDocument().collection("transactions")

    override fun observeGoal(): Flow<Goal?> = callbackFlow {

        val uid = auth.currentUser?.uid

        if (uid == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = firestore
            .collection(FirestorePaths.USERS)
            .document(uid)
            .collection(FirestorePaths.GOALS)
            .document(FirestorePaths.PRIMARY_GOAL_ID)
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot == null || !snapshot.exists()) {
                    trySend(null)
                    return@addSnapshotListener
                }

                val goal = Goal(
                    id = snapshot.id,
                    name = snapshot.getString("name") ?: "MacBook Pro M4",
                    targetAmount = snapshot.getDouble("targetAmount") ?: 490000.0,
                    startingAmount = snapshot.getDouble("startingAmount") ?: 0.0,
                    deadline = snapshot.getString("deadline").orEmpty(),
                    createdAt = snapshot.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                    updatedAt = snapshot.getTimestamp("updatedAt")?.toDate()?.time ?: 0L
                )

                trySend(goal)
            }

        awaitClose {
            listener.remove()
        }
    }

    override fun observeTransactions(): Flow<List<GoalTransaction>> =
        callbackFlow {

            val uid = auth.currentUser?.uid

            if (uid == null) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }

            val listener = firestore
                .collection(FirestorePaths.USERS)
                .document(uid)
                .collection(FirestorePaths.GOALS)
                .document(FirestorePaths.PRIMARY_GOAL_ID)
                .collection("transactions")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->

                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }

                    val transactions = snapshot
                        ?.documents
                        ?.mapNotNull { document ->

                            val typeString =
                                document.getString("type")
                                    ?: return@mapNotNull null

                            val type = runCatching {
                                GoalTransactionType.valueOf(typeString)
                            }.getOrDefault(GoalTransactionType.DEPOSIT)

                            GoalTransaction(
                                id = document.id,
                                type = type,
                                amount = document.getDouble("amount") ?: 0.0,
                                note = document.getString("note").orEmpty(),
                                createdAt = document
                                    .getTimestamp("createdAt")
                                    ?.toDate()
                                    ?.time
                                    ?: 0L
                            )
                        }
                        .orEmpty()

                    trySend(transactions)
                }

            awaitClose {
                listener.remove()
            }
        }

    override suspend fun updateGoal(
        name: String,
        targetAmount: Double,
        deadline: String
    ): Result<Unit> = runCatching {

        require(targetAmount > 0) {
            "Target amount must be greater than zero"
        }

        require(name.isNotBlank()) {
            "Goal name cannot be empty"
        }

        goalDocument()
            .update(
                mapOf(
                    "name" to name.trim(),
                    "targetAmount" to targetAmount,
                    "deadline" to deadline,
                    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
            )
            .await()

        Unit
    }

    override suspend fun deposit(
        amount: Double,
        note: String
    ): Result<Unit> = runCatching {

        require(amount > 0) {
            "Deposit amount must be greater than zero"
        }

        transactionsCollection()
            .add(
                mapOf(
                    "type" to GoalTransactionType.DEPOSIT.name,
                    "amount" to amount,
                    "note" to note.trim(),
                    "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
            )
            .await()

        Unit
    }

    override suspend fun withdraw(
        amount: Double,
        note: String
    ): Result<Unit> = runCatching {

        require(amount > 0) {
            "Withdrawal amount must be greater than zero"
        }

        transactionsCollection()
            .add(
                mapOf(
                    "type" to GoalTransactionType.WITHDRAWAL.name,
                    "amount" to amount,
                    "note" to note.trim(),
                    "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
            )
            .await()

        Unit
    }
}