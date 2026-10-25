package com.clarity.finance.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.clarity.finance.ui.theme.ClarityShapes
import com.clarity.finance.ui.theme.ClarityTheme
import com.clarity.finance.ui.theme.ClarityType

// =============================================================================
// Activity indicator
// =============================================================================

private const val SPOKE_COUNT = 8

/** The iOS spoked activity indicator, for indeterminate waits. */
@Composable
fun ActivityIndicator(
    modifier: Modifier = Modifier,
    color: Color = ClarityTheme.colors.secondaryLabel,
    size: Dp = 24.dp
) {
    val transition = rememberInfiniteTransition(label = "activityIndicator")
    val step by transition.animateValue(
        initialValue = 0,
        targetValue = SPOKE_COUNT,
        typeConverter = Int.VectorConverter,
        animationSpec = infiniteRepeatable(tween(durationMillis = 800, easing = LinearEasing)),
        label = "activityIndicatorStep"
    )

    Canvas(
        modifier = modifier
            .size(size)
            .semantics {
                contentDescription = "Loading"
                progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
            }
    ) {
        val dimension = this.size.minDimension
        val strokeWidth = dimension * 0.1f
        val outerRadius = dimension / 2f - strokeWidth / 2f
        val innerRadius = dimension * 0.24f
        val head = step % SPOKE_COUNT
        for (index in 0 until SPOKE_COUNT) {
            val distanceFromHead = (head - index + SPOKE_COUNT) % SPOKE_COUNT
            val alpha = 1f - distanceFromHead * (0.85f / SPOKE_COUNT)
            rotate(degrees = index * (360f / SPOKE_COUNT)) {
                drawLine(
                    color = color.copy(alpha = color.alpha * alpha),
                    start = Offset(center.x, center.y - innerRadius),
                    end = Offset(center.x, center.y - outerRadius),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

// =============================================================================
// Alerts
// =============================================================================

enum class AlertActionStyle { Default, Cancel, Destructive, Preferred }

data class AlertAction(
    val label: String,
    val style: AlertActionStyle = AlertActionStyle.Default,
    val enabled: Boolean = true,
    val onClick: () -> Unit
)

/**
 * iOS alert: centered title and message with full-width action buttons separated by hairlines.
 * Two actions sit side by side (Cancel on the leading side); more stack vertically.
 */
@Composable
fun ClarityAlertDialog(
    title: String,
    actions: List<AlertAction>,
    onDismissRequest: () -> Unit,
    message: String? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null
) {
    val colors = ClarityTheme.colors
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .width(290.dp)
                .clip(ClarityShapes.alert)
                .background(colors.alertBackground)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = ClarityType.headline,
                    color = colors.label,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() }
                )
                if (!message.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = message,
                        style = ClarityType.footnote,
                        color = colors.label,
                        textAlign = TextAlign.Center
                    )
                }
                if (content != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    content()
                }
            }

            Hairline()
            if (actions.size == 2) {
                Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                    AlertButton(actions[0], Modifier.weight(1f))
                    VerticalHairline(modifier = Modifier.fillMaxHeight())
                    AlertButton(actions[1], Modifier.weight(1f))
                }
            } else {
                actions.forEachIndexed { index, action ->
                    if (index > 0) Hairline()
                    AlertButton(action, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun AlertButton(action: AlertAction, modifier: Modifier) {
    val colors = ClarityTheme.colors
    val textColor = when {
        !action.enabled -> colors.tertiaryLabel
        action.style == AlertActionStyle.Destructive -> colors.redText
        else -> colors.accent
    }
    val weight = when (action.style) {
        AlertActionStyle.Cancel, AlertActionStyle.Preferred -> FontWeight.SemiBold
        else -> FontWeight.Normal
    }
    Box(
        modifier = modifier
            .heightIn(min = 46.dp)
            .rowHighlight(enabled = action.enabled, onClick = action.onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = action.label,
            style = ClarityType.body.copy(fontWeight = weight),
            color = textColor,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

// =============================================================================
// Toasts
// =============================================================================

/** Snackbar host restyled as a rounded, floating iOS-style banner. */
@Composable
fun ClaritySnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val colors = ClarityTheme.colors
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth()
                .shadow(elevation = 10.dp, shape = ClarityShapes.alert)
                .background(colors.toastBackground, ClarityShapes.alert)
                .semantics { liveRegion = LiveRegionMode.Polite }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = data.visuals.message,
                style = ClarityType.subheadline,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            val actionLabel = data.visuals.actionLabel
            if (actionLabel != null) {
                Spacer(modifier = Modifier.width(12.dp))
                TextLinkButton(
                    text = actionLabel,
                    onClick = { data.performAction() },
                    color = ClarityTheme.colors.green,
                    style = ClarityType.subheadline.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}

// =============================================================================
// Inline errors
// =============================================================================

/** Inline validation or server error shown next to the form that caused it. */
@Composable
fun InlineErrorMessage(
    message: String,
    modifier: Modifier = Modifier
) {
    val colors = ClarityTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = Icons.Rounded.Error,
            contentDescription = "Error",
            tint = colors.redText,
            modifier = Modifier
                .padding(top = 1.dp)
                .size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = message, style = ClarityType.footnote, color = colors.redText)
    }
}
