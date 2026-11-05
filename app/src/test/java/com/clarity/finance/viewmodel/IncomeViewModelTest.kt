package com.clarity.finance.viewmodel

import com.clarity.finance.data.repository.FakeExchangeRateProvider
import com.clarity.finance.data.repository.FakeIncomeRepository
import com.clarity.finance.model.CurrencyType
import com.clarity.finance.model.Income
import com.clarity.finance.model.IncomeSource
import com.clarity.finance.model.IncomeStatus
import com.clarity.finance.model.toItem
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
        assertEquals(IncomeSource.SALARY.name, saved.sourceId)
        assertEquals("Salary", saved.sourceName)
        assertEquals(CurrencyType.LKR, saved.currency)
        assertEquals(1.0, saved.exchangeRate, 0.0)
        assertEquals(185000.0, saved.amountLKR, 0.001)
        assertEquals(IncomeStatus.RECEIVED, saved.status)
        assertEquals(saved.date.take(7), saved.month)
    }

    @Test
    fun selectingAdSense_defaultsToUsdWithLiveRate() {
        viewModel.onSourceSelected(IncomeSource.ADSENSE.toItem())

        val form = viewModel.formState.value
        assertEquals(CurrencyType.USD, form.currency)
        assertEquals("300.00", form.rateInput)
    }

    @Test
    fun foreignIncome_keepsOriginalAmountAndStoresLkrEquivalent() {
        viewModel.onSourceSelected(IncomeSource.ADSENSE.toItem())
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
        viewModel.onSourceSelected(IncomeSource.ADSENSE.toItem())
        viewModel.onAmountChanged("50")
        viewModel.onRateChanged("")
        viewModel.submitIncome()

        assertNotNull(viewModel.formState.value.rateError)
        assertTrue(repository.incomesState.value.isEmpty())
    }

    @Test
    fun pendingStatus_isOnlyAvailableForFreelance() {
        viewModel.onSourceSelected(IncomeSource.SALARY.toItem())
        viewModel.onStatusSelected(IncomeStatus.PENDING)
        assertEquals(IncomeStatus.RECEIVED, viewModel.formState.value.status)

        viewModel.onSourceSelected(IncomeSource.FREELANCE.toItem())
        viewModel.onStatusSelected(IncomeStatus.PENDING)
        assertEquals(IncomeStatus.PENDING, viewModel.formState.value.status)

        // Switching away from freelance drops the pending status.
        viewModel.onSourceSelected(IncomeSource.CRYPTO.toItem())
        assertEquals(IncomeStatus.RECEIVED, viewModel.formState.value.status)
    }

    @Test
    fun pendingFreelance_requiresClientSoItCanBeFollowedUp() {
        viewModel.onSourceSelected(IncomeSource.FREELANCE.toItem())
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
        viewModel.onSourceSelected(IncomeSource.CRYPTO.toItem())
        viewModel.onAmountChanged("40")
        viewModel.onLossSelected(true)
        viewModel.submitIncome()

        val saved = repository.incomesState.value.single()
        assertEquals(-40.0, saved.amount, 0.0)
        assertEquals(-40.0 * 300.0, saved.amountLKR, 0.0)
    }

    @Test
    fun successfulSubmit_resetsTheForm() {
        viewModel.onSourceSelected(IncomeSource.FREELANCE.toItem())
        viewModel.onAmountChanged("15000")
        viewModel.submitIncome()

        val form = viewModel.formState.value
        assertEquals("", form.amountInput)
        assertEquals(IncomeSource.SALARY.name, form.source.id)
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

        val salary = state.sourceTotals.first { it.source.id == IncomeSource.SALARY.name }
        val freelance = state.sourceTotals.first { it.source.id == IncomeSource.FREELANCE.name }
        val crypto = state.sourceTotals.first { it.source.id == IncomeSource.CRYPTO.name }
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

    // -------------------------------------------------------------------------
    // Custom income sources
    // -------------------------------------------------------------------------

    @Test
    fun customSource_isCreatedSelectedAndUsedForNewIncome() = runTest {
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.sources.collect {} }

        assertNull(viewModel.addCustomSource("  Tutoring ", CurrencyType.USD))

        val created = repository.customSourcesState.value.single()
        assertEquals("Tutoring", created.name)
        assertTrue(created.isCustom)
        assertTrue(viewModel.sources.value.any { it.id == created.id })

        // The new source is selected and applies its usual currency.
        val form = viewModel.formState.value
        assertEquals(created.id, form.source.id)
        assertEquals(CurrencyType.USD, form.currency)

        viewModel.onAmountChanged("100")
        viewModel.submitIncome()

        val saved = repository.incomesState.value.single()
        assertEquals(created.id, saved.sourceId)
        assertEquals("Tutoring", saved.sourceName)
        assertTrue(saved.isCustomSource)

        job.cancel()
    }

    @Test
    fun duplicateOrBlankSourceNames_areRejected() = runTest {
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.sources.collect {} }

        assertNotNull(viewModel.addCustomSource("   ", CurrencyType.LKR))
        assertNotNull(viewModel.addCustomSource("salary", CurrencyType.LKR))
        assertNull(viewModel.addCustomSource("Rental", CurrencyType.LKR))
        assertNotNull(viewModel.addCustomSource("RENTAL ", CurrencyType.LKR))

        assertEquals(listOf("Rental"), repository.customSourcesState.value.map { it.name })
        job.cancel()
    }

    @Test
    fun customSources_canBeLoggedAsPending() = runTest {
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.sources.collect {} }
        viewModel.addCustomSource("Consulting", CurrencyType.LKR)

        viewModel.onStatusSelected(IncomeStatus.PENDING)
        viewModel.onProjectNameChanged("Acme Ltd")
        viewModel.onAmountChanged("45000")
        viewModel.submitIncome()

        val saved = repository.incomesState.value.single()
        assertEquals(IncomeStatus.PENDING, saved.status)
        assertEquals("Acme Ltd", saved.projectName)
        job.cancel()
    }

    // -------------------------------------------------------------------------
    // Editing saved income
    // -------------------------------------------------------------------------

    @Test
    fun startEditing_loadsEveryFieldIntoTheForm() {
        val saved = Income(
            id = "inv-7",
            sourceId = IncomeSource.FREELANCE.name,
            sourceName = "Freelance",
            sourceIcon = IncomeSource.FREELANCE.icon,
            amount = 180.5,
            currency = CurrencyType.USD,
            exchangeRate = 298.75,
            amountLKR = 180.5 * 298.75,
            status = IncomeStatus.PENDING,
            date = "$thisMonth-01",
            month = thisMonth,
            projectName = "Upwork",
            note = "Logo redesign",
            createdAt = 1234L
        )

        viewModel.startEditing(saved)

        val form = viewModel.formState.value
        assertTrue(form.isEditing)
        assertEquals(IncomeSource.FREELANCE.name, form.source.id)
        assertEquals("180.5", form.amountInput)
        assertEquals(CurrencyType.USD, form.currency)
        assertEquals("298.75", form.rateInput)
        assertEquals(IncomeStatus.PENDING, form.status)
        assertEquals("$thisMonth-01", form.date)
        assertEquals("Upwork", form.projectName)
        assertEquals("Logo redesign", form.note)
    }

    @Test
    fun savingAnEdit_updatesTheSameEntryInPlace() {
        val original = income("e1", IncomeSource.SALARY, 150_000.0).copy(createdAt = 42L, note = "Base")
        repository.setIncomes(listOf(original))

        viewModel.startEditing(original)
        viewModel.onSourceSelected(IncomeSource.FREELANCE.toItem())
        viewModel.onAmountChanged("165000")
        viewModel.onNoteChanged("Base + bonus")
        viewModel.submitIncome()

        val updated = repository.incomesState.value.single()
        assertEquals("e1", updated.id)
        assertEquals(42L, updated.createdAt)
        assertEquals(IncomeSource.FREELANCE.name, updated.sourceId)
        assertEquals(165_000.0, updated.amountLKR, 0.001)
        assertEquals("Base + bonus", updated.note)
        assertEquals("Freelance updated", viewModel.userMessage.value)
        assertTrue(!viewModel.formState.value.isEditing)
    }

    @Test
    fun editingACryptoLoss_showsPositiveAmountWithLossSelected() {
        val loss = income("c1", IncomeSource.CRYPTO, -12_000.0)

        viewModel.startEditing(loss)

        val form = viewModel.formState.value
        assertEquals("12000", form.amountInput)
        assertTrue(form.isLoss)
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
        sourceId = source.name,
        sourceName = source.displayName,
        sourceIcon = source.icon,
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
