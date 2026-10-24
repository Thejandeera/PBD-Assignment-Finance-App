package com.clarity.finance.viewmodel

import com.clarity.finance.data.repository.FakeExpenseRepository
import com.clarity.finance.model.Expense
import com.clarity.finance.model.PaymentMethod
import com.clarity.finance.model.SpendingType
import com.clarity.finance.model.getDefaultCategories
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
class ExpenseViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeRepository: FakeExpenseRepository
    private lateinit var viewModel: ExpenseViewModel

    @Before
    fun setUp() {
        fakeRepository = FakeExpenseRepository()
        viewModel = ExpenseViewModel(fakeRepository)
    }

    @Test
    fun whenAmountIsZeroOrNegative_validationErrorIsSet() = runTest {
        viewModel.onAmountChanged("0")
        viewModel.submitExpense()

        val state = viewModel.formState.value
        assertNotNull(state.amountError)
        assertEquals("Enter an amount greater than 0", state.amountError)
        assertFalse(state.isSuccess)
    }

    @Test
    fun whenDateIsInTheFuture_validationErrorIsSet() = runTest {
        viewModel.onAmountChanged("1500")
        val futureDate = LocalDate.now().plusDays(5).toString()
        viewModel.onDateChanged(futureDate)
        viewModel.submitExpense()

        val state = viewModel.formState.value
        assertNotNull(state.dateError)
        assertEquals("Date cannot be in the future", state.dateError)
        assertFalse(state.isSuccess)
    }

    @Test
    fun whenValidExpenseSubmitted_savesToRepositoryAndResetsForm() = runTest {
        viewModel.onAmountChanged("1500")
        val category = getDefaultCategories().first { it.id == "FOOD_AND_COFFEE" }
        viewModel.onCategorySelected(category)
        viewModel.onNoteChanged("Test coffee")

        viewModel.submitExpense()

        assertEquals(1, fakeRepository.expensesState.value.size)
        val saved = fakeRepository.expensesState.value.first()
        assertEquals(1500.0, saved.amount, 0.01)
        assertEquals("Food & Coffee", saved.categoryName)
        assertEquals("Test coffee", saved.note)
    }

    @Test
    fun quickIncrements_correctlyAccumulateToAmount() {
        viewModel.onAmountChanged("1000")
        viewModel.applyQuickIncrement(500)
        assertEquals("1500", viewModel.formState.value.amountInput)

        viewModel.applyQuickIncrement(1000)
        assertEquals("2500", viewModel.formState.value.amountInput)

        viewModel.applyQuickIncrement(5000)
        assertEquals("7500", viewModel.formState.value.amountInput)
    }

    @Test
    fun committedCategorySelection_autoSelectsCommittedType() {
        val rent = getDefaultCategories().first { it.id == "RENT" }
        viewModel.onCategorySelected(rent)
        assertEquals(SpendingType.COMMITTED, viewModel.formState.value.spendingType)

        val utilities = getDefaultCategories().first { it.id == "UTILITIES" }
        viewModel.onCategorySelected(utilities)
        assertEquals(SpendingType.COMMITTED, viewModel.formState.value.spendingType)
    }

    @Test
    fun discretionaryCategorySelection_autoSelectsDiscretionaryType() {
        val coffee = getDefaultCategories().first { it.id == "FOOD_AND_COFFEE" }
        viewModel.onCategorySelected(coffee)
        assertEquals(SpendingType.DISCRETIONARY, viewModel.formState.value.spendingType)

        val transport = getDefaultCategories().first { it.id == "TRANSPORT" }
        viewModel.onCategorySelected(transport)
        assertEquals(SpendingType.DISCRETIONARY, viewModel.formState.value.spendingType)
    }

    @Test
    fun manualSpendingTypeOverride_isPreserved() {
        val coffee = getDefaultCategories().first { it.id == "FOOD_AND_COFFEE" }
        viewModel.onCategorySelected(coffee)
        assertEquals(SpendingType.DISCRETIONARY, viewModel.formState.value.spendingType)

        // Manual override to Committed
        viewModel.onSpendingTypeSelected(SpendingType.COMMITTED)
        assertEquals(SpendingType.COMMITTED, viewModel.formState.value.spendingType)
    }

    @Test
    fun historyMetrics_calculatesCommittedAndDiscretionaryPercentagesCorrectly() = runTest {
        val currentMonth = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
        val expenses = listOf(
            Expense(
                id = "1",
                amount = 30000.0,
                categoryId = "RENT",
                categoryName = "Rent",
                type = SpendingType.COMMITTED,
                month = currentMonth,
                date = "$currentMonth-01"
            ),
            Expense(
                id = "2",
                amount = 20000.0,
                categoryId = "FOOD_AND_COFFEE",
                categoryName = "Food & Coffee",
                type = SpendingType.DISCRETIONARY,
                month = currentMonth,
                date = "$currentMonth-05"
            )
        )
        fakeRepository.setExpenses(expenses)

        // Start collecting historyState
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.historyState.collect()
        }

        val history = viewModel.historyState.value
        assertEquals(50000.0, history.metrics.totalExpense, 0.01)
        assertEquals(30000.0, history.metrics.committedTotal, 0.01)
        assertEquals(20000.0, history.metrics.discretionaryTotal, 0.01)
        assertEquals(60, history.metrics.committedPercentage)
        assertEquals(40, history.metrics.discretionaryPercentage)

        job.cancel()
    }

    @Test
    fun searchQuery_filtersExpensesByCategoryAndNote() = runTest {
        val currentMonth = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
        val expenses = listOf(
            Expense(
                id = "1",
                amount = 850.0,
                categoryId = "FOOD_AND_COFFEE",
                categoryName = "Food & Coffee",
                note = "Java Lounge meeting",
                month = currentMonth,
                date = "$currentMonth-02"
            ),
            Expense(
                id = "2",
                amount = 5800.0,
                categoryId = "UTILITIES",
                categoryName = "Utilities",
                note = "Dialog fiber internet",
                month = currentMonth,
                date = "$currentMonth-03"
            )
        )
        fakeRepository.setExpenses(expenses)

        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.historyState.collect()
        }

        viewModel.onSearchQueryChanged("Java")
        val filteredByNote = viewModel.historyState.value.filteredExpenses
        assertEquals(1, filteredByNote.size)
        assertEquals("Food & Coffee", filteredByNote.first().categoryName)

        viewModel.onSearchQueryChanged("Utilities")
        val filteredByCategory = viewModel.historyState.value.filteredExpenses
        assertEquals(1, filteredByCategory.size)
        assertEquals("Utilities", filteredByCategory.first().categoryName)

        job.cancel()
    }

    @Test
    fun deleteExpense_removesFromRepositoryAndRecomputesState() = runTest {
        val currentMonth = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
        val expense = Expense(
            id = "exp_delete_1",
            amount = 4500.0,
            categoryId = "DELIVERY",
            categoryName = "Delivery",
            month = currentMonth,
            date = "$currentMonth-10"
        )
        fakeRepository.setExpenses(listOf(expense))

        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.historyState.collect()
        }

        assertEquals(1, viewModel.historyState.value.expenses.size)

        viewModel.deleteExpense("exp_delete_1")

        assertTrue(fakeRepository.expensesState.value.isEmpty())

        job.cancel()
    }
}
