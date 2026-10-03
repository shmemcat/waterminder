package io.github.shmemcat.waterminder.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.staticCompositionLocalOf

data class AppPalette(
    val Ink: Color,
    val Paper: Color,
    val Muted: Color,
    val Line: Color,
    val Card: Color,
    val Sage: Color,
    val SagePale: Color,
    val SettingsSurface: Color,
    val SelectionSurface: Color,
    val Floor: Color,
    val ToggleTrack: Color,
    val ToggleThumb: Color,
    val IdleThumb: Color,
)

object Palette {
    val Ink = Color(0xFF4B4858)
    val Paper = Color(0xFFFAF8F3)
    val Muted = Color(0xFF787581)
    val Line = Color(0xFFE5E1D9)
    val Sage = Color(0xFFB6D6BC)
    val SagePale = Color(0xFFEAF0E5)
    val Peach = Color(0xFFEABDA6)
    val Lavender = Color(0xFFD7CCED)
    val Blue = Color(0xFFB8D6E6)

    // Artwork outlines and pastel fills stay consistent in both appearances.
    val Light = AppPalette(
        Ink = Ink, Paper = Paper, Muted = Muted, Line = Line,
        Card = Color(0xFFF4F1EA), Sage = Sage, SagePale = SagePale,
        SettingsSurface = Color(0xFFEEE8F1), SelectionSurface = Lavender,
        Floor = Color(0xFFF0ECE3), ToggleTrack = Ink, ToggleThumb = Paper, IdleThumb = Paper,
    )
    val Dark = AppPalette(
        Ink = Color(0xFFEBE5EF), Paper = Color(0xFF211F26),
        Muted = Color(0xFFB3AABB), Line = Color(0xFF443F49),
        Card = Color(0xFF2C2932), Sage = Sage, SagePale = Color(0xFF303B40),
        SettingsSurface = Color(0xFF393442), SelectionSurface = Color(0xFF635674),
        Floor = Color(0xFF302C33), ToggleTrack = Sage, ToggleThumb = Ink,
        IdleThumb = Color(0xFFA69EAF),
    )
}

val LocalPalette = staticCompositionLocalOf { Palette.Light }
