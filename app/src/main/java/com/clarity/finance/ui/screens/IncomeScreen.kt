package com.clarity.finance.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clarity.finance.model.Income
import com.clarity.finance.ui.components.ActivityIndicator
import com.clarity.finance.ui.components.AddExpenseSheet
import com.clarity.finance.ui.components.AddIncomeButton
import com.clarity.finance.ui.components.AddIncomeSheet
import com.clarity.finance.ui.components.ClarityBottomBar
import com.clarity.finance.ui.components.bottomPaddingBelowBump
import com.clarity.finance.ui.components.ClarityCard
import com.clarity.finance.ui.components.ClarityNavTab
import com.clarity.finance.ui.components.ClaritySnackbarHost
import com.clarity.finance.ui.components.CollapsingTitleBar
import com.clarity.finance.ui.components.DeleteIncomeAlert
import com.clarity.finance.ui.components.DisplayDates
import com.clarity.finance.ui.components.EmptyStateView
import com.clarity.finance.ui.components.GroupedSection
import com.clarity.finance.ui.components.IconTile
import com.clarity.finance.ui.components.IncomeDetailSheet
import com.clarity.finance.ui.components.IncomeRow
import com.clarity.finance.ui.components.LargeTitleHeader
import com.clarity.finance.ui.components.LegendDot
import com.clarity.finance.ui.components.ListRow
import com.clarity.finance.ui.components.SectionFooterLabel
import com.clarity.finance.ui.components.SectionTitle
import com.clarity.finance.ui.components.StackedBar
import com.clarity.finance.ui.components.SwipeToDeleteContainer
import com.clarity.finance.ui.components.incomeSourceColor
import com.clarity.finance.ui.components.isScrolledPast
import com.clarity.finance.ui.theme.ClarityTheme
import com.clarity.finance.ui.theme.ClarityType
import com.clarity.finance.util.CurrencyFormatter
import com.clarity.finance.viewmodel.AuthViewModel
import com.clarity.finance.viewmodel.ExpenseViewModel
import com.clarity.finance.viewmodel.IncomeSourceTotal
import com.clarity.finance.viewmodel.IncomeUiState
import com.clarity.finance.viewmodel.IncomeViewModel

/**
 * Income tab (FR-03 – FR-06, wireframe 12.2): the month's actual received income by source,
 * every outstanding freelance payment, and the received entries for the selected month.
 */
