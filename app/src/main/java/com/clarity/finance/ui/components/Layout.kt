package com.clarity.finance.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clarity.finance.ui.theme.ClarityShapes
import com.clarity.finance.ui.theme.ClarityTheme
import com.clarity.finance.ui.theme.ClarityType
import java.util.Locale

// =============================================================================
// Separators
// =============================================================================

/** One-physical-pixel separator, matching iOS table separators. */
@Composable
fun Hairline(
    modifier: Modifier = Modifier,
    color: Color = ClarityTheme.colors.separator
) {
    val thickness = with(LocalDensity.current) { 1f.toDp() }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(thickness)
            .background(color)
    )
}

@Composable
fun VerticalHairline(
    modifier: Modifier = Modifier,
    color: Color = ClarityTheme.colors.separator
) {
    val thickness = with(LocalDensity.current) { 1f.toDp() }
    Box(
        modifier = modifier
            .width(thickness)
            .background(color)
    )
}

// =============================================================================
// Navigation chrome
// =============================================================================

/** True once a scrolling column has moved its large title out of view. */
@Composable
fun ScrollState.isScrolledPast(threshold: Dp = 44.dp): Boolean {
    val thresholdPx = with(LocalDensity.current) { threshold.toPx() }
    val scrolledPast by remember(this, thresholdPx) { derivedStateOf { value > thresholdPx } }
    return scrolledPast
}

/** True once a lazy list has moved its large-title item out of view. */
@Composable
fun LazyListState.isScrolledPast(threshold: Dp = 44.dp): Boolean {
    val thresholdPx = with(LocalDensity.current) { threshold.toPx() }
    val scrolledPast by remember(this, thresholdPx) {
        derivedStateOf { firstVisibleItemIndex > 0 || firstVisibleItemScrollOffset > thresholdPx }
    }
    return scrolledPast
}

/**
 * Large title block that begins a screen's scrolling content (HIG > Navigation bars).
 * [trailing] holds the screen's toolbar buttons, aligned with the title baseline area.
 */
@Composable
fun LargeTitleHeader(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    subtitle: String? = null,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    val colors = ClarityTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp, top = 6.dp, bottom = 10.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (eyebrow != null) {
                Text(
                    text = eyebrow.uppercase(Locale.getDefault()),
                    style = ClarityType.footnote.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.secondaryLabel
                )
            }
            Text(
                text = title,
                style = ClarityType.largeTitle,
                color = colors.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = ClarityType.subheadline,
                    color = colors.secondaryLabel
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            content = trailing
        )
    }
}

/**
 * Status-bar backdrop plus an inline title that fades in once the large title scrolls away.
 * Overlay it on top of a tab's root content.
 */
@Composable
fun CollapsingTitleBar(
    title: String,
    collapsed: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = ClarityTheme.colors
    val barColor by animateColorAsState(
        targetValue = if (collapsed) colors.barBackground else colors.background,
        animationSpec = tween(durationMillis = 200),
        label = "collapsingBarColor"
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            // Like a real navigation bar, the overlay must not pass taps to content beneath it.
            .pointerInput(Unit) {}
            .background(barColor)
    ) {
        Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
        AnimatedVisibility(
            visible = collapsed,
            enter = fadeIn(tween(180)) + expandVertically(tween(220)),
            exit = fadeOut(tween(150)) + shrinkVertically(tween(200))
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        style = ClarityType.headline,
                        color = colors.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Hairline()
            }
        }
    }
}

/**
 * Navigation bar for pushed screens: back chevron, centered inline title and trailing actions.
 * The bar material and hairline appear once content scrolls under it, as in iOS.
 */
