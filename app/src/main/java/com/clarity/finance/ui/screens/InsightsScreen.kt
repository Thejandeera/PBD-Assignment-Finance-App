package com.clarity.finance.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.SwapHoriz
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clarity.finance.model.CategorySpending
import com.clarity.finance.model.PaymentChannelSpending
import com.clarity.finance.model.PaymentMethod
import com.clarity.finance.model.SpendingLeakAlert
import com.clarity.finance.model.VelocityMetrics
import com.clarity.finance.ui.components.ActivityIndicator
import com.clarity.finance.ui.components.AddExpenseButton
import com.clarity.finance.ui.components.AddExpenseSheet
import com.clarity.finance.ui.components.CapsuleProgressBar
import com.clarity.finance.ui.components.CircleIconButton
import com.clarity.finance.ui.components.ClarityBottomBar
import com.clarity.finance.ui.components.ClarityCard
import com.clarity.finance.ui.components.ClarityNavTab
import com.clarity.finance.ui.components.CollapsingTitleBar
import com.clarity.finance.ui.components.EmojiBadge
import com.clarity.finance.ui.components.EmptyStateView
import com.clarity.finance.ui.components.GroupedSection
import com.clarity.finance.ui.components.IconTile
import com.clarity.finance.ui.components.LargeTitleHeader
import com.clarity.finance.ui.components.ListRow
import com.clarity.finance.ui.components.SectionTitle
import com.clarity.finance.ui.components.StackedBar
import com.clarity.finance.ui.components.VerticalHairline
import com.clarity.finance.ui.components.isScrolledPast
import com.clarity.finance.ui.components.spendingTypeColor
import com.clarity.finance.ui.theme.ClarityTheme
import com.clarity.finance.ui.theme.ClarityType
import com.clarity.finance.util.CurrencyFormatter
import com.clarity.finance.viewmodel.AuthViewModel
import com.clarity.finance.viewmodel.ExpenseViewModel
import com.clarity.finance.viewmodel.InsightsViewModel

