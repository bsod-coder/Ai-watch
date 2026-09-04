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
//
// NOTE: Wear Compose Material 3's ColorScheme has a different (smaller,
// differently-named) set of 28 parameters than the phone's
// androidx.compose.material3.ColorScheme -- notably there is no plain
// `surface`, `surfaceVariant`, `scrim`, `surfaceTint` or
// `inverseSurface`/`inverseOnSurface` here, and there IS a `primaryDim` /
// `secondaryDim` / `tertiaryDim` per color group instead. See
// https://developer.android.com/training/wearables/compose/migrate-to-material3
// or androidx.wear.compose.material3.ColorScheme in the library sources
// for the authoritative parameter list -- don't guess against the phone
// Material 3 API, they've diverged.
private val Charcoal950 = Color(0xFF0E0F11)
private val Charcoal900 = Color(0xFF121316)
private val Charcoal800 = Color(0xFF1B1D21)
private val Charcoal700 = Color(0xFF26282D)
private val OffWhite = Color(0xFFE7E6E3)
private val MutedGrey = Color(0xFFB8B7B4)
private val Outline = Color(0xFF4A4C50)
private val Accent = Color(0xFF5FA8A0)
private val AccentDim = Color(0xFF478079)
private val AccentContainer = Color(0xFF243B39)
private val ErrorRed = Color(0xFFCF6679)
private val ErrorContainer = Color(0xFF4A2328)

private val AiWatchFallbackColorScheme = ColorScheme(
    primary = Accent,
    primaryDim = AccentDim,
    primaryContainer = AccentContainer,
    onPrimary = Charcoal950,
    onPrimaryContainer = OffWhite,
    secondary = Accent,
    secondaryDim = AccentDim,
    secondaryContainer = Charcoal700,
    onSecondary = Charcoal950,
    onSecondaryContainer = OffWhite,
    tertiary = Accent,
    tertiaryDim = AccentDim,
    tertiaryContainer = Charcoal700,
    onTertiary = Charcoal950,
    onTertiaryContainer = OffWhite,
    surfaceContainerLow = Charcoal900,
    surfaceContainer = Charcoal800,
    surfaceContainerHigh = Charcoal700,
    onSurface = OffWhite,
    onSurfaceVariant = MutedGrey,
    outline = Outline,
    outlineVariant = Charcoal700,
    background = Charcoal950,
    onBackground = OffWhite,
    error = ErrorRed,
    onError = Charcoal950,
    errorContainer = ErrorContainer,
    onErrorContainer = OffWhite
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
