package com.gghez.game2048.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpawnerTest {
    @Test fun spawnValueIsTwoOrFour() {
        val s = RandomTileSpawner(seed = 42)
        repeat(100) { assertTrue(s.spawnValue() in setOf(2, 4)) }
    }

    @Test fun chooseReturnsAnEmptyIndex() {
        val s = RandomTileSpawner(seed = 1)
        val empties = listOf(3, 7, 11)
        repeat(50) { assertTrue(s.choose(empties) in empties) }
    }

    @Test fun seededSpawnerIsDeterministic() {
        val a = RandomTileSpawner(seed = 7)
        val b = RandomTileSpawner(seed = 7)
        val seqA = List(10) { a.spawnValue() to a.choose(listOf(0, 1, 2, 3)) }
        val seqB = List(10) { b.spawnValue() to b.choose(listOf(0, 1, 2, 3)) }
        assertEquals(seqA, seqB)
    }
}
