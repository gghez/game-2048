package com.gghez.game2048.data.settings

enum class ThemeMode { LIGHT, DARK }
enum class OrientationMode { PORTRAIT, AUTO, LANDSCAPE }

data class GameSettings(
    val theme: ThemeMode = ThemeMode.LIGHT,
    val fastAnimations: Boolean = false,
    val vibration: Boolean = false,
    val orientation: OrientationMode = OrientationMode.PORTRAIT,
)
