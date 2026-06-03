package com.example.solochef.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = Sage900,
    onPrimary = Sage50,
    primaryContainer = Sage200,
    onPrimaryContainer = Sage900,
    secondary = Sage500,
    onSecondary = Sage50,
    secondaryContainer = Sage100,
    onSecondaryContainer = Sage900,
    tertiary = Indigo500,
    background = Sage100,
    onBackground = Sage900,
    surface = Sage50,
    onSurface = Sage900,
    surfaceVariant = Sage200,
    onSurfaceVariant = Sage500,
    outline = Sage200,
    outlineVariant = Sage100,
    error = Red500,
    onError = Sage50,
)

@Composable
fun SoloChefTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = SoloChefTypography,
        content = content
    )
}
