package com.gghez.game2048.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightScheme = lightColorScheme(
    background = AppBackgroundLight,
    surface = AppBackgroundLight,
    onBackground = BrownText,
    onSurface = BrownText,
    primary = Accent,
    onPrimary = Color.White,
)

private val DarkScheme = darkColorScheme(
    background = AppBackgroundDark,
    surface = AppBackgroundDark,
    onBackground = CreamText,
    onSurface = CreamText,
    primary = Accent,
    onPrimary = Color.White,
)

@Composable
fun Game2048Theme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = AppTypography,
        content = content,
    )
}
