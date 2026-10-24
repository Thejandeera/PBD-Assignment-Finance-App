package com.clarity.finance.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clarity.finance.model.CategorySpending
import com.clarity.finance.model.PaymentChannelSpending
import com.clarity.finance.model.SpendingLeakAlert
import com.clarity.finance.model.SpendingType
import com.clarity.finance.model.VelocityMetrics
import com.clarity.finance.ui.components.AddExpenseSheet
import com.clarity.finance.ui.components.ClarityBottomBar
import com.clarity.finance.ui.components.ClarityNavTab
import com.clarity.finance.ui.theme.ClarityAmber
import com.clarity.finance.ui.theme.ClarityAmberLight
import com.clarity.finance.ui.theme.ClarityBlue
import com.clarity.finance.ui.theme.ClarityBlueLight
import com.clarity.finance.ui.theme.ClarityGreen
import com.clarity.finance.ui.theme.ClarityGreenBright
import com.clarity.finance.ui.theme.ClarityRed
import com.clarity.finance.ui.theme.ClarityRedLight
import com.clarity.finance.ui.theme.ClarityWhite
import com.clarity.finance.util.CurrencyFormatter
import com.clarity.finance.viewmodel.AuthViewModel
import com.clarity.finance.viewmodel.ExpenseViewModel
import com.clarity.finance.viewmodel.InsightsViewModel
import java.util.Locale

