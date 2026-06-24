package com.gghez.game2048.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gghez.game2048.R
import com.gghez.game2048.data.settings.ThemeMode
import com.gghez.game2048.domain.Direction
import com.gghez.game2048.domain.GameStatus
import com.gghez.game2048.ui.components.GameGrid
import com.gghez.game2048.ui.components.NewGameDialog
import com.gghez.game2048.ui.components.ScoreCard
import com.gghez.game2048.ui.components.TipsCarousel

/**
 * Portrait layout, bottom-anchored for thumb reach: the header and the rotating tips card
 * sit at the top; a flexible spacer then pushes the score row, the full-width square board
 * and the footer (timer, undo, move count) to the bottom. The board is ALWAYS full board
 * width and never shrinks — the tips and score blocks are sized down instead so everything
 * fits above the system bars on short screens.
 */
@Composable
fun GameScreen(
    ui: GameUiState,
    onSwipe: (Direction) -> Unit,
    onNewGame: () -> Unit,
    onConfirmNewGame: () -> Unit,
    onDismissDialog: () -> Unit,
    onUndo: () -> Unit,
    onContinue: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLeaderboard: () -> Unit,
) {
    val dark = ui.settings.theme == ThemeMode.DARK
    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onOpenLeaderboard) { Icon(Icons.Default.EmojiEvents, stringResource(R.string.cd_leaderboard)) }
            IconButton(onClick = onNewGame) { Icon(Icons.Default.Refresh, stringResource(R.string.cd_restart)) }
            IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, stringResource(R.string.cd_settings)) }
        }
        Spacer(Modifier.height(8.dp))
        // Rotating tips card (fixed two-line height — never reflows the layout).
        TipsCarousel(fastAnimations = ui.settings.fastAnimations)
        // Fixed gap keeps the tips card off the score row even when the flexible space
        // collapses on a short screen; the weight below then bottom-anchors the score row,
        // board and footer for thumb reach on taller screens.
        Spacer(Modifier.height(12.dp))
        Spacer(Modifier.weight(1f))
        // Score row
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ScoreCard(stringResource(R.string.score), ui.state.score, highlighted = true, modifier = Modifier.weight(1f))
            ScoreCard(stringResource(R.string.best_score), ui.bestScore, highlighted = false, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        // Board — ALWAYS full board width (square). It never shrinks: the tips and score
        // blocks above are sized down instead so the whole column fits above the system bars.
        Box(Modifier.fillMaxWidth()) {
            GameGrid(
                board = ui.state.board,
                dark = dark,
                fastAnimations = ui.settings.fastAnimations,
                onSwipe = onSwipe,
                modifier = Modifier.fillMaxWidth(),
            )
            when (ui.state.status) {
                GameStatus.WON -> Overlay(stringResource(R.string.you_win)) {
                    TextButton(onClick = onContinue) { Text(stringResource(R.string.continue_playing)) }
                    TextButton(onClick = onConfirmNewGame) { Text(stringResource(R.string.start)) }
                }
                GameStatus.LOST -> Overlay(stringResource(R.string.game_over)) {
                    TextButton(onClick = onConfirmNewGame) { Text(stringResource(R.string.start)) }
                }
                GameStatus.PLAYING -> {}
            }
        }
        Spacer(Modifier.height(10.dp))
        // Footer
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(formatTime(ui.elapsedSeconds), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(Modifier.weight(1f))
            // Always render the button (disabled when there is nothing to undo) so the
            // footer keeps a constant height and the board never shifts on undo.
            IconButton(onClick = onUndo, enabled = ui.canUndo) {
                Icon(Icons.AutoMirrored.Filled.Undo, stringResource(R.string.undo))
            }
            Text("${ui.state.moves} ${stringResource(R.string.moves_suffix)}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
    if (ui.showNewGameDialog) NewGameDialog(onConfirm = onConfirmNewGame, onDismiss = onDismissDialog)
}

@Composable
private fun BoxScope.Overlay(title: String, actions: @Composable () -> Unit) {
    Box(
        Modifier
            .matchParentSize()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f))
            // Swallow every pointer event so taps and drags never reach the board
            // behind the overlay. The action buttons sit above this Box and keep
            // their own clicks; this only blocks the translucent backdrop area.
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent().changes.forEach { it.consume() }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            actions()
        }
    }
}

private fun formatTime(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}
