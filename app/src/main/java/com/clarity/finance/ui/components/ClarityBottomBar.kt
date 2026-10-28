package com.clarity.finance.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clarity.finance.ui.theme.ClarityTheme
import com.clarity.finance.ui.theme.ClarityType

enum class ClarityNavTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Rounded.Home),
    INCOME("Income", Icons.Rounded.AccountBalanceWallet),
    INSIGHTS("Insights", Icons.Rounded.BarChart),
    GOAL("Goal", Icons.Rounded.Flag)
}

/**
 * iOS tab bar (HIG > Tab bars): bar material with a top hairline, filled glyphs, short labels
 * and the app tint on the selected tab. Tab bars are for navigation only, so the add-expense
 * action lives in each screen's header instead of a floating button in the bar.
 */
@Composable
fun ClarityBottomBar(
    currentTab: ClarityNavTab,
    onTabSelected: (ClarityNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ClarityTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.barBackground)
    ) {
        Hairline()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(54.dp)
                .selectableGroup(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ClarityNavTab.entries.forEach { tab ->
                TabBarItem(
                    tab = tab,
                    isSelected = tab == currentTab,
                    onClick = { if (tab != currentTab) onTabSelected(tab) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TabBarItem(
    tab: ClarityNavTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ClarityTheme.colors
    val tint by animateColorAsState(
        targetValue = if (isSelected) colors.accent else colors.gray,
        animationSpec = tween(durationMillis = 160),
        label = "tabTint"
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .selectable(
                selected = isSelected,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = tab.label,
            style = ClarityType.caption2.copy(
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
            ),
            color = tint,
            maxLines = 1
        )
    }
}
