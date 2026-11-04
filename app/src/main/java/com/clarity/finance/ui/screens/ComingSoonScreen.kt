package com.clarity.finance.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.clarity.finance.ui.components.AddExpenseSheet
import com.clarity.finance.ui.components.ClarityBottomBar
import com.clarity.finance.ui.components.bottomPaddingBelowBump
import com.clarity.finance.ui.components.ClarityNavTab
import com.clarity.finance.ui.components.EmptyStateView
import com.clarity.finance.ui.components.LargeTitleHeader
import com.clarity.finance.ui.theme.ClarityTheme
import com.clarity.finance.viewmodel.ExpenseViewModel

/**
 * Placeholder for tabs whose feature slice hasn't shipped yet (Income, Goal). The HIG asks that
 * tab items stay available and explain why their content isn't, rather than doing nothing.
 */
@Composable
fun ComingSoonScreen(
    tab: ClarityNavTab,
    title: String,
    message: String,
    onTabSelected: (ClarityNavTab) -> Unit,
    modifier: Modifier = Modifier,
    expenseViewModel: ExpenseViewModel = hiltViewModel()
) {
    var showAddExpenseSheet by remember { mutableStateOf(false) }

    if (showAddExpenseSheet) {
        AddExpenseSheet(
            viewModel = expenseViewModel,
            onDismiss = { showAddExpenseSheet = false }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ClarityTheme.colors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            ClarityBottomBar(
                currentTab = tab,
                onTabSelected = onTabSelected,
                onAddClick = { showAddExpenseSheet = true }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.bottomPaddingBelowBump())
        ) {
            Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            LargeTitleHeader(title = tab.label)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateView(
                    icon = tab.icon,
                    title = title,
                    message = message
                )
            }
        }
    }
}
