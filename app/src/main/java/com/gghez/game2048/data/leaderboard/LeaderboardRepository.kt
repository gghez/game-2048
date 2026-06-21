package com.gghez.game2048.data.leaderboard

import android.app.Activity

interface LeaderboardRepository {
    val isAvailable: Boolean

    /** Binds the current Activity (required by the Play Games v2 clients). */
    fun attach(activity: Activity?)

    suspend fun signInSilently(): Boolean
    suspend fun submit(kind: LeaderboardKind, value: Long)
    fun showLeaderboards(activity: Activity)
}
