package com.bsodcoder.aiwatch.wear.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

// Same restrained palette as the phone app: charcoal + off-white,
// a single muted teal accent, no loud colours.
private val Charcoal900 = Color(0xFF121316)
private val Charcoal800 = Color(0xFF1B1D21)
private val OffWhite = Color(0xFFE7E6E3)
private val Accent = Color(0xFF5FA8A0)
private val ErrorRed = Color(0xFFCF6679)

private val AiWatchWearColors = Colors(
    primary = Accent,
    primaryVariant = Accent,
    secondary = Accent,
    secondaryVariant = Accent,
    background = Charcoal900,
    surface = Charcoal800,
    error = ErrorRed,
    onPrimary = Charcoal900,
    onSecondary = Charcoal900,
    onBackground = OffWhite,
    onSurface = OffWhite,
    onError = Charcoal900
)

@Composable
fun AiWatchWearTheme(content: @Composable () -> Unit) {
    MaterialTheme(colors = AiWatchWearColors, content = content)
}
