package com.gghez.game2048.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.gghez.game2048.domain.Board
import com.gghez.game2048.domain.Board.Companion.SIZE
import com.gghez.game2048.domain.Direction
import com.gghez.game2048.ui.theme.BoardDark
import com.gghez.game2048.ui.theme.BoardLight
import com.gghez.game2048.ui.theme.EmptyCellDark
import com.gghez.game2048.ui.theme.EmptyCellLight
import kotlin.math.abs

@Composable
fun GameGrid(
    board: Board,
    dark: Boolean,
    fastAnimations: Boolean,
    onSwipe: (Direction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dragAmount = remember { mutableStateOf(0f to 0f) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(if (dark) BoardDark else BoardLight)
            .padding(6.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { dragAmount.value = 0f to 0f },
                    onDrag = { _, delta ->
                        dragAmount.value = (dragAmount.value.first + delta.x) to (dragAmount.value.second + delta.y)
                    },
                    onDragEnd = {
                        val (dx, dy) = dragAmount.value
                        val threshold = 40f
                        if (abs(dx) > abs(dy)) {
                            if (dx > threshold) onSwipe(Direction.RIGHT)
                            else if (dx < -threshold) onSwipe(Direction.LEFT)
                        } else {
                            if (dy > threshold) onSwipe(Direction.DOWN)
                            else if (dy < -threshold) onSwipe(Direction.UP)
                        }
                    },
                )
            },
    ) {
        Column(Modifier.fillMaxSize()) {
            for (r in 0 until SIZE) {
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    for (c in 0 until SIZE) {
                        val tile = board[r, c]
                        Box(Modifier.weight(1f).fillMaxSize()) {
                            if (tile == null) {
                                Box(
                                    Modifier
                                        .padding(4.dp)
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (dark) EmptyCellDark else EmptyCellLight),
                                )
                            } else {
                                androidx.compose.runtime.key(tile.id) {
                                    TileView(tile.value, dark, fastAnimations, Modifier.fillMaxSize())
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
