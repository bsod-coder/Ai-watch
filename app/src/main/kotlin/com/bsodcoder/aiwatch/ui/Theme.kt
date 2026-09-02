package com.bsodcoder.aiwatch.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Deliberately restrained, near-monochrome palette: charcoal surfaces,
// off-white text, a single muted teal accent used sparingly.
private val Charcoal900 = Color(0xFF121316)
private val Charcoal800 = Color(0xFF1B1D21)
private val Charcoal700 = Color(0xFF25272C)
private val OffWhite = Color(0xFFE7E6E3)
private val MutedGray = Color(0xFF9B9DA3)
private val Accent = Color(0xFF5FA8A0)

private val AiWatchColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = Charcoal900,
    background = Charcoal900,
    onBackground = OffWhite,
    surface = Charcoal800,
    onSurface = OffWhite,
    surfaceVariant = Charcoal700,
    onSurfaceVariant = MutedGray,
    outline = MutedGray
)

@Composable
fun AiWatchTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AiWatchColorScheme,
        content = content
    )
}
