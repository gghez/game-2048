package com.gghez.game2048.data.leaderboard

import android.app.Activity

/** Fallback used when Google Play Games is not configured. The app runs normally without it. */
class NoopLeaderboard : LeaderboardRepository {
    override val isAvailable: Boolean = false
    override fun attach(activity: Activity?) {}
    override suspend fun signInSilently(): Boolean = false
    override suspend fun submit(kind: LeaderboardKind, value: Long) {}
    override fun showLeaderboards(activity: Activity) {}
}