@Composable
fun IncomeScreen(
    onTabSelected: (ClarityNavTab) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: IncomeViewModel = hiltViewModel(),
    expenseViewModel: ExpenseViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val colors = ClarityTheme.colors
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()
    val currentCurrency by authViewModel.currencyFlow.collectAsStateWithLifecycle()
    val exchangeRate by authViewModel.exchangeRateFlow.collectAsStateWithLifecycle()

    var showAddIncomeSheet by remember { mutableStateOf(false) }
    var showAddExpenseSheet by remember { mutableStateOf(false) }
    var selectedIncome by remember { mutableStateOf<Income?>(null) }
    var incomeToDelete by remember { mutableStateOf<Income?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val scrollState = rememberScrollState()
    val isCollapsed = scrollState.isScrolledPast(52.dp)
    val monthTitle = DisplayDates.month(state.currentMonth)

    LaunchedEffect(userMessage) {
        userMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onUserMessageShown()
        }
    }

    if (showAddIncomeSheet) {
        AddIncomeSheet(
            viewModel = viewModel,
            onDismiss = { showAddIncomeSheet = false }
        )
    }

    if (showAddExpenseSheet) {
        AddExpenseSheet(
            viewModel = expenseViewModel,
            onDismiss = { showAddExpenseSheet = false }
        )
    }

    selectedIncome?.let { income ->
        IncomeDetailSheet(
            income = income,
            currency = currentCurrency,
            exchangeRate = exchangeRate,
            onDismiss = { selectedIncome = null },
            onMarkReceived = { viewModel.markAsReceived(income) },
            onDelete = { viewModel.deleteIncome(income) }
        )
    }

    incomeToDelete?.let { income ->
        DeleteIncomeAlert(
            income = income,
            onConfirm = {
                viewModel.deleteIncome(income)
                incomeToDelete = null
            },
            onDismiss = { incomeToDelete = null }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { ClaritySnackbarHost(snackbarHostState) },
        bottomBar = {
            ClarityBottomBar(
                currentTab = ClarityNavTab.INCOME,
                onTabSelected = onTabSelected,
                onAddClick = { showAddExpenseSheet = true }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.bottomPaddingBelowBump())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

                LargeTitleHeader(
                    title = "Income",
                    trailing = { AddIncomeButton(onClick = { showAddIncomeSheet = true }) }
                )

                MonthSwitcher(
                    title = monthTitle,
                    canGoForward = state.canGoForward,
                    onPrevious = { viewModel.selectPreviousMonth() },
                    onNext = { viewModel.selectNextMonth() },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                when {
                    state.isLoading -> Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ActivityIndicator(size = 28.dp)
                    }

                    state.errorMessage != null -> GroupedSection {
                        EmptyStateView(
                            icon = Icons.Rounded.CloudOff,
                            title = "Couldn't Load Income",
                            message = state.errorMessage.orEmpty()
                        )
                    }

                    else -> IncomeContent(
                        state = state,
                        monthTitle = monthTitle,
                        currency = currentCurrency,
                        exchangeRate = exchangeRate,
                        onAddClick = { showAddIncomeSheet = true },
                        onIncomeClick = { selectedIncome = it },
                        onDeleteRequest = { incomeToDelete = it }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }

            CollapsingTitleBar(title = "Income", collapsed = isCollapsed)
        }
    }
}

@Composable
private fun IncomeContent(
    state: IncomeUiState,
    monthTitle: String,
    currency: String,
    exchangeRate: Double,
    onAddClick: () -> Unit,
    onIncomeClick: (Income) -> Unit,
    onDeleteRequest: (Income) -> Unit
) {
    val colors = ClarityTheme.colors

    ReceivedSummaryCard(
        state = state,
        monthTitle = monthTitle,
        currency = currency,
        exchangeRate = exchangeRate,
        modifier = Modifier.padding(horizontal = 16.dp)
    )

    // Pending payments are listed from every month so none are forgotten.
    Spacer(modifier = Modifier.height(24.dp))
    SectionTitle(title = "Pending Payments")
    Spacer(modifier = Modifier.height(4.dp))
    if (state.pendingIncomes.isEmpty()) {
        GroupedSection {
            ListRow(
                title = "Nothing outstanding",
                subtitle = "Log a freelance invoice as Pending to follow it up here.",
                leading = { IconTile(icon = Icons.Rounded.CheckCircle, background = colors.green) }
            )
        }
    } else {
        IncomeList(
            incomes = state.pendingIncomes,
            currency = currency,
            exchangeRate = exchangeRate,
            onIncomeClick = onIncomeClick,
            onDeleteRequest = onDeleteRequest
        )
        SectionFooterLabel(
            text = "${CurrencyFormatter.format(state.pendingTotalLkr, currency, exchangeRate)} outstanding. " +
                "Tap a payment to mark it received.",
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }

    Spacer(modifier = Modifier.height(24.dp))
    SectionTitle(title = "Received")
    Spacer(modifier = Modifier.height(4.dp))
    if (state.receivedIncomes.isEmpty()) {
        GroupedSection {
            EmptyStateView(
                icon = Icons.Rounded.AccountBalanceWallet,
                title = "No Income in $monthTitle",
                message = "Add salary, freelance, AdSense or crypto income to see what you really earned.",
                actionLabel = "Add Income",
                onAction = onAddClick
            )
        }
    } else {
        IncomeList(
            incomes = state.receivedIncomes,
            currency = currency,
            exchangeRate = exchangeRate,
            onIncomeClick = onIncomeClick,
            onDeleteRequest = onDeleteRequest
        )
    }
}

@Composable
private fun IncomeList(
    incomes: List<Income>,
    currency: String,
    exchangeRate: Double,
    onIncomeClick: (Income) -> Unit,
    onDeleteRequest: (Income) -> Unit
) {
    val colors = ClarityTheme.colors
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        incomes.forEachIndexed { index, income ->
            SwipeToDeleteContainer(
                onDeleteRequest = { onDeleteRequest(income) },
                shape = groupedItemShape(index, incomes.size)
            ) {
                Box(modifier = Modifier.background(colors.card)) {
                    IncomeRow(
                        income = income,
                        currency = currency,
                        exchangeRate = exchangeRate,
                        showDivider = index < incomes.lastIndex,
                        onClick = { onIncomeClick(income) },
                        onDeleteRequest = { onDeleteRequest(income) }
                    )
                }
            }
        }
    }
}

/** The month's actual income (received only, net of crypto losses) split by source. */
@Composable
private fun ReceivedSummaryCard(
    state: IncomeUiState,
    monthTitle: String,
    currency: String,
    exchangeRate: Double,
    modifier: Modifier = Modifier
) {
    val colors = ClarityTheme.colors
    val entryCount = state.receivedIncomes.size

    ClarityCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.Top) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .semantics(mergeDescendants = true) {}
            ) {
                Text(
                    text = "Received in $monthTitle",
                    style = ClarityType.subheadline.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.secondaryLabel
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = CurrencyFormatter.format(state.receivedTotalLkr, currency, exchangeRate),
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
            segments = state.sourceTotals.map { total ->
                total.amountLkr.coerceAtLeast(0.0).toFloat() to incomeSourceColor(total.source)
            }
        )
        Spacer(modifier = Modifier.height(14.dp))

        if (state.sourceTotals.isEmpty()) {
            Text(
                text = "No income received this month yet.",
                style = ClarityType.footnote,
                color = colors.secondaryLabel
            )
        } else {
            state.sourceTotals.forEachIndexed { index, total ->
                if (index > 0) Spacer(modifier = Modifier.height(8.dp))
                SourceLegendRow(total = total, currency = currency, exchangeRate = exchangeRate)
            }
        }
    }
}

@Composable
private fun SourceLegendRow(
    total: IncomeSourceTotal,
    currency: String,
    exchangeRate: Double
) {
    val colors = ClarityTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendDot(color = incomeSourceColor(total.source), size = 10.dp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = total.source.displayName, style = ClarityType.subheadline, color = colors.label)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = if (total.amountLkr < 0) "Net loss" else "${total.percentage}%",
            style = ClarityType.subheadline,
            color = if (total.amountLkr < 0) colors.redText else colors.secondaryLabel,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = CurrencyFormatter.format(total.amountLkr, currency, exchangeRate),
            style = ClarityType.amountCallout,
            color = colors.label,
            maxLines = 1
        )
    }
}
