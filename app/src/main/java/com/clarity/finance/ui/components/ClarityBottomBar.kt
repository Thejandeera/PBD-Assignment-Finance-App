package com.clarity.finance.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clarity.finance.ui.theme.ClarityTheme
import com.clarity.finance.ui.theme.ClarityType
import kotlin.math.atan2
import kotlin.math.sqrt

enum class ClarityNavTab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Rounded.Home),
    INCOME("Income", Icons.Rounded.AccountBalanceWallet),
    INSIGHTS("Insights", Icons.Rounded.BarChart),
    GOAL("Goal", Icons.Rounded.Flag)
}

// Geometry of the raised centre button and the rounded bump that cradles it.
private val TabRowHeight = 54.dp
private val AddButtonSize = 56.dp
private val BumpRadius = 34.dp // button radius plus a 6dp rim of bar material
private val BumpFillet = 10.dp // softens the join between the bump and the bar's top edge
private val BumpRise = 30.dp // how far the bump rises above the bar's top edge
private val ButtonDrop = 4.dp // button centre sits this far below the bar's top edge

/**
 * Bottom padding for a tab screen's content. The space beside the bump is transparent, so
 * content scrolls beneath it instead of stopping in a hard line at the top of the bump.
 * Screens should end their scrolling content with at least [BumpRise] of spacing.
 */
fun PaddingValues.bottomPaddingBelowBump(): Dp =
    (calculateBottomPadding() - BumpRise).coerceAtLeast(0.dp)

/**
 * Tab bar with bar material, a top hairline, filled glyphs, short labels and the app tint on
 * the selected tab. When [onAddClick] is set, a raised "+" button for adding an expense sits
 * in the middle of the bar on a rounded bump, between Income and Insights.
 */
@Composable
fun ClarityBottomBar(
    currentTab: ClarityNavTab,
    onTabSelected: (ClarityNavTab) -> Unit,
    modifier: Modifier = Modifier,
    onAddClick: (() -> Unit)? = null
) {
    val colors = ClarityTheme.colors
    val tabs = ClarityNavTab.entries

    if (onAddClick == null) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .background(colors.barBackground)
        ) {
            Hairline()
            TabRow(tabs = tabs, currentTab = currentTab, onTabSelected = onTabSelected, centreGap = false)
        }
        return
    }

    val barColor = colors.barBackground
    val separatorColor = colors.separator
    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val edges = bumpedTopEdge(
                    width = size.width,
                    barTop = BumpRise.toPx(),
                    bumpRadius = BumpRadius.toPx(),
                    bumpCentreY = (BumpRise + ButtonDrop).toPx(),
                    fillet = BumpFillet.toPx()
                )
                val fill = Path().apply {
                    addPath(edges)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(fill, barColor)
                drawPath(edges, separatorColor, style = Stroke(width = 1f))
            }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.height(BumpRise))
            TabRow(tabs = tabs, currentTab = currentTab, onTabSelected = onTabSelected, centreGap = true)
        }

        AddExpenseCentreButton(
            onClick = onAddClick,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = BumpRise + ButtonDrop - AddButtonSize / 2)
        )
    }
}

@Composable
private fun TabRow(
    tabs: List<ClarityNavTab>,
    currentTab: ClarityNavTab,
    onTabSelected: (ClarityNavTab) -> Unit,
    centreGap: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .height(TabRowHeight)
            .selectableGroup(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        tabs.forEachIndexed { index, tab ->
            // Keeps the centre column free for the raised add button.
            if (centreGap && index == tabs.size / 2) Spacer(modifier = Modifier.weight(1f))
            TabBarItem(tab, tab == currentTab, { if (tab != currentTab) onTabSelected(tab) }, Modifier.weight(1f))
        }
    }
}

/** Filled, raised circular "+" that opens the add-expense sheet. */
@Composable
private fun AddExpenseCentreButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = AddButtonSize
) {
    val colors = ClarityTheme.colors
    Box(
        modifier = modifier
            .pressScale(onClickLabel = "Add expense", onClick = onClick)
            .size(size)
            .shadow(elevation = 6.dp, shape = CircleShape)
            .clip(CircleShape)
            .background(colors.accentFill),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = "Add expense",
            tint = colors.onAccent,
            modifier = Modifier.size(30.dp)
        )
    }
}

/**
 * The bar's top edge, left to right: flat at [barTop], rising through a [fillet] arc onto a
 * circular bump of [bumpRadius] centred at ([width] / 2, [bumpCentreY]), then back down.
 */
private fun bumpedTopEdge(
    width: Float,
    barTop: Float,
    bumpRadius: Float,
    bumpCentreY: Float,
    fillet: Float
): Path {
    val cx = width / 2f
    // Each fillet circle touches the flat edge from above and the bump from outside.
    val rise = bumpCentreY - barTop + fillet
    val dx = sqrt((bumpRadius + fillet) * (bumpRadius + fillet) - rise * rise)
    val joinAngle = Math.toDegrees(atan2(rise, dx).toDouble()).toFloat()

    fun circle(x: Float, y: Float, r: Float) = Rect(x - r, y - r, x + r, y + r)

    return Path().apply {
        moveTo(0f, barTop)
        lineTo(cx - dx, barTop)
        arcTo(circle(cx - dx, barTop - fillet, fillet), 90f, joinAngle - 90f, false)
        arcTo(circle(cx, bumpCentreY, bumpRadius), 180f + joinAngle, 180f - 2f * joinAngle, false)
        arcTo(circle(cx + dx, barTop - fillet, fillet), 180f - joinAngle, joinAngle - 90f, false)
        lineTo(width, barTop)
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
