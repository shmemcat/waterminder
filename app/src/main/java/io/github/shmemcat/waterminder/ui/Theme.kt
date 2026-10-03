package io.github.shmemcat.waterminder.ui

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun WaterminderTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val palette = if (darkTheme) Palette.Dark else Palette.Light
    val colors = if (darkTheme) darkColorScheme(
        primary = Palette.Sage, onPrimary = Palette.Ink,
        primaryContainer = Palette.Sage, onPrimaryContainer = Palette.Ink,
        secondary = Palette.Lavender, onSecondary = Palette.Ink,
        secondaryContainer = palette.SelectionSurface, onSecondaryContainer = palette.Ink,
        background = palette.Paper, onBackground = palette.Ink,
        surface = palette.Paper, onSurface = palette.Ink,
        surfaceContainer = palette.Card, surfaceContainerLow = palette.Card,
        surfaceContainerHigh = palette.SettingsSurface, surfaceVariant = palette.Card,
        onSurfaceVariant = palette.Muted, outline = palette.Line,
    ) else lightColorScheme(
        primary = palette.Ink, onPrimary = palette.Paper,
        primaryContainer = palette.Sage, onPrimaryContainer = Palette.Ink,
        secondary = palette.Ink, secondaryContainer = Palette.Lavender,
        background = palette.Paper, surface = palette.Paper,
        surfaceContainer = palette.SagePale, surfaceVariant = palette.SagePale,
        onBackground = palette.Ink, onSurface = palette.Ink,
        onSurfaceVariant = palette.Muted, outline = palette.Line,
    )
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as? Activity)?.window?.let { window ->
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }
    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(colorScheme = colors, content = content)
    }
}
