package com.gghez.game2048.data.score

data class SavedGame(
    val values: List<Int>,
    val score: Int,
    val moves: Int,
    val elapsedSeconds: Long,
    val status: String,
    val keepPlaying: Boolean,
) {
    fun serialize(): String =
        listOf(values.joinToString(","), score, moves, elapsedSeconds, status, keepPlaying).joinToString(";")

    companion object {
        fun parse(s: String): SavedGame? = runCatching {
            val parts = s.split(";")
            SavedGame(
                values = parts[0].split(",").map { it.toInt() },
                score = parts[1].toInt(),
                moves = parts[2].toInt(),
                elapsedSeconds = parts[3].toLong(),
                status = parts[4],
                keepPlaying = parts[5].toBoolean(),
            )
        }.getOrNull()
    }
}
