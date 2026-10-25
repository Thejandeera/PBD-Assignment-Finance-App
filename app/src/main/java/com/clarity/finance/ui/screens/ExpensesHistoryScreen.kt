package com.clarity.finance.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clarity.finance.model.Expense
import com.clarity.finance.model.SpendingType
import com.clarity.finance.ui.components.ActivityIndicator
import com.clarity.finance.ui.components.ClarityCard
import com.clarity.finance.ui.components.ClarityNavigationBar
import com.clarity.finance.ui.components.ClaritySearchField
import com.clarity.finance.ui.components.DeleteExpenseAlert
import com.clarity.finance.ui.components.DisplayDates
import com.clarity.finance.ui.components.EmptyStateView
import com.clarity.finance.ui.components.ExpenseDetailSheet
import com.clarity.finance.ui.components.ExpenseRow
import com.clarity.finance.ui.components.GroupedSection
import com.clarity.finance.ui.components.IconTile
import com.clarity.finance.ui.components.LargeTitleHeader
import com.clarity.finance.ui.components.LegendDot
import com.clarity.finance.ui.components.ListRow
import com.clarity.finance.ui.components.SectionHeaderLabel
import com.clarity.finance.ui.components.SegmentedControl
import com.clarity.finance.ui.components.StackedBar
import com.clarity.finance.ui.components.SwipeToDeleteContainer
import com.clarity.finance.ui.components.VerticalHairline
import com.clarity.finance.ui.components.isScrolledPast
import com.clarity.finance.ui.theme.ClarityTheme
import com.clarity.finance.ui.theme.ClarityType
import com.clarity.finance.util.CurrencyFormatter
import com.clarity.finance.viewmodel.AuthViewModel
import com.clarity.finance.viewmodel.DraftViewModel
import com.clarity.finance.viewmodel.ExpenseMonthlyMetrics
import com.clarity.finance.viewmodel.ExpenseViewModel

