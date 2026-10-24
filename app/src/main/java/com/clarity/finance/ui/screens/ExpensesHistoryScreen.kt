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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clarity.finance.model.Expense
import com.clarity.finance.model.SpendingType
import com.clarity.finance.ui.theme.ClarityBlack
import com.clarity.finance.ui.theme.ClarityBorder
import com.clarity.finance.ui.theme.ClarityBorderDark
import com.clarity.finance.ui.theme.ClarityGrayBackground
import com.clarity.finance.ui.theme.ClarityRed
import com.clarity.finance.ui.theme.ClarityTextMuted
import com.clarity.finance.ui.theme.ClarityTextPrimary
import com.clarity.finance.ui.theme.ClarityTextSecondary
import com.clarity.finance.ui.theme.ClarityWhite
import com.clarity.finance.viewmodel.ExpenseViewModel
import java.text.NumberFormat
import java.util.Locale

import com.clarity.finance.viewmodel.DraftViewModel

@Composable
fun ExpensesHistoryScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ExpenseViewModel = hiltViewModel(),
    draftViewModel: DraftViewModel = hiltViewModel(),
    onNavigateToDrafts: () -> Unit = {}
) {
    val historyState by viewModel.historyState.collectAsStateWithLifecycle()
    val draftCount by draftViewModel.pendingDraftCount.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }

    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }

    // Delete Confirmation Dialog
    expenseToDelete?.let { expense ->
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = {
                Text(
                    text = "Delete Expense?",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete ${expense.categoryName} (${numberFormat.format(expense.amount)} LKR)?",
                    fontSize = 13.sp,
                    color = ClarityTextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteExpense(expense.id)
                        expenseToDelete = null
                    }
                ) {
                    Text("Delete", color = ClarityRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { expenseToDelete = null }) {
                    Text("Cancel", color = ClarityTextPrimary)
                }
            },
            containerColor = ClarityWhite,
            shape = RoundedCornerShape(12.dp)
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ClarityWhite,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ClarityWhite)
                    .border(BorderStroke(1.dp, ClarityBorder))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = ClarityTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = "EXPENSES HISTORY",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = ClarityTextPrimary
                        )
                        Text(
                            text = historyState.currentMonth,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = ClarityTextMuted
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            // SMS Drafts notification banner
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
                                    text = "$draftCount Transactions Drafted from SMS",
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

            // =================================================================
            // 1. Monthly Summary Cards (Wireframe 3 Top Metrics)
            // =================================================================
            val metrics = historyState.metrics

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ClarityGrayBackground, RoundedCornerShape(12.dp))
                    .border(BorderStroke(1.dp, ClarityBorder), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TOTAL MONTHLY OUTFLOW",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = ClarityTextMuted
                        )
                        Text(
                            text = "${historyState.expenses.size} ENTRIES",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = ClarityTextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "LKR ${numberFormat.format(metrics.totalExpense)}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = ClarityTextPrimary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Breakdown row: Committed vs Discretionary
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Committed Card
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(ClarityWhite, RoundedCornerShape(8.dp))
                                .border(BorderStroke(1.dp, ClarityBorder), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(
                                    text = "COMMITTED",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = ClarityTextSecondary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "LKR ${numberFormat.format(metrics.committedTotal)}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = ClarityTextPrimary
                                )
                                Text(
                                    text = "${metrics.committedPercentage}% of outflow",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = ClarityTextMuted
                                )
                            }
                        }

                        // Discretionary Card
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(ClarityWhite, RoundedCornerShape(8.dp))
                                .border(BorderStroke(1.dp, ClarityBorder), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(
                                    text = "DISCRETIONARY",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = ClarityTextSecondary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "LKR ${numberFormat.format(metrics.discretionaryTotal)}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = ClarityTextPrimary
                                )
                                Text(
                                    text = "${metrics.discretionaryPercentage}% of outflow",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = ClarityTextMuted
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // =================================================================
            // 2. Live Search Field
            // =================================================================
            OutlinedTextField(
                value = historyState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = {
                    Text(
                        text = "Search by category or note...",
                        fontSize = 13.sp,
                        color = ClarityTextMuted
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = ClarityTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = if (historyState.searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = ClarityTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ClarityBlack,
                    unfocusedBorderColor = ClarityBorder,
                    focusedTextColor = ClarityTextPrimary,
                    unfocusedTextColor = ClarityTextPrimary,
                    cursorColor = ClarityBlack
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // =================================================================
            // 3. Filter Chips (All, Committed, Discretionary)
            // =================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HistoryFilterChip(
                    label = "All",
                    isSelected = historyState.selectedTypeFilter == null,
                    onClick = { viewModel.onFilterSelected(null) }
                )
                HistoryFilterChip(
                    label = "Committed",
                    isSelected = historyState.selectedTypeFilter == SpendingType.COMMITTED,
                    onClick = { viewModel.onFilterSelected(SpendingType.COMMITTED) }
                )
                HistoryFilterChip(
                    label = "Discretionary",
                    isSelected = historyState.selectedTypeFilter == SpendingType.DISCRETIONARY,
                    onClick = { viewModel.onFilterSelected(SpendingType.DISCRETIONARY) }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // =================================================================
            // 4. Grouped Transaction List
            // =================================================================
            if (historyState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = ClarityBlack,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(28.dp)
                    )
                }
            } else if (historyState.filteredExpenses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(ClarityGrayBackground, RoundedCornerShape(12.dp))
                        .border(BorderStroke(1.dp, ClarityBorder), RoundedCornerShape(12.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "[!]",
                            fontSize = 20.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = ClarityTextMuted
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (historyState.searchQuery.isNotEmpty())
                                "No transactions found matching \"${historyState.searchQuery}\""
                            else
                                "No expenses recorded for this month yet.",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = ClarityTextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = historyState.filteredExpenses,
                        key = { it.id }
                    ) { expense ->
                        HistoryExpenseCard(
                            expense = expense,
                            numberFormat = numberFormat,
                            onDeleteClick = { expenseToDelete = expense }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) ClarityBlack else ClarityGrayBackground)
            .border(
                BorderStroke(1.dp, if (isSelected) ClarityBlack else ClarityBorder),
                RoundedCornerShape(6.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) ClarityWhite else ClarityTextPrimary
        )
    }
}

@Composable
private fun HistoryExpenseCard(
    expense: Expense,
    numberFormat: NumberFormat,
    onDeleteClick: () -> Unit
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
            // Icon
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(ClarityGrayBackground, RoundedCornerShape(6.dp))
                    .border(BorderStroke(1.dp, ClarityBorder), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = expense.categoryIcon.ifBlank { "🏷️" },
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.categoryName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ClarityTextPrimary
                )

                if (!expense.note.isNullOrBlank()) {
                    Text(
                        text = expense.note,
                        fontSize = 11.sp,
                        color = ClarityTextSecondary,
                        maxLines = 1
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = expense.date,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = ClarityTextMuted
                    )

                    Text(
                        text = "•",
                        fontSize = 10.sp,
                        color = ClarityTextMuted
                    )

                    // Obligation badge
                    Box(
                        modifier = Modifier
                            .background(
                                if (expense.type == SpendingType.COMMITTED) ClarityBlack.copy(alpha = 0.08f)
                                else ClarityGrayBackground,
                                RoundedCornerShape(3.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = if (expense.type == SpendingType.COMMITTED) "COMMITTED" else "DISCRETIONARY",
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (expense.type == SpendingType.COMMITTED) ClarityTextPrimary else ClarityTextSecondary
                        )
                    }

                    // Payment method tag
                    Text(
                        text = expense.paymentMethod.displayName,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = ClarityTextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Amount and Delete Action
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "-${numberFormat.format(expense.amount)} LKR",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = ClarityTextPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = ClarityTextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
