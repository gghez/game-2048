package com.gghez.game2048.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gghez.game2048.ui.theme.TileColors

@Composable
fun TileView(value: Int, dark: Boolean, fastAnimations: Boolean, modifier: Modifier = Modifier) {
    // Animate a pop from a small scale -> 1 when this tile id first appears (appear / merge).
    var target by remember { mutableStateOf(0.6f) }
    val scale by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = if (fastAnimations) 70 else 140),
        label = "tileScale",
    )
    remember { target = 1f }

    Box(
        modifier = modifier
            .padding(4.dp)
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .background(TileColors.bg(value, dark)),
        contentAlignment = Alignment.Center,
    ) {
        val fontSize = when {
            value < 100 -> 32.sp
            value < 1000 -> 28.sp
            else -> 22.sp
        }
        Text(
            text = value.toString(),
            color = TileColors.fg(value, dark),
            fontWeight = FontWeight.Bold,
            fontSize = fontSize,
        )
    }
}
