package com.clarity.finance.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.LaptopMac
import androidx.compose.material.icons.rounded.Receipt
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clarity.finance.model.Expense
import com.clarity.finance.model.Income
import com.clarity.finance.ui.components.AddExpenseSheet
import com.clarity.finance.ui.components.CapsuleProgressBar
import com.clarity.finance.ui.components.ClarityBottomBar
import com.clarity.finance.ui.components.bottomPaddingBelowBump
import com.clarity.finance.ui.components.ClarityCard
import com.clarity.finance.ui.components.ClarityNavTab
import com.clarity.finance.ui.components.DisplayDates
import com.clarity.finance.ui.components.EmptyStateView
import com.clarity.finance.ui.components.ExpenseDetailSheet
import com.clarity.finance.ui.components.ExpenseRow
import com.clarity.finance.ui.components.GroupedSection
import com.clarity.finance.ui.components.Hairline
import com.clarity.finance.ui.components.IconTile
import com.clarity.finance.ui.components.InfoNote
import com.clarity.finance.ui.components.LargeTitleHeader
import com.clarity.finance.ui.components.LegendDot
import com.clarity.finance.ui.components.ListRow
import com.clarity.finance.ui.components.ProfileAvatar
import com.clarity.finance.ui.components.SectionTitle
import com.clarity.finance.ui.components.StackedBar
import com.clarity.finance.ui.components.VerticalHairline
import com.clarity.finance.ui.components.pressFade
import com.clarity.finance.ui.theme.ClarityTheme
import com.clarity.finance.ui.theme.ClarityType
import com.clarity.finance.util.CurrencyFormatter
import com.clarity.finance.util.ProfileImageManager
import com.clarity.finance.viewmodel.AuthViewModel
import com.clarity.finance.viewmodel.DraftViewModel
import com.clarity.finance.viewmodel.ExpenseMonthlyMetrics
import com.clarity.finance.viewmodel.ExpenseViewModel
import com.clarity.finance.viewmodel.IncomeViewModel
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Home tab. [onSignOutSuccess] is kept for API compatibility; signing out now lives in Settings.
 */
