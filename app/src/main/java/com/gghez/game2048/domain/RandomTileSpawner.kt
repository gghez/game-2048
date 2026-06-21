package com.gghez.game2048.domain

import kotlin.random.Random

interface TileSpawner {
    fun spawnValue(): Int
    fun choose(emptyIndices: List<Int>): Int
}

class RandomTileSpawner(seed: Long? = null) : TileSpawner {
    private val random: Random = if (seed != null) Random(seed) else Random.Default
    override fun spawnValue(): Int = if (random.nextDouble() < 0.9) 2 else 4
    override fun choose(emptyIndices: List<Int>): Int = emptyIndices[random.nextInt(emptyIndices.size)]
}
