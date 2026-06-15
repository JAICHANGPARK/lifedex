package com.lifedex.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SleekLightColorScheme = lightColorScheme(
    primary = NeonCyan,                  // Violet-600
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE9FE), // Light purple accent
    onPrimaryContainer = Color(0xFF2E1065),
    secondary = NeonMagenta,              // Pink-500
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFCE7F3),
    onSecondaryContainer = Color(0xFF4D002B),
    tertiary = CyberYellow,
    onTertiary = Color.White,
    background = CyberSlate,              // Light Gray-Blue (#F3F4F9)
    onBackground = Color(0xFF0F172A),     // Slate-900 text
    surface = CardSlate,                  // White (#FFFFFF)
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),   // Slate-100
    onSurfaceVariant = Color(0xFF475569), // Slate-600
    outline = Color(0xFFCBD5E1),          // Slate-300
    error = CrimsonAlert,
    onError = Color.White
)

private val SleekDarkColorScheme = darkColorScheme(
    primary = Color(0xFFC084FC),          // Light neon purple
    onPrimary = Color(0xFF3B0764),
    primaryContainer = Color(0xFF6B21A8),
    onPrimaryContainer = Color(0xFFF3E8FF),
    secondary = Color(0xFFF472B6),        // Light neon pink
    onSecondary = Color(0xFF500730),
    secondaryContainer = Color(0xFF9D174D),
    onSecondaryContainer = Color(0xFFFCE7F3),
    tertiary = CyberYellow,
    onTertiary = Color.Black,
    background = Color(0xFF0B0F17),       // Deep slate-black cyber background
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF141B25),          // Dark card surface
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF1E293B),   // Dark surface variant
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF475569),
    error = CrimsonAlert,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) SleekDarkColorScheme else SleekLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
