package com.gghez.game2048.ui.game

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.gghez.game2048.Game2048App
import com.gghez.game2048.data.leaderboard.LeaderboardKind
import com.gghez.game2048.data.leaderboard.LeaderboardRepository
import com.gghez.game2048.data.score.SavedGame
import com.gghez.game2048.data.score.ScoreRepository
import com.gghez.game2048.data.settings.GameSettings
import com.gghez.game2048.data.settings.OrientationMode
import com.gghez.game2048.data.settings.SettingsRepository
import com.gghez.game2048.data.settings.ThemeMode
import com.gghez.game2048.domain.Board
import com.gghez.game2048.domain.Direction
import com.gghez.game2048.domain.GameEngine
import com.gghez.game2048.domain.GameState
import com.gghez.game2048.domain.GameStatus
import com.gghez.game2048.domain.RandomTileSpawner
import com.gghez.game2048.domain.Tile
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Owns the authoritative game state and exposes it as a single immutable
 * [GameUiState] via [ui]. The UI is a pure function of that snapshot; all events
 * (swipe, undo, new game, settings) flow back through this class.
 *
 * Responsibilities: run the engine, generate stable tile ids, keep one-level undo,
 * tick the timer, persist best score + the in-progress game, and submit scores to
 * the leaderboard when a game ends.
 */
class GameViewModel(
    private val settingsRepo: SettingsRepository,
    private val scoreRepo: ScoreRepository,
    private val leaderboard: LeaderboardRepository,
) : ViewModel() {

    private val engine = GameEngine()
    private val spawner = RandomTileSpawner()
    // Monotonic source of tile ids; new tiles get a fresh id, ensuring uniqueness.
    private var idCounter = 0
    private fun nextId(): Int = idCounter++
    private var previous: GameState? = null // snapshot for single-level undo
    private var timerJob: Job? = null
    private var paused = false

    private val _ui = MutableStateFlow(GameUiState.initial(GameSettings()))
    val ui: StateFlow<GameUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            val settings = settingsRepo.settings.first()
            val best = scoreRepo.bestScore.first()
            val saved = scoreRepo.savedGame.first()
            val state = saved?.let { restore(it) } ?: engine.newGame(spawner, ::nextId)
            if (saved != null) {
                idCounter = (state.board.cells.maxOfOrNull { it?.id ?: -1 } ?: -1) + 1
            }
            _ui.value = GameUiState(
                state = state,
                bestScore = best,
                elapsedSeconds = saved?.elapsedSeconds ?: 0,
                settings = settings,
                leaderboardAvailable = leaderboard.isAvailable,
            )
            observeSettings()
            observeBest()
            startTimer()
            leaderboard.signInSilently()
        }
    }

    private fun restore(s: SavedGame): GameState {
        val cells = s.values.mapIndexed { i, v -> if (v == 0) null else Tile(i, v) }
        return GameState(Board(cells), s.score, s.moves, GameStatus.valueOf(s.status), s.keepPlaying)
    }

    private fun observeSettings() = viewModelScope.launch {
        settingsRepo.settings.collect { s -> _ui.value = _ui.value.copy(settings = s) }
    }

    private fun observeBest() = viewModelScope.launch {
        scoreRepo.bestScore.collect { b -> _ui.value = _ui.value.copy(bestScore = b) }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val u = _ui.value
                if (!paused && u.state.status != GameStatus.LOST) {
                    _ui.value = u.copy(elapsedSeconds = u.elapsedSeconds + 1)
                    persist()
                }
            }
        }
    }

    fun pauseTimer() {
        paused = true
        persist()
    }

    fun resumeTimer() {
        paused = false
    }

    fun onSwipe(dir: Direction) {
        val current = _ui.value.state
        if (current.status == GameStatus.LOST) return
        val result = engine.move(current, dir)
        if (!result.moved) return
        previous = current
        val spawned = engine.spawn(result.state, spawner, ::nextId)
        _ui.value = _ui.value.copy(state = spawned, canUndo = true)
        viewModelScope.launch {
            scoreRepo.updateBestScore(spawned.score)
            if (spawned.status == GameStatus.LOST || spawned.status == GameStatus.WON) submitScores(spawned)
            persist()
        }
    }

    fun undo() {
        val prev = previous ?: return
        _ui.value = _ui.value.copy(state = prev, canUndo = false)
        previous = null
        persist()
    }

    fun requestNewGame() {
        _ui.value = _ui.value.copy(showNewGameDialog = true)
    }

    fun dismissNewGameDialog() {
        _ui.value = _ui.value.copy(showNewGameDialog = false)
    }

    fun confirmNewGame() {
        val fresh = engine.newGame(spawner, ::nextId)
        previous = null
        _ui.value = _ui.value.copy(
            state = fresh,
            elapsedSeconds = 0,
            showNewGameDialog = false,
            canUndo = false,
        )
        viewModelScope.launch {
            scoreRepo.clearSavedGame()
            persist()
        }
    }

    fun continueAfterWin() {
        val s = _ui.value.state
        _ui.value = _ui.value.copy(state = s.copy(status = GameStatus.PLAYING, keepPlaying = true))
    }

    fun setTheme(m: ThemeMode) = viewModelScope.launch { settingsRepo.setTheme(m) }.let { }
    fun setFast(on: Boolean) = viewModelScope.launch { settingsRepo.setFastAnimations(on) }.let { }
    fun setVibration(on: Boolean) = viewModelScope.launch { settingsRepo.setVibration(on) }.let { }
    fun setOrientation(m: OrientationMode) = viewModelScope.launch { settingsRepo.setOrientation(m) }.let { }

    fun showLeaderboards(activity: Activity) = leaderboard.showLeaderboards(activity)

    private suspend fun submitScores(s: GameState) {
        leaderboard.submit(LeaderboardKind.SPEED, s.score.toLong())
        leaderboard.submit(LeaderboardKind.EFFICIENCY, (s.board.values().maxOrNull() ?: 0).toLong())
    }

    private fun persist() {
        val u = _ui.value
        viewModelScope.launch {
            scoreRepo.saveGame(
                SavedGame(
                    values = u.state.board.values(),
                    score = u.state.score,
                    moves = u.state.moves,
                    elapsedSeconds = u.elapsedSeconds,
                    status = u.state.status.name,
                    keepPlaying = u.state.keepPlaying,
                )
            )
        }
    }

    companion object {
        fun factory(app: Game2048App): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                GameViewModel(
                    app.container.settingsRepo,
                    app.container.scoreRepo,
                    app.container.leaderboard,
                )
            }
        }
    }
}
