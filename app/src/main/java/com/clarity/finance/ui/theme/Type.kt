package com.clarity.finance.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Apple's iOS text styles at the default ("Large") Dynamic Type size (HIG > Typography).
 *
 * Sizes are in sp, so they scale with the user's Android font-size setting just like Dynamic
 * Type. SF Pro is licensed for Apple platforms only, so the system sans-serif is used with
 * slightly tightened tracking to match SF's density. Amount styles use tabular figures so
 * digits line up in columns and don't shift while values change.
 */
object ClarityType {
    private const val TABULAR_FIGURES = "tnum"

    val largeTitle = TextStyle(fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp)
    val title1 = TextStyle(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp)
    val title2 = TextStyle(fontSize = 21.sp, lineHeight = 27.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp)
    val title3 = TextStyle(fontSize = 19.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.25).sp)
    val headline = TextStyle(fontSize = 16.5.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp)
    val body = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Normal, letterSpacing = (-0.15).sp)
    val callout = TextStyle(fontSize = 15.5.sp, lineHeight = 21.sp, fontWeight = FontWeight.Normal, letterSpacing = (-0.1).sp)
    val subheadline = TextStyle(fontSize = 14.5.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal, letterSpacing = (-0.05).sp)
    val footnote = TextStyle(fontSize = 12.5.sp, lineHeight = 17.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.sp)
    val caption1 = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.05.sp)
    val caption2 = TextStyle(fontSize = 11.sp, lineHeight = 13.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.08.sp)

    /** Hero balance figures (Wallet-style). */
    val amountHero = largeTitle.copy(fontFeatureSettings = TABULAR_FIGURES)

    /** Prominent amounts inside cards. */
    val amountTitle = title2.copy(fontFeatureSettings = TABULAR_FIGURES)

    /** Row-level amounts in lists. */
    val amountBody = body.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = TABULAR_FIGURES)

    /** Secondary amounts under stats and legends. */
    val amountCallout = callout.copy(fontWeight = FontWeight.SemiBold, fontFeatureSettings = TABULAR_FIGURES)

    /** Large editable amount in the add-expense sheet. */
    val amountEntry = TextStyle(
        fontSize = 52.sp,
        lineHeight = 60.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.8).sp,
        fontFeatureSettings = TABULAR_FIGURES
    )
}

/**
 * Material typography mapped onto the iOS scale, so the few Material components still in use
 * (menus, snackbars, progress text) match the rest of the interface.
 */
val Typography = Typography(
    displayLarge = ClarityType.largeTitle,
    displayMedium = ClarityType.title1,
    displaySmall = ClarityType.title2,
    headlineLarge = ClarityType.title1,
    headlineMedium = ClarityType.title2,
    headlineSmall = ClarityType.title3,
    titleLarge = ClarityType.title2,
    titleMedium = ClarityType.headline,
    titleSmall = ClarityType.subheadline.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = ClarityType.body,
    bodyMedium = ClarityType.subheadline,
    bodySmall = ClarityType.footnote,
    labelLarge = ClarityType.body,
    labelMedium = ClarityType.footnote.copy(fontWeight = FontWeight.Medium),
    labelSmall = ClarityType.caption2.copy(fontWeight = FontWeight.Medium)
)
