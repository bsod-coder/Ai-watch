package com.bsodcoder.aiwatch.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val Teal40 = Color(0xFF006A6A)
private val Teal90 = Color(0xFF9CF1F0)
private val Teal10 = Color(0xFF002020)
private val Teal80 = Color(0xFF80D5D4)
private val Teal20 = Color(0xFF003737)

private val Neutral99 = Color(0xFFF7FAF9)
private val Neutral10 = Color(0xFF191C1C)
private val Neutral94 = Color(0xFFE9EEED)
private val Neutral24 = Color(0xFF2E3131)
private val Neutral90 = Color(0xFFE1E3E3)
private val Neutral20 = Color(0xFF2D3131)

private val Error40 = Color(0xFFBA1A1A)
private val Error90 = Color(0xFFFFDAD6)
private val Error80 = Color(0xFFFFB4AB)
private val Error20 = Color(0xFF690005)

private val LightColors = lightColorScheme(
    primary = Teal40,
    onPrimary = Color.White,
    primaryContainer = Teal90,
    onPrimaryContainer = Teal10,
    secondary = Color(0xFF4A6363),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCE8E7),
    onSecondaryContainer = Color(0xFF051F1F),
    tertiary = Color(0xFF4B607C),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD3E4FE),
    onTertiaryContainer = Color(0xFF041C35),
    error = Error40,
    onError = Color.White,
    errorContainer = Error90,
    onErrorContainer = Error20,
    background = Neutral99,
    onBackground = Neutral10,
    surface = Neutral99,
    onSurface = Neutral10,
    surfaceVariant = Neutral94,
    onSurfaceVariant = Color(0xFF3F4948),
    outline = Color(0xFF6F7978),
    outlineVariant = Color(0xFFBEC9C8),
    inverseSurface = Neutral20,
    inverseOnSurface = Neutral90,
    inversePrimary = Teal80,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF1F5F4),
    surfaceContainer = Color(0xFFEBEEED),
    surfaceContainerHigh = Color(0xFFE5E9E8),
    surfaceContainerHighest = Color(0xFFDFE4E3)
)

private val DarkColors = darkColorScheme(
    primary = Teal80,
    onPrimary = Teal20,
    primaryContainer = Teal40,
    onPrimaryContainer = Teal90,
    secondary = Color(0xFFB0CCCB),
    onSecondary = Color(0xFF1C3534),
    secondaryContainer = Color(0xFF324B4B),
    onSecondaryContainer = Color(0xFFCCE8E7),
    tertiary = Color(0xFFB3C8DB),
    onTertiary = Color(0xFF1C314B),
    tertiaryContainer = Color(0xFF334863),
    onTertiaryContainer = Color(0xFFD3E4FE),
    error = Error80,
    onError = Error20,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Error90,
    background = Neutral10,
    onBackground = Neutral90,
    surface = Neutral10,
    onSurface = Neutral90,
    surfaceVariant = Neutral24,
    onSurfaceVariant = Color(0xFFBEC9C8),
    outline = Color(0xFF889392),
    outlineVariant = Color(0xFF3F4948),
    inverseSurface = Neutral90,
    inverseOnSurface = Neutral20,
    inversePrimary = Teal40,
    surfaceContainerLowest = Color(0xFF0E1111),
    surfaceContainerLow = Color(0xFF1D2020),
    surfaceContainer = Color(0xFF212525),
    surfaceContainerHigh = Color(0xFF2B2F2F),
    surfaceContainerHighest = Color(0xFF363A3A)
)

private val AiWatchShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
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
        shapes = AiWatchShapes,
        content = content
    )
}
