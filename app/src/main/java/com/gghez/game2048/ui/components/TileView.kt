package com.gghez.game2048.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gghez.game2048.ui.theme.TileColors

/** Pure visual of a single tile: coloured rounded background + the number. */
@Composable
fun TileView(value: Int, dark: Boolean, modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(TileColors.bg(value, dark)),
        contentAlignment = Alignment.Center,
    ) {
        // Size the digits relative to the tile so they fill it like the mockup,
        // shrinking only as the number grows longer. Uses the system font.
        val digits = value.toString().length
        val factor = when {
            digits <= 2 -> 0.50f
            digits == 3 -> 0.40f
            digits == 4 -> 0.31f
            else -> 0.25f
        }
        Text(
            text = value.toString(),
            color = TileColors.fg(value, dark),
            fontWeight = FontWeight.Bold,
            fontSize = (maxWidth.value * factor).sp,
        )
    }
}
