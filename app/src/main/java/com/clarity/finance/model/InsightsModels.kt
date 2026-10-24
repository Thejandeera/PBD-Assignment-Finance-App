package com.clarity.finance.model

/**
 * Aggregated spending breakdown for a specific category.
 */
data class CategorySpending(
    val categoryId: String,
    val categoryName: String,
    val categoryIcon: String,
    val amount: Double,
    val percentageOfTotal: Int,
    val type: SpendingType
)

/**
 * Aggregated spending distribution across payment channels (Card, Cash, Online).
 */
data class PaymentChannelSpending(
    val channel: PaymentMethod,
    val amount: Double,
    val percentageOfTotal: Int
)

/**
 * Daily velocity metrics and month-end projection to eliminate end-of-month shock.
 */
data class VelocityMetrics(
    val dailyAverage: Double = 0.0,
    val projectedMonthEnd: Double = 0.0,
    val daysElapsed: Int = 1,
    val totalDaysInMonth: Int = 30
)

/**
 * Detects the highest discretionary expenditure leak and ties it directly to Kavindu's MacBook Pro M4 goal.
 */
data class SpendingLeakAlert(
    val categoryName: String,
    val categoryIcon: String,
    val amount: Double,
    val percentageOfDiscretionary: Int,
    val potentialMonthlySavings: Double,
    val recommendationText: String
)

/**
 * UI State for the Insights Screen (Phase 4: Insights Engine).
 */
data class InsightsUiState(
    val isLoading: Boolean = false,
    val currentMonth: String = "", // e.g. "2026-10"
    val displayMonthHeader: String = "", // e.g. "October 2026"
    val canGoForward: Boolean = false,
    val totalOutflow: Double = 0.0,
    val committedAmount: Double = 0.0,
    val committedPercentage: Int = 0,
    val discretionaryAmount: Double = 0.0,
    val discretionaryPercentage: Int = 0,
    val topCategories: List<CategorySpending> = emptyList(),
    val channelBreakdown: List<PaymentChannelSpending> = emptyList(),
    val velocity: VelocityMetrics = VelocityMetrics(),
    val spendingLeak: SpendingLeakAlert? = null,
    val errorMessage: String? = null
)
