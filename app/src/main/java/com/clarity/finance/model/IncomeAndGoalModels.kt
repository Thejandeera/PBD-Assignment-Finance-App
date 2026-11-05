package com.clarity.finance.model

/**
 * An income source: one of the built-in [IncomeSource]s (id = enum name) or one the user
 * created (id = Firestore document id), mirroring how expenses use [CategoryItem].
 */
data class IncomeSourceItem(
    val id: String = "",
    val name: String = "",
    val icon: String = CUSTOM_INCOME_SOURCE_ICON,
    val defaultCurrency: CurrencyType = CurrencyType.LKR,
    val isCustom: Boolean = false
) {
    /**
     * Freelance and user-created sources may be invoiced before they're paid. Salary, AdSense
     * and crypto are recorded when received (mirrors firestore.rules).
     */
    val canBePending: Boolean get() = isCustom || id == IncomeSource.FREELANCE.name

    /** Crypto is recorded as a net position, so a month can end in a loss. */
    val allowsLoss: Boolean get() = id == IncomeSource.CRYPTO.name
}

const val CUSTOM_INCOME_SOURCE_ICON = "💰"

fun IncomeSource.toItem(): IncomeSourceItem = IncomeSourceItem(
    id = name,
    name = displayName,
    icon = icon,
    defaultCurrency = defaultCurrency,
    isCustom = false
)

/** The four built-in sources, in display order. */
fun getDefaultIncomeSources(): List<IncomeSourceItem> = IncomeSource.entries.map { it.toItem() }

/**
 * One income entry. The original [amount] and [currency] are kept alongside an [exchangeRate]
 * snapshot and the derived [amountLKR], so past months never change when the live rate moves.
 *
 * @property sourceId Built-in [IncomeSource] name or a custom source's document id.
 * @property sourceName / [sourceIcon] are copied from the source so entries still display if a
 *   custom source is later renamed or removed.
 * @property amount Value in [currency]. Negative only for a crypto net loss.
 * @property exchangeRate LKR per one unit of [currency]; always 1 for LKR.
 * @property amountLKR [amount] × [exchangeRate], used for every monthly total.
 * @property date "YYYY-MM-DD" — the day it was received, or the invoice date while pending.
 * @property month "YYYY-MM", denormalised from [date] for monthly queries.
 * @property projectName Client or project for invoiced work, so pending payments can be chased.
 */
data class Income(
    val id: String = "",
    val sourceId: String = IncomeSource.SALARY.name,
    val sourceName: String = IncomeSource.SALARY.displayName,
    val sourceIcon: String = IncomeSource.SALARY.icon,
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
) {
    val isPending: Boolean get() = status == IncomeStatus.PENDING

    val isForeignCurrency: Boolean get() = currency != CurrencyType.LKR

    val isCustomSource: Boolean get() = IncomeSource.fromId(sourceId) == null

    /** Row title: the client/project when known, otherwise the source name. */
    val title: String get() = projectName.ifBlank { sourceName }
}

/**
 * A monthly recurring income (e.g. Salary on the 7th of every month).
 * [dayOfMonth] is 1..31.
 * [lastCommittedMonth] tracks "YYYY-MM" to auto-commit on that day once per month.
 */
data class RecurringIncome(
    val id: String = "",
    val name: String = "",
    val sourceId: String = IncomeSource.SALARY.name,
    val sourceName: String = IncomeSource.SALARY.displayName,
    val sourceIcon: String = IncomeSource.SALARY.icon,
    val amount: Double = 0.0,
    val currency: CurrencyType = CurrencyType.LKR,
    val dayOfMonth: Int = 1,
    val lastCommittedMonth: String = "",
    val createdAt: Long = System.currentTimeMillis()
)


data class Goal(
    val id: String = "",
    val name: String = "",
    val targetAmount: Double = 0.0,
    val startingAmount: Double = 0.0,
    val deadline: String = "",
    val isPrimary: Boolean = false,
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