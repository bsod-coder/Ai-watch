package com.aiwatch.phone.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Graphite + sage.
 *
 * One muted accent, warm near-neutral greys, and no saturated hues anywhere.
 * Contrast still clears WCAG AA for body text in both schemes.
 */
object Palette {
    // Light
    val LightBackground = Color(0xFFF3F1ED)
    val LightSurface = Color(0xFFFBFAF7)
    val LightSurfaceVariant = Color(0xFFE9E5DE)
    val LightSurfaceContainer = Color(0xFFEFEBE4)
    val LightOnSurface = Color(0xFF1A1B1E)
    val LightOnSurfaceVariant = Color(0xFF5C5B57)
    val LightOutline = Color(0xFFD3CFC8)
    val LightOutlineVariant = Color(0xFFE2DED7)
    val LightPrimary = Color(0xFF4E7268)
    val LightOnPrimary = Color(0xFFF6F8F6)
    val LightPrimaryContainer = Color(0xFFDDE7E1)
    val LightOnPrimaryContainer = Color(0xFF274039)

    // Dark
    val DarkBackground = Color(0xFF131417)
    val DarkSurface = Color(0xFF1B1D21)
    val DarkSurfaceVariant = Color(0xFF23262B)
    val DarkSurfaceContainer = Color(0xFF1F2226)
    val DarkOnSurface = Color(0xFFE9E7E3)
    val DarkOnSurfaceVariant = Color(0xFFA5A49F)
    val DarkOutline = Color(0xFF33373D)
    val DarkOutlineVariant = Color(0xFF2A2D32)
    val DarkPrimary = Color(0xFF8FAEA4)
    val DarkOnPrimary = Color(0xFF10130F)
    val DarkPrimaryContainer = Color(0xFF2C3A35)
    val DarkOnPrimaryContainer = Color(0xFFD6E2DC)

    // Shared semantic colours, kept desaturated on purpose.
    val SuccessLight = Color(0xFF4F7A5A)
    val SuccessDark = Color(0xFF8FB99A)
    val WarningLight = Color(0xFF8A6A3F)
    val WarningDark = Color(0xFFC9A876)
    val DangerLight = Color(0xFF8E5550)
    val DangerDark = Color(0xFFC99490)
}
