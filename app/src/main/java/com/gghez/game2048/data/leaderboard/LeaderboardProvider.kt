package com.gghez.game2048.data.leaderboard

import android.content.Context
import com.gghez.game2048.R

object LeaderboardProvider {
    /**
     * Returns the real Play Games implementation when ids are configured (via
     * local.properties -> resValue), otherwise a Noop fallback so the app runs
     * without a Play Console account.
     */
    fun create(context: Context): LeaderboardRepository {
        val appId = context.getString(R.string.game_services_project_id)
        val speed = context.getString(R.string.leaderboard_speed)
        val efficiency = context.getString(R.string.leaderboard_efficiency)
        val time = context.getString(R.string.leaderboard_time)
        val configured = appId.isNotEmpty() && speed.isNotEmpty() &&
            efficiency.isNotEmpty() && time.isNotEmpty()
        return if (configured) {
            PlayGamesLeaderboard(speed, efficiency, time)
        } else {
            NoopLeaderboard()
        }
    }
}
