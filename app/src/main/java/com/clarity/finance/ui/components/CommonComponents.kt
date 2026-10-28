package com.clarity.finance.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.clarity.finance.ui.theme.ClarityShapes
import com.clarity.finance.ui.theme.ClarityTheme
import com.clarity.finance.ui.theme.ClarityType

// =============================================================================
// Text input
// =============================================================================

/**
 * Rounded, filled text field in the iOS style: a persistent label above the field (so it stays
 * visible while typing), a placeholder inside, and an inline error message below.
 */
@Composable
fun ClarityTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingText: String? = null,
    onTrailingTextClick: (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    isError: Boolean = false,
    errorMessage: String? = null,
    trailingIcon: ImageVector? = null,
    trailingIconContentDescription: String? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    containerColor: Color = Color.Unspecified,
    enabled: Boolean = true
) {
    val colors = ClarityTheme.colors
    val fieldColor = containerColor.takeOrElse { colors.tertiaryFill }
    val hasTrailing = trailingIcon != null || trailingText != null

    Column(modifier = modifier.fillMaxWidth()) {
        if (label.isNotEmpty()) {
            Text(
                text = label,
                style = ClarityType.footnote.copy(fontWeight = FontWeight.Medium),
                color = colors.secondaryLabel,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            textStyle = ClarityType.body.copy(color = if (enabled) colors.label else colors.secondaryLabel),
            cursorBrush = SolidColor(colors.accent),
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .clip(ClarityShapes.control)
                        .background(fieldColor)
                        .then(
                            if (isError) Modifier.border(1.dp, colors.redText, ClarityShapes.control)
                            else Modifier
                        )
                        .padding(start = 14.dp, end = if (hasTrailing) 4.dp else 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 14.dp)
                    ) {
                        if (value.isEmpty() && placeholder.isNotEmpty()) {
                            Text(
                                text = placeholder,
                                style = ClarityType.body,
                                color = colors.placeholder,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        innerTextField()
                    }
                    when {
                        trailingIcon != null -> {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .then(
                                        if (onTrailingIconClick != null) Modifier.pressFade(onClick = onTrailingIconClick)
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = trailingIcon,
                                    contentDescription = trailingIconContentDescription,
                                    tint = colors.secondaryLabel,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        trailingText != null -> {
                            Box(
                                modifier = Modifier
                                    .heightIn(min = 44.dp)
                                    .then(
                                        if (onTrailingTextClick != null) Modifier.pressFade(onClick = onTrailingTextClick)
                                        else Modifier
                                    )
                                    .padding(horizontal = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = trailingText,
                                    style = ClarityType.subheadline.copy(fontWeight = FontWeight.SemiBold),
                                    color = colors.accent
                                )
                            }
                        }
                    }
                }
            }
        )
        if (isError && !errorMessage.isNullOrEmpty()) {
            Text(
                text = errorMessage,
                style = ClarityType.footnote,
                color = colors.redText,
                modifier = Modifier.padding(start = 4.dp, top = 6.dp)
            )
        }
    }
}

/** iOS search bar: magnifying glass, placeholder and a clear button once text is entered. */
@Composable
fun ClaritySearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search",
    onSearch: () -> Unit = {}
) {
    val colors = ClarityTheme.colors
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = ClarityType.body.copy(color = colors.label),
        cursorBrush = SolidColor(colors.accent),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        modifier = modifier.fillMaxWidth(),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(ClarityShapes.control)
                    .background(colors.tertiaryFill)
                    .padding(start = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = colors.placeholder,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = ClarityType.body,
                            color = colors.placeholder,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    innerTextField()
                }
                if (query.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .pressFade(onClickLabel = "Clear search") { onQueryChange("") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Cancel,
                            contentDescription = "Clear search",
                            tint = colors.placeholder,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        }
    )
}

// =============================================================================
// Buttons
// =============================================================================

/** Filled, full-width primary action (iOS "borderedProminent" large button). */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null,
    minHeight: Dp = 50.dp
) {
    val colors = ClarityTheme.colors
    val isActive = enabled && !isLoading
    val containerColor = if (enabled || isLoading) colors.accentFill else colors.fill
    val contentColor = if (enabled || isLoading) colors.onAccent else colors.tertiaryLabel

    Box(
        modifier = modifier
            .pressScale(enabled = isActive, onClick = onClick)
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .clip(ClarityShapes.button)
            .background(containerColor)
            .semantics { if (isLoading) stateDescription = "Loading" }
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            ActivityIndicator(color = contentColor, size = 22.dp)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    style = ClarityType.headline,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Tinted secondary action (iOS "bordered" button): translucent tint fill with tinted label. */
@Composable
fun TintedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    tint: Color = ClarityTheme.colors.accent,
    minHeight: Dp = 50.dp,
    textStyle: TextStyle = ClarityType.headline
) {
    val colors = ClarityTheme.colors
    val contentColor = if (enabled) tint else colors.tertiaryLabel
    val containerColor = if (enabled) tint.copy(alpha = if (colors.isDark) 0.22f else 0.13f) else colors.quaternaryFill

    Box(
        modifier = modifier
            .pressScale(enabled = enabled, onClick = onClick)
            .heightIn(min = minHeight)
            .clip(ClarityShapes.button)
            .background(containerColor)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                style = textStyle,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Borderless text button (iOS "plain" button) with a 44dp minimum touch target. */
@Composable
fun TextLinkButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = ClarityTheme.colors.accent,
    style: TextStyle = ClarityType.body
) {
    Box(
        modifier = modifier
            .heightIn(min = 44.dp)
            .pressFade(enabled = enabled, onClick = onClick)
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = style,
            color = if (enabled) color else ClarityTheme.colors.tertiaryLabel,
            maxLines = 1
        )
    }
}

/** Circular toolbar button, e.g. the "+" add action in a large-title header. */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    enabled: Boolean = true,
    diameter: Dp = 34.dp
) {
    val colors = ClarityTheme.colors
    Box(
        modifier = modifier
            .size(44.dp)
            .pressFade(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(diameter)
                .clip(CircleShape)
                .background(if (filled) colors.accentFill else colors.tertiaryFill),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = when {
                    !enabled -> colors.tertiaryLabel
                    filled -> colors.onAccent
                    else -> colors.accent
                },
                modifier = Modifier.size(diameter * 0.6f)
            )
        }
    }
}

