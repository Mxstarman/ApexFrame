package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ApexDarkColorScheme = darkColorScheme(
    primary = ApexRecordRed,
    onPrimary = Color.White,
    primaryContainer = ApexSurfaceElevated,
    onPrimaryContainer = ApexTextPrimary,
    secondary = ApexCyan,
    onSecondary = Color.Black,
    secondaryContainer = ApexSurfaceElevated,
    onSecondaryContainer = ApexCyan,
    tertiary = ApexEmerald,
    onTertiary = Color.Black,
    background = ApexBackground,
    onBackground = ApexTextPrimary,
    surface = ApexSurface,
    onSurface = ApexTextPrimary,
    surfaceVariant = ApexSurfaceCard,
    onSurfaceVariant = ApexTextSecondary,
    outline = ApexBorder,
    error = ApexRecordRed,
    onError = Color.White
)

@Composable
fun ApexFrameTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ApexDarkColorScheme,
        typography = Typography,
        content = content
    )
}
