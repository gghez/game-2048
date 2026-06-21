package com.gghez.game2048.data.settings

enum class ThemeMode { LIGHT, DARK }

data class GameSettings(
    val theme: ThemeMode = ThemeMode.LIGHT,
    val fastAnimations: Boolean = false,
    val vibration: Boolean = false,
    val sound: Boolean = true,
)