@Composable
fun ClarityNavigationBar(
    title: String,
    modifier: Modifier = Modifier,
    collapsed: Boolean = true,
    onBack: (() -> Unit)? = null,
    backLabel: String = "Back",
    trailing: @Composable RowScope.() -> Unit = {}
) {
    val colors = ClarityTheme.colors
    val barColor by animateColorAsState(
        targetValue = if (collapsed) colors.barBackground else colors.background,
        animationSpec = tween(durationMillis = 200),
        label = "navigationBarColor"
    )
    val titleAlpha by animateFloatAsState(
        targetValue = if (collapsed) 1f else 0f,
        animationSpec = tween(durationMillis = 180),
        label = "navigationTitleAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(barColor)
            .statusBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
        ) {
            if (onBack != null) {
                Row(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .heightIn(min = 44.dp)
                        .pressFade(onClickLabel = "Go back", onClick = onBack)
                        .padding(start = 10.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBackIos,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = backLabel,
                        style = ClarityType.body,
                        color = colors.accent,
                        maxLines = 1
                    )
                }
            }
            Text(
                text = title,
                style = ClarityType.headline,
                color = colors.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 104.dp)
                    .alpha(titleAlpha)
                    .semantics { heading() }
            )
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = trailing
            )
        }
        Hairline(color = colors.separator.copy(alpha = titleAlpha))
    }
}

// =============================================================================
// Containers
// =============================================================================

/** Rounded card on the grouped background (secondarySystemGroupedBackground). */
@Composable
fun ClarityCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    color: Color = ClarityTheme.colors.card,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.pressScale(onClickLabel = onClickLabel, pressedScale = 0.98f, onClick = onClick)
                } else {
                    Modifier
                }
            )
            .clip(ClarityShapes.card)
            .background(color)
            .padding(contentPadding),
        content = content
    )
}

/** Inset-grouped list section with optional uppercase header and explanatory footer. */
@Composable
fun GroupedSection(
    modifier: Modifier = Modifier,
    header: String? = null,
    footer: String? = null,
    containerColor: Color = ClarityTheme.colors.card,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        if (header != null) SectionHeaderLabel(header)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(ClarityShapes.card)
                .background(containerColor),
            content = content
        )
        if (footer != null) SectionFooterLabel(footer)
    }
}

@Composable
fun SectionHeaderLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(Locale.getDefault()),
        style = ClarityType.footnote,
        color = ClarityTheme.colors.secondaryLabel,
        modifier = modifier
            .padding(start = 16.dp, end = 16.dp, bottom = 7.dp)
            .semantics { heading() }
    )
}

@Composable
fun SectionFooterLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = ClarityType.footnote,
        color = ClarityTheme.colors.secondaryLabel,
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 7.dp)
    )
}

/** Bold section title with an optional trailing link, as on Health/Fitness summary screens. */
@Composable
fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = ClarityType.title3.copy(fontWeight = FontWeight.Bold),
            color = ClarityTheme.colors.label,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp)
                .semantics { heading() }
        )
        if (actionLabel != null && onAction != null) {
            TextLinkButton(text = actionLabel, onClick = onAction, style = ClarityType.body)
        }
    }
}

// =============================================================================
// Rows
// =============================================================================

/**
 * Table-view style row: optional leading icon, title + subtitle, trailing accessory and an
 * inset separator that starts at the text, exactly like UITableView cells.
 */
@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    titleColor: Color = Color.Unspecified,
    titleStyle: TextStyle = ClarityType.body,
    showChevron: Boolean = false,
    showDivider: Boolean = false,
    onClick: (() -> Unit)? = null,
    onClickLabel: String? = null,
    role: Role? = Role.Button,
    below: (@Composable ColumnScope.() -> Unit)? = null
) {
    val colors = ClarityTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.rowHighlight(role = role, onClickLabel = onClickLabel, onClick = onClick)
                } else {
                    Modifier.semantics(mergeDescendants = true) {}
                }
            )
            .padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .padding(top = 10.dp, bottom = 10.dp, end = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = titleStyle,
                        color = titleColor.takeOrElse { colors.label },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtitle != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            style = ClarityType.subheadline,
                            color = colors.secondaryLabel,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (below != null) below()
                }
                if (trailing != null) {
                    Spacer(modifier = Modifier.width(10.dp))
                    trailing()
                }
                if (showChevron) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                        contentDescription = null,
                        tint = colors.tertiaryLabel,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            if (showDivider) Hairline()
        }
    }
}

