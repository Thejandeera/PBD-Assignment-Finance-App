package com.clarity.finance.model

enum class SpendingType(val displayName: String) {
    COMMITTED("Committed"),
    DISCRETIONARY("Discretionary")
}

enum class PaymentMethod(val displayName: String) {
    CARD("Card"),
    CASH("Cash"),
    SECONDARY("Digital"),
    ONLINE("Online Transfer")
}

/**
 * Kavindu's four built-in income streams. Users can add their own sources on top of these
 * (see IncomeSourceItem). [defaultCurrency] is pre-selected when the source is picked.
 */
enum class IncomeSource(
    val displayName: String,
    val icon: String,
    val defaultCurrency: CurrencyType
) {
    SALARY("Salary", "💼", CurrencyType.LKR),
    FREELANCE("Freelance", "💻", CurrencyType.LKR),
    ADSENSE("AdSense", "📈", CurrencyType.USD),
    CRYPTO("Crypto", "🪙", CurrencyType.USDT);

    companion object {
        fun fromId(id: String): IncomeSource? = entries.firstOrNull { it.name == id }
    }
}

/** USDT is pegged to the US dollar, so both convert with the LKR-per-USD rate. */
enum class CurrencyType { LKR, USD, USDT }

enum class IncomeStatus(val displayName: String) {
    RECEIVED("Received"),
    PENDING("Pending")
}

enum class ExpenseCategory(
    val title: String,
    val icon: String,
    val defaultType: SpendingType
) {
    RENT("Rent", "🏠", SpendingType.COMMITTED),
    UTILITIES("Utilities", "💡", SpendingType.COMMITTED),
    SUBSCRIPTIONS("Subscriptions", "🔁", SpendingType.COMMITTED),
    GYM("Gym", "🏋️", SpendingType.COMMITTED),
    TRANSPORT("Transport", "🚕", SpendingType.DISCRETIONARY),
    FOOD_AND_COFFEE("Food & Coffee", "☕", SpendingType.DISCRETIONARY),
    DELIVERY("Delivery", "🛵", SpendingType.DISCRETIONARY),
    SHOPPING("Shopping", "🛍️", SpendingType.DISCRETIONARY),
    FUN("Fun", "🎮", SpendingType.DISCRETIONARY),
    OTHER("Other", "📦", SpendingType.DISCRETIONARY)
}