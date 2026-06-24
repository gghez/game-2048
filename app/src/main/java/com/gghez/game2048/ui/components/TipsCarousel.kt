package com.gghez.game2048.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gghez.game2048.R
import kotlinx.coroutines.delay

private const val TIP_ROTATION_MS = 5_000L

/**
 * A small card that shows one gameplay tip at a time and rotates to the next one
 * every few seconds, looping. The rotation index is purely local presentation
 * state (a carousel position is not game logic), so it lives here rather than in
 * GameUiState. Tips come from the localized `game_tips` string array.
 */
@Composable
fun TipsCarousel(fastAnimations: Boolean, modifier: Modifier = Modifier) {
    val tips = stringArrayResource(R.array.game_tips)
    if (tips.isEmpty()) return

    var index by remember { mutableIntStateOf(0) }
    LaunchedEffectRotation(tipCount = tips.size) { index = (index + 1) % tips.size }

    val border = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.18f)
    val fadeMs = if (fastAnimations) 150 else 400
    Crossfade(
        targetState = index,
        animationSpec = tween(fadeMs),
        label = "tip",
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .heightIn(min = 48.dp),
    ) { i ->
        Row(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Default.Lightbulb,
                contentDescription = stringResource(R.string.cd_tip),
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            )
            Text(
                tips[i],
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                // Reserve a constant two-line height so the card never resizes as tips
                // rotate: a one-line tip and a two-line tip would otherwise give the card
                // different heights and shift the score row + board below it. Two lines fit
                // the longest tip in every locale at normal widths; anything longer at very
                // narrow widths ellipsizes rather than growing the card.
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Advances the tip on a fixed interval. Restarts only when the tip count changes;
 * a single tip never rotates.
 */
@Composable
private fun LaunchedEffectRotation(tipCount: Int, advance: () -> Unit) {
    androidx.compose.runtime.LaunchedEffect(tipCount) {
        if (tipCount <= 1) return@LaunchedEffect
        while (true) {
            delay(TIP_ROTATION_MS)
            advance()
        }
    }
}
