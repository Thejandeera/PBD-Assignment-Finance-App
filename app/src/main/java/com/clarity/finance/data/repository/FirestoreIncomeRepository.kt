package com.clarity.finance.data.repository

import android.util.Log
import com.clarity.finance.data.remote.FirestorePaths
import com.clarity.finance.data.remote.asFlow
import com.clarity.finance.model.CurrencyType
import com.clarity.finance.model.Income
import com.clarity.finance.model.RecurringIncome
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

    private fun recurringIncomesOrNull(): CollectionReference? {
        val uid = auth.currentUser?.uid ?: return null
        return firestore.collection(FirestorePaths.USERS).document(uid).collection(FirestorePaths.RECURRING)
    }

    private fun recurringIncomes(): CollectionReference =
        recurringIncomesOrNull() ?: throw IllegalStateException("User is not authenticated")

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

    override fun observeRecurringIncomes(): Flow<List<RecurringIncome>> {
        val collection = recurringIncomesOrNull() ?: return flowOf(emptyList())
        return collection
            .orderBy("dayOfMonth", Query.Direction.ASCENDING)
            .asFlow { snapshot -> snapshot.documents.mapNotNull { it.toRecurringIncome() } }
            .catch { error ->
                Log.e(TAG, "Recurring income stream error: ${error.message}", error)
                emit(emptyList())
            }
    }

    override suspend fun addRecurringIncome(recurring: RecurringIncome): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            val collection = recurringIncomes()
            val id = recurring.id.ifEmpty { collection.document().id }
            collection.document(id).set(recurring.copy(id = id).toMap()).await()
            processAutoCommitRecurringIncomes()
            Unit
        }
    }

    override suspend fun updateRecurringIncome(recurring: RecurringIncome): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            require(recurring.id.isNotEmpty()) { "ID cannot be empty" }
            recurringIncomes().document(recurring.id).set(recurring.toMap()).await()
            processAutoCommitRecurringIncomes()
            Unit
        }
    }

    override suspend fun deleteRecurringIncome(recurringId: String): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            require(recurringId.isNotEmpty()) { "ID cannot be empty" }
            recurringIncomes().document(recurringId).delete().await()
            Unit
        }
    }

    override suspend fun processAutoCommitRecurringIncomes(): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            val collection = recurringIncomesOrNull() ?: return@runCatching Unit
            val snapshot = collection.get().await()
            val recurringList = snapshot.documents.mapNotNull { it.toRecurringIncome() }
            val today = java.time.LocalDate.now()
            val currentMonth = java.time.YearMonth.now().toString()
            val daysInMonth = today.lengthOfMonth()

            for (rec in recurringList) {
                if (rec.lastCommittedMonth != currentMonth) {
                    val targetDay = rec.dayOfMonth.coerceIn(1, daysInMonth)
                    if (today.dayOfMonth >= targetDay) {
                        val incomeDate = java.time.LocalDate.of(today.year, today.month, targetDay).toString()
                        val autoIncome = Income(
                            id = "rec_${rec.id}_$currentMonth",
                            sourceId = rec.sourceId,
                            sourceName = rec.sourceName,
                            sourceIcon = rec.sourceIcon,
                            amount = rec.amount,
                            currency = rec.currency,
                            exchangeRate = 1.0,
                            amountLKR = rec.amount,
                            status = IncomeStatus.RECEIVED,
                            date = incomeDate,
                            month = currentMonth,
                            projectName = rec.name.ifBlank { rec.sourceName },
                            note = "Auto-committed monthly recurring income"
                        )
                        addIncome(autoIncome)
                        collection.document(rec.id).update("lastCommittedMonth", currentMonth).await()
                    }
                }
            }
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

    private fun RecurringIncome.toMap(): Map<String, Any?> = mapOf(
        "name" to name,
        "sourceId" to sourceId,
        "sourceName" to sourceName,
        "sourceIcon" to sourceIcon,
        "amount" to amount,
        "currency" to currency.name,
        "dayOfMonth" to dayOfMonth,
        "lastCommittedMonth" to lastCommittedMonth,
        "createdAt" to createdAt
    )

    private fun DocumentSnapshot.toRecurringIncome(): RecurringIncome? {
        if (!exists()) return null
        return RecurringIncome(
            id = id,
            name = getString("name").orEmpty(),
            sourceId = getString("sourceId") ?: IncomeSource.SALARY.name,
            sourceName = getString("sourceName") ?: IncomeSource.SALARY.displayName,
            sourceIcon = getString("sourceIcon") ?: IncomeSource.SALARY.icon,
            amount = getDouble("amount") ?: 0.0,
            currency = getString("currency")?.let { runCatching { CurrencyType.valueOf(it) }.getOrNull() } ?: CurrencyType.LKR,
            dayOfMonth = (getLong("dayOfMonth") ?: 1L).toInt(),
            lastCommittedMonth = getString("lastCommittedMonth").orEmpty(),
            createdAt = getLong("createdAt") ?: 0L
        )
    }

    private companion object {
        const val TAG = "FirestoreIncomeRepo"
    }
}
