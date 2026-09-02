package com.aiwatch.wear.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.MaterialTheme as WearMaterialTheme

/**
 * Wraps Wear's Material theme with its default colours. Our own palette lives in
 * [WearPalette] and is applied explicitly per composable, so the app does not
 * depend on the Wear colour API.
 */
@Composable
fun WearAppTheme(content: @Composable () -> Unit) {
    WearMaterialTheme(content = content)
}

/**
 * Near-black surfaces, warm off-white text, and one desaturated sage accent.
 *
 * Wear's own `Colors` are deliberately left untouched: rather than construct
 * that class, every composable takes its colour from here. That keeps the theme
 * independent of the Wear Material colour API surface.
 */
object WearPalette {
    val Background = Color(0xFF0B0C0E)
    val Surface = Color(0xFF16181B)
    val SurfaceRaised = Color(0xFF1E2126)
    val OnSurface = Color(0xFFECEAE6)
    val OnSurfaceMuted = Color(0xFF9A9A96)
    val Hairline = Color(0xFF282B30)

    val Accent = Color(0xFF8FAEA4)
    val AccentDim = Color(0xFF5E7A72)
    val OnAccent = Color(0xFF0B0C0E)

    val UserBubble = Color(0xFF23262B)
    val AssistantBubble = Color(0xFF15171A)

    val Warning = Color(0xFFC9A876)
    val Danger = Color(0xFFC99490)
    val Success = Color(0xFF8FB99A)
}

/** Explicit styles, so nothing depends on Wear's typography naming. */
object WearType {
    val ScreenTitle = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 21.sp,
        letterSpacing = (-0.2).sp,
    )
    val SectionLabel = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.8.sp,
    )
    val Body = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
    )
    val BodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 15.sp,
    )
    val RowTitle = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    )
    val Mono = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 15.sp,
    )
}
