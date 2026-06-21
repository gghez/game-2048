package com.gghez.game2048.data.leaderboard

import android.app.Activity

interface LeaderboardRepository {
    val isAvailable: Boolean
    suspend fun signInSilently(): Boolean
    suspend fun submit(kind: LeaderboardKind, value: Long)
    fun showLeaderboards(activity: Activity)
}
