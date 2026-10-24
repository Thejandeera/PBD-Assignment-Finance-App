package com.clarity.finance.viewmodel

import com.clarity.finance.data.repository.FakeExpenseRepository
import com.clarity.finance.model.Expense
import com.clarity.finance.model.PaymentMethod
import com.clarity.finance.model.SpendingType
import com.clarity.finance.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalCoroutinesApi::class)
class InsightsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeRepository: FakeExpenseRepository
    private lateinit var viewModel: InsightsViewModel

    @Before
    fun setUp() {
        fakeRepository = FakeExpenseRepository()
        viewModel = InsightsViewModel(fakeRepository)
    }

    @Test
    fun monthlyAggregation_correctlyCalculatesTotalsAndRatios() = runTest {
        val currentMonth = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
        val expenses = listOf(
            Expense(id = "1", amount = 30000.0, categoryId = "RENT", categoryName = "Rent", type = SpendingType.COMMITTED, month = currentMonth, date = "$currentMonth-01"),
            Expense(id = "2", amount = 10000.0, categoryId = "FOOD", categoryName = "Food & Coffee", type = SpendingType.DISCRETIONARY, month = currentMonth, date = "$currentMonth-02"),
            Expense(id = "3", amount = 10000.0, categoryId = "TRANSPORT", categoryName = "Transport", type = SpendingType.DISCRETIONARY, month = currentMonth, date = "$currentMonth-03")
        )
        fakeRepository.setExpenses(expenses)

        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        val state = viewModel.uiState.value
        assertEquals(50000.0, state.totalOutflow, 0.01)
        assertEquals(30000.0, state.committedAmount, 0.01)
        assertEquals(20000.0, state.discretionaryAmount, 0.01)
        assertEquals(60, state.committedPercentage)
        assertEquals(40, state.discretionaryPercentage)

        job.cancel()
    }

    @Test
    fun categoryBreakdown_correctlyRanksCategoriesByTotalSpending() = runTest {
        val currentMonth = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
        val expenses = listOf(
            Expense(id = "1", amount = 10000.0, categoryId = "TRANSPORT", categoryName = "Transport", month = currentMonth, date = "$currentMonth-01"),
            Expense(id = "2", amount = 35000.0, categoryId = "RENT", categoryName = "Rent", month = currentMonth, date = "$currentMonth-01"),
            Expense(id = "3", amount = 25000.0, categoryId = "FOOD", categoryName = "Food & Coffee", month = currentMonth, date = "$currentMonth-02")
        )
        fakeRepository.setExpenses(expenses)

        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        val categories = viewModel.uiState.value.topCategories
        assertEquals(3, categories.size)
        // Highest first: Rent (35k), Food (25k), Transport (10k)
        assertEquals("Rent", categories[0].categoryName)
        assertEquals(35000.0, categories[0].amount, 0.01)

        assertEquals("Food & Coffee", categories[1].categoryName)
        assertEquals(25000.0, categories[1].amount, 0.01)

        assertEquals("Transport", categories[2].categoryName)
        assertEquals(10000.0, categories[2].amount, 0.01)

        job.cancel()
    }

    @Test
    fun paymentChannelDistribution_groupsByCardCashOnline() = runTest {
        val currentMonth = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
        val expenses = listOf(
            Expense(id = "1", amount = 15000.0, paymentMethod = PaymentMethod.CARD, month = currentMonth, date = "$currentMonth-01"),
            Expense(id = "2", amount = 5000.0, paymentMethod = PaymentMethod.CARD, month = currentMonth, date = "$currentMonth-02"),
            Expense(id = "3", amount = 10000.0, paymentMethod = PaymentMethod.CASH, month = currentMonth, date = "$currentMonth-03")
        )
        fakeRepository.setExpenses(expenses)

        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        val channels = viewModel.uiState.value.channelBreakdown
        val card = channels.firstOrNull { it.channel == PaymentMethod.CARD }
        val cash = channels.firstOrNull { it.channel == PaymentMethod.CASH }

        assertNotNull(card)
        assertNotNull(cash)
        assertEquals(20000.0, card!!.amount, 0.01)
        assertEquals(10000.0, cash!!.amount, 0.01)

        job.cancel()
    }

    @Test
    fun dailyBurnRate_calculatesAverageBasedOnDaysElapsed() = runTest {
        val currentMonth = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
        val daysElapsed = LocalDate.now().dayOfMonth.coerceAtLeast(1)
        val expenses = listOf(
            Expense(id = "1", amount = (daysElapsed * 2000.0), month = currentMonth, date = "$currentMonth-01")
        )
        fakeRepository.setExpenses(expenses)

        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        val velocity = viewModel.uiState.value.velocity
        assertEquals(daysElapsed, velocity.daysElapsed)
        assertEquals(2000.0, velocity.dailyAverage, 0.1)

        job.cancel()
    }

    @Test
    fun monthEndProjection_accuratelyProjectsRemainingMonthSpend() = runTest {
        val currentMonth = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
        val now = LocalDate.now()
        val daysElapsed = now.dayOfMonth.coerceAtLeast(1)
        val totalDays = YearMonth.now().lengthOfMonth()

        val dailyBurn = 1500.0
        val totalSpent = dailyBurn * daysElapsed
        fakeRepository.setExpenses(listOf(Expense(id = "1", amount = totalSpent, month = currentMonth, date = "$currentMonth-01")))

        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        val velocity = viewModel.uiState.value.velocity
        val expectedProjected = dailyBurn * totalDays
        assertEquals(expectedProjected, velocity.projectedMonthEnd, 1.0)

        job.cancel()
    }

    @Test
    fun topSpendingLeak_identifiesHighestDiscretionaryCategory() = runTest {
        val currentMonth = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
        val expenses = listOf(
            Expense(id = "1", amount = 34000.0, categoryId = "RENT", categoryName = "Rent", type = SpendingType.COMMITTED, month = currentMonth, date = "$currentMonth-01"),
            Expense(id = "2", amount = 24000.0, categoryId = "FOOD", categoryName = "Food & Coffee", type = SpendingType.DISCRETIONARY, month = currentMonth, date = "$currentMonth-02"),
            Expense(id = "3", amount = 5000.0, categoryId = "TRANSPORT", categoryName = "Transport", type = SpendingType.DISCRETIONARY, month = currentMonth, date = "$currentMonth-03")
        )
        fakeRepository.setExpenses(expenses)

        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        val leak = viewModel.uiState.value.spendingLeak
        assertNotNull(leak)
        // Rent (34k) is committed, so highest discretionary is Food & Coffee (24k)
        assertEquals("Food & Coffee", leak!!.categoryName)
        assertEquals(24000.0, leak.amount, 0.01)

        job.cancel()
    }

    @Test
    fun spendingLeakRecommendation_calculates25PercentMacBookSavings() = runTest {
        val currentMonth = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
        val expenses = listOf(
            Expense(id = "1", amount = 24000.0, categoryId = "FOOD", categoryName = "Food & Coffee", type = SpendingType.DISCRETIONARY, month = currentMonth, date = "$currentMonth-02")
        )
        fakeRepository.setExpenses(expenses)

        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        val leak = viewModel.uiState.value.spendingLeak
        assertNotNull(leak)
        // 25% of 24,000 = 6,000
        assertEquals(6000.0, leak!!.potentialMonthlySavings, 0.01)
        assertTrue(leak.recommendationText.contains("MacBook Pro M4"))

        job.cancel()
    }

    @Test
    fun monthNavigation_movesBetweenMonthsAndCapsAtCurrentMonth() = runTest {
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }

        assertFalse(viewModel.uiState.value.canGoForward)

        // Go to previous month
        viewModel.selectPreviousMonth()
        assertTrue(viewModel.uiState.value.canGoForward)

        // Go back to current month
        viewModel.selectNextMonth()
        assertFalse(viewModel.uiState.value.canGoForward)

        job.cancel()
    }
}
