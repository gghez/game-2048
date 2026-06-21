package com.gghez.game2048.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BoardTest {
    @Test fun emptyBoardHas16EmptyCells() {
        val b = Board.empty()
        assertEquals(16, b.cells.size)
        assertEquals(16, b.emptyIndices().size)
        assertFalse(b.isFull())
    }

    @Test fun getReadsRowMajor() {
        val tile = Tile(1, 2)
        val cells = MutableList<Tile?>(16) { null }
        cells[4 * 1 + 2] = tile // row 1, col 2
        val b = Board(cells)
        assertEquals(tile, b[1, 2])
        assertEquals(listOf(0, 0, 2), b.values().subList(4, 7))
    }

    @Test fun fullBoardReportsFull() {
        val b = Board(List(16) { Tile(it, 2) })
        assertTrue(b.isFull())
        assertEquals(0, b.emptyIndices().size)
    }
}
