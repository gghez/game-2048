package com.gghez.game2048.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val FAST = booleanPreferencesKey("fast_animations")
        val VIBRATION = booleanPreferencesKey("vibration")
        val SOUND = booleanPreferencesKey("sound")
        val LANGUAGE = stringPreferencesKey("language")
    }

    val settings: Flow<GameSettings> = context.settingsDataStore.data.map { p ->
        GameSettings(
            theme = p[Keys.THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.LIGHT,
            fastAnimations = p[Keys.FAST] ?: false,
            vibration = p[Keys.VIBRATION] ?: false,
            sound = p[Keys.SOUND] ?: true,
            language = p[Keys.LANGUAGE] ?: "",
        )
    }

    /** Read the persisted language tag once, blocking-friendly for early locale setup. */
    suspend fun currentLanguageTag(): String = settings.first().language

    suspend fun setTheme(mode: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.THEME] = mode.name }
    }

    suspend fun setFastAnimations(on: Boolean) {
        context.settingsDataStore.edit { it[Keys.FAST] = on }
    }

    suspend fun setVibration(on: Boolean) {
        context.settingsDataStore.edit { it[Keys.VIBRATION] = on }
    }

    suspend fun setSound(on: Boolean) {
        context.settingsDataStore.edit { it[Keys.SOUND] = on }
    }

    /** Persist the in-app language; an empty tag clears the override (follow the system). */
    suspend fun setLanguage(tag: String) {
        context.settingsDataStore.edit {
            if (tag.isEmpty()) it.remove(Keys.LANGUAGE) else it[Keys.LANGUAGE] = tag
        }
    }
}
