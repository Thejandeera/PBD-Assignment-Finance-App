package com.clarity.finance.viewmodel

import com.clarity.finance.data.repository.FakeExchangeRateProvider
import com.clarity.finance.data.repository.FakeIncomeRepository
import com.clarity.finance.model.CurrencyType
import com.clarity.finance.model.Income
import com.clarity.finance.model.IncomeSource
import com.clarity.finance.model.IncomeStatus
import com.clarity.finance.util.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalCoroutinesApi::class)
class IncomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeIncomeRepository
    private lateinit var viewModel: IncomeViewModel

    private val thisMonth = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))

    @Before
    fun setUp() {
        repository = FakeIncomeRepository()
        viewModel = IncomeViewModel(repository, FakeExchangeRateProvider(rate = 300.0))
    }

    // -------------------------------------------------------------------------
    // Form validation (FR-03, FR-04, FR-05)
    // -------------------------------------------------------------------------

    @Test
    fun zeroAmount_showsErrorAndSavesNothing() {
        viewModel.onAmountChanged("0")
        viewModel.submitIncome()

        assertEquals("Enter an amount greater than 0", viewModel.formState.value.amountError)
        assertTrue(repository.incomesState.value.isEmpty())
    }

    @Test
    fun futureDate_isRejected() {
        viewModel.onAmountChanged("1000")
        viewModel.onDateSelected(LocalDate.now().plusDays(3))
        viewModel.submitIncome()

        assertNotNull(viewModel.formState.value.dateError)
        assertTrue(repository.incomesState.value.isEmpty())
    }

    @Test
    fun salaryInLkr_savesWithRateOneAndMatchingLkrAmount() {
        viewModel.onAmountChanged("185000")
        viewModel.submitIncome()

        val saved = repository.incomesState.value.single()
        assertEquals(IncomeSource.SALARY, saved.source)
        assertEquals(CurrencyType.LKR, saved.currency)
        assertEquals(1.0, saved.exchangeRate, 0.0)
        assertEquals(185000.0, saved.amountLKR, 0.001)
        assertEquals(IncomeStatus.RECEIVED, saved.status)
        assertEquals(saved.date.take(7), saved.month)
    }

    @Test
    fun selectingAdSense_defaultsToUsdWithLiveRate() {
        viewModel.onSourceSelected(IncomeSource.ADSENSE)

        val form = viewModel.formState.value
        assertEquals(CurrencyType.USD, form.currency)
        assertEquals("300.00", form.rateInput)
    }

    @Test
    fun foreignIncome_keepsOriginalAmountAndStoresLkrEquivalent() {
        viewModel.onSourceSelected(IncomeSource.ADSENSE)
        viewModel.onAmountChanged("120.50")
        viewModel.onRateChanged("298.75")
        viewModel.submitIncome()

        val saved = repository.incomesState.value.single()
        assertEquals(120.50, saved.amount, 0.0)
        assertEquals(CurrencyType.USD, saved.currency)
        assertEquals(298.75, saved.exchangeRate, 0.0)
        assertEquals(120.50 * 298.75, saved.amountLKR, 0.0)
    }

    @Test
    fun foreignIncomeWithoutRate_showsRateError() {
        viewModel.onSourceSelected(IncomeSource.ADSENSE)
        viewModel.onAmountChanged("50")
        viewModel.onRateChanged("")
        viewModel.submitIncome()

        assertNotNull(viewModel.formState.value.rateError)
        assertTrue(repository.incomesState.value.isEmpty())
    }

    @Test
    fun pendingStatus_isOnlyAvailableForFreelance() {
        viewModel.onSourceSelected(IncomeSource.SALARY)
        viewModel.onStatusSelected(IncomeStatus.PENDING)
        assertEquals(IncomeStatus.RECEIVED, viewModel.formState.value.status)

        viewModel.onSourceSelected(IncomeSource.FREELANCE)
        viewModel.onStatusSelected(IncomeStatus.PENDING)
        assertEquals(IncomeStatus.PENDING, viewModel.formState.value.status)

        // Switching away from freelance drops the pending status.
        viewModel.onSourceSelected(IncomeSource.CRYPTO)
        assertEquals(IncomeStatus.RECEIVED, viewModel.formState.value.status)
    }

    @Test
    fun pendingFreelance_requiresClientSoItCanBeFollowedUp() {
        viewModel.onSourceSelected(IncomeSource.FREELANCE)
        viewModel.onStatusSelected(IncomeStatus.PENDING)
        viewModel.onAmountChanged("30000")
        viewModel.submitIncome()

        assertNotNull(viewModel.formState.value.projectError)
        assertTrue(repository.incomesState.value.isEmpty())

        viewModel.onProjectNameChanged("  DevContract  ")
        viewModel.submitIncome()

        val saved = repository.incomesState.value.single()
        assertEquals(IncomeStatus.PENDING, saved.status)
        assertEquals("DevContract", saved.projectName)
    }

    @Test
    fun cryptoLoss_isStoredAsNegativeAmount() {
        viewModel.onSourceSelected(IncomeSource.CRYPTO)
        viewModel.onAmountChanged("40")
        viewModel.onLossSelected(true)
        viewModel.submitIncome()

        val saved = repository.incomesState.value.single()
        assertEquals(-40.0, saved.amount, 0.0)
        assertEquals(-40.0 * 300.0, saved.amountLKR, 0.0)
    }

    @Test
    fun successfulSubmit_resetsTheForm() {
        viewModel.onSourceSelected(IncomeSource.FREELANCE)
        viewModel.onAmountChanged("15000")
        viewModel.submitIncome()

        val form = viewModel.formState.value
        assertEquals("", form.amountInput)
        assertEquals(IncomeSource.SALARY, form.source)
        assertNull(form.errorMessage)
    }

    // -------------------------------------------------------------------------
    // Monthly aggregation & pending tracking (FR-05, FR-06, FR-12)
    // -------------------------------------------------------------------------

    @Test
    fun monthlyTotals_countOnlyReceivedIncomeAndNetCryptoLosses() = runTest {
        repository.setIncomes(
            listOf(
                income("1", IncomeSource.SALARY, 150_000.0),
                income("2", IncomeSource.FREELANCE, 50_000.0),
                income("3", IncomeSource.CRYPTO, -10_000.0),
                income("4", IncomeSource.FREELANCE, 30_000.0, status = IncomeStatus.PENDING, project = "DevContract")
            )
        )
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        val state = viewModel.uiState.value
        assertEquals(190_000.0, state.receivedTotalLkr, 0.001)
        assertEquals(3, state.receivedIncomes.size)
        assertEquals(1, state.pendingIncomes.size)
        assertEquals(30_000.0, state.pendingTotalLkr, 0.001)

        val salary = state.sourceTotals.first { it.source == IncomeSource.SALARY }
        val freelance = state.sourceTotals.first { it.source == IncomeSource.FREELANCE }
        val crypto = state.sourceTotals.first { it.source == IncomeSource.CRYPTO }
        assertEquals(75, salary.percentage)
        assertEquals(25, freelance.percentage)
        assertEquals(0, crypto.percentage)
        assertEquals(-10_000.0, crypto.amountLkr, 0.001)

        job.cancel()
    }

    @Test
    fun pendingPaymentsFromEarlierMonths_areStillListed() = runTest {
        val lastMonth = YearMonth.now().minusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM"))
        repository.setIncomes(
            listOf(
                income("old", IncomeSource.FREELANCE, 54_000.0, status = IncomeStatus.PENDING, month = lastMonth, project = "Upwork")
            )
        )
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        val state = viewModel.uiState.value
        assertEquals(0.0, state.receivedTotalLkr, 0.0)
        assertEquals(listOf("old"), state.pendingIncomes.map { it.id })

        job.cancel()
    }

    @Test
    fun markAsReceived_movesPaymentIntoThisMonthsIncome() = runTest {
        val lastMonth = YearMonth.now().minusMonths(1).format(DateTimeFormatter.ofPattern("yyyy-MM"))
        val pending = income("p1", IncomeSource.FREELANCE, 30_000.0, status = IncomeStatus.PENDING, month = lastMonth, project = "DevContract")
        repository.setIncomes(listOf(pending))
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.markAsReceived(pending)

        val state = viewModel.uiState.value
        assertTrue(state.pendingIncomes.isEmpty())
        assertEquals(30_000.0, state.receivedTotalLkr, 0.001)
        val updated = repository.incomesState.value.single()
        assertEquals(IncomeStatus.RECEIVED, updated.status)
        assertEquals(LocalDate.now().toString(), updated.date)
        assertEquals("DevContract marked as received", viewModel.userMessage.value)

        job.cancel()
    }

    @Test
    fun cannotNavigatePastTheCurrentMonth() = runTest {
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.selectNextMonth()
        assertEquals(thisMonth, viewModel.uiState.value.currentMonth)

        viewModel.selectPreviousMonth()
        assertTrue(viewModel.uiState.value.canGoForward)

        job.cancel()
    }

    private fun income(
        id: String,
        source: IncomeSource,
        amountLkr: Double,
        status: IncomeStatus = IncomeStatus.RECEIVED,
        month: String = thisMonth,
        project: String = ""
    ) = Income(
        id = id,
        source = source,
        amount = amountLkr,
        currency = CurrencyType.LKR,
        exchangeRate = 1.0,
        amountLKR = amountLkr,
        status = status,
        date = "$month-01",
        month = month,
        projectName = project
    )
}
