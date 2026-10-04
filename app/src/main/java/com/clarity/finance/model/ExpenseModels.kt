package com.clarity.finance.model

data class Expense(
    val id: String = "",
    val amount: Double = 0.0,
    val category: ExpenseCategory = ExpenseCategory.OTHER,
    val type: SpendingType = SpendingType.DISCRETIONARY,
    val paymentMethod: PaymentMethod = PaymentMethod.CARD,
    val date: String = "", // "YYYY-MM-DD"
    val month: String = "", // "YYYY-MM"
    val note: String = "",
    val recurringId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class RecurringExpense(
    val id: String = "",
    val category: ExpenseCategory = ExpenseCategory.OTHER,
    val amount: Double = 0.0,
    val dayOfMonth: Int = 1,
    val type: SpendingType = SpendingType.COMMITTED,
    val paymentMethod: PaymentMethod = PaymentMethod.CARD,
    val note: String = "",
    val active: Boolean = true,
    val lastAppliedMonth: String = ""
)