@Composable
fun InsightsScreen(
    onNavigateToDashboard: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InsightsViewModel = hiltViewModel(),
    expenseViewModel: ExpenseViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    onNavigateToIncome: () -> Unit = {},
    onNavigateToGoal: () -> Unit = {}
) {
    val colors = ClarityTheme.colors
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentCurrency by authViewModel.currencyFlow.collectAsStateWithLifecycle()
    val exchangeRate by authViewModel.exchangeRateFlow.collectAsStateWithLifecycle()
    var showAddExpenseSheet by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val isCollapsed = scrollState.isScrolledPast(52.dp)

    if (showAddExpenseSheet) {
        AddExpenseSheet(
            viewModel = expenseViewModel,
            onDismiss = { showAddExpenseSheet = false }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            ClarityBottomBar(
                currentTab = ClarityNavTab.INSIGHTS,
                onTabSelected = { tab ->
                    when (tab) {
                        ClarityNavTab.HOME -> onNavigateToDashboard()
                        ClarityNavTab.INCOME -> onNavigateToIncome()
                        ClarityNavTab.INSIGHTS -> Unit
                        ClarityNavTab.GOAL -> onNavigateToGoal()
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

                LargeTitleHeader(
                    title = "Insights",
                    trailing = { AddExpenseButton(onClick = { showAddExpenseSheet = true }) }
                )

                MonthSwitcher(
                    title = state.displayMonthHeader,
                    canGoForward = state.canGoForward,
                    onPrevious = { viewModel.selectPreviousMonth() },
                    onNext = { viewModel.selectNextMonth() },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                when {
                    state.isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            ActivityIndicator(size = 28.dp)
                        }
                    }

                    state.totalOutflow == 0.0 -> {
                        GroupedSection {
                            EmptyStateView(
                                icon = Icons.Rounded.BarChart,
                                title = "No Spending in ${state.displayMonthHeader}",
                                message = "Log expenses to see where your money goes, by category and payment method.",
                                actionLabel = "Add Expense",
                                onAction = { showAddExpenseSheet = true }
                            )
                        }
                    }

                    else -> {
                        TotalSpentCard(
                            totalOutflow = state.totalOutflow,
                            committedAmount = state.committedAmount,
                            committedPercentage = state.committedPercentage,
                            discretionaryAmount = state.discretionaryAmount,
                            discretionaryPercentage = state.discretionaryPercentage,
                            currency = currentCurrency,
                            exchangeRate = exchangeRate,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        state.spendingLeak?.let { leak ->
                            Spacer(modifier = Modifier.height(16.dp))
                            SpendingLeakCard(
                                leak = leak,
                                currency = currentCurrency,
                                exchangeRate = exchangeRate,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        SpendingPaceCard(
                            velocity = state.velocity,
                            currency = currentCurrency,
                            exchangeRate = exchangeRate,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        CategoryBreakdownSection(
                            categories = state.topCategories,
                            currency = currentCurrency,
                            exchangeRate = exchangeRate
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        PaymentChannelSection(
                            channels = state.channelBreakdown,
                            currency = currentCurrency,
                            exchangeRate = exchangeRate
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }

            CollapsingTitleBar(title = "Insights", collapsed = isCollapsed)
        }
    }
}

@Composable
internal fun MonthSwitcher(
    title: String,
    canGoForward: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    ClarityCard(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircleIconButton(
                icon = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                contentDescription = "Previous month",
                onClick = onPrevious,
                diameter = 30.dp
            )
            Text(
                text = title,
                style = ClarityType.headline,
                color = ClarityTheme.colors.label,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .semantics { liveRegion = LiveRegionMode.Polite }
            )
            CircleIconButton(
                icon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = "Next month",
                onClick = onNext,
                enabled = canGoForward,
                diameter = 30.dp
            )
        }
    }
}

@Composable
private fun TotalSpentCard(
    totalOutflow: Double,
    committedAmount: Double,
    committedPercentage: Int,
    discretionaryAmount: Double,
    discretionaryPercentage: Int,
    currency: String,
    exchangeRate: Double,
    modifier: Modifier = Modifier
) {
    val colors = ClarityTheme.colors
    ClarityCard(modifier = modifier) {
        Column(modifier = Modifier.semantics(mergeDescendants = true) {}) {
            Text(
                text = "Total Spent",
                style = ClarityType.subheadline.copy(fontWeight = FontWeight.SemiBold),
                color = colors.secondaryLabel
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = CurrencyFormatter.format(totalOutflow, currency, exchangeRate),
                style = ClarityType.amountHero,
                color = colors.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        StackedBar(
            segments = listOf(
                committedAmount.toFloat() to colors.committed,
                discretionaryAmount.toFloat() to colors.discretionary
            )
        )
        Spacer(modifier = Modifier.height(14.dp))
        LegendRow(
            label = "Committed",
            percentage = committedPercentage,
            amount = CurrencyFormatter.format(committedAmount, currency, exchangeRate),
            color = colors.committed
        )
        Spacer(modifier = Modifier.height(8.dp))
        LegendRow(
            label = "Discretionary",
            percentage = discretionaryPercentage,
            amount = CurrencyFormatter.format(discretionaryAmount, currency, exchangeRate),
            color = colors.discretionary
        )
    }
}

@Composable
private fun SpendingLeakCard(
    leak: SpendingLeakAlert,
    currency: String,
    exchangeRate: Double,
    modifier: Modifier = Modifier
) {
    val colors = ClarityTheme.colors
    val amount = CurrencyFormatter.format(leak.amount, currency, exchangeRate)
    ClarityCard(modifier = modifier.semantics(mergeDescendants = true) {}) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(icon = Icons.Rounded.Lightbulb, background = colors.orange)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Biggest Spending Leak",
                style = ClarityType.headline,
                color = colors.label
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "${leak.categoryIcon} ${leak.categoryName} is ${leak.percentageOfDiscretionary}% of your discretionary spending ($amount).",
            style = ClarityType.body,
            color = colors.label
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = leak.recommendationText,
            style = ClarityType.subheadline,
            color = colors.secondaryLabel
        )
    }
}

@Composable
private fun SpendingPaceCard(
    velocity: VelocityMetrics,
    currency: String,
    exchangeRate: Double,
    modifier: Modifier = Modifier
) {
    ClarityCard(modifier = modifier) {
        Text(
            text = "Spending Pace",
            style = ClarityType.headline,
            color = ClarityTheme.colors.label
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            PaceStat(
                label = "Daily Average",
                value = CurrencyFormatter.format(velocity.dailyAverage, currency, exchangeRate),
                caption = if (velocity.daysElapsed == 1) "Over 1 day" else "Over ${velocity.daysElapsed} days",
                modifier = Modifier.weight(1f)
            )
            VerticalHairline(modifier = Modifier.fillMaxHeight())
            PaceStat(
                label = "Projected Month End",
                value = CurrencyFormatter.format(velocity.projectedMonthEnd, currency, exchangeRate),
                caption = "At the current pace",
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp)
            )
        }
    }
}

@Composable
private fun PaceStat(
    label: String,
    value: String,
    caption: String,
    modifier: Modifier = Modifier
) {
    val colors = ClarityTheme.colors
    Column(modifier = modifier.semantics(mergeDescendants = true) {}) {
        Text(text = label, style = ClarityType.footnote, color = colors.secondaryLabel)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = ClarityType.amountCallout.copy(fontSize = ClarityType.title3.fontSize),
            color = colors.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(text = caption, style = ClarityType.footnote, color = colors.secondaryLabel)
    }
}

@Composable
private fun CategoryBreakdownSection(
    categories: List<CategorySpending>,
    currency: String,
    exchangeRate: Double
) {
    SectionTitle(title = "By Category")
    Spacer(modifier = Modifier.height(4.dp))
    GroupedSection {
        categories.forEachIndexed { index, category ->
            val typeColor = spendingTypeColor(category.type)
            ListRow(
                title = category.categoryName,
                subtitle = "${category.type.displayName} · ${category.percentageOfTotal}% of spending",
                leading = { EmojiBadge(emoji = category.categoryIcon, tint = typeColor) },
                trailing = {
                    Text(
                        text = CurrencyFormatter.format(category.amount, currency, exchangeRate),
                        style = ClarityType.amountBody,
                        color = ClarityTheme.colors.label,
                        maxLines = 1
                    )
                },
                below = {
                    Spacer(modifier = Modifier.height(8.dp))
                    CapsuleProgressBar(
                        progress = category.percentageOfTotal / 100f,
                        color = typeColor,
                        height = 4.dp
                    )
                },
                showDivider = index < categories.lastIndex
            )
        }
    }
}

@Composable
private fun PaymentChannelSection(
    channels: List<PaymentChannelSpending>,
    currency: String,
    exchangeRate: Double
) {
    if (channels.isEmpty()) return
    SectionTitle(title = "By Payment Method")
    Spacer(modifier = Modifier.height(4.dp))
    GroupedSection {
        channels.forEachIndexed { index, channel ->
            val (icon, tint) = channelAppearance(channel.channel)
            ListRow(
                title = channel.channel.displayName,
                subtitle = "${channel.percentageOfTotal}% of spending",
                leading = { IconTile(icon = icon, background = tint) },
                trailing = {
                    Text(
                        text = CurrencyFormatter.format(channel.amount, currency, exchangeRate),
                        style = ClarityType.amountBody,
                        color = ClarityTheme.colors.label,
                        maxLines = 1
                    )
                },
                showDivider = index < channels.lastIndex
            )
        }
    }
}

@Composable
private fun channelAppearance(method: PaymentMethod): Pair<ImageVector, Color> {
    val colors = ClarityTheme.colors
    return when (method) {
        PaymentMethod.CASH -> Icons.Rounded.Payments to colors.green
        PaymentMethod.CARD -> Icons.Rounded.CreditCard to colors.blue
        PaymentMethod.SECONDARY -> Icons.Rounded.Smartphone to colors.indigo
        PaymentMethod.ONLINE -> Icons.Rounded.SwapHoriz to colors.teal
    }
}
