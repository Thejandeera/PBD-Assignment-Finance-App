package com.clarity.finance.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic color tokens modelled on Apple's iOS system colors (HIG > Color, Dark Mode).
 *
 * Screens read these through [ClarityTheme.colors] instead of hard-coding hex values, so every
 * surface adapts to light and dark appearance automatically. Fills use Apple's translucent
 * values so they read correctly on both grouped backgrounds and cards.
 */
@Immutable
data class ClarityColors(
    val isDark: Boolean,

    // Backgrounds (systemGroupedBackground family)
    val background: Color,
    val card: Color,
    val elevatedCard: Color,
    val sheetBackground: Color,
    val barBackground: Color,
    val alertBackground: Color,

    // Text (label family)
    val label: Color,
    val secondaryLabel: Color,
    val tertiaryLabel: Color,
    val placeholder: Color,

    // Separators, fills and control chrome
    val separator: Color,
    val fill: Color,
    val secondaryFill: Color,
    val tertiaryFill: Color,
    val quaternaryFill: Color,
    val highlight: Color,
    val segmentThumb: Color,
    val switchTrackOff: Color,
    val grabber: Color,
    val toastBackground: Color,

    // App tint — accessible green used for links, icons and filled buttons
    val accent: Color,
    val accentFill: Color,
    val onAccent: Color,

    // Vivid system colors — icon tiles, chart marks and status dots
    val green: Color,
    val red: Color,
    val orange: Color,
    val yellow: Color,
    val blue: Color,
    val indigo: Color,
    val purple: Color,
    val teal: Color,
    val pink: Color,
    val gray: Color,

    // Accessible text variants of the system colors (WCAG AA on cards)
    val greenText: Color,
    val redText: Color,
    val orangeText: Color,
    val blueText: Color,
    val purpleText: Color
) {
    /** Money coming in. */
    val income: Color get() = green

    /** Money going out. Only used for direction glyphs, never for whole amounts. */
    val outflow: Color get() = red

    /** Fixed obligations: rent, utilities, subscriptions. */
    val committed: Color get() = blue

    /** Flexible lifestyle spending. Orange, so red stays reserved for errors and deletion. */
    val discretionary: Color get() = orange

    /** Savings goal progress. */
    val goal: Color get() = purple
}

val LightClarityColors = ClarityColors(
    isDark = false,
    background = Color(0xFFF2F2F7),
    card = Color(0xFFFFFFFF),
    elevatedCard = Color(0xFFFFFFFF),
    sheetBackground = Color(0xFFF2F2F7),
    barBackground = Color(0xFFF9F9F9),
    alertBackground = Color(0xFFF9F9F9),
    label = Color(0xFF000000),
    secondaryLabel = Color(0xFF6C6C70),
    tertiaryLabel = Color(0xFFAEAEB2),
    placeholder = Color(0xFF8E8E93),
    separator = Color(0xFFC6C6C8),
    fill = Color(0x33787880),
    secondaryFill = Color(0x29787880),
    tertiaryFill = Color(0x1F767680),
    quaternaryFill = Color(0x14747480),
    highlight = Color(0xFFD1D1D6),
    segmentThumb = Color(0xFFFFFFFF),
    switchTrackOff = Color(0xFFE9E9EB),
    grabber = Color(0xFFC7C7CC),
    toastBackground = Color(0xFF2C2C2E),
    accent = Color(0xFF248A3D),
    accentFill = Color(0xFF248A3D),
    onAccent = Color(0xFFFFFFFF),
    green = Color(0xFF34C759),
    red = Color(0xFFFF3B30),
    orange = Color(0xFFFF9500),
    yellow = Color(0xFFFFCC00),
    blue = Color(0xFF007AFF),
    indigo = Color(0xFF5856D6),
    purple = Color(0xFFAF52DE),
    teal = Color(0xFF30B0C7),
    pink = Color(0xFFFF2D55),
    gray = Color(0xFF8E8E93),
    greenText = Color(0xFF248A3D),
    redText = Color(0xFFD70015),
    orangeText = Color(0xFFC93400),
    blueText = Color(0xFF0062CC),
    purpleText = Color(0xFF8944AB)
)

