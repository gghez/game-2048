package com.gghez.game2048.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gghez.game2048.domain.Board
import com.gghez.game2048.domain.Board.Companion.SIZE
import com.gghez.game2048.domain.Direction
import com.gghez.game2048.ui.theme.BoardDark
import com.gghez.game2048.ui.theme.BoardLight
import com.gghez.game2048.ui.theme.EmptyCellDark
import com.gghez.game2048.ui.theme.EmptyCellLight
import kotlin.math.abs

/**
 * The 4x4 board.
 *
 * Rendering strategy: a static background grid of empty cells, with the live
 * tiles drawn on top in ABSOLUTE position (not in a Row/Column flow). Each tile
 * is keyed by its stable engine id, so when the engine moves a tile to a new
 * cell the same composable survives and its x/y offset animates — that is what
 * produces the sliding animation. New/merged tiles also pop via a scale spring.
 */
@Composable
fun GameGrid(
    board: Board,
    dark: Boolean,
    fastAnimations: Boolean,
    onSwipe: (Direction) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Accumulated drag delta for the current gesture; turned into a Direction on release.
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
                        val threshold = 40f // ignore tiny/accidental drags
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
        BoxWithConstraints(Modifier.fillMaxSize()) {
            // One cell = a quarter of the inner board width. Tiles are positioned
            // and animated against this unit, matching the background grid below.
            val cell: Dp = maxWidth / SIZE

            // Static background grid of empty cells.
            Column(Modifier.fillMaxSize()) {
                for (r in 0 until SIZE) {
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        for (c in 0 until SIZE) {
                            Box(
                                Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .padding(4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (dark) EmptyCellDark else EmptyCellLight),
                            )
                        }
                    }
                }
            }

            // Live tiles, sorted by id for a stable composition order, then drawn
            // absolutely. key(id) ties each composable to a tile across moves.
            val tiles = board.cells.mapIndexedNotNull { index, tile ->
                tile?.let { Triple(it, index / SIZE, index % SIZE) }
            }.sortedBy { it.first.id }

            tiles.forEach { (tile, row, col) ->
                key(tile.id) {
                    MovableTile(
                        value = tile.value,
                        row = row,
                        col = col,
                        cell = cell,
                        dark = dark,
                        fast = fastAnimations,
                    )
                }
            }
        }
    }
}

/**
 * A single tile positioned at (row, col) within the board.
 *
 * The x/y offsets animate whenever the tile's cell changes (slide). A scale
 * animation runs once on first appearance (pop in), and again whenever the
 * value changes — i.e. on a merge — for a small bounce.
 */
@Composable
private fun MovableTile(value: Int, row: Int, col: Int, cell: Dp, dark: Boolean, fast: Boolean) {
    val slideSpec = tween<Dp>(durationMillis = if (fast) 90 else 170, easing = FastOutSlowInEasing)
    val x by animateDpAsState(targetValue = cell * col, animationSpec = slideSpec, label = "tileX")
    val y by animateDpAsState(targetValue = cell * row, animationSpec = slideSpec, label = "tileY")

    val scale = remember { Animatable(0.5f) }
    var previousValue by remember { mutableStateOf(value) }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, tween(durationMillis = if (fast) 90 else 150))
    }
    LaunchedEffect(value) {
        if (value != previousValue) {
            previousValue = value
            scale.snapTo(1.18f)
            scale.animateTo(1f, tween(durationMillis = if (fast) 80 else 130))
        }
    }

    Box(
        Modifier
            .size(cell)
            .offset(x = x, y = y)
            .padding(4.dp)
            .scale(scale.value),
    ) {
        TileView(value = value, dark = dark, modifier = Modifier.fillMaxSize())
    }
}
