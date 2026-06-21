package com.gghez.game2048.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val FAST = booleanPreferencesKey("fast_animations")
        val VIBRATION = booleanPreferencesKey("vibration")
        val ORIENTATION = stringPreferencesKey("orientation")
    }

    val settings: Flow<GameSettings> = context.settingsDataStore.data.map { p ->
        GameSettings(
            theme = p[Keys.THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.LIGHT,
            fastAnimations = p[Keys.FAST] ?: false,
            vibration = p[Keys.VIBRATION] ?: false,
            orientation = p[Keys.ORIENTATION]?.let { runCatching { OrientationMode.valueOf(it) }.getOrNull() } ?: OrientationMode.PORTRAIT,
        )
    }

    suspend fun setTheme(mode: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.THEME] = mode.name }
    }

    suspend fun setFastAnimations(on: Boolean) {
        context.settingsDataStore.edit { it[Keys.FAST] = on }
    }

    suspend fun setVibration(on: Boolean) {
        context.settingsDataStore.edit { it[Keys.VIBRATION] = on }
    }

    suspend fun setOrientation(mode: OrientationMode) {
        context.settingsDataStore.edit { it[Keys.ORIENTATION] = mode.name }
    }
}
