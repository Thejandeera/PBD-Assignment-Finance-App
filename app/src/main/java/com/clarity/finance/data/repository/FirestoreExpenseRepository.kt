package com.clarity.finance.data.repository

import com.clarity.finance.data.remote.FirestorePaths
import com.clarity.finance.data.remote.asFlow
import com.clarity.finance.model.CategoryItem
import com.clarity.finance.model.Expense
import com.clarity.finance.model.PaymentMethod
import com.clarity.finance.model.RecurringExpense
import com.clarity.finance.model.SpendingType
import com.clarity.finance.model.getDefaultCategories
import com.clarity.finance.util.DispatcherProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.YearMonth
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreExpenseRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val dispatchers: DispatcherProvider
) : ExpenseRepository {

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    private fun userDoc(): DocumentReference {
        val uid = currentUserId ?: throw IllegalStateException("User is not authenticated")
        return firestore.collection(FirestorePaths.USERS).document(uid)
    }

    // =========================================================================
    // Expense CRUD Operations
    // =========================================================================

    override fun observeExpensesForMonth(month: String): Flow<List<Expense>> {
        val uid = currentUserId ?: return flowOf(emptyList())
        val query = firestore.collection(FirestorePaths.USERS).document(uid)
            .collection(FirestorePaths.EXPENSES)
            .whereEqualTo("month", month)
            .orderBy("date", Query.Direction.DESCENDING)

        return query.asFlow { snapshot ->
            snapshot.documents.mapNotNull { it.toExpense() }
        }
    }

    override suspend fun addExpense(expense: Expense): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            val collection = userDoc().collection(FirestorePaths.EXPENSES)
            val id = expense.id.ifEmpty { collection.document().id }
            val resolvedMonth = if (expense.month.isNotEmpty()) expense.month else expense.date.take(7)
            val itemToSave = expense.copy(id = id, month = resolvedMonth)

            collection.document(id).set(itemToSave.toMap()).await()
            Unit
        }
    }

    override suspend fun updateExpense(expense: Expense): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            require(expense.id.isNotEmpty()) { "Expense ID cannot be empty for an update" }
            val resolvedMonth = if (expense.month.isNotEmpty()) expense.month else expense.date.take(7)
            val itemToSave = expense.copy(month = resolvedMonth)

            userDoc().collection(FirestorePaths.EXPENSES)
                .document(expense.id)
                .set(itemToSave.toMap())
                .await()
            Unit
        }
    }

    override suspend fun deleteExpense(expenseId: String): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            require(expenseId.isNotEmpty()) { "Expense ID cannot be empty" }
            userDoc().collection(FirestorePaths.EXPENSES)
                .document(expenseId)
                .delete()
                .await()
            Unit
        }
    }

    // =========================================================================
    // Recurring Expense Operations
    // =========================================================================

    override fun observeRecurringExpenses(): Flow<List<RecurringExpense>> {
        val uid = currentUserId ?: return flowOf(emptyList())
        val query = firestore.collection(FirestorePaths.USERS).document(uid)
            .collection(FirestorePaths.RECURRING)
            .orderBy("dayOfMonth", Query.Direction.ASCENDING)

        return query.asFlow { snapshot ->
            snapshot.documents.mapNotNull { it.toRecurringExpense() }
        }
    }

    override suspend fun saveRecurringExpense(recurring: RecurringExpense): Result<Unit> =
        withContext(dispatchers.io) {
            runCatching {
                val collection = userDoc().collection(FirestorePaths.RECURRING)
                val id = recurring.id.ifEmpty { collection.document().id }
                val itemToSave = recurring.copy(id = id)

                collection.document(id).set(itemToSave.toMap()).await()
                Unit
            }
        }

    override suspend fun deleteRecurringExpense(recurringId: String): Result<Unit> =
        withContext(dispatchers.io) {
            runCatching {
                require(recurringId.isNotEmpty()) { "Recurring ID cannot be empty" }
                userDoc().collection(FirestorePaths.RECURRING)
                    .document(recurringId)
                    .delete()
                    .await()
                Unit
            }
        }

    override suspend fun checkAndApplyRecurringExpenses(currentMonth: String): Result<Unit> =
        withContext(dispatchers.io) {
            runCatching {
                val user = userDoc()
                val recurringSnapshot = user.collection(FirestorePaths.RECURRING)
                    .whereEqualTo("active", true)
                    .get()
                    .await()

                val recurringRules = recurringSnapshot.documents.mapNotNull { it.toRecurringExpense() }
                if (recurringRules.isEmpty()) return@runCatching

                // Extract year and month for valid date clamping
                val parts = currentMonth.split("-")
                require(parts.size == 2) { "Invalid month format: $currentMonth. Expected YYYY-MM" }
                val year = parts[0].toInt()
                val month = parts[1].toInt()
                val daysInMonth = YearMonth.of(year, month).lengthOfMonth()

                val expensesCollection = user.collection(FirestorePaths.EXPENSES)
                val recurringCollection = user.collection(FirestorePaths.RECURRING)

                for (rule in recurringRules) {
                    if (rule.lastAppliedMonth == currentMonth) continue

                    // Check if an expense generated for this recurring rule already exists in current month
                    val existing = expensesCollection
                        .whereEqualTo("month", currentMonth)
                        .whereEqualTo("recurringId", rule.id)
                        .get()
                        .await()

                    if (!existing.isEmpty) {
                        // Mark as applied to keep state in sync
                        recurringCollection.document(rule.id).update("lastAppliedMonth", currentMonth).await()
                        continue
                    }

                    // Compute valid date within the month
                    val targetDay = rule.dayOfMonth.coerceIn(1, daysInMonth)
                    val formattedDate = "$currentMonth-${targetDay.toString().padStart(2, '0')}"
                    val newExpenseId = UUID.randomUUID().toString()

                    val generatedExpense = Expense(
                        id = newExpenseId,
                        amount = rule.amount,
                        categoryId = rule.categoryId,
                        categoryName = rule.categoryName,
                        categoryIcon = rule.categoryIcon,
                        type = rule.type,
                        paymentMethod = rule.paymentMethod,
                        date = formattedDate,
                        month = currentMonth,
                        note = rule.note ?: "Recurring: ${rule.categoryName}",
                        recurringId = rule.id,
                        createdAt = System.currentTimeMillis()
                    )

                    // Atomically add the expense and update the recurring rule's lastAppliedMonth
                    firestore.batch()
                        .set(expensesCollection.document(newExpenseId), generatedExpense.toMap())
                        .update(recurringCollection.document(rule.id), "lastAppliedMonth", currentMonth)
                        .commit()
                        .await()
                }
            }
        }

    // =========================================================================
    // Category Operations (Default + User Custom)
    // =========================================================================

    override fun observeCategories(): Flow<List<CategoryItem>> {
        val uid = currentUserId ?: return flowOf(getDefaultCategories())
        val query = firestore.collection(FirestorePaths.USERS).document(uid)
            .collection(FirestorePaths.CATEGORIES)

        return query.asFlow { snapshot ->
            val custom: List<CategoryItem> = snapshot.documents.mapNotNull { it.toCategoryItem() }
            getDefaultCategories() + custom
        }
    }

    override suspend fun addCustomCategory(category: CategoryItem): Result<Unit> =
        withContext(dispatchers.io) {
            runCatching {
                val collection = userDoc().collection(FirestorePaths.CATEGORIES)
                val id = category.id.ifEmpty { collection.document().id }
                val itemToSave = category.copy(id = id, isCustom = true)

                collection.document(id).set(itemToSave.toMap()).await()
                Unit
            }
        }

    override suspend fun deleteCustomCategory(categoryId: String): Result<Unit> =
        withContext(dispatchers.io) {
            runCatching {
                require(categoryId.isNotEmpty()) { "Category ID cannot be empty" }
                userDoc().collection(FirestorePaths.CATEGORIES)
                    .document(categoryId)
                    .delete()
                    .await()
                Unit
            }
        }

    // =========================================================================
    // Mappers & Serialization Helpers
    // =========================================================================

    private fun DocumentSnapshot.toExpense(): Expense? {
        if (!exists()) return null
        val typeStr = getString("type") ?: SpendingType.DISCRETIONARY.name
        val type = runCatching { SpendingType.valueOf(typeStr) }.getOrDefault(SpendingType.DISCRETIONARY)

        val paymentStr = getString("paymentMethod") ?: PaymentMethod.CARD.name
        val paymentMethod = runCatching { PaymentMethod.valueOf(paymentStr) }.getOrDefault(PaymentMethod.CARD)

        return Expense(
            id = id,
            amount = getDouble("amount") ?: 0.0,
            categoryId = getString("categoryId") ?: "OTHER",
            categoryName = getString("categoryName") ?: "Other",
            categoryIcon = getString("categoryIcon") ?: "📦",
            type = type,
            paymentMethod = paymentMethod,
            date = getString("date").orEmpty(),
            month = getString("month").orEmpty(),
            note = getString("note"),
            recurringId = getString("recurringId"),
            createdAt = getLong("createdAt") ?: System.currentTimeMillis()
        )
    }

    private fun Expense.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "amount" to amount,
        "categoryId" to categoryId,
        "categoryName" to categoryName,
        "categoryIcon" to categoryIcon,
        "type" to type.name,
        "paymentMethod" to paymentMethod.name,
        "date" to date,
        "month" to month,
        "note" to note,
        "recurringId" to recurringId,
        "createdAt" to createdAt
    )

    private fun DocumentSnapshot.toRecurringExpense(): RecurringExpense? {
        if (!exists()) return null
        val typeStr = getString("type") ?: SpendingType.COMMITTED.name
        val type = runCatching { SpendingType.valueOf(typeStr) }.getOrDefault(SpendingType.COMMITTED)

        val paymentStr = getString("paymentMethod") ?: PaymentMethod.CARD.name
        val paymentMethod = runCatching { PaymentMethod.valueOf(paymentStr) }.getOrDefault(PaymentMethod.CARD)

        return RecurringExpense(
            id = id,
            categoryId = getString("categoryId") ?: "OTHER",
            categoryName = getString("categoryName") ?: "Other",
            categoryIcon = getString("categoryIcon") ?: "📦",
            amount = getDouble("amount") ?: 0.0,
            dayOfMonth = getLong("dayOfMonth")?.toInt() ?: 1,
            type = type,
            paymentMethod = paymentMethod,
            note = getString("note"),
            active = getBoolean("active") ?: true,
            lastAppliedMonth = getString("lastAppliedMonth").orEmpty()
        )
    }

    private fun RecurringExpense.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "categoryId" to categoryId,
        "categoryName" to categoryName,
        "categoryIcon" to categoryIcon,
        "amount" to amount,
        "dayOfMonth" to dayOfMonth,
        "type" to type.name,
        "paymentMethod" to paymentMethod.name,
        "note" to note,
        "active" to active,
        "lastAppliedMonth" to lastAppliedMonth
    )

    private fun DocumentSnapshot.toCategoryItem(): CategoryItem? {
        if (!exists()) return null
        val typeStr = getString("defaultType") ?: SpendingType.DISCRETIONARY.name
        val defaultType = runCatching { SpendingType.valueOf(typeStr) }.getOrDefault(SpendingType.DISCRETIONARY)

        return CategoryItem(
            id = id,
            name = getString("name").orEmpty(),
            icon = getString("icon") ?: "🏷️",
            defaultType = defaultType,
            isCustom = getBoolean("isCustom") ?: true
        )
    }

    private fun CategoryItem.toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "name" to name,
        "icon" to icon,
        "defaultType" to defaultType.name,
        "isCustom" to isCustom
    )
}
