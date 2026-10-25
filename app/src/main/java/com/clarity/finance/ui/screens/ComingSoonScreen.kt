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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.clarity.finance.ui.components.ClarityBottomBar
import com.clarity.finance.ui.components.ClarityNavTab
import com.clarity.finance.ui.components.EmptyStateView
import com.clarity.finance.ui.components.LargeTitleHeader
import com.clarity.finance.ui.theme.ClarityTheme

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
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ClarityTheme.colors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            ClarityBottomBar(currentTab = tab, onTabSelected = onTabSelected)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
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
