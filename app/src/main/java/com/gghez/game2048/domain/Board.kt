package com.gghez.game2048.domain

class Board(val cells: List<Tile?>) {
    init { require(cells.size == SIZE * SIZE) { "Board must have ${SIZE * SIZE} cells" } }

    operator fun get(row: Int, col: Int): Tile? = cells[row * SIZE + col]

    fun values(): List<Int> = cells.map { it?.value ?: 0 }

    fun emptyIndices(): List<Int> = cells.indices.filter { cells[it] == null }

    fun isFull(): Boolean = cells.none { it == null }

    companion object {
        const val SIZE = 4
        fun empty(): Board = Board(List(SIZE * SIZE) { null })
    }
}
