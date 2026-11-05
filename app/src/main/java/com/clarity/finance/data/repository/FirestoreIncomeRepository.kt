package com.clarity.finance.data.repository

import android.util.Log
import com.clarity.finance.data.remote.FirestorePaths
import com.clarity.finance.data.remote.asFlow
import com.clarity.finance.model.CurrencyType
import com.clarity.finance.model.Income
import com.clarity.finance.model.CUSTOM_INCOME_SOURCE_ICON
import com.clarity.finance.model.IncomeSource
import com.clarity.finance.model.IncomeSourceItem
import com.clarity.finance.model.IncomeStatus
import com.clarity.finance.model.getDefaultIncomeSources
import com.clarity.finance.util.DispatcherProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores income under users/{uid}/incomes. Documents are validated by isValidIncome() in
 * firestore.rules, so [toMap] writes exactly the fields that rule allows (the id is the
 * document key, not a field).
 */
@Singleton
class FirestoreIncomeRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val dispatchers: DispatcherProvider
) : IncomeRepository {

    private fun incomesOrNull(): CollectionReference? {
        val uid = auth.currentUser?.uid ?: return null
        return firestore.collection(FirestorePaths.USERS).document(uid).collection(FirestorePaths.INCOMES)
    }

    private fun incomes(): CollectionReference =
        incomesOrNull() ?: throw IllegalStateException("User is not authenticated")

    private fun incomeSourcesOrNull(): CollectionReference? {
        val uid = auth.currentUser?.uid ?: return null
        return firestore.collection(FirestorePaths.USERS).document(uid).collection(FirestorePaths.INCOME_SOURCES)
    }

    // Stream errors are rethrown so the view model can show a meaningful error state (FR-18).

    override fun observeIncomesForMonth(month: String): Flow<List<Income>> {
        val collection = incomesOrNull() ?: return flowOf(emptyList())
        return collection
            .whereEqualTo("month", month)
            .orderBy("date", Query.Direction.DESCENDING)
            .asFlow { snapshot -> snapshot.documents.mapNotNull { it.toIncome() } }
            .catch { error ->
                Log.e(TAG, "Income stream error: ${error.message}", error)
                throw error
            }
    }

    override fun observePendingIncomes(): Flow<List<Income>> {
        val collection = incomesOrNull() ?: return flowOf(emptyList())
        return collection
            .whereEqualTo("status", IncomeStatus.PENDING.name)
            .orderBy("date", Query.Direction.ASCENDING)
            .asFlow { snapshot -> snapshot.documents.mapNotNull { it.toIncome() } }
            .catch { error ->
                Log.e(TAG, "Pending income stream error: ${error.message}", error)
                throw error
            }
    }

    override suspend fun addIncome(income: Income): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            val collection = incomes()
            val id = income.id.ifEmpty { collection.document().id }
            collection.document(id).set(income.normalised().toMap()).await()
            Unit
        }
    }

    override suspend fun updateIncome(income: Income): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            require(income.id.isNotEmpty()) { "Income ID cannot be empty for an update" }
            incomes().document(income.id).set(income.normalised().toMap()).await()
            Unit
        }
    }

    override suspend fun deleteIncome(incomeId: String): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            require(incomeId.isNotEmpty()) { "Income ID cannot be empty" }
            incomes().document(incomeId).delete().await()
            Unit
        }
    }

    // =========================================================================
    // Income sources (built-in + user-defined)
    // =========================================================================

    override fun observeIncomeSources(): Flow<List<IncomeSourceItem>> {
        val collection = incomeSourcesOrNull() ?: return flowOf(getDefaultIncomeSources())
        return collection
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .asFlow { snapshot ->
                getDefaultIncomeSources() + snapshot.documents.mapNotNull { it.toIncomeSourceItem() }
            }
            .catch { error ->
                // Custom sources are optional; the built-ins keep income entry working.
                Log.w(TAG, "Income sources read failed (${error.message}); using built-in sources.")
                emit(getDefaultIncomeSources())
            }
    }

    override suspend fun addCustomIncomeSource(source: IncomeSourceItem): Result<Unit> =
        withContext(dispatchers.io) {
            runCatching {
                val collection = incomeSourcesOrNull() ?: throw IllegalStateException("User is not authenticated")
                val id = source.id.ifEmpty { collection.document().id }
                collection.document(id).set(
                    mapOf(
                        "name" to source.name,
                        "icon" to source.icon,
                        "defaultCurrency" to source.defaultCurrency.name,
                        "createdAt" to System.currentTimeMillis()
                    )
                ).await()
                Unit
            }
        }

    // =========================================================================
    // Mappers
    // =========================================================================

    /** Re-derives the fields the security rules cross-check, so a write can't be rejected. */
    private fun Income.normalised(): Income {
        val rate = if (currency == CurrencyType.LKR) 1.0 else exchangeRate
        return copy(
            exchangeRate = rate,
            amountLKR = amount * rate,
            month = date.take(7)
        )
    }

    private fun Income.toMap(): Map<String, Any> = mapOf(
        "source" to sourceId,
        "sourceName" to sourceName,
        "sourceIcon" to sourceIcon,
        "amount" to amount,
        "currency" to currency.name,
        "exchangeRate" to exchangeRate,
        "amountLKR" to amountLKR,
        "status" to status.name,
        "date" to date,
        "month" to month,
        "projectName" to projectName,
        "note" to note,
        "createdAt" to createdAt
    )

    private fun DocumentSnapshot.toIncome(): Income? {
        if (!exists()) return null
        val sourceId = getString("source")?.takeIf { it.isNotBlank() } ?: return null
        // Entries saved before custom sources existed only carry the built-in id.
        val builtIn = IncomeSource.fromId(sourceId)
        val currency = getString("currency")
            ?.let { runCatching { CurrencyType.valueOf(it) }.getOrNull() }
            ?: CurrencyType.LKR
        val status = getString("status")
            ?.let { runCatching { IncomeStatus.valueOf(it) }.getOrNull() }
            ?: IncomeStatus.RECEIVED
        val amount = getDouble("amount") ?: 0.0
        val rate = getDouble("exchangeRate") ?: 1.0

        return Income(
            id = id,
            sourceId = sourceId,
            sourceName = getString("sourceName")?.takeIf { it.isNotBlank() } ?: builtIn?.displayName ?: "Other",
            sourceIcon = getString("sourceIcon")?.takeIf { it.isNotBlank() } ?: builtIn?.icon ?: CUSTOM_INCOME_SOURCE_ICON,
            amount = amount,
            currency = currency,
            exchangeRate = rate,
            amountLKR = getDouble("amountLKR") ?: (amount * rate),
            status = status,
            date = getString("date").orEmpty(),
            month = getString("month").orEmpty(),
            projectName = getString("projectName").orEmpty(),
            note = getString("note").orEmpty(),
            createdAt = getLong("createdAt") ?: 0L
        )
    }

    private fun DocumentSnapshot.toIncomeSourceItem(): IncomeSourceItem? {
        if (!exists()) return null
        val name = getString("name")?.takeIf { it.isNotBlank() } ?: return null
        return IncomeSourceItem(
            id = id,
            name = name,
            icon = getString("icon")?.takeIf { it.isNotBlank() } ?: CUSTOM_INCOME_SOURCE_ICON,
            defaultCurrency = getString("defaultCurrency")
                ?.let { runCatching { CurrencyType.valueOf(it) }.getOrNull() }
                ?: CurrencyType.LKR,
            isCustom = true
        )
    }

    private companion object {
        const val TAG = "FirestoreIncomeRepo"
    }
}