// =============================================================================
// Selection controls
// =============================================================================

/**
 * iOS segmented control: a gray track with a raised thumb that slides to the selected segment.
 * Use for 2–5 mutually exclusive, equally weighted options.
 */
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ClarityTheme.colors
    val haptics = LocalHapticFeedback.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(colors.tertiaryFill)
            .padding(2.dp)
    ) {
        val segmentWidth = maxWidth / options.size.coerceAtLeast(1)
        val thumbOffset by animateDpAsState(
            targetValue = segmentWidth * selectedIndex.coerceAtLeast(0),
            animationSpec = spring(dampingRatio = 0.86f, stiffness = 520f),
            label = "segmentThumbOffset"
        )

        if (selectedIndex in options.indices) {
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset)
                    .width(segmentWidth)
                    .fillMaxHeight()
                    .shadow(
                        elevation = if (colors.isDark) 0.dp else 1.5.dp,
                        shape = RoundedCornerShape(7.dp),
                        clip = false
                    )
                    .background(colors.segmentThumb, RoundedCornerShape(7.dp))
            )
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .selectableGroup()
        ) {
            options.forEachIndexed { index, option ->
                val isSelected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .selectable(
                            selected = isSelected,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            role = Role.Tab,
                            onClick = {
                                if (!isSelected) {
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelect(index)
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option,
                        style = ClarityType.footnote.copy(
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                        ),
                        color = colors.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                }
            }
        }
    }
}

/**
 * iOS-style switch (51 × 31). Pass `onCheckedChange = null` when the surrounding row handles
 * the toggle, so the whole row is one large touch target with a single accessibility node.
 */
@Composable
fun ClaritySwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = ClarityTheme.colors
    val trackColor by animateColorAsState(
        targetValue = if (checked) colors.green else colors.switchTrackOff,
        animationSpec = tween(durationMillis = 220),
        label = "switchTrack"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 20.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 650f),
        label = "switchThumb"
    )
    val toggleModifier = if (onCheckedChange != null) {
        Modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Switch,
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onValueChange = onCheckedChange
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .then(toggleModifier)
            .alpha(if (enabled) 1f else 0.45f)
            .size(width = 51.dp, height = 31.dp)
            .clip(CircleShape)
            .background(trackColor)
            .padding(2.dp)
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(27.dp)
                .shadow(elevation = 2.dp, shape = CircleShape)
                .background(Color.White, CircleShape)
        )
    }
}

// =============================================================================
// Notes & callouts
// =============================================================================

/** Quiet explanatory note with a leading symbol — the iOS footnote-callout pattern. */
@Composable
fun InfoNote(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Rounded.Info,
    emoji: String? = null,
    iconTint: Color = ClarityTheme.colors.secondaryLabel
) {
    val colors = ClarityTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(ClarityShapes.control)
            .background(colors.quaternaryFill)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        if (emoji != null) {
            Text(text = emoji, style = ClarityType.subheadline)
        } else {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier
                    .padding(top = 1.dp)
                    .size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = ClarityType.footnote,
            color = colors.secondaryLabel
        )
    }
}

/** Former dashed info box, kept for existing callers; now renders as an [InfoNote]. */
@Composable
fun DashedInfoBox(
    text: String,
    modifier: Modifier = Modifier,
    prefix: String = "[i]"
) {
    InfoNote(
        text = text,
        modifier = modifier,
        emoji = prefix.takeUnless { it == "[i]" }
    )
}

/** Tappable informational callout. */
@Composable
fun DemoModeCallout(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    InfoNote(
        text = text,
        modifier = modifier.pressFade(onClick = onClick)
    )
}

