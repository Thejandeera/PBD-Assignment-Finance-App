package com.clarity.finance.model

enum class SpendingType(val displayName: String) {
    COMMITTED("Committed"),
    DISCRETIONARY("Discretionary")
}

enum class PaymentMethod(val displayName: String) {
    CARD("Card"),
    CASH("Cash"),
    SECONDARY("Secondary acct"),
    ONLINE("Online Transfer")
}

enum class IncomeSource(val displayName: String, val icon: String) {
    SALARY("Salary", "💼"),
    FREELANCE("Freelance", "💻"),
    ADSENSE("AdSense", "📈"),
    CRYPTO("Crypto", "🪙")
}

enum class CurrencyType { LKR, USD, USDT }

enum class IncomeStatus { RECEIVED, PENDING }

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