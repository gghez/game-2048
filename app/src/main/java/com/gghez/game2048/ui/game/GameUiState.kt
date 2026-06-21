package com.gghez.game2048.ui.game

import com.gghez.game2048.data.settings.GameSettings
import com.gghez.game2048.domain.Board
import com.gghez.game2048.domain.GameState
import com.gghez.game2048.domain.GameStatus

data class GameUiState(
    val state: GameState,
    val bestScore: Int,
    val elapsedSeconds: Long,
    val settings: GameSettings,
    val showNewGameDialog: Boolean = false,
    val canUndo: Boolean = false,
    val leaderboardAvailable: Boolean = false,
) {
    companion object {
        fun initial(settings: GameSettings) = GameUiState(
            state = GameState(Board.empty(), 0, 0, GameStatus.PLAYING),
            bestScore = 0,
            elapsedSeconds = 0,
            settings = settings,
        )
    }
}
