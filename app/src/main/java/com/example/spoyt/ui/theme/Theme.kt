package com.example.spoyt.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Spotify-like dark theme. Dark background with a green accent.
 * No Spotify logo or trademark is used; "Spoyt" is the app name.
 */
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF1DB954),   // Spotify-like green accent
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF1ED760),
    onPrimaryContainer = Color(0xFF000000),
    secondary = Color(0xFF2E2E2E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF3A3A3A),
    onSecondaryContainer = Color(0xFFFFFFFF),
    background = Color(0xFF121212), // dark background
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFCACACA),
    error = Color(0xFFCF6679),
    onError = Color(0xFF000000)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1DB954),
    onPrimary = Color(0xFF000000),
    background = Color(0xFFF5F5F5),
    surface = Color(0xFFFFFFFF)
)

/** Spotify-like typography (kept simple). */
private val AppTypography = Typography(
    titleLarge = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White),
    titleMedium = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White),
    bodyLarge = TextStyle(fontSize = 16.sp, color = Color.White),
    bodyMedium = TextStyle(fontSize = 14.sp, color = Color(0xFFCACACA)),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.White)
)

@Composable
fun SpoytTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