@Composable
fun DashboardScreen(
    authViewModel: AuthViewModel,
    @Suppress("UNUSED_PARAMETER") onSignOutSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    expenseViewModel: ExpenseViewModel = hiltViewModel(),
    draftViewModel: DraftViewModel = hiltViewModel(),
    incomeViewModel: IncomeViewModel = hiltViewModel(),
    onNavigateToExpensesHistory: () -> Unit = {},
    onNavigateToInsights: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToDrafts: () -> Unit = {},
    onNavigateToIncome: () -> Unit = {},
    onNavigateToGoal: () -> Unit = {}
) {
    val colors = ClarityTheme.colors
    val currentUser by authViewModel.currentUserFlow.collectAsStateWithLifecycle()
    val historyState by expenseViewModel.historyState.collectAsStateWithLifecycle()
    val draftCount by draftViewModel.pendingDraftCount.collectAsStateWithLifecycle()
    val currentCurrency by authViewModel.currencyFlow.collectAsStateWithLifecycle()
    val exchangeRate by authViewModel.exchangeRateFlow.collectAsStateWithLifecycle()
    val incomeState by incomeViewModel.uiState.collectAsStateWithLifecycle()

    var showAddExpenseSheet by remember { mutableStateOf(false) }
    var selectedExpense by remember { mutableStateOf<Expense?>(null) }

    val displayName = currentUser?.displayName?.ifBlank { "User" } ?: "User"
    val firstName = displayName.trim().substringBefore(' ').ifBlank { displayName }
    val greeting = "Hi, $firstName"

    val photoPath = authViewModel.profilePhotoPath
    val avatarBitmap = remember(photoPath) { ProfileImageManager.loadBitmap(photoPath) }

    val scrollState = rememberScrollState()
    // Shows a divider under the pinned balance card once content scrolls beneath it.
    val isScrolled by remember { derivedStateOf { scrollState.value > 0 } }

    if (showAddExpenseSheet) {
        AddExpenseSheet(
            viewModel = expenseViewModel,
            onDismiss = { showAddExpenseSheet = false },
            onNavigateToDrafts = {
                showAddExpenseSheet = false
                onNavigateToDrafts()
            },
            pendingDraftCount = draftCount
        )
    }

    selectedExpense?.let { expense ->
        ExpenseDetailSheet(
            expense = expense,
            currency = currentCurrency,
            exchangeRate = exchangeRate,
            onDismiss = { selectedExpense = null },
            onDelete = { expenseViewModel.deleteExpense(expense.id) }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            ClarityBottomBar(
                currentTab = ClarityNavTab.HOME,
                onAddClick = { showAddExpenseSheet = true },
                onTabSelected = { tab ->
                    when (tab) {
                        ClarityNavTab.HOME -> Unit
                        ClarityNavTab.INCOME -> onNavigateToIncome()
                        ClarityNavTab.INSIGHTS -> onNavigateToInsights()
                        ClarityNavTab.GOAL -> onNavigateToGoal()
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.bottomPaddingBelowBump())
        ) {
            // Pinned: the greeting and the balance card stay put while the rest scrolls.
            Column(modifier = Modifier.background(colors.background)) {
                Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

                LargeTitleHeader(
                    title = greeting,
                    eyebrow = DisplayDates.todayEyebrow(),
                    trailing = {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .pressFade(onClickLabel = "Open settings", onClick = onNavigateToSettings),
                            contentAlignment = Alignment.Center
                        ) {
                            ProfileAvatar(
                                bitmap = avatarBitmap,
                                name = displayName,
                                size = 36.dp,
                                contentDescription = "Profile and settings"
                            )
                        }
                    }
                )

                BalanceCard(
                    income = incomeState.receivedTotalLkr,
                    expenses = historyState.metrics.totalExpense,
                    currency = currentCurrency,
                    exchangeRate = exchangeRate,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))
                Hairline(color = if (isScrolled) colors.separator else Color.Transparent)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState)
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                AnimatedVisibility(visible = draftCount > 0) {
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

                SavingsGoalCard(
                    currency = currentCurrency,
                    exchangeRate = exchangeRate,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                PendingPaymentsCard(
                    pending = incomeState.pendingIncomes,
                    pendingTotalLkr = incomeState.pendingTotalLkr,
                    currency = currentCurrency,
                    exchangeRate = exchangeRate,
                    onClick = onNavigateToIncome
                )

                Spacer(modifier = Modifier.height(16.dp))

                SpendingMixCard(
                    metrics = historyState.metrics,
                    currency = currentCurrency,
                    exchangeRate = exchangeRate,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                RecentTransactionsSection(
                    expenses = historyState.expenses,
                    currency = currentCurrency,
                    exchangeRate = exchangeRate,
                    onSeeAllClick = onNavigateToExpensesHistory,
                    onExpenseClick = { selectedExpense = it },
                    onAddClick = { showAddExpenseSheet = true }
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun BalanceCard(
    income: Double,
    expenses: Double,
    currency: String,
    exchangeRate: Double,
    modifier: Modifier = Modifier
) {
    val colors = ClarityTheme.colors
    val available = (income - expenses).coerceAtLeast(0.0)
    val spentRatio = if (income > 0.0) (expenses / income).toFloat() else 0f
    val spentPercent = (spentRatio * 100).roundToInt()
    val progressColor = when {
        spentRatio > 1f -> colors.red
        spentRatio >= 0.75f -> colors.orange
        else -> colors.green
    }

    ClarityCard(modifier = modifier) {
        Column(modifier = Modifier.semantics(mergeDescendants = true) {}) {
            Text(
                text = "Available to Spend",
                style = ClarityType.subheadline.copy(fontWeight = FontWeight.SemiBold),
                color = colors.secondaryLabel
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = CurrencyFormatter.format(available, currency, exchangeRate),
                style = ClarityType.amountHero,
                color = colors.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (income > 0.0) {
                Spacer(modifier = Modifier.height(12.dp))
                CapsuleProgressBar(progress = spentRatio, color = progressColor, height = 6.dp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (spentRatio > 1f) {
                        "Spending is above this month's income"
                    } else {
                        "$spentPercent% of this month's income spent"
                    },
                    style = ClarityType.footnote,
                    color = colors.secondaryLabel
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Hairline()
        Spacer(modifier = Modifier.height(14.dp))

        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            BalanceStat(
                label = "Income",
                amount = CurrencyFormatter.format(income, currency, exchangeRate),
                icon = Icons.Rounded.ArrowDownward,
                tint = colors.income,
                modifier = Modifier.weight(1f)
            )
            VerticalHairline(modifier = Modifier.fillMaxHeight())
            BalanceStat(
                label = "Expenses",
                amount = CurrencyFormatter.format(expenses, currency, exchangeRate),
                icon = Icons.Rounded.ArrowUpward,
                tint = colors.outflow,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp)
            )
        }
    }
}

@Composable
private fun BalanceStat(
    label: String,
    amount: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val colors = ClarityTheme.colors
    Row(
        modifier = modifier.semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = if (colors.isDark) 0.24f else 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = label, style = ClarityType.footnote, color = colors.secondaryLabel)
            Text(
                text = amount,
                style = ClarityType.amountCallout,
                color = colors.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SavingsGoalCard(
    currency: String,
    exchangeRate: Double,
    modifier: Modifier = Modifier
) {
    val colors = ClarityTheme.colors
    val savedInLkr = 11_200.0
    val targetInLkr = 490_000.0
    val monthlyTargetInLkr = 39_900.0
    val progress = (savedInLkr / targetInLkr).toFloat()

    ClarityCard(modifier = modifier) {
        Row(
            modifier = Modifier.semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconTile(icon = Icons.Rounded.LaptopMac, background = colors.goal, size = 40.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "MacBook Pro M4", style = ClarityType.headline, color = colors.label)
                Text(text = "Primary savings goal", style = ClarityType.subheadline, color = colors.secondaryLabel)
            }
            Text(
                text = String.format(Locale.US, "%.1f%%", progress * 100),
                style = ClarityType.headline,
                color = colors.purpleText
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
        CapsuleProgressBar(progress = progress, color = colors.goal, height = 8.dp)
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "${CurrencyFormatter.format(savedInLkr, currency, exchangeRate)} saved",
                style = ClarityType.footnote,
                color = colors.secondaryLabel,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "of ${CurrencyFormatter.format(targetInLkr, currency, exchangeRate)}",
                style = ClarityType.footnote,
                color = colors.secondaryLabel
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        InfoNote(
            text = "Save ${CurrencyFormatter.format(monthlyTargetInLkr, currency, exchangeRate)} a month to reach it by October 2027.",
            icon = Icons.Rounded.TrackChanges,
            iconTint = colors.goal
        )
    }
}

/** Outstanding freelance payments (FR-17), linking to the Income tab to follow them up. */
@Composable
private fun PendingPaymentsCard(
    pending: List<Income>,
    pendingTotalLkr: Double,
    currency: String,
    exchangeRate: Double,
    onClick: () -> Unit
) {
    val colors = ClarityTheme.colors
    val count = pending.size
    val subtitle = if (pending.isEmpty()) {
        "Freelance invoices waiting for payment appear here"
    } else {
        val names = pending.take(2).joinToString(" · ") { income ->
            "${income.title} ${CurrencyFormatter.formatOriginal(income.amount, income.currency.name)}"
        }
        if (count > 2) "$names · +${count - 2} more" else names
    }
    GroupedSection {
        ListRow(
            title = when (count) {
                0 -> "No Pending Payments"
                1 -> "1 Pending Payment"
                else -> "$count Pending Payments"
            },
            subtitle = subtitle,
            leading = { IconTile(icon = Icons.Rounded.Schedule, background = colors.teal) },
            trailing = if (count > 0) {
                {
                    Text(
                        text = CurrencyFormatter.format(pendingTotalLkr, currency, exchangeRate),
                        style = ClarityType.amountCallout,
                        color = colors.secondaryLabel,
                        maxLines = 1
                    )
                }
            } else {
                null
            },
            showChevron = true,
            onClick = onClick,
            onClickLabel = "Open income"
        )
    }
}

@Composable
private fun SpendingMixCard(
    metrics: ExpenseMonthlyMetrics,
    currency: String,
    exchangeRate: Double,
    modifier: Modifier = Modifier
) {
    val colors = ClarityTheme.colors
    val hasSpending = metrics.totalExpense > 0.0

    ClarityCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Spending Mix",
                style = ClarityType.headline,
                color = colors.label,
                modifier = Modifier.weight(1f)
            )
            Text(text = "Target 60 / 40", style = ClarityType.footnote, color = colors.secondaryLabel)
        }
        Spacer(modifier = Modifier.height(12.dp))
        StackedBar(
            segments = listOf(
                metrics.committedTotal.toFloat() to colors.committed,
                metrics.discretionaryTotal.toFloat() to colors.discretionary
            )
        )
        Spacer(modifier = Modifier.height(14.dp))
        if (hasSpending) {
            LegendRow(
                label = "Committed",
                percentage = metrics.committedPercentage,
                amount = CurrencyFormatter.format(metrics.committedTotal, currency, exchangeRate),
                color = colors.committed
            )
            Spacer(modifier = Modifier.height(8.dp))
            LegendRow(
                label = "Discretionary",
                percentage = metrics.discretionaryPercentage,
                amount = CurrencyFormatter.format(metrics.discretionaryTotal, currency, exchangeRate),
                color = colors.discretionary
            )
        } else {
            Text(
                text = "No spending recorded this month yet.",
                style = ClarityType.footnote,
                color = colors.secondaryLabel
            )
        }
    }
}

@Composable
internal fun LegendRow(
    label: String,
    percentage: Int,
    amount: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val colors = ClarityTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendDot(color = color, size = 10.dp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, style = ClarityType.subheadline, color = colors.label)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$percentage%",
            style = ClarityType.subheadline,
            color = colors.secondaryLabel,
            modifier = Modifier.weight(1f)
        )
        Text(text = amount, style = ClarityType.amountCallout, color = colors.label, maxLines = 1)
    }
}

@Composable
private fun RecentTransactionsSection(
    expenses: List<Expense>,
    currency: String,
    exchangeRate: Double,
    onSeeAllClick: () -> Unit,
    onExpenseClick: (Expense) -> Unit,
    onAddClick: () -> Unit
) {
    SectionTitle(
        title = "Recent Transactions",
        actionLabel = if (expenses.isNotEmpty()) "See All" else null,
        onAction = onSeeAllClick
    )
    Spacer(modifier = Modifier.height(4.dp))

    GroupedSection {
        if (expenses.isEmpty()) {
            EmptyStateView(
                icon = Icons.Rounded.Receipt,
                title = "No Expenses Yet",
                message = "Expenses you log this month will appear here.",
                actionLabel = "Add Expense",
                onAction = onAddClick
            )
        } else {
            val recent = expenses.take(4)
            recent.forEachIndexed { index, expense ->
                ExpenseRow(
                    expense = expense,
                    currency = currency,
                    exchangeRate = exchangeRate,
                    showDivider = index < recent.lastIndex,
                    onClick = { onExpenseClick(expense) }
                )
            }
        }
    }
}
