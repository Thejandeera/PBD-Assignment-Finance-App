package com.clarity.finance.viewmodel

import com.clarity.finance.model.CurrencyType
import com.clarity.finance.model.Income
import com.clarity.finance.model.IncomeSource
import com.clarity.finance.model.IncomeSourceItem
import com.clarity.finance.model.IncomeStatus
import com.clarity.finance.model.toItem
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * State of the Add / Edit Income sheet (wireframe 12.2). Amounts are typed as positive
 * numbers; a crypto loss is expressed with [isLoss] instead of a minus sign.
 */
data class IncomeFormState(
    val source: IncomeSourceItem = IncomeSource.SALARY.toItem(),
    val currency: CurrencyType = IncomeSource.SALARY.defaultCurrency,
    val amountInput: String = "",
    val isLoss: Boolean = false,
    /** LKR per one unit of [currency]; unused (blank) for LKR. */
    val rateInput: String = "",
    val status: IncomeStatus = IncomeStatus.RECEIVED,
    val date: String = LocalDate.now().toString(), // YYYY-MM-DD
    val projectName: String = "",
    val note: String = "",
    val amountError: String? = null,
    val rateError: String? = null,
    val dateError: String? = null,
    val projectError: String? = null,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    /** Set while an existing entry is being edited; null when adding a new one. */
    val editingIncomeId: String? = null,
    val editingCreatedAt: Long? = null
) {
    val isEditing: Boolean get() = editingIncomeId != null

    val isForeignCurrency: Boolean get() = currency != CurrencyType.LKR

    /** Signed LKR equivalent of the typed amount, or null until amount (and rate) are valid. */
    val amountLkrPreview: Double?
        get() {
            val amount = amountInput.toDoubleOrNull()?.takeIf { it > 0.0 } ?: return null
            val rate = if (isForeignCurrency) rateInput.toDoubleOrNull()?.takeIf { it > 0.0 } ?: return null else 1.0
            return (if (isLoss) -amount else amount) * rate
        }
}

/** One source's share of a month's received income. */
data class IncomeSourceTotal(
    val source: IncomeSourceItem,
    val amountLkr: Double,
    val entryCount: Int,
    /** Share of the month's positive income; 0 for a source that ended the month negative. */
    val percentage: Int
)

/** State of the Income tab (and the income figures on the dashboard). */
data class IncomeUiState(
    val isLoading: Boolean = false,
    val currentMonth: String = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM")),
    val canGoForward: Boolean = false,
    /** Received entries dated in [currentMonth], newest first. */
    val receivedIncomes: List<Income> = emptyList(),
    /** Actual income for the month: the net LKR total of received entries (FR-12). */
    val receivedTotalLkr: Double = 0.0,
    val sourceTotals: List<IncomeSourceTotal> = emptyList(),
    /** Outstanding freelance payments from every month, oldest first (FR-05, FR-06). */
    val pendingIncomes: List<Income> = emptyList(),
    val pendingTotalLkr: Double = 0.0,
    val recurringIncomes: List<com.clarity.finance.model.RecurringIncome> = emptyList(),
    val errorMessage: String? = null
)
