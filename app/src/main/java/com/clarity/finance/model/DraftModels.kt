package com.clarity.finance.model

import java.util.UUID

enum class DraftTransactionType {
    EXPENSE,
    INCOME
}

enum class DraftStatus {
    PENDING,
    APPROVED,
    DISCARDED
}

data class DraftTransaction(
    val id: String = UUID.randomUUID().toString(),
    val type: DraftTransactionType,
    val amount: Double,
    val currency: String = "LKR",
    val sender: String, // e.g. "BOC", "COMBANK"
    val merchantOrTitle: String,
    val accountNumber: String? = null,
    val suggestedCategoryId: String = "FOOD_AND_COFFEE",
    val suggestedCategoryName: String = "Food & Coffee",
    val spendingType: SpendingType = SpendingType.DISCRETIONARY,
    val paymentMethod: PaymentMethod = PaymentMethod.CARD,
    val date: String, // YYYY-MM-DD
    val time: String? = null, // e.g. "01:56 PM"
    val rawMessage: String,
    val status: DraftStatus = DraftStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis()
)
