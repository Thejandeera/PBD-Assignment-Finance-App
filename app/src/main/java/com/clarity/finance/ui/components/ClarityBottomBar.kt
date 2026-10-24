package com.clarity.finance.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clarity.finance.ui.theme.ClarityBlack
import com.clarity.finance.ui.theme.ClarityBorder
import com.clarity.finance.ui.theme.ClarityTextMuted
import com.clarity.finance.ui.theme.ClarityTextPrimary
import com.clarity.finance.ui.theme.ClarityWhite

enum class ClarityNavTab {
    HOME,
    INCOME,
    INSIGHTS,
    GOAL
}

@Composable
fun ClarityBottomBar(
    currentTab: ClarityNavTab,
    onTabSelected: (ClarityNavTab) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Total container height accommodates raised FAB and curved notch
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // 1. Bottom Bar Base Container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .background(ClarityWhite)
                .border(BorderStroke(1.dp, ClarityBorder))
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left items
                ClarityBottomNavItem(
                    label = "HOME",
                    selectedIcon = Icons.Filled.Home,
                    unselectedIcon = Icons.Outlined.Home,
                    isSelected = currentTab == ClarityNavTab.HOME,
                    onClick = { onTabSelected(ClarityNavTab.HOME) },
                    modifier = Modifier.weight(1f)
                )

                ClarityBottomNavItem(
                    label = "INCOME",
                    selectedIcon = Icons.Filled.AccountBalanceWallet,
                    unselectedIcon = Icons.Outlined.AccountBalanceWallet,
                    isSelected = currentTab == ClarityNavTab.INCOME,
                    onClick = { onTabSelected(ClarityNavTab.INCOME) },
                    modifier = Modifier.weight(1f)
                )

                // Space reserved for the raised + FAB cradle
                Spacer(modifier = Modifier.width(60.dp))

                // Right items
                ClarityBottomNavItem(
                    label = "INSIGHTS",
                    selectedIcon = Icons.Filled.BarChart,
                    unselectedIcon = Icons.Outlined.BarChart,
                    isSelected = currentTab == ClarityNavTab.INSIGHTS,
                    onClick = { onTabSelected(ClarityNavTab.INSIGHTS) },
                    modifier = Modifier.weight(1f)
                )

                ClarityBottomNavItem(
                    label = "GOAL",
                    selectedIcon = Icons.Filled.EmojiEvents,
                    unselectedIcon = Icons.Outlined.EmojiEvents,
                    isSelected = currentTab == ClarityNavTab.GOAL,
                    onClick = { onTabSelected(ClarityNavTab.GOAL) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 2. Center Top Curved Cradle for the FAB
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 2.dp)
                .size(56.dp)
                .clip(CircleShape)
                .background(ClarityWhite)
                .border(BorderStroke(1.dp, ClarityBorder), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Fill bottom portion to merge seamlessly with navbar background
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(28.dp)
                    .background(ClarityWhite)
            )
        }

        // 3. Raised '+' Floating Action Button
        FloatingActionButton(
            onClick = onAddClick,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 6.dp)
                .size(46.dp),
            shape = CircleShape,
            containerColor = ClarityBlack,
            contentColor = ClarityWhite,
            elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp, pressedElevation = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Expense",
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun ClarityBottomNavItem(
    label: String,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = if (isSelected) selectedIcon else unselectedIcon,
            contentDescription = label,
            tint = if (isSelected) ClarityTextPrimary else ClarityTextMuted,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = label,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) ClarityTextPrimary else ClarityTextMuted
        )
    }
}
