package com.clarity.finance.data.repository

import com.clarity.finance.data.remote.FirestorePaths
import com.clarity.finance.data.remote.asFlow
import com.clarity.finance.model.DraftStatus
import com.clarity.finance.model.DraftTransaction
import com.clarity.finance.model.DraftTransactionType
import com.clarity.finance.model.PaymentMethod
import com.clarity.finance.model.SpendingType
import com.clarity.finance.util.DispatcherProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreDraftRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val dispatchers: DispatcherProvider
) : DraftRepository {

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    private fun draftsCollection() =
        firestore.collection(FirestorePaths.USERS)
            .document(currentUserId ?: error("User not logged in"))
            .collection(FirestorePaths.DRAFTS)

    override val pendingDraftCount: Flow<Int> =
        observePendingDrafts().map { it.size }

    override fun observePendingDrafts(): Flow<List<DraftTransaction>> {
        val uid = currentUserId ?: return flowOf(emptyList())
        val query = firestore.collection(FirestorePaths.USERS).document(uid)
            .collection(FirestorePaths.DRAFTS)
            .whereEqualTo("status", DraftStatus.PENDING.name)
            .orderBy("createdAt", Query.Direction.DESCENDING)

        return query.asFlow { snapshot ->
            snapshot.documents.mapNotNull { it.toDraftTransaction() }
        }.catch { emit(emptyList()) }
    }

    override suspend fun saveDraft(draft: DraftTransaction): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            draftsCollection().document(draft.id).set(draft.toMap()).await()
            Unit
        }
    }

    override suspend fun saveDrafts(drafts: List<DraftTransaction>): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            if (drafts.isEmpty()) return@runCatching Unit
            val batch = firestore.batch()
            val collection = draftsCollection()
            drafts.forEach { draft ->
                batch.set(collection.document(draft.id), draft.toMap())
            }
            batch.commit().await()
            Unit
        }
    }

    override suspend fun discardDraft(draftId: String): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            draftsCollection().document(draftId).delete().await()
            Unit
        }
    }

    override suspend fun markDraftApproved(draftId: String): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            // Delete from drafts once approved, so it doesn't clutter drafts
            draftsCollection().document(draftId).delete().await()
            Unit
        }
    }

    override suspend fun clearAllDrafts(): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            val snapshot = draftsCollection().get().await()
            if (snapshot.isEmpty) return@runCatching Unit
            val batch = firestore.batch()
            snapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            batch.commit().await()
            Unit
        }
    }

    private fun DraftTransaction.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "type" to type.name,
        "amount" to amount,
        "currency" to currency,
        "sender" to sender,
        "merchantOrTitle" to merchantOrTitle,
        "accountNumber" to accountNumber,
        "suggestedCategoryId" to suggestedCategoryId,
        "suggestedCategoryName" to suggestedCategoryName,
        "spendingType" to spendingType.name,
        "paymentMethod" to paymentMethod.name,
        "date" to date,
        "time" to time,
        "rawMessage" to rawMessage,
        "status" to status.name,
        "createdAt" to createdAt
    )

    private fun DocumentSnapshot.toDraftTransaction(): DraftTransaction? {
        val id = getString("id") ?: id
        val typeStr = getString("type") ?: return null
        val amount = getDouble("amount") ?: return null
        val sender = getString("sender") ?: "BANK"
        val merchantOrTitle = getString("merchantOrTitle") ?: ""
        val categoryId = getString("suggestedCategoryId") ?: "OTHER"
        val categoryName = getString("suggestedCategoryName") ?: "Other"
        val spendingTypeStr = getString("spendingType") ?: SpendingType.DISCRETIONARY.name
        val paymentMethodStr = getString("paymentMethod") ?: PaymentMethod.CARD.name
        val date = getString("date") ?: ""
        val time = getString("time")
        val rawMessage = getString("rawMessage") ?: ""
        val statusStr = getString("status") ?: DraftStatus.PENDING.name
        val createdAt = getLong("createdAt") ?: System.currentTimeMillis()

        return DraftTransaction(
            id = id,
            type = runCatching { DraftTransactionType.valueOf(typeStr) }.getOrDefault(DraftTransactionType.EXPENSE),
            amount = amount,
            currency = getString("currency") ?: "LKR",
            sender = sender,
            merchantOrTitle = merchantOrTitle,
            accountNumber = getString("accountNumber"),
            suggestedCategoryId = categoryId,
            suggestedCategoryName = categoryName,
            spendingType = runCatching { SpendingType.valueOf(spendingTypeStr) }.getOrDefault(SpendingType.DISCRETIONARY),
            paymentMethod = runCatching { PaymentMethod.valueOf(paymentMethodStr) }.getOrDefault(PaymentMethod.CARD),
            date = date,
            time = time,
            rawMessage = rawMessage,
            status = runCatching { DraftStatus.valueOf(statusStr) }.getOrDefault(DraftStatus.PENDING),
            createdAt = createdAt
        )
    }
}
