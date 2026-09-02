package com.aiwatch.phone.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Palette.LightPrimary,
    onPrimary = Palette.LightOnPrimary,
    primaryContainer = Palette.LightPrimaryContainer,
    onPrimaryContainer = Palette.LightOnPrimaryContainer,
    secondary = Palette.LightPrimary,
    onSecondary = Palette.LightOnPrimary,
    background = Palette.LightBackground,
    onBackground = Palette.LightOnSurface,
    surface = Palette.LightSurface,
    onSurface = Palette.LightOnSurface,
    surfaceVariant = Palette.LightSurfaceVariant,
    onSurfaceVariant = Palette.LightOnSurfaceVariant,
    outline = Palette.LightOutline,
    outlineVariant = Palette.LightOutlineVariant,
)

private val DarkColors = darkColorScheme(
    primary = Palette.DarkPrimary,
    onPrimary = Palette.DarkOnPrimary,
    primaryContainer = Palette.DarkPrimaryContainer,
    onPrimaryContainer = Palette.DarkOnPrimaryContainer,
    secondary = Palette.DarkPrimary,
    onSecondary = Palette.DarkOnPrimary,
    background = Palette.DarkBackground,
    onBackground = Palette.DarkOnSurface,
    surface = Palette.DarkSurface,
    onSurface = Palette.DarkOnSurface,
    surfaceVariant = Palette.DarkSurfaceVariant,
    onSurfaceVariant = Palette.DarkOnSurfaceVariant,
    outline = Palette.DarkOutline,
    outlineVariant = Palette.DarkOutlineVariant,
)

/** Semantic colours that are not part of Material's scheme. */
@Immutable
data class SemanticColors(
    val success: Color,
    val warning: Color,
    val danger: Color,
    val hairline: Color,
)

private val LightSemantic = SemanticColors(
    success = Palette.SuccessLight,
    warning = Palette.WarningLight,
    danger = Palette.DangerLight,
    hairline = Palette.LightOutlineVariant,
)

private val DarkSemantic = SemanticColors(
    success = Palette.SuccessDark,
    warning = Palette.WarningDark,
    danger = Palette.DangerDark,
    hairline = Palette.DarkOutlineVariant,
)

val LocalSemanticColors = staticCompositionLocalOf { LightSemantic }

@Composable
fun AiWatchTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val semantic = if (darkTheme) DarkSemantic else LightSemantic

    androidx.compose.runtime.CompositionLocalProvider(
        LocalSemanticColors provides semantic,
    ) {
        MaterialTheme(
            colorScheme = colors,
            typography = AiWatchTypography,
            content = content,
        )
    }
}
