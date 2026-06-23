package com.gghez.game2048.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightScheme = lightColorScheme(
    background = AppBackgroundLight,
    surface = SurfaceLight,
    onBackground = BodyTextLight,
    onSurface = BodyTextLight,
    primary = Accent,
    onPrimary = Base3,
)

private val DarkScheme = darkColorScheme(
    background = AppBackgroundDark,
    surface = SurfaceDark,
    onBackground = BodyTextDark,
    onSurface = BodyTextDark,
    primary = Accent,
    onPrimary = Base3,
)

@Composable
fun Game2048Theme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = AppTypography,
        content = content,
    )
}
