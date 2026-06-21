package com.gghez.game2048.data.leaderboard

import android.content.Context

object LeaderboardProvider {
    fun create(context: Context): LeaderboardRepository {
        // Replaced in the Play Games task with a check for configured game ids.
        return NoopLeaderboard()
    }
}
