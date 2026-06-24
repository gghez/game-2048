package com.gghez.game2048

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gghez.game2048.data.settings.SettingsRepository
import com.gghez.game2048.data.settings.ThemeMode
import com.gghez.game2048.ui.game.GameScreen
import com.gghez.game2048.ui.game.GameViewModel
import com.gghez.game2048.ui.settings.SettingsBottomSheet
import com.gghez.game2048.ui.theme.Game2048Theme
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Locale

class MainActivity : ComponentActivity() {

    private var vm: GameViewModel? = null
    // Silent sign-in needs an attached Activity, which only exists from onResume.
    // Fire it once per process the first time we attach, so a relaunch of the
    // Activity (e.g. config change) does not retrigger redundant sign-in attempts.
    private var attemptedSilentSignIn = false

    // Apply the persisted in-app language before the UI inflates, so every launch
    // honours the user's choice. Empty tag = follow the system locale (no override).
    // The persisted value is tiny, so the blocking read here is effectively instant.
    override fun attachBaseContext(newBase: Context) {
        val tag = runBlocking { SettingsRepository(newBase).currentLanguageTag() }
        super.attachBaseContext(if (tag.isEmpty()) newBase else newBase.withLocale(tag))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as Game2048App
        setContent {
            val viewModel: GameViewModel = viewModel(factory = GameViewModel.factory(app))
            vm = viewModel
            val ui by viewModel.ui.collectAsStateWithLifecycle()
            val dark = ui.settings.theme == ThemeMode.DARK

            var showSettings by remember { mutableStateOf(false) }

            Game2048Theme(darkTheme = dark) {
                // Surface paints the themed background edge-to-edge and, crucially,
                // sets the default content color (onBackground) so text/icons follow
                // the light/dark theme instead of defaulting to black.
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
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
                }
                if (showSettings) {
                    SettingsBottomSheet(
                        settings = ui.settings,
                        onTheme = viewModel::setTheme,
                        onFast = viewModel::setFast,
                        onVibration = viewModel::setVibration,
                        onSound = viewModel::setSound,
                        onLanguage = { tag ->
                            // Persist first, then recreate so attachBaseContext re-applies
                            // the new locale to the whole Activity.
                            lifecycleScope.launch {
                                app.container.settingsRepo.setLanguage(tag)
                                recreate()
                            }
                        },
                        onDismiss = { showSettings = false },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val leaderboard = (application as Game2048App).container.leaderboard
        leaderboard.attach(this)
        // Trigger silent sign-in here (not in the ViewModel init): the Play Games v2
        // client requires the Activity bound just above. NoopLeaderboard returns false
        // and does nothing. Guarded so it runs at most once per process.
        if (!attemptedSilentSignIn) {
            attemptedSilentSignIn = true
            lifecycleScope.launch { leaderboard.signInSilently() }
        }
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

/**
 * Return a context configured for [tag]. Uses the Locale(tag) constructor (not
 * forLanguageTag) so the legacy Hebrew tag "iw" is preserved and resolves to
 * res/values-iw; setLocale also flips layout direction for RTL languages.
 */
private fun Context.withLocale(tag: String): Context {
    val locale = Locale(tag)
    Locale.setDefault(locale)
    val config = Configuration(resources.configuration)
    config.setLocale(locale)
    return createConfigurationContext(config)
}