/** Row whose whole surface toggles an iOS switch — one large target, one TalkBack node. */
@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    showDivider: Boolean = false
) {
    ListRow(
        title = title,
        subtitle = subtitle,
        leading = leading,
        showDivider = showDivider,
        titleColor = if (enabled) Color.Unspecified else ClarityTheme.colors.secondaryLabel,
        trailing = { ClaritySwitch(checked = checked, onCheckedChange = null, enabled = enabled) },
        modifier = modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Switch,
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onValueChange = onCheckedChange
        )
    )
}

/** Single-choice row with a trailing checkmark, as in iOS Settings selection lists. */
@Composable
fun CheckmarkRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    detail: String? = null,
    leading: (@Composable () -> Unit)? = null,
    showDivider: Boolean = false
) {
    val colors = ClarityTheme.colors
    ListRow(
        title = title,
        leading = leading,
        showDivider = showDivider,
        onClick = onClick,
        role = Role.RadioButton,
        modifier = modifier.semantics { this.selected = selected },
        trailing = {
            if (detail != null) {
                Text(text = detail, style = ClarityType.body, color = colors.secondaryLabel)
                Spacer(modifier = Modifier.width(12.dp))
            }
            Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    )
}

// =============================================================================
// Icons & badges
// =============================================================================

/** Rounded-square glyph tile, as used for rows in iOS Settings. */
@Composable
fun IconTile(
    icon: ImageVector,
    background: Color,
    modifier: Modifier = Modifier,
    size: Dp = 30.dp,
    contentDescription: String? = null
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.24f))
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(size * 0.6f)
        )
    }
}

/** Category emoji in a softly tinted circle. */
@Composable
fun EmojiBadge(
    emoji: String,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(tint.copy(alpha = if (ClarityTheme.colors.isDark) 0.24f else 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Text(text = emoji.ifBlank { "🏷️" }, fontSize = (size.value * 0.45f).sp)
    }
}

/** Small colored dot paired with a text label (never color alone). */
@Composable
fun LegendDot(color: Color, modifier: Modifier = Modifier, size: Dp = 8.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color)
    )
}

// =============================================================================
// Progress
// =============================================================================

/** Capsule progress bar with an animated fill. [progress] is clamped to 0..1. */
@Composable
fun CapsuleProgressBar(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    trackColor: Color = ClarityTheme.colors.tertiaryFill,
    height: Dp = 6.dp
) {
    val clamped = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = clamped,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "capsuleProgress"
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(trackColor)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(clamped, 0f..1f) }
    ) {
        if (animatedProgress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

/** Horizontal stacked bar (like iPhone Storage) with thin gaps between segments. */
@Composable
fun StackedBar(
    segments: List<Pair<Float, Color>>,
    modifier: Modifier = Modifier,
    height: Dp = 12.dp
) {
    val visible = segments.filter { it.first > 0f }
    val total = visible.sumOf { it.first.toDouble() }.toFloat()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(if (total > 0f) Color.Transparent else ClarityTheme.colors.tertiaryFill),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (total > 0f) {
            visible.forEach { (value, color) ->
                Box(
                    modifier = Modifier
                        .weight(value / total)
                        .fillMaxHeight()
                        .background(color)
                )
            }
        }
    }
}

// =============================================================================
// Empty states & sheets
// =============================================================================

/** ContentUnavailableView equivalent: symbol, title, explanation and an optional action. */
@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val colors = ClarityTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.secondaryLabel,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = title,
            style = ClarityType.title3.copy(fontWeight = FontWeight.Bold),
            color = colors.label,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = message,
            style = ClarityType.subheadline,
            color = colors.secondaryLabel,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(8.dp))
            TextLinkButton(
                text = actionLabel,
                onClick = onAction,
                style = ClarityType.body.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }
}

/** Sheet grabber capsule. */
@Composable
fun SheetGrabber(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(top = 6.dp, bottom = 2.dp)
            .size(width = 36.dp, height = 5.dp)
            .clip(CircleShape)
            .background(ClarityTheme.colors.grabber)
    )
}
