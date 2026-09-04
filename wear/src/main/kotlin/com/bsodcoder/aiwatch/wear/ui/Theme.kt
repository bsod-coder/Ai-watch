package com.bsodcoder.aiwatch.wear.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.Typography
import androidx.wear.compose.material3.dynamicColorScheme

// Same restrained palette as the phone app: charcoal + off-white,
// a single muted teal accent. Used as the fixed fallback color scheme;
// on watches/OSes that expose a Material You dynamic palette we prefer
// that instead so the app matches the wearer's watch face / system theme.
private val Charcoal950 = Color(0xFF0E0F11)
private val Charcoal900 = Color(0xFF121316)
private val Charcoal800 = Color(0xFF1B1D21)
private val Charcoal700 = Color(0xFF26282D)
private val OffWhite = Color(0xFFE7E6E3)
private val Accent = Color(0xFF5FA8A0)
private val AccentContainer = Color(0xFF243B39)
private val ErrorRed = Color(0xFFCF6679)

private val AiWatchFallbackColorScheme = ColorScheme(
    primary = Accent,
    onPrimary = Charcoal950,
    primaryContainer = AccentContainer,
    onPrimaryContainer = OffWhite,
    secondary = Accent,
    onSecondary = Charcoal950,
    secondaryContainer = Charcoal700,
    onSecondaryContainer = OffWhite,
    tertiary = Accent,
    onTertiary = Charcoal950,
    tertiaryContainer = Charcoal700,
    onTertiaryContainer = OffWhite,
    background = Charcoal950,
    onBackground = OffWhite,
    surface = Charcoal900,
    onSurface = OffWhite,
    surfaceContainer = Charcoal800,
    surfaceContainerHigh = Charcoal700,
    surfaceContainerLow = Charcoal900,
    error = ErrorRed,
    onError = Charcoal950,
    errorContainer = Color(0xFF4A2328),
    onErrorContainer = ErrorRed
)

/**
 * Material You theme for the watch app. Prefers the system's dynamic color
 * scheme (derived from the user's watch face / wallpaper, same idea as
 * dynamic color on phones) when the platform provides one, and otherwise
 * falls back to our fixed charcoal/teal palette so the app still looks
 * intentional rather than defaulting to Wear's stock colors.
 */
@Composable
fun AiWatchWearTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val colorScheme = dynamicColorScheme(context) ?: AiWatchFallbackColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
