package com.bsodcoder.aiwatch.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val Teal = Color(0xFF5FA8A0)
private val TealDark = Color(0xFF006A6A)
private val Charcoal900 = Color(0xFF121316)
private val Charcoal800 = Color(0xFF1B1D21)
private val Charcoal700 = Color(0xFF25272C)
private val OffWhite = Color(0xFFE7E6E3)
private val MutedGray = Color(0xFF9B9DA3)
private val Paper = Color(0xFFF7FAF9)
private val Ink = Color(0xFF191C1C)

private val LightColors = lightColorScheme(
    primary = TealDark,
    onPrimary = Color.White,
    secondary = TealDark,
    onSecondary = Color.White,
    background = Paper,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE1E3E3),
    onSurfaceVariant = Color(0xFF3F4948),
    outline = Color(0xFF6F7978),
    error = Color(0xFFBA1A1A)
)

private val DarkColors = darkColorScheme(
    primary = Teal,
    onPrimary = Charcoal900,
    secondary = Teal,
    onSecondary = Charcoal900,
    background = Charcoal900,
    onBackground = OffWhite,
    surface = Charcoal800,
    onSurface = OffWhite,
    surfaceVariant = Charcoal700,
    onSurfaceVariant = MutedGray,
    outline = MutedGray,
    error = Color(0xFFFFB4AB)
)

@Composable
fun AiWatchTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