val DarkClarityColors = ClarityColors(
    isDark = true,
    background = Color(0xFF000000),
    card = Color(0xFF1C1C1E),
    elevatedCard = Color(0xFF2C2C2E),
    sheetBackground = Color(0xFF1C1C1E),
    barBackground = Color(0xFF161618),
    alertBackground = Color(0xFF2C2C2E),
    label = Color(0xFFFFFFFF),
    secondaryLabel = Color(0xFF98989F),
    tertiaryLabel = Color(0xFF636366),
    placeholder = Color(0xFF8E8E93),
    separator = Color(0xFF38383A),
    fill = Color(0x5C787880),
    secondaryFill = Color(0x52787880),
    tertiaryFill = Color(0x3D767680),
    quaternaryFill = Color(0x2E747480),
    highlight = Color(0xFF3A3A3C),
    segmentThumb = Color(0xFF636366),
    switchTrackOff = Color(0xFF39393D),
    grabber = Color(0xFF5A5A5F),
    toastBackground = Color(0xFF3A3A3C),
    accent = Color(0xFF30D158),
    accentFill = Color(0xFF248A3D),
    onAccent = Color(0xFFFFFFFF),
    green = Color(0xFF30D158),
    red = Color(0xFFFF453A),
    orange = Color(0xFFFF9F0A),
    yellow = Color(0xFFFFD60A),
    blue = Color(0xFF0A84FF),
    indigo = Color(0xFF5E5CE6),
    purple = Color(0xFFBF5AF2),
    teal = Color(0xFF40C8E0),
    pink = Color(0xFFFF375F),
    gray = Color(0xFF8E8E93),
    greenText = Color(0xFF30D158),
    redText = Color(0xFFFF6961),
    orangeText = Color(0xFFFF9F0A),
    blueText = Color(0xFF409CFF),
    purpleText = Color(0xFFBF5AF2)
)

val LocalClarityColors = staticCompositionLocalOf { LightClarityColors }

// -----------------------------------------------------------------------------
// Legacy palette constants. New UI code reads ClarityTheme.colors; these remain so
// code on other branches keeps compiling, and now point at the Apple system palette.
// -----------------------------------------------------------------------------
val ClarityBlack = Color(0xFF000000)
val ClarityWhite = Color(0xFFFFFFFF)
val ClarityGrayBackground = Color(0xFFF2F2F7)
val ClarityBorder = Color(0xFFC6C6C8)
val ClarityBorderDark = Color(0xFF38383A)
val ClarityTextPrimary = Color(0xFF000000)
val ClarityTextSecondary = Color(0xFF6C6C70)
val ClarityTextMuted = Color(0xFF8E8E93)

val ClarityGreen = Color(0xFF248A3D)
val ClarityGreenBright = Color(0xFF34C759)
val ClarityGreenLight = Color(0xFFE8F7EC)
val ClarityGreenDark = Color(0xFF1B6E30)

val ClarityRed = Color(0xFFFF3B30)
val ClarityRedLight = Color(0xFFFFECEB)
val ClarityRedDark = Color(0xFFD70015)

val ClarityBlue = Color(0xFF007AFF)
val ClarityBlueLight = Color(0xFFE5F1FF)
val ClarityBlueDark = Color(0xFF0062CC)

val ClarityPurple = Color(0xFFAF52DE)
val ClarityPurpleLight = Color(0xFFF5EBFB)

val ClarityAmber = Color(0xFFFF9500)
val ClarityAmberLight = Color(0xFFFFF4E5)
val ClarityAmberDark = Color(0xFFC93400)

val ClarityDarkBackground = Color(0xFF000000)
val ClarityDarkSurface = Color(0xFF1C1C1E)
val ClarityDarkSurfaceVariant = Color(0xFF2C2C2E)
val ClarityDarkBorder = Color(0xFF38383A)
val ClarityDarkTextPrimary = Color(0xFFFFFFFF)
val ClarityDarkTextSecondary = Color(0xFF98989F)
val ClarityDarkTextMuted = Color(0xFF8E8E93)
