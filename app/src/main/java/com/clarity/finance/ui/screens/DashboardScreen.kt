package com.clarity.finance.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clarity.finance.model.Expense
import com.clarity.finance.model.SpendingType
import com.clarity.finance.ui.components.AddExpenseSheet
import com.clarity.finance.ui.components.ClarityBottomBar
import com.clarity.finance.ui.components.ClarityNavTab
import com.clarity.finance.ui.components.DashedInfoBox
import com.clarity.finance.ui.theme.ClarityAmber
import com.clarity.finance.ui.theme.ClarityAmberLight
import com.clarity.finance.ui.theme.ClarityBlack
import com.clarity.finance.ui.theme.ClarityBlue
import com.clarity.finance.ui.theme.ClarityBlueLight
import com.clarity.finance.ui.theme.ClarityBorder
import com.clarity.finance.ui.theme.ClarityGrayBackground
import com.clarity.finance.ui.theme.ClarityGreen
import com.clarity.finance.ui.theme.ClarityGreenBright
import com.clarity.finance.ui.theme.ClarityGreenLight
import com.clarity.finance.ui.theme.ClarityPurple
import com.clarity.finance.ui.theme.ClarityPurpleLight
import com.clarity.finance.ui.theme.ClarityRed
import com.clarity.finance.ui.theme.ClarityRedLight
import com.clarity.finance.ui.theme.ClarityTextMuted
import com.clarity.finance.ui.theme.ClarityTextPrimary
import com.clarity.finance.ui.theme.ClarityTextSecondary
import com.clarity.finance.ui.theme.ClarityWhite
import com.clarity.finance.util.CurrencyFormatter
import com.clarity.finance.util.ProfileImageManager
import com.clarity.finance.viewmodel.AuthViewModel
import com.clarity.finance.viewmodel.DraftViewModel
import com.clarity.finance.viewmodel.ExpenseViewModel

