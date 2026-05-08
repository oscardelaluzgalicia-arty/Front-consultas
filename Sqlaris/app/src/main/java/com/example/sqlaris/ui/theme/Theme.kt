// Global Update: Reporting System Visual Identity
package com.example.sqlaris.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ReportingRed,
    onPrimary = ReportingBlack,
    secondary = ReportingCyan,
    onSecondary = ReportingBlack,
    tertiary = ReportingOrange,
    background = ReportingBlack,
    surface = ReportingSurface,
    onSurface = ReportingWhite,
    onBackground = ReportingWhite,
    error = RedError,
    outline = ReportingGray
)

// High contrast dark theme for reporting
private val LightColorScheme = darkColorScheme(
    primary = ReportingRed,
    onPrimary = Color.White,
    secondary = ReportingCyan,
    onSecondary = Color.Black,
    tertiary = ReportingOrange,
    background = ReportingBlack,
    surface = ReportingSurface,
    onSurface = ReportingWhite,
    onBackground = ReportingWhite,
    error = RedError,
    outline = ReportingGray
)

@Composable
fun SqlarisTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Consistency for professional reporting tool
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
