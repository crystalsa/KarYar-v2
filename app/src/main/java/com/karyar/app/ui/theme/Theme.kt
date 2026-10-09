package com.karyar.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = AmberAccent,
    onPrimary = Color.White,
    primaryContainer = AmberLight,
    onPrimaryContainer = AmberDark,
    secondary = CyanAccent,
    onSecondary = Color.White,
    secondaryContainer = CyanLight,
    onSecondaryContainer = CyanAccent,
    tertiary = EmeraldAccent,
    onTertiary = Color.White,
    tertiaryContainer = EmeraldLight,
    onTertiaryContainer = EmeraldDark,
    background = Slate50,
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate600,
    outline = Slate200,
    outlineVariant = Slate100,
    error = RoseAccent,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Always Light Theme as requested by user
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
