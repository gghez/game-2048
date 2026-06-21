package com.gghez.game2048

import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gghez.game2048.data.settings.OrientationMode
import com.gghez.game2048.data.settings.ThemeMode
import com.gghez.game2048.ui.game.GameScreen
import com.gghez.game2048.ui.game.GameViewModel
import com.gghez.game2048.ui.settings.SettingsBottomSheet
import com.gghez.game2048.ui.theme.Game2048Theme

class MainActivity : ComponentActivity() {

    private var vm: GameViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as Game2048App
        setContent {
            val viewModel: GameViewModel = viewModel(factory = GameViewModel.factory(app))
            vm = viewModel
            val ui by viewModel.ui.collectAsStateWithLifecycle()
            val dark = ui.settings.theme == ThemeMode.DARK

            LaunchedEffect(ui.settings.orientation) {
                applyOrientation(ui.settings.orientation)
            }

            var showSettings by remember { mutableStateOf(false) }

            Game2048Theme(darkTheme = dark) {
                androidx.compose.foundation.layout.Box(
                    Modifier.windowInsetsPadding(WindowInsets.systemBars),
                ) {
                    GameScreen(
                        ui = ui,
                        onSwipe = viewModel::onSwipe,
                        onNewGame = viewModel::requestNewGame,
                        onConfirmNewGame = viewModel::confirmNewGame,
                        onDismissDialog = viewModel::dismissNewGameDialog,
                        onUndo = viewModel::undo,
                        onContinue = viewModel::continueAfterWin,
                        onOpenSettings = { showSettings = true },
                        onOpenLeaderboard = { viewModel.showLeaderboards(this@MainActivity) },
                    )
                }
                if (showSettings) {
                    SettingsBottomSheet(
                        settings = ui.settings,
                        onTheme = viewModel::setTheme,
                        onFast = viewModel::setFast,
                        onVibration = viewModel::setVibration,
                        onOrientation = viewModel::setOrientation,
                        onDismiss = { showSettings = false },
                    )
                }
            }
        }
    }

    private fun applyOrientation(mode: OrientationMode) {
        requestedOrientation = when (mode) {
            OrientationMode.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            OrientationMode.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            OrientationMode.AUTO -> ActivityInfo.SCREEN_ORIENTATION_SENSOR
        }
    }

    override fun onResume() {
        super.onResume()
        (application as Game2048App).container.leaderboard.attach(this)
        vm?.resumeTimer()
    }

    override fun onPause() {
        super.onPause()
        vm?.pauseTimer()
    }

    override fun onDestroy() {
        (application as Game2048App).container.leaderboard.attach(null)
        super.onDestroy()
    }
}
