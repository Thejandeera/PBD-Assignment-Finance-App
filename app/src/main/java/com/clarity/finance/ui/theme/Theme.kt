package com.clarity.finance.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val LocalIsDarkTheme = compositionLocalOf { false }

/** Entry point for the app's design tokens, mirroring how MaterialTheme is read. */
object ClarityTheme {
    val colors: ClarityColors
        @Composable
        @ReadOnlyComposable
        get() = LocalClarityColors.current
}

/** Continuous-looking corner radii used throughout (iOS inset-grouped cards use ~10–12pt). */
object ClarityShapes {
    val card = RoundedCornerShape(12.dp)
    val control = RoundedCornerShape(10.dp)
    val button = RoundedCornerShape(12.dp)
    val sheet = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    val alert = RoundedCornerShape(14.dp)
}

private val ClarityMaterialShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp)
)

private fun ClarityColors.toMaterialColorScheme(): ColorScheme {
    val onAccentText = if (isDark) Color.Black else Color.White
    return if (isDark) {
        darkColorScheme(
            primary = accent,
            onPrimary = onAccentText,
            primaryContainer = accentFill,
            onPrimaryContainer = onAccent,
            inversePrimary = LightClarityColors.accent,
            secondary = blue,
            onSecondary = Color.White,
            tertiary = purple,
            onTertiary = Color.White,
            background = background,
            onBackground = label,
            surface = card,
            onSurface = label,
            surfaceVariant = elevatedCard,
            onSurfaceVariant = secondaryLabel,
            surfaceTint = Color.Transparent,
            inverseSurface = toastBackground,
            inverseOnSurface = Color.White,
            error = redText,
            onError = Color.Black,
            outline = separator,
            outlineVariant = separator,
            scrim = Color.Black,
            surfaceBright = elevatedCard,
            surfaceContainer = elevatedCard,
            surfaceContainerHigh = elevatedCard,
            surfaceContainerHighest = elevatedCard,
            surfaceContainerLow = card,
            surfaceContainerLowest = background,
            surfaceDim = background
        )
    } else {
        lightColorScheme(
            primary = accent,
            onPrimary = onAccentText,
            primaryContainer = accentFill,
            onPrimaryContainer = onAccent,
            inversePrimary = DarkClarityColors.accent,
            secondary = blue,
            onSecondary = Color.White,
            tertiary = purple,
            onTertiary = Color.White,
            background = background,
            onBackground = label,
            surface = card,
            onSurface = label,
            surfaceVariant = background,
            onSurfaceVariant = secondaryLabel,
            surfaceTint = Color.Transparent,
            inverseSurface = toastBackground,
            inverseOnSurface = Color.White,
            error = redText,
            onError = Color.White,
            outline = separator,
            outlineVariant = separator,
            scrim = Color.Black,
            surfaceBright = card,
            surfaceContainer = card,
            surfaceContainerHigh = card,
            surfaceContainerHighest = card,
            surfaceContainerLow = card,
            surfaceContainerLowest = card,
            surfaceDim = background
        )
    }
}

@Composable
fun ClarityFinanceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkClarityColors else LightClarityColors

    CompositionLocalProvider(
        LocalIsDarkTheme provides darkTheme,
        LocalClarityColors provides colors
    ) {
        MaterialTheme(
            colorScheme = colors.toMaterialColorScheme(),
            typography = Typography,
            shapes = ClarityMaterialShapes,
            content = content
        )
    }
}
