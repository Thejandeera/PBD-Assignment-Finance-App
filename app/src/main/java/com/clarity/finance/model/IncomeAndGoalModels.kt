package com.clarity.finance.model

data class Income(
    val id: String = "",
    val source: IncomeSource = IncomeSource.SALARY,
    val amount: Double = 0.0,
    val currency: CurrencyType = CurrencyType.LKR,
    val exchangeRate: Double = 1.0,
    val amountLKR: Double = 0.0,
    val status: IncomeStatus = IncomeStatus.RECEIVED,
    val date: String = "",
    val month: String = "",
    val projectName: String = "",
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class Goal(
    val id: String = "primary_goal",
    val name: String = "MacBook Pro M4",
    val targetAmount: Double = 490000.0,
    val startingAmount: Double = 11200.0,
    val deadline: String = "2027-10-01",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

enum class GoalTransactionType {
    DEPOSIT,
    WITHDRAWAL
}

data class GoalTransaction(
    val id: String = "",
    val type: GoalTransactionType = GoalTransactionType.DEPOSIT,
    val amount: Double = 0.0,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)