@Composable
fun InsightsScreen(
    onNavigateToDashboard: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: InsightsViewModel = hiltViewModel(),
    expenseViewModel: ExpenseViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentCurrency by authViewModel.currencyFlow.collectAsStateWithLifecycle()
    val exchangeRate by authViewModel.exchangeRateFlow.collectAsStateWithLifecycle()
    var showAddExpenseSheet by remember { mutableStateOf(false) }

    if (showAddExpenseSheet) {
        AddExpenseSheet(
            viewModel = expenseViewModel,
            onDismiss = { showAddExpenseSheet = false }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline))
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "INSIGHTS & ANALYTICS",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        bottomBar = {
            ClarityBottomBar(
                currentTab = ClarityNavTab.INSIGHTS,
                onTabSelected = { tab ->
                    when (tab) {
                        ClarityNavTab.HOME -> onNavigateToDashboard()
                        ClarityNavTab.INSIGHTS -> { /* Already on insights */ }
                        ClarityNavTab.INCOME -> { /* Future slice */ }
                        ClarityNavTab.GOAL -> { /* Future slice */ }
                    }
                },
                onAddClick = { showAddExpenseSheet = true }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            // =================================================================
            // 1. Month Switcher Bar (< October 2026 >)
            // =================================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.selectPreviousMonth() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous Month",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = state.displayMonthHeader.uppercase(Locale.US),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(
                    onClick = { viewModel.selectNextMonth() },
                    enabled = state.canGoForward,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next Month",
                        tint = if (state.canGoForward) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(32.dp)
                    )
                }
            } else if (state.totalOutflow == 0.0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(12.dp))
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "[i]",
                            fontSize = 22.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No expenses recorded for ${state.displayMonthHeader}.",
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Log transactions via the '+' button on Dashboard to see visual spending patterns.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // =============================================================
                // 2. Hero: Committed vs Discretionary Ratio
                // =============================================================
                CommittedVsDiscretionaryCard(
                    totalOutflow = state.totalOutflow,
                    committedAmount = state.committedAmount,
                    committedPercentage = state.committedPercentage,
                    discretionaryAmount = state.discretionaryAmount,
                    discretionaryPercentage = state.discretionaryPercentage,
                    currentCurrency = currentCurrency,
                    exchangeRate = exchangeRate
                )

                Spacer(modifier = Modifier.height(16.dp))

                // =============================================================
                // 3. Top Spending Leak & Goal Link
                // =============================================================
                state.spendingLeak?.let { leak ->
                    SpendingLeakCard(
                        leak = leak,
                        currentCurrency = currentCurrency,
                        exchangeRate = exchangeRate
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // =============================================================
                // 4. Burn Rate Velocity & Month-End Projection
                // =============================================================
                BurnRateCard(
                    velocity = state.velocity,
                    currentCurrency = currentCurrency,
                    exchangeRate = exchangeRate
                )

                Spacer(modifier = Modifier.height(16.dp))

                // =============================================================
                // 5. Category Breakdown Ranked Progress Bars
                // =============================================================
                CategoryBreakdownSection(
                    categories = state.topCategories,
                    currentCurrency = currentCurrency,
                    exchangeRate = exchangeRate
                )

                Spacer(modifier = Modifier.height(16.dp))

                // =============================================================
                // 6. Payment Channel Distribution
                // =============================================================
                PaymentChannelSection(
                    channels = state.channelBreakdown,
                    currentCurrency = currentCurrency,
                    exchangeRate = exchangeRate
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun CommittedVsDiscretionaryCard(
    totalOutflow: Double,
    committedAmount: Double,
    committedPercentage: Int,
    discretionaryAmount: Double,
    discretionaryPercentage: Int,
    currentCurrency: String,
    exchangeRate: Double
) {
    val totalFormatted = CurrencyFormatter.format(totalOutflow, currentCurrency, exchangeRate)
    val committedFormatted = CurrencyFormatter.format(committedAmount, currentCurrency, exchangeRate)
    val discretionaryFormatted = CurrencyFormatter.format(discretionaryAmount, currentCurrency, exchangeRate)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .border(BorderStroke(1.2.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = "TOTAL MONTHLY OUTFLOW",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = totalFormatted,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = ClarityRed
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Dual-segment ratio bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(8.dp))
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (committedPercentage > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight((committedPercentage.coerceAtLeast(1)).toFloat())
                                .background(ClarityBlue)
                        )
                    }
                    if (discretionaryPercentage > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .weight((discretionaryPercentage.coerceAtLeast(1)).toFloat())
                                .background(ClarityRed)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Legend indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Committed Legend
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(ClarityBlue, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "COMMITTED ($committedPercentage%)",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = ClarityBlue
                        )
                        Text(
                            text = committedFormatted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Discretionary Legend
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(ClarityRed, RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "DISCRETIONARY ($discretionaryPercentage%)",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = ClarityRed
                        )
                        Text(
                            text = discretionaryFormatted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpendingLeakCard(
    leak: SpendingLeakAlert,
    currentCurrency: String,
    exchangeRate: Double
) {
    val formattedLeakAmount = CurrencyFormatter.format(leak.amount, currentCurrency, exchangeRate)
    val isDark = MaterialTheme.colorScheme.background == Color(0xFF000000)
    val cardBg = if (isDark) ClarityAmber.copy(alpha = 0.15f) else ClarityAmberLight

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardBg, RoundedCornerShape(12.dp))
            .border(BorderStroke(1.2.dp, ClarityAmber), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "💡 TOP SPENDING LEAK DETECTED",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = ClarityAmber
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${leak.categoryIcon} ${leak.categoryName} accounts for ${leak.percentageOfDiscretionary}% of your discretionary spending ($formattedLeakAmount).",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = leak.recommendationText,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun BurnRateCard(
    velocity: VelocityMetrics,
    currentCurrency: String,
    exchangeRate: Double
) {
    val dailyBurn = CurrencyFormatter.format(velocity.dailyAverage, currentCurrency, exchangeRate)
    val projectedBurn = CurrencyFormatter.format(velocity.projectedMonthEnd, currentCurrency, exchangeRate)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Daily Average
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "DAILY AVERAGE BURN",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$dailyBurn / day",
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = ClarityRed
                )
                Text(
                    text = "Over ${velocity.daysElapsed} days elapsed",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Month-End Projection
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "PROJECTED OUTFLOW",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = projectedBurn,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "At current burn velocity",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CategoryBreakdownSection(
    categories: List<CategorySpending>,
    currentCurrency: String,
    exchangeRate: Double
) {
    val isDark = MaterialTheme.colorScheme.background == Color(0xFF000000)

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "SPENDING BY CATEGORY",
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(10.dp))

        categories.forEach { category ->
            val isCommitted = category.type == SpendingType.COMMITTED
            val badgeBg = if (isCommitted) {
                if (isDark) ClarityBlue.copy(alpha = 0.2f) else ClarityBlueLight
            } else {
                if (isDark) ClarityRed.copy(alpha = 0.2f) else ClarityRedLight
            }
            val badgeColor = if (isCommitted) ClarityBlue else ClarityRed
            val barColor = if (isCommitted) ClarityBlue else ClarityGreenBright

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = category.categoryIcon, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = category.categoryName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(badgeBg, RoundedCornerShape(3.dp))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = if (isCommitted) "COMMITTED" else "DISCRETIONARY",
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeColor
                                )
                            }
                        }

                        Text(
                            text = "-${CurrencyFormatter.format(category.amount, currentCurrency, exchangeRate)}",
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = ClarityRed
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Progress bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth((category.percentageOfTotal / 100f).coerceIn(0.01f, 1f))
                                    .background(barColor)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${category.percentageOfTotal}%",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentChannelSection(
    channels: List<PaymentChannelSpending>,
    currentCurrency: String,
    exchangeRate: Double
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "PAYMENT CHANNELS",
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            channels.forEach { channel ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = channel.channel.displayName.uppercase(Locale.US),
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = CurrencyFormatter.format(channel.amount, currentCurrency, exchangeRate),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${channel.percentageOfTotal}% of total",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
