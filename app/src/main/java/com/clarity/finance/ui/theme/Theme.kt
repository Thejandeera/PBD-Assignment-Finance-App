package com.clarity.finance.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = ClarityBlack,
    onPrimary = ClarityWhite,
    secondary = ClarityTextSecondary,
    onSecondary = ClarityWhite,
    background = ClarityWhite,
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
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = ClarityWhite.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}