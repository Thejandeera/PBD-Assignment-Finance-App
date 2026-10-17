package com.clarity.finance.model

/**
 * Represents a spending category (either built-in default or user-defined custom).
 */
data class CategoryItem(
    val id: String = "",
    val name: String = "",
    val icon: String = "🏷️",
    val defaultType: SpendingType = SpendingType.DISCRETIONARY,
    val isCustom: Boolean = false
)

/**
 * Returns the default system categories tailored to Kavindu's financial lifestyle.
 */
fun getDefaultCategories(): List<CategoryItem> = ExpenseCategory.values().map {
    CategoryItem(
        id = it.name,
        name = it.title,
        icon = it.icon,
        defaultType = it.defaultType,
        isCustom = false
    )
}

/**
 * Represents an individual logged expense.
 *
 * @property note Optional contextual note (nullable, e.g. "Java Lounge meeting" or null).
 */
data class Expense(
    val id: String = "",
    val amount: Double = 0.0,
    val categoryId: String = "OTHER",
    val categoryName: String = "Other",
    val categoryIcon: String = "📦",
    val type: SpendingType = SpendingType.DISCRETIONARY,
    val paymentMethod: PaymentMethod = PaymentMethod.CARD,
    val date: String = "", // "YYYY-MM-DD"
    val month: String = "", // "YYYY-MM"
    val note: String? = null, // Optional, can be null
    val recurringId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Represents a monthly recurring expense rule (e.g. Rent, Gym, Subscriptions).
 */
data class RecurringExpense(
    val id: String = "",
    val categoryId: String = "OTHER",
    val categoryName: String = "Other",
    val categoryIcon: String = "📦",
    val amount: Double = 0.0,
    val dayOfMonth: Int = 1,
    val type: SpendingType = SpendingType.COMMITTED,
    val paymentMethod: PaymentMethod = PaymentMethod.CARD,
    val note: String? = null, // Optional, can be null
    val active: Boolean = true,
    val lastAppliedMonth: String = ""
)