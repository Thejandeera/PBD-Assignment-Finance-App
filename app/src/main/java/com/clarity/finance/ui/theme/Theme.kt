package com.clarity.finance.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = ClarityGreenBright,
    onPrimary = Color(0xFF000000),
    secondary = ClarityBlue,
    onSecondary = ClarityWhite,
    background = ClarityDarkBackground,
    onBackground = ClarityDarkTextPrimary,
    surface = ClarityDarkSurface,
    onSurface = ClarityDarkTextPrimary,
    surfaceVariant = ClarityDarkSurfaceVariant,
    onSurfaceVariant = ClarityDarkTextSecondary,
    outline = ClarityDarkBorder,
    error = ClarityRed,
    onError = ClarityWhite
)

private val LightColorScheme = lightColorScheme(
    primary = ClarityGreen,
    onPrimary = ClarityWhite,
    secondary = ClarityBlue,
    onSecondary = ClarityWhite,
    background = ClarityGrayBackground,
    onBackground = ClarityTextPrimary,
    surface = ClarityWhite,
    onSurface = ClarityTextPrimary,
    surfaceVariant = ClarityGrayBackground,
    onSurfaceVariant = ClarityTextSecondary,
    outline = ClarityBorder,
    error = ClarityRed,
    onError = ClarityWhite
)

@Composable
fun ClarityFinanceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}