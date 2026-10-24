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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clarity.finance.ui.components.DashedInfoBox
import com.clarity.finance.ui.theme.ClarityBlack
import com.clarity.finance.ui.theme.ClarityBorder
import com.clarity.finance.ui.theme.ClarityGrayBackground
import com.clarity.finance.ui.theme.ClarityTextMuted
import com.clarity.finance.ui.theme.ClarityTextPrimary
import com.clarity.finance.ui.theme.ClarityTextSecondary
import com.clarity.finance.ui.theme.ClarityWhite
import com.clarity.finance.viewmodel.AuthViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.clarity.finance.model.Expense
import com.clarity.finance.model.SpendingType
import com.clarity.finance.ui.components.AddExpenseSheet
import com.clarity.finance.ui.components.ClarityBottomBar
import com.clarity.finance.ui.components.ClarityNavTab
import com.clarity.finance.viewmodel.ExpenseViewModel
import java.text.NumberFormat
import java.util.Locale

import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.Settings
import com.clarity.finance.util.ProfileImageManager
import com.clarity.finance.viewmodel.DraftViewModel

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
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showAddExpenseSheet by remember { mutableStateOf(false) }

    val displayName = currentUser?.displayName?.ifBlank { "Kavindu" } ?: "Kavindu"

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
                    Text(text = "Sign Out", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) {
                    Text(text = "Cancel", color = ClarityTextPrimary)
                }
            },
            containerColor = ClarityWhite,
            shape = RoundedCornerShape(12.dp)
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ClarityWhite,
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Header: Month & User & Settings Icon
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
                        .border(BorderStroke(1.2.dp, ClarityBorder), CircleShape)
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
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
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

            Spacer(modifier = Modifier.height(16.dp))

            // Bank SMS Drafts notification banner if pending drafts exist
            if (draftCount > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ClarityGrayBackground)
                        .border(BorderStroke(1.dp, ClarityBlack), RoundedCornerShape(8.dp))
                        .clickable { onNavigateToDrafts() }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "⚡", fontSize = 14.sp)
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
                                    color = ClarityTextMuted
                                )
                            }
                        }
                        Text(
                            text = "REVIEW →",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = ClarityBlack
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Summary Cards Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryCard(
                    title = "INCOME",
                    amount = "185k",
                    currency = "LKR",
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "EXPENSES",
                    amount = "121.5k",
                    currency = "LKR",
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "AVAILABLE",
                    amount = "63.5k",
                    currency = "LKR",
                    isHighlighted = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Primary Savings Goal
            SavingsGoalCard()

            Spacer(modifier = Modifier.height(16.dp))

            // Pending Payments
            PendingPaymentsCard()

            Spacer(modifier = Modifier.height(16.dp))

            // Spending Overview
            SpendingOverviewCard()

            Spacer(modifier = Modifier.height(20.dp))

            // Recent Transactions
            RecentTransactionsSection(
                expenses = historyState.expenses,
                onSeeAllClick = onNavigateToExpensesHistory
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    amount: String,
    currency: String,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false
) {
    val borderStroke = if (isHighlighted) {
        BorderStroke(2.dp, ClarityBlack)
    } else {
        BorderStroke(1.dp, ClarityBorder)
    }

    Box(
        modifier = modifier
            .background(ClarityWhite, RoundedCornerShape(8.dp))
            .border(borderStroke, RoundedCornerShape(8.dp))
            .padding(vertical = 14.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = ClarityTextMuted,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = amount,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = ClarityTextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = currency,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = ClarityTextSecondary
            )
        }
    }
}

@Composable
private fun SavingsGoalCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(ClarityWhite, RoundedCornerShape(8.dp))
            .border(BorderStroke(1.dp, ClarityBorder), RoundedCornerShape(8.dp))
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
                    color = ClarityTextMuted
                )
                Box(
                    modifier = Modifier
                        .border(BorderStroke(1.dp, ClarityBorder), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "2.3%",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = ClarityTextPrimary
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
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = ClarityBlack,
                trackColor = ClarityBorder
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "LKR 11,200",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = ClarityTextSecondary
                )
                Text(
                    text = "Target: LKR 490,000",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = ClarityTextSecondary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            DashedInfoBox(
                text = "Save LKR 39,900/mo to finish in Oct 2027   View Goal →",
                prefix = ""
            )
        }
    }
}

@Composable
private fun PendingPaymentsCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(ClarityWhite, RoundedCornerShape(8.dp))
            .border(BorderStroke(1.dp, ClarityBorder), RoundedCornerShape(8.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .border(BorderStroke(1.dp, ClarityBorder), RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "!",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = ClarityTextPrimary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "2 Pending Payments",
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
                    .background(ClarityBlack, RoundedCornerShape(4.dp))
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
private fun SpendingOverviewCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(ClarityWhite, RoundedCornerShape(8.dp))
            .border(BorderStroke(1.dp, ClarityBorder), RoundedCornerShape(8.dp))
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

            // Committed vs Discretionary Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(18.dp)
                    .clip(RoundedCornerShape(4.dp))
            ) {
                Box(
                    modifier = Modifier
                        .weight(0.62f)
                        .background(ClarityBlack),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "62% Committed",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = ClarityWhite
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(0.38f)
                        .background(ClarityBorder),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "38% Disc.",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = ClarityTextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Committed: LKR 75,330",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = ClarityTextSecondary
                )
                Text(
                    text = "Discretionary: LKR 46,170",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = ClarityTextSecondary
                )
            }
        }
    }
}

@Composable
private fun RecentTransactionsSection(
    expenses: List<Expense>,
    onSeeAllClick: () -> Unit
) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }

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
                text = "See all",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = ClarityTextSecondary,
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
                    .background(ClarityGrayBackground, RoundedCornerShape(8.dp))
                    .border(BorderStroke(1.dp, ClarityBorder), RoundedCornerShape(8.dp))
                    .padding(16.dp),
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
                TransactionItem(
                    iconTag = expense.categoryIcon.ifBlank { "🏷️" },
                    title = expense.categoryName,
                    subtitle = "${expense.date} • ${if (expense.type == SpendingType.COMMITTED) "Committed" else "Discretionary"}",
                    amount = "-${numberFormat.format(expense.amount)} LKR"
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun TransactionItem(
    iconTag: String,
    title: String,
    subtitle: String,
    amount: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(ClarityWhite, RoundedCornerShape(8.dp))
            .border(BorderStroke(1.dp, ClarityBorder), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .border(BorderStroke(1.dp, ClarityBorder), RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = iconTag,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = ClarityTextSecondary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ClarityTextPrimary
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = ClarityTextSecondary
                )
            }

            Text(
                text = amount,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = ClarityTextPrimary
            )
        }
    }
}

