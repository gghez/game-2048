package com.gghez.game2048.ui.theme

import androidx.compose.ui.graphics.Color

val AppBackgroundLight = Color(0xFFFAF8EF)
val BoardLight = Color(0xFF8C7B68)
val EmptyCellLight = Color(0xFFA89B89)
val BrownText = Color(0xFF5C5248)
val Accent = Color(0xFF8A7A66)

val AppBackgroundDark = Color(0xFF1E1B18)
val BoardDark = Color(0xFF3A332D)
val EmptyCellDark = Color(0xFF4A423A)
val CreamText = Color(0xFFF2ECE0)

object TileColors {
    private val palette = mapOf(
        2 to (Color(0xFFEEE8DC) to BrownText),
        4 to (Color(0xFFEDE0C8) to BrownText),
        8 to (Color(0xFFE8A96B) to Color.White),
        16 to (Color(0xFFE8965C) to Color.White),
        32 to (Color(0xFFE86B5C) to Color.White),
        64 to (Color(0xFFE5503E) to Color.White),
        128 to (Color(0xFFEDC850) to Color.White),
        256 to (Color(0xFFEDC53F) to Color.White),
        512 to (Color(0xFFEDC22E) to Color.White),
        1024 to (Color(0xFFE8B923) to Color.White),
        2048 to (Color(0xFFE8A800) to Color.White),
    )
    private val fallback = Color(0xFF3D3530) to Color.White

    fun bg(value: Int, dark: Boolean): Color = palette[value]?.first ?: fallback.first

    fun fg(value: Int, dark: Boolean): Color = palette[value]?.second ?: fallback.second
}
