package com.gghez.game2048.domain

data class MoveResult(val state: GameState, val moved: Boolean, val gained: Int)
