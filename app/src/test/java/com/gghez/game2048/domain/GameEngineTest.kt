package com.gghez.game2048.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameEngineTest {
    private val engine = GameEngine()

    private fun boardOf(vararg values: Int): Board {
        require(values.size == 16)
        return Board(values.mapIndexed { i, v -> if (v == 0) null else Tile(i, v) })
    }

    private fun state(b: Board) = GameState(b, score = 0, moves = 0, status = GameStatus.PLAYING)

    @Test fun slideMergesPair() {
        assertEquals(listOf(4, 0, 0, 0) to 4, engine.slideLine(listOf(2, 2, 0, 0)))
    }

    @Test fun slideMergesOnlyOncePerMove() {
        assertEquals(listOf(4, 4, 0, 0) to 8, engine.slideLine(listOf(2, 2, 2, 2)))
    }

    @Test fun slideMergesHigherPairFirst() {
        assertEquals(listOf(8, 4, 0, 0) to 12, engine.slideLine(listOf(4, 4, 2, 2)))
    }

    @Test fun slideClosesGaps() {
        assertEquals(listOf(4, 0, 0, 0) to 4, engine.slideLine(listOf(2, 0, 2, 0)))
    }

    @Test fun slideNoMerge() {
        assertEquals(listOf(4, 2, 0, 0) to 0, engine.slideLine(listOf(4, 2, 0, 0)))
    }

    @Test fun moveLeftMergesRowAndScores() {
        val s = state(boardOf(
            2, 2, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
            4, 4, 2, 2,
        ))
        val r = engine.move(s, Direction.LEFT)
        assertTrue(r.moved)
        // gained = 4 (top row) + 8 + 4 (bottom row) = 16
        assertEquals(16, r.gained)
        assertEquals(listOf(
            4, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
            8, 4, 0, 0,
        ), r.state.board.values())
        assertEquals(16, r.state.score)
    }

    @Test fun moveRightPacksToTheRight() {
        val s = state(boardOf(
            2, 0, 2, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
        ))
        val r = engine.move(s, Direction.RIGHT)
        assertEquals(listOf(0, 0, 0, 4) + List(12) { 0 }, r.state.board.values())
    }

    @Test fun moveUpAndDownWorkOnColumns() {
        val s = state(boardOf(
            2, 0, 0, 0,
            2, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
        ))
        assertEquals(4, engine.move(s, Direction.UP).state.board.values()[0])
        assertEquals(4, engine.move(s, Direction.DOWN).state.board.values()[12])
    }

    @Test fun noOpMoveReportsNotMoved() {
        val s = state(boardOf(
            2, 4, 2, 4,
            4, 2, 4, 2,
            2, 4, 2, 4,
            4, 2, 4, 2,
        ))
        val r = engine.move(s, Direction.LEFT)
        assertFalse(r.moved)
        assertEquals(0, r.gained)
        assertEquals(s.board.values(), r.state.board.values())
    }

    @Test fun winStatusWhen2048Reached() {
        val s = state(boardOf(
            1024, 1024, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
            0, 0, 0, 0,
        ))
        val r = engine.move(s, Direction.LEFT)
        assertEquals(GameStatus.WON, r.state.status)
    }

    @Test fun newGameHasExactlyTwoTiles() {
        var id = 0
        val s = engine.newGame(RandomTileSpawner(seed = 3)) { id++ }
        assertEquals(2, s.board.cells.count { it != null })
        assertEquals(GameStatus.PLAYING, s.status)
        assertEquals(0, s.score)
        assertEquals(0, s.moves)
    }

    @Test fun spawnOnFullUnmovableBoardSetsLost() {
        val full = boardOf(
            2, 4, 2, 4,
            4, 2, 4, 2,
            2, 4, 2, 4,
            4, 2, 4, 2,
        )
        var id = 100
        val s = engine.spawn(state(full), RandomTileSpawner(seed = 1)) { id++ }
        assertEquals(GameStatus.LOST, s.status)
    }

    @Test fun canMoveTrueWhenEmptyCellExists() {
        assertTrue(engine.canMove(boardOf(
            2, 4, 2, 4,
            4, 2, 4, 2,
            2, 4, 2, 4,
            4, 2, 4, 0,
        )))
    }

    @Test fun canMoveTrueWhenAdjacentEqual() {
        assertTrue(engine.canMove(boardOf(
            2, 2, 4, 8,
            4, 8, 16, 32,
            2, 4, 8, 16,
            4, 8, 16, 32,
        )))
    }
}
