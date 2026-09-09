package com.example.noisewatch.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val NoiseWatchColorScheme = lightColorScheme(
    primary = MutedAmberGold,
    onPrimary = DeepNavyCharcoal,
    primaryContainer = NavIndicatorAmber,
    onPrimaryContainer = DeepNavyCharcoal,
    secondary = PaleSlateBlue,
    onSecondary = DeepNavyCharcoal,
    secondaryContainer = PaleSlateBlue,
    onSecondaryContainer = DeepNavyCharcoal,
    background = WarmIvory,
    onBackground = DeepNavyCharcoal,
    surface = WarmIvory,
    onSurface = DeepNavyCharcoal,
    surfaceVariant = PaleSlateBlue,
    onSurfaceVariant = DeepNavyCharcoal,
    outline = MutedTextSlate,
    error = MutedBrickRed,
    onError = WarmIvory,
    errorContainer = VeryPaleWarmAmber,
    onErrorContainer = DeepNavyCharcoal
)

@Composable
fun NoiseWatchTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NoiseWatchColorScheme,
        typography = Typography,
        content = content
    )
}
