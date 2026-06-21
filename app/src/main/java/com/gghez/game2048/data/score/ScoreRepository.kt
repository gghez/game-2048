package com.gghez.game2048.data.score

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.scoreDataStore by preferencesDataStore(name = "score")

class ScoreRepository(private val context: Context) {
    private object Keys {
        val BEST = intPreferencesKey("best_score")
        val SAVED = stringPreferencesKey("saved_game")
    }

    val bestScore: Flow<Int> = context.scoreDataStore.data.map { it[Keys.BEST] ?: 0 }

    suspend fun updateBestScore(score: Int) {
        context.scoreDataStore.edit { p ->
            val current = p[Keys.BEST] ?: 0
            if (score > current) p[Keys.BEST] = score
        }
    }

    val savedGame: Flow<SavedGame?> = context.scoreDataStore.data.map { p ->
        p[Keys.SAVED]?.let { SavedGame.parse(it) }
    }

    suspend fun saveGame(game: SavedGame) {
        context.scoreDataStore.edit { it[Keys.SAVED] = game.serialize() }
    }

    suspend fun clearSavedGame() {
        context.scoreDataStore.edit { it.remove(Keys.SAVED) }
    }
}