@Composable
fun DashboardScreen(
    authViewModel: AuthViewModel,
    onSignOutSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    expenseViewModel: ExpenseViewModel = hiltViewModel(),
    draftViewModel: DraftViewModel = hiltViewModel(),
    onNavigateToExpensesHistory: () -> Unit = {},
    onNavigateToInsights: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToDrafts: () -> Unit = {}
) {
    val currentUser by authViewModel.currentUserFlow.collectAsStateWithLifecycle()
    val historyState by expenseViewModel.historyState.collectAsStateWithLifecycle()
    val draftCount by draftViewModel.pendingDraftCount.collectAsStateWithLifecycle()
    val currentCurrency by authViewModel.currencyFlow.collectAsStateWithLifecycle()

    var showSignOutDialog by remember { mutableStateOf(false) }
    var showAddExpenseSheet by remember { mutableStateOf(false) }

    val displayName = currentUser?.displayName?.ifBlank { "User" } ?: "User"

    // Real financial data calculations
    val estimatedIncome = 185_000.0 // Base monthly income in LKR
    val totalExpense = historyState.metrics.totalExpense
    val availableAmount = (estimatedIncome - totalExpense).coerceAtLeast(0.0)

    val incomeFormatted = CurrencyFormatter.formatCompact(estimatedIncome, currentCurrency)
    val expensesFormatted = CurrencyFormatter.formatCompact(totalExpense, currentCurrency)
    val availableFormatted = CurrencyFormatter.formatCompact(availableAmount, currentCurrency)
    val currencySymbol = CurrencyFormatter.getSymbol(currentCurrency)

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

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = {
                Text(
                    text = "Sign Out",
                    fontWeight = FontWeight.Bold,
                    color = ClarityTextPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to sign out from Clarity Finance?",
                    color = ClarityTextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutDialog = false
                        authViewModel.signOut()
                        onSignOutSuccess()
                    }
                ) {
                    Text(text = "Sign Out", color = ClarityRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text(text = "Cancel", color = ClarityTextPrimary)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp)
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            ClarityBottomBar(
                currentTab = ClarityNavTab.HOME,
                onTabSelected = { tab ->
                    when (tab) {
                        ClarityNavTab.HOME -> { /* Already on Home */ }
                        ClarityNavTab.INSIGHTS -> onNavigateToInsights()
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
        ) {
            // =========================================================================
            // 1. STICKY TOP SECTION: Header Greeting & Financial Summary Cards
            // (Pins on top while rest of dashboard scrolls underneath)
            // =========================================================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                // Header: Month & User Greeting & Settings Icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "OCTOBER 2026 ▼",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = ClarityTextMuted,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Hi, $displayName",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = ClarityTextPrimary
                        )
                    }

                    // Settings & Profile Avatar button
                    val photoPath = authViewModel.profilePhotoPath
                    val avatarBitmap = remember(photoPath) {
                        ProfileImageManager.loadBitmap(photoPath)
                    }

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(ClarityGrayBackground)
                            .border(BorderStroke(1.5.dp, ClarityGreen), CircleShape)
                            .clickable { onNavigateToSettings() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (avatarBitmap != null) {
                            Image(
                                bitmap = avatarBitmap,
                                contentDescription = "Settings & Profile",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = ClarityTextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Sticky Summary Cards Row (INCOME, EXPENSES, AVAILABLE)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ColorfulSummaryCard(
                        title = "INCOME",
                        amount = incomeFormatted,
                        currency = currencySymbol,
                        tag = "▲ Inflow",
                        accentColor = ClarityGreen,
                        containerColor = ClarityGreenLight,
                        modifier = Modifier.weight(1f)
                    )
                    ColorfulSummaryCard(
                        title = "EXPENSES",
                        amount = expensesFormatted,
                        currency = currencySymbol,
                        tag = "▼ Outflow",
                        accentColor = ClarityRed,
                        containerColor = ClarityRedLight,
                        modifier = Modifier.weight(1f)
                    )
                    ColorfulSummaryCard(
                        title = "AVAILABLE",
                        amount = availableFormatted,
                        currency = currencySymbol,
                        tag = "★ Net Cash",
                        accentColor = ClarityBlue,
                        containerColor = ClarityBlueLight,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Divider separating sticky summary header from scrollable body
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(ClarityBorder)
            )

            // =========================================================================
            // 2. SCROLLABLE BODY CONTENT
            // =========================================================================
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                // Bank SMS Drafts notification banner if pending drafts exist
                if (draftCount > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(ClarityAmberLight)
                            .border(BorderStroke(1.2.dp, ClarityAmber), RoundedCornerShape(10.dp))
                            .clickable { onNavigateToDrafts() }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "⚡", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "$draftCount Bank SMS Transactions Drafted",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ClarityTextPrimary
                                    )
                                    Text(
                                        text = "Tap to review & approve into expenses",
                                        fontSize = 10.sp,
                                        color = ClarityTextSecondary
                                    )
                                }
                            }
                            Text(
                                text = "REVIEW →",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = ClarityAmber
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Primary Savings Goal Card
                SavingsGoalCard(currentCurrency = currentCurrency)

                Spacer(modifier = Modifier.height(14.dp))

                // Pending Payments Card
                PendingPaymentsCard()

                Spacer(modifier = Modifier.height(14.dp))

                // Live Spending Overview Card (Committed vs Discretionary)
                SpendingOverviewCard(
                    metrics = historyState.metrics,
                    currentCurrency = currentCurrency
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Recent Transactions
                RecentTransactionsSection(
                    expenses = historyState.expenses,
                    currentCurrency = currentCurrency,
                    onSeeAllClick = onNavigateToExpensesHistory
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ColorfulSummaryCard(
    title: String,
    amount: String,
    currency: String,
    tag: String,
    accentColor: Color,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(containerColor, RoundedCornerShape(10.dp))
            .border(BorderStroke(1.2.dp, accentColor.copy(alpha = 0.5f)), RoundedCornerShape(10.dp))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = accentColor,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = amount,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = ClarityTextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$currency • $tag",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = ClarityTextSecondary
            )
        }
    }
}

@Composable
private fun SavingsGoalCard(
    currentCurrency: String
) {
    val currentSavedInLkr = 11_200.0
    val targetInLkr = 490_000.0
    val monthlyTargetInLkr = 39_900.0

    val currentFormatted = CurrencyFormatter.format(currentSavedInLkr, currentCurrency)
    val targetFormatted = CurrencyFormatter.format(targetInLkr, currentCurrency)
    val monthlyFormatted = CurrencyFormatter.format(monthlyTargetInLkr, currentCurrency)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
            .border(BorderStroke(1.2.dp, ClarityPurple.copy(alpha = 0.4f)), RoundedCornerShape(10.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PRIMARY SAVINGS GOAL",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = ClarityPurple
                )
                Box(
                    modifier = Modifier
                        .background(ClarityPurpleLight, RoundedCornerShape(4.dp))
                        .border(BorderStroke(1.dp, ClarityPurple.copy(alpha = 0.5f)), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "2.3%",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = ClarityPurple
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "MacBook Pro M4",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = ClarityTextPrimary
            )

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { 0.023f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = ClarityPurple,
                trackColor = ClarityBorder
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = currentFormatted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = ClarityTextSecondary
                )
                Text(
                    text = "Target: $targetFormatted",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = ClarityTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            DashedInfoBox(
                text = "Save $monthlyFormatted/mo to finish in Oct 2027   View Goal →",
                prefix = "🎯"
            )
        }
    }
}

@Composable
private fun PendingPaymentsCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
            .border(BorderStroke(1.2.dp, ClarityAmber.copy(alpha = 0.4f)), RoundedCornerShape(10.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(ClarityAmberLight, RoundedCornerShape(6.dp))
                    .border(BorderStroke(1.dp, ClarityAmber.copy(alpha = 0.6f)), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⚠️",
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "2 Pending Inflow Payments",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = ClarityTextPrimary
                )
                Text(
                    text = "Upwork $180 & DevContract LKR 30k",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = ClarityTextSecondary
                )
            }

            Box(
                modifier = Modifier
                    .background(ClarityAmber, RoundedCornerShape(6.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "VIEW",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ClarityWhite
                )
            }
        }
    }
}

@Composable
private fun SpendingOverviewCard(
    metrics: com.clarity.finance.viewmodel.ExpenseMonthlyMetrics,
    currentCurrency: String
) {
    val total = metrics.totalExpense
    val committedPct = if (total > 0) metrics.committedPercentage else 60
    val discretionaryPct = if (total > 0) metrics.discretionaryPercentage else 40

    val committedFormatted = CurrencyFormatter.format(metrics.committedTotal, currentCurrency)
    val discretionaryFormatted = CurrencyFormatter.format(metrics.discretionaryTotal, currentCurrency)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
            .border(BorderStroke(1.dp, ClarityBorder), RoundedCornerShape(10.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Spending Overview",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = ClarityTextPrimary
                )
                Text(
                    text = "60/40 Target Rule",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = ClarityTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Committed vs Discretionary Multi-Color Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .clip(RoundedCornerShape(6.dp))
            ) {
                val committedWeight = (committedPct.coerceAtLeast(5)).toFloat()
                val discretionaryWeight = (discretionaryPct.coerceAtLeast(5)).toFloat()

                Box(
                    modifier = Modifier
                        .weight(committedWeight)
                        .background(ClarityBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$committedPct% Committed",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = ClarityWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(discretionaryWeight)
                        .background(ClarityRed),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$discretionaryPct% Disc.",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = ClarityWhite,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(ClarityBlue, CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Committed: $committedFormatted",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = ClarityTextSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(ClarityRed, CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Discretionary: $discretionaryFormatted",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = ClarityTextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentTransactionsSection(
    expenses: List<Expense>,
    currentCurrency: String,
    onSeeAllClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RECENT TRANSACTIONS",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = ClarityTextPrimary
            )
            Text(
                text = "See all →",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = ClarityGreen,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onSeeAllClick() }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (expenses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ClarityGrayBackground, RoundedCornerShape(10.dp))
                    .border(BorderStroke(1.dp, ClarityBorder), RoundedCornerShape(10.dp))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No expenses recorded yet. Tap + to add one.",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = ClarityTextSecondary
                )
            }
        } else {
            expenses.take(4).forEach { expense ->
                val amountFormatted = CurrencyFormatter.format(expense.amount, currentCurrency)
                val badgeColor = when (expense.type) {
                    SpendingType.COMMITTED -> ClarityBlueLight
                    SpendingType.DISCRETIONARY -> ClarityRedLight
                }
                val badgeTextColor = when (expense.type) {
                    SpendingType.COMMITTED -> ClarityBlue
                    SpendingType.DISCRETIONARY -> ClarityRed
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
                        .border(BorderStroke(1.dp, ClarityBorder), RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(badgeColor, RoundedCornerShape(8.dp))
                                .border(BorderStroke(1.dp, badgeTextColor.copy(alpha = 0.3f)), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = expense.categoryIcon.ifBlank { "🏷️" },
                                fontSize = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = expense.categoryName,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ClarityTextPrimary
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = expense.date,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = ClarityTextMuted
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(badgeColor, RoundedCornerShape(3.dp))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = if (expense.type == SpendingType.COMMITTED) "COMMITTED" else "DISC.",
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = badgeTextColor
                                    )
                                }
                            }
                        }

                        Text(
                            text = "-$amountFormatted",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = ClarityRed
                        )
                    }
                }
            }
        }
    }
}
