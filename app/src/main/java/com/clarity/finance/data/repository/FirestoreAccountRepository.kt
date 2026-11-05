package com.clarity.finance.data.repository

import android.util.Log
import com.clarity.finance.data.remote.FirestorePaths
import com.clarity.finance.data.remote.asFlow
import com.clarity.finance.model.Account
import com.clarity.finance.model.AccountType
import com.clarity.finance.model.CurrencyType
import com.clarity.finance.util.DispatcherProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreAccountRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val dispatchers: DispatcherProvider
) : AccountRepository {

    private fun accountsOrNull(): CollectionReference? {
        val uid = auth.currentUser?.uid ?: return null
        return firestore.collection(FirestorePaths.USERS).document(uid)
            .collection(FirestorePaths.ACCOUNTS)
    }

    private fun accounts(): CollectionReference =
        accountsOrNull() ?: throw IllegalStateException("User is not authenticated")

    override fun observeAccounts(): Flow<List<Account>> {
        val collection = accountsOrNull() ?: return flowOf(emptyList())
        return collection
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .asFlow { snapshot -> snapshot.documents.mapNotNull { it.toAccount() } }
            .catch { error ->
                Log.e(TAG, "Accounts stream error: ${error.message}", error)
                emit(emptyList())
            }
    }

    override suspend fun getAccount(accountId: String): Account? = withContext(dispatchers.io) {
        if (accountId.isBlank()) return@withContext null
        runCatching {
            accountsOrNull()?.document(accountId)?.get()?.await()?.toAccount()
        }.getOrNull()
    }

    override suspend fun addAccount(account: Account): Result<String> = withContext(dispatchers.io) {
        runCatching {
            val collection = accounts()
            val id = account.id.ifBlank { collection.document().id }
            val now = System.currentTimeMillis()
            val toSave = account.copy(
                id = id,
                name = account.name.trim(),
                createdAt = if (account.createdAt > 0L) account.createdAt else now,
                updatedAt = now
            )
            collection.document(id).set(toSave.toMap()).await()
            id
        }
    }

    override suspend fun updateAccount(account: Account): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            require(account.id.isNotBlank()) { "Account ID cannot be empty" }
            val toSave = account.copy(
                name = account.name.trim(),
                updatedAt = System.currentTimeMillis()
            )
            accounts().document(account.id).set(toSave.toMap()).await()
            Unit
        }
    }

    override suspend fun deleteAccount(accountId: String): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            require(accountId.isNotBlank()) { "Account ID cannot be empty" }
            accounts().document(accountId).delete().await()
            Unit
        }
    }

    override suspend fun adjustBalance(
        accountId: String,
        deltaInAccountCurrency: Double
    ): Result<Unit> = withContext(dispatchers.io) {
        if (accountId.isBlank() || deltaInAccountCurrency == 0.0) return@withContext Result.success(Unit)
        runCatching {
            accounts().document(accountId).update(
                mapOf(
                    "currentBalance" to FieldValue.increment(deltaInAccountCurrency),
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()
            Unit
        }.recoverCatching { err ->
            // Deleted accounts should not block removing or editing historical entries.
            val code = (err as? FirebaseFirestoreException)?.code
            if (code == FirebaseFirestoreException.Code.NOT_FOUND) Unit else throw err
        }
    }

    private fun Account.toMap(): Map<String, Any> = mapOf(
        "name" to name,
        "type" to type.name,
        "currency" to currency.name,
        "currentBalance" to currentBalance,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt
    )

    private fun DocumentSnapshot.toAccount(): Account? {
        if (!exists()) return null
        val name = getString("name")?.takeIf { it.isNotBlank() } ?: return null
        val type = AccountType.fromId(getString("type"))
        val currency = getString("currency")
            ?.let { runCatching { CurrencyType.valueOf(it) }.getOrNull() }
            ?: CurrencyType.LKR
        return Account(
            id = id,
            name = name,
            type = type,
            currency = currency,
            currentBalance = getDouble("currentBalance") ?: 0.0,
            createdAt = getLong("createdAt") ?: 0L,
            updatedAt = getLong("updatedAt") ?: 0L
        )
    }

    private companion object {
        const val TAG = "FirestoreAccountRepo"
    }
}
