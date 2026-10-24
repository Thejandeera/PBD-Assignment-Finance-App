package com.clarity.finance.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clarity.finance.data.repository.ExpenseRepository
import com.clarity.finance.model.CategorySpending
import com.clarity.finance.model.InsightsUiState
import com.clarity.finance.model.PaymentChannelSpending
import com.clarity.finance.model.PaymentMethod
import com.clarity.finance.model.SpendingLeakAlert
import com.clarity.finance.model.SpendingType
import com.clarity.finance.model.VelocityMetrics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(YearMonth.now())

    val uiState: StateFlow<InsightsUiState> = _selectedMonth
        .flatMapLatest { yearMonth ->
            val monthKey = yearMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))
            val displayHeader = yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US))
            val isCurrentMonth = (yearMonth == YearMonth.now())
            val canForward = yearMonth.isBefore(YearMonth.now())

            expenseRepository.observeExpensesForMonth(monthKey).map { expenses ->
                val total = expenses.sumOf { it.amount }
                val committed = expenses.filter { it.type == SpendingType.COMMITTED }.sumOf { it.amount }
                val discretionary = expenses.filter { it.type == SpendingType.DISCRETIONARY }.sumOf { it.amount }

                val committedPct = if (total > 0) ((committed / total) * 100).roundToInt() else 0
                val discretionaryPct = if (total > 0) ((discretionary / total) * 100).roundToInt() else 0

                // 1. Group by category and rank descending
                val categories = expenses.groupBy { it.categoryId }.map { (_, group) ->
                    val sample = group.first()
                    val catAmount = group.sumOf { it.amount }
                    val catPct = if (total > 0) ((catAmount / total) * 100).roundToInt() else 0
                    CategorySpending(
                        categoryId = sample.categoryId,
                        categoryName = sample.categoryName,
                        categoryIcon = sample.categoryIcon.ifBlank { "🏷️" },
                        amount = catAmount,
                        percentageOfTotal = catPct,
                        type = sample.type
                    )
                }.sortedByDescending { it.amount }

                // 2. Group by payment channel
                val channels = PaymentMethod.values().map { method ->
                    val methodExpenses = expenses.filter { it.paymentMethod == method }
                    val methodAmount = methodExpenses.sumOf { it.amount }
                    val methodPct = if (total > 0) ((methodAmount / total) * 100).roundToInt() else 0
                    PaymentChannelSpending(
                        channel = method,
                        amount = methodAmount,
                        percentageOfTotal = methodPct
                    )
                }.filter { it.amount > 0 }

                // 3. Velocity and Month-End Projection
                val daysElapsed = if (isCurrentMonth) LocalDate.now().dayOfMonth.coerceAtLeast(1) else yearMonth.lengthOfMonth()
                val totalDays = yearMonth.lengthOfMonth()
                val dailyAverage = if (daysElapsed > 0) total / daysElapsed else 0.0
                val projectedMonthEnd = if (isCurrentMonth) dailyAverage * totalDays else total

                val velocity = VelocityMetrics(
                    dailyAverage = dailyAverage,
                    projectedMonthEnd = projectedMonthEnd,
                    daysElapsed = daysElapsed,
                    totalDaysInMonth = totalDays
                )

                // 4. Top Spending Leak & MacBook Goal Link
                val highestDiscretionary = categories.firstOrNull { it.type == SpendingType.DISCRETIONARY }
                val leakAlert = if (highestDiscretionary != null && highestDiscretionary.amount > 0) {
                    val discShare = if (discretionary > 0) {
                        ((highestDiscretionary.amount / discretionary) * 100).roundToInt()
                    } else 0
                    val potentialSavings = highestDiscretionary.amount * 0.25
                    val formattedSavings = NumberFormat.getNumberInstance(Locale.US).format(potentialSavings.roundToInt())
                    SpendingLeakAlert(
                        categoryName = highestDiscretionary.categoryName,
                        categoryIcon = highestDiscretionary.categoryIcon,
                        amount = highestDiscretionary.amount,
                        percentageOfDiscretionary = discShare,
                        potentialMonthlySavings = potentialSavings,
                        recommendationText = "Cutting this by 25% frees up LKR $formattedSavings/mo for your MacBook Pro M4 fund!"
                    )
                } else null

                InsightsUiState(
                    isLoading = false,
                    currentMonth = monthKey,
                    displayMonthHeader = displayHeader,
                    canGoForward = canForward,
                    totalOutflow = total,
                    committedAmount = committed,
                    committedPercentage = committedPct,
                    discretionaryAmount = discretionary,
                    discretionaryPercentage = discretionaryPct,
                    topCategories = categories,
                    channelBreakdown = channels,
                    velocity = velocity,
                    spendingLeak = leakAlert,
                    errorMessage = null
                )
            }
        }
        .catch { error ->
            emit(InsightsUiState(isLoading = false, errorMessage = error.message))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = InsightsUiState(isLoading = true)
        )

    fun selectPreviousMonth() {
        _selectedMonth.value = _selectedMonth.value.minusMonths(1)
    }

    fun selectNextMonth() {
        if (_selectedMonth.value.isBefore(YearMonth.now())) {
            _selectedMonth.value = _selectedMonth.value.plusMonths(1)
        }
    }
}
