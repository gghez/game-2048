package com.gghez.game2048.ui.theme

import androidx.compose.ui.graphics.Color

// Solarized palette (Ethan Schoonover) — https://ethanschoonover.com/solarized/
// Base tones
val Base03 = Color(0xFF002B36) // darkest background (dark mode)
val Base02 = Color(0xFF073642) // dark highlight background
val Base01 = Color(0xFF586E75) // light-mode emphasized text / dark-mode comments
val Base00 = Color(0xFF657B83) // light-mode body text
val Base0 = Color(0xFF839496)  // dark-mode body text
val Base1 = Color(0xFF93A1A1)  // dark-mode emphasized text / light highlight
val Base2 = Color(0xFFEEE8D5)  // light highlight background
val Base3 = Color(0xFFFDF6E3)  // lightest background (light mode)
// Accents (mode-agnostic)
val Yellow = Color(0xFFB58900)
val Orange = Color(0xFFCB4B16)
val SolarRed = Color(0xFFDC322F)
val Magenta = Color(0xFFD33682)
val Violet = Color(0xFF6C71C4)
val Blue = Color(0xFF268BD2)
val Cyan = Color(0xFF2AA198)
val Green = Color(0xFF859900)

// Semantic aliases used by the Material color scheme
val AppBackgroundLight = Base3
val SurfaceLight = Base2
val BodyTextLight = Base00
val EmphasizedTextLight = Base01

val AppBackgroundDark = Base03
val SurfaceDark = Base02
val BodyTextDark = Base0
val EmphasizedTextDark = Base1

// Board & empty cells follow the highlighted-surface tone of the active mode.
val BoardLight = Base2
val EmptyCellLight = Base3
val BoardDark = Base02
val EmptyCellDark = Base03

// Shared accent for primary controls
val Accent = Blue

object TileColors {
    // Accent tiles (8 and above) are mode-agnostic, per Solarized's design intent.
    // Light text reads cleanly on every accent.
    private val accentPalette = mapOf(
        8 to (Yellow to Base3),
        16 to (Orange to Base3),
        32 to (SolarRed to Base3),
        64 to (Magenta to Base3),
        128 to (Violet to Base3),
        256 to (Blue to Base3),
        512 to (Cyan to Base3),
        1024 to (Green to Base3),
        2048 to (Yellow to Base3),
    )
    // Gold emphasis for anything beyond 2048.
    private val fallback = Yellow to Base3

    fun bg(value: Int, dark: Boolean): Color = when (value) {
        2 -> if (dark) Base02 else Base2
        4 -> Base1
        else -> accentPalette[value]?.first ?: fallback.first
    }

    fun fg(value: Int, dark: Boolean): Color = when (value) {
        2 -> if (dark) Base1 else Base01
        4 -> Base03
        else -> accentPalette[value]?.second ?: fallback.second
    }
}
