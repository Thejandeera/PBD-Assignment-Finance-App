package com.clarity.finance.model

import com.clarity.finance.util.CurrencyFormatter

/**
 * Supported financial account categories.
 */
enum class AccountType(
    val displayName: String,
    val icon: String
) {
    BANK("Bank", "🏦"),
    CASH("Cash", "💵"),
    CRYPTO("Crypto", "🪙"),
    SAVINGS("Savings", "🐷"),
    INVESTMENTS("Investments", "📈"),
    OTHERS("Others", "🗂️");

    companion object {
        fun fromId(id: String?): AccountType =
            entries.firstOrNull { it.name == id } ?: OTHERS
    }
}

/** Account balances are stored in either LKR or USD. */
val ACCOUNT_CURRENCIES = listOf(CurrencyType.LKR, CurrencyType.USD)

/**
 * A user-owned financial account (bank account, cash wallet, crypto wallet, savings, etc.)
 * stored under users/{uid}/accounts/{accountId}.
 */
data class Account(
    val id: String = "",
    val name: String = "",
    val type: AccountType = AccountType.BANK,
    val currency: CurrencyType = CurrencyType.LKR,
    val currentBalance: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Shared balance-delta math for expenses (stored in LKR) and incomes (LKR/USD/USDT)
 * against an account in LKR or USD.
 */
object AccountBalanceCalculator {
    fun resolveDefaultAccount(accounts: List<Account>, primaryAccountId: String?): Account? =
        accounts.firstOrNull { it.id == primaryAccountId } ?: accounts.firstOrNull()

    private fun safeRate(lkrPerUsd: Double): Double =
        if (lkrPerUsd > 0.0) lkrPerUsd else CurrencyFormatter.FALLBACK_LKR_PER_USD

    fun toLkr(amount: Double, currency: CurrencyType, lkrPerUsd: Double): Double = when (currency) {
        CurrencyType.LKR -> amount
        CurrencyType.USD, CurrencyType.USDT -> amount * safeRate(lkrPerUsd)
    }

    fun expenseDelta(amountLkr: Double, accountCurrency: CurrencyType, lkrPerUsd: Double): Double = when (accountCurrency) {
        CurrencyType.LKR -> -amountLkr
        CurrencyType.USD, CurrencyType.USDT -> -(amountLkr / safeRate(lkrPerUsd))
    }

    fun incomeDelta(income: Income, accountCurrency: CurrencyType, lkrPerUsd: Double): Double {
        if (income.status != IncomeStatus.RECEIVED) return 0.0
        return when {
            income.currency == accountCurrency -> income.amount
            accountCurrency == CurrencyType.LKR -> income.amountLKR
            income.currency == CurrencyType.USDT && accountCurrency == CurrencyType.USD -> income.amount
            else -> income.amountLKR / safeRate(lkrPerUsd)
        }
    }
}
