package com.gghez.game2048

import android.app.Application
import android.content.Context
import com.gghez.game2048.data.leaderboard.LeaderboardProvider
import com.gghez.game2048.data.leaderboard.LeaderboardRepository
import com.gghez.game2048.data.score.ScoreRepository
import com.gghez.game2048.data.settings.SettingsRepository

class AppContainer(context: Context) {
    val settingsRepo = SettingsRepository(context)
    val scoreRepo = ScoreRepository(context)
    val leaderboard: LeaderboardRepository = LeaderboardProvider.create(context)
}

class Game2048App : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
