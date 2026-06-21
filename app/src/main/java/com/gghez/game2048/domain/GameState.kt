package com.gghez.game2048.domain

data class GameState(
    val board: Board,
    val score: Int,
    val moves: Int,
    val status: GameStatus,
    val keepPlaying: Boolean = false,
)
