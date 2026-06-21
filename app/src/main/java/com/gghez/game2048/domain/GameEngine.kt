package com.gghez.game2048.domain

import com.gghez.game2048.domain.Board.Companion.SIZE

class GameEngine {

    /** Slides a line of 4 values toward index 0, merging equal neighbours once. */
    fun slideLine(line: List<Int>): Pair<List<Int>, Int> {
        val nonZero = line.filter { it != 0 }
        val merged = ArrayList<Int>(SIZE)
        var gained = 0
        var i = 0
        while (i < nonZero.size) {
            if (i + 1 < nonZero.size && nonZero[i] == nonZero[i + 1]) {
                val v = nonZero[i] * 2
                merged.add(v)
                gained += v
                i += 2
            } else {
                merged.add(nonZero[i])
                i += 1
            }
        }
        while (merged.size < SIZE) merged.add(0)
        return merged to gained
    }

    /** Applies a move (no spawn). Status recomputed for win; loss handled after spawn. */
    fun move(state: GameState, dir: Direction): MoveResult {
        val grid = state.board.values()
        val lines = extractLines(grid, dir)
        var gained = 0
        val newLines = lines.map { line ->
            val (slid, g) = slideLine(line)
            gained += g
            slid
        }
        val newGrid = restoreLines(newLines, dir)
        val moved = newGrid != grid
        if (!moved) return MoveResult(state, moved = false, gained = 0)

        val board = gridToBoard(newGrid)
        val won = newGrid.any { it >= WIN_VALUE }
        val status = when {
            won && !state.keepPlaying -> GameStatus.WON
            else -> GameStatus.PLAYING
        }
        val newState = state.copy(
            board = board,
            score = state.score + gained,
            moves = state.moves + 1,
            status = status,
        )
        return MoveResult(newState, moved = true, gained = gained)
    }

    fun newGame(spawner: TileSpawner, nextId: () -> Int): GameState {
        val empty = GameState(Board.empty(), score = 0, moves = 0, status = GameStatus.PLAYING)
        return spawn(spawn(empty, spawner, nextId), spawner, nextId)
    }

    fun spawn(state: GameState, spawner: TileSpawner, nextId: () -> Int): GameState {
        val empties = state.board.emptyIndices()
        if (empties.isEmpty()) {
            return if (canMove(state.board)) state else state.copy(status = GameStatus.LOST)
        }
        val index = spawner.choose(empties)
        val value = spawner.spawnValue()
        val cells = state.board.cells.toMutableList()
        cells[index] = Tile(nextId(), value)
        val board = Board(cells)
        val status = if (!canMove(board)) GameStatus.LOST else state.status
        return state.copy(board = board, status = status)
    }

    fun canMove(board: Board): Boolean {
        if (!board.isFull()) return true
        val v = board.values()
        for (r in 0 until SIZE) {
            for (c in 0 until SIZE) {
                val cur = v[r * SIZE + c]
                if (c + 1 < SIZE && v[r * SIZE + c + 1] == cur) return true
                if (r + 1 < SIZE && v[(r + 1) * SIZE + c] == cur) return true
            }
        }
        return false
    }

    private fun gridToBoard(grid: List<Int>): Board =
        Board(grid.mapIndexed { i, v -> if (v == 0) null else Tile(i, v) })

    /** Extracts 4 lines oriented so that sliding toward index 0 equals moving in [dir]. */
    private fun extractLines(grid: List<Int>, dir: Direction): List<List<Int>> =
        (0 until SIZE).map { idx ->
            (0 until SIZE).map { j ->
                grid[indexFor(dir, idx, j)]
            }
        }

    private fun restoreLines(lines: List<List<Int>>, dir: Direction): List<Int> {
        val out = MutableList(SIZE * SIZE) { 0 }
        for (idx in 0 until SIZE) {
            for (j in 0 until SIZE) {
                out[indexFor(dir, idx, j)] = lines[idx][j]
            }
        }
        return out
    }

    /**
     * Maps line [idx] position [j] to a flat grid index, oriented per direction so that
     * j=0 is the destination edge for that direction.
     */
    private fun indexFor(dir: Direction, idx: Int, j: Int): Int = when (dir) {
        Direction.LEFT -> idx * SIZE + j
        Direction.RIGHT -> idx * SIZE + (SIZE - 1 - j)
        Direction.UP -> j * SIZE + idx
        Direction.DOWN -> (SIZE - 1 - j) * SIZE + idx
    }

    companion object {
        const val WIN_VALUE = 2048
    }
}