private val TypeFilters = listOf<SpendingType?>(null, SpendingType.COMMITTED, SpendingType.DISCRETIONARY)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpensesHistoryScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExpenseViewModel = hiltViewModel(),
    draftViewModel: DraftViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    onNavigateToDrafts: () -> Unit = {}
) {
    val colors = ClarityTheme.colors
    val historyState by viewModel.historyState.collectAsStateWithLifecycle()
    val draftCount by draftViewModel.pendingDraftCount.collectAsStateWithLifecycle()
    val currentCurrency by authViewModel.currencyFlow.collectAsStateWithLifecycle()
    val exchangeRate by authViewModel.exchangeRateFlow.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }
    var selectedExpense by remember { mutableStateOf<Expense?>(null) }

    val listState = rememberLazyListState()
    val isCollapsed = listState.isScrolledPast(40.dp)
    val monthTitle = DisplayDates.month(historyState.currentMonth)
    val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val groupedExpenses = remember(historyState.filteredExpenses) {
        historyState.filteredExpenses
            .groupBy { it.date }
            .toSortedMap(reverseOrder())
            .toList()
    }

    expenseToDelete?.let { expense ->
        DeleteExpenseAlert(
            expense = expense,
            currency = currentCurrency,
            exchangeRate = exchangeRate,
            onConfirm = {
                viewModel.deleteExpense(expense.id)
                expenseToDelete = null
            },
            onDismiss = { expenseToDelete = null }
        )
    }

    selectedExpense?.let { expense ->
        ExpenseDetailSheet(
            expense = expense,
            currency = currentCurrency,
            exchangeRate = exchangeRate,
            onDismiss = { selectedExpense = null },
            onDelete = { viewModel.deleteExpense(expense.id) }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            ClarityNavigationBar(
                title = "Expenses",
                collapsed = isCollapsed,
                onBack = onNavigateBack,
                backLabel = "Home"
            )
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .imePadding(),
            contentPadding = PaddingValues(bottom = navigationBarPadding + 32.dp)
        ) {
            item(key = "title") {
                LargeTitleHeader(title = "Expenses", subtitle = monthTitle)
            }

            if (draftCount > 0) {
                item(key = "drafts") {
                    GroupedSection(modifier = Modifier.padding(bottom = 16.dp)) {
                        ListRow(
                            title = if (draftCount == 1) "1 bank SMS to review" else "$draftCount bank SMS to review",
                            subtitle = "Approve drafted transactions as expenses",
                            leading = { IconTile(icon = Icons.Rounded.Sms, background = colors.indigo) },
                            showChevron = true,
                            onClick = onNavigateToDrafts
                        )
                    }
                }
            }

            item(key = "summary") {
                MonthlySummaryCard(
                    metrics = historyState.metrics,
                    entryCount = historyState.expenses.size,
                    currency = currentCurrency,
                    exchangeRate = exchangeRate,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            item(key = "search") {
                ClaritySearchField(
                    query = historyState.searchQuery,
                    onQueryChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = "Search categories and notes",
                    onSearch = { focusManager.clearFocus() },
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp)
                )
            }

            item(key = "filter") {
                SegmentedControl(
                    options = listOf("All", "Committed", "Discretionary"),
                    selectedIndex = TypeFilters.indexOf(historyState.selectedTypeFilter).coerceAtLeast(0),
                    onSelect = { index -> viewModel.onFilterSelected(TypeFilters[index]) },
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)
                )
            }

            when {
                historyState.isLoading -> item(key = "loading") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ActivityIndicator(size = 28.dp)
                    }
                }

                historyState.errorMessage != null && historyState.expenses.isEmpty() -> item(key = "error") {
                    EmptyStateView(
                        icon = Icons.Rounded.CloudOff,
                        title = "Couldn't Load Expenses",
                        message = historyState.errorMessage.orEmpty()
                    )
                }

                groupedExpenses.isEmpty() -> item(key = "empty") {
                    val query = historyState.searchQuery
                    val filter = historyState.selectedTypeFilter
                    when {
                        query.isNotBlank() -> EmptyStateView(
                            icon = Icons.Rounded.Search,
                            title = "No Results for “$query”",
                            message = "Check the spelling or try a new search."
                        )
                        filter != null -> EmptyStateView(
                            icon = Icons.Rounded.Receipt,
                            title = "No ${filter.displayName} Expenses",
                            message = "${filter.displayName} expenses you log in $monthTitle will appear here."
                        )
                        else -> EmptyStateView(
                            icon = Icons.Rounded.Receipt,
                            title = "No Expenses Yet",
                            message = "Expenses you log in $monthTitle will appear here."
                        )
                    }
                }

                else -> groupedExpenses.forEach { (date, expenses) ->
                    item(key = "header-$date") {
                        DayHeader(
                            title = DisplayDates.sectionTitle(date),
                            total = CurrencyFormatter.format(expenses.sumOf { it.amount }, currentCurrency, exchangeRate),
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp)
                        )
                    }
                    itemsIndexed(items = expenses, key = { _, expense -> expense.id }) { index, expense ->
                        SwipeToDeleteContainer(
                            onDeleteRequest = { expenseToDelete = expense },
                            shape = groupedItemShape(index, expenses.size),
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .animateItemPlacement()
                        ) {
                            Box(modifier = Modifier.background(colors.card)) {
                                ExpenseRow(
                                    expense = expense,
                                    currency = currentCurrency,
                                    exchangeRate = exchangeRate,
                                    showDate = false,
                                    showDivider = index < expenses.lastIndex,
                                    onClick = { selectedExpense = expense },
                                    onDeleteRequest = { expenseToDelete = expense }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthlySummaryCard(
    metrics: ExpenseMonthlyMetrics,
    entryCount: Int,
    currency: String,
    exchangeRate: Double,
    modifier: Modifier = Modifier
) {
    val colors = ClarityTheme.colors
    ClarityCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.Top) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .semantics(mergeDescendants = true) {}
            ) {
                Text(
                    text = "Total Spent",
                    style = ClarityType.subheadline.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.secondaryLabel
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = CurrencyFormatter.format(metrics.totalExpense, currency, exchangeRate),
                    style = ClarityType.amountHero,
                    color = colors.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = if (entryCount == 1) "1 entry" else "$entryCount entries",
                style = ClarityType.footnote,
                color = colors.secondaryLabel,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        StackedBar(
            segments = listOf(
                metrics.committedTotal.toFloat() to colors.committed,
                metrics.discretionaryTotal.toFloat() to colors.discretionary
            )
        )
        Spacer(modifier = Modifier.height(14.dp))
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            SummaryStat(
                label = "Committed",
                amount = CurrencyFormatter.format(metrics.committedTotal, currency, exchangeRate),
                caption = "${metrics.committedPercentage}% of spending",
                color = colors.committed,
                modifier = Modifier.weight(1f)
            )
            VerticalHairline(modifier = Modifier.fillMaxHeight())
            SummaryStat(
                label = "Discretionary",
                amount = CurrencyFormatter.format(metrics.discretionaryTotal, currency, exchangeRate),
                caption = "${metrics.discretionaryPercentage}% of spending",
                color = colors.discretionary,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp)
            )
        }
    }
}

@Composable
private fun SummaryStat(
    label: String,
    amount: String,
    caption: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val colors = ClarityTheme.colors
    Column(modifier = modifier.semantics(mergeDescendants = true) {}) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LegendDot(color = color)
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = label, style = ClarityType.footnote, color = colors.secondaryLabel)
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = amount,
            style = ClarityType.amountCallout,
            color = colors.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(text = caption, style = ClarityType.footnote, color = colors.secondaryLabel)
    }
}

@Composable
private fun DayHeader(
    title: String,
    total: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom
    ) {
        SectionHeaderLabel(text = title, modifier = Modifier.weight(1f))
        Text(
            text = total,
            style = ClarityType.footnote,
            color = ClarityTheme.colors.secondaryLabel,
            modifier = Modifier.padding(end = 16.dp, bottom = 7.dp)
        )
    }
}

/** Rounds only the outer corners so consecutive rows read as one inset-grouped card. */
private fun groupedItemShape(index: Int, count: Int): Shape {
    val radius = 12.dp
    return when {
        count == 1 -> RoundedCornerShape(radius)
        index == 0 -> RoundedCornerShape(topStart = radius, topEnd = radius)
        index == count - 1 -> RoundedCornerShape(bottomStart = radius, bottomEnd = radius)
        else -> RectangleShape
    }
}
