package com.example.spoyt.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Spoyt palette. A deep near-black canvas with a vivid green signature and a violet
 * secondary used for gradients, so the app has its own identity instead of a flat two-tone
 * Material default.
 *
 * No Spotify logo or trademark is used; "Spoyt" is the app name.
 */
val SpoytGreen = Color(0xFF1ED760)
val SpoytGreenDim = Color(0xFF12B34B)
val SpoytViolet = Color(0xFF7C6BFF)
val SpoytInk = Color(0xFF07070A)
val SpoytSurface = Color(0xFF101015)
val SpoytSurfaceHigh = Color(0xFF18181F)
val SpoytOutline = Color(0xFF2A2A34)

private val SpoytColorScheme = darkColorScheme(
    primary = SpoytGreen,
    onPrimary = Color(0xFF04220F),
    primaryContainer = Color(0xFF0E3D22),
    onPrimaryContainer = Color(0xFFB9FFD3),

    secondary = SpoytViolet,
    onSecondary = Color(0xFF0B0820),
    secondaryContainer = Color(0xFF241C4D),
    onSecondaryContainer = Color(0xFFDCD6FF),

    tertiary = Color(0xFFFF7AA8),
    onTertiary = Color(0xFF2C0614),

    background = SpoytInk,
    onBackground = Color(0xFFF4F4F7),
    surface = SpoytSurface,
    onSurface = Color(0xFFF4F4F7),
    surfaceVariant = SpoytSurfaceHigh,
    onSurfaceVariant = Color(0xFF9A9AA8),
    surfaceContainerLowest = Color(0xFF050507),
    surfaceContainerLow = Color(0xFF0B0B0F),
    surfaceContainer = SpoytSurface,
    surfaceContainerHigh = SpoytSurfaceHigh,
    surfaceContainerHighest = Color(0xFF212129),

    outline = SpoytOutline,
    outlineVariant = Color(0xFF1C1C24),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF2A0505),
    scrim = Color(0xFF000000)
)

private val SpoytTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.8).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        lineHeight = 27.sp,
        letterSpacing = (-0.3).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        lineHeight = 25.sp,
        letterSpacing = (-0.2).sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 11.5.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.1.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.5.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.4.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.6.sp
    )
)

private val SpoytShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
)

/**
 * Spoyt is always dark: a music player looks wrong in a light theme and the artwork pops
 * against near-black. [darkTheme] is kept as a parameter for previews.
 */
@Composable
fun SpoytTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SpoytColorScheme,
        typography = SpoytTypography,
        shapes = SpoytShapes,
        content = content
    )
}
