package com.gghez.game2048.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gghez.game2048.R
import com.gghez.game2048.data.settings.GameSettings
import com.gghez.game2048.data.settings.ThemeMode

/**
 * Settings sheet. Opened fully expanded (skipPartiallyExpanded) so the whole
 * content is visible without dragging. Selectable chips share a uniform layout
 * (centered icon + label) with the checkmark drawn as a corner overlay, so
 * selected and unselected chips keep the same height.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    settings: GameSettings,
    onTheme: (ThemeMode) -> Unit,
    onFast: (Boolean) -> Unit,
    onVibration: (Boolean) -> Unit,
    onSound: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                stringResource(R.string.settings_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SelectableChip(
                    label = stringResource(R.string.theme_light),
                    icon = Icons.Default.LightMode,
                    selected = settings.theme == ThemeMode.LIGHT,
                    modifier = Modifier.weight(1f),
                ) { onTheme(ThemeMode.LIGHT) }
                SelectableChip(
                    label = stringResource(R.string.theme_dark),
                    icon = Icons.Default.DarkMode,
                    selected = settings.theme == ThemeMode.DARK,
                    modifier = Modifier.weight(1f),
                ) { onTheme(ThemeMode.DARK) }
            }
            ToggleRow(stringResource(R.string.sound_title), stringResource(R.string.sound_desc), settings.sound, onSound)
            ToggleRow(stringResource(R.string.fast_anim_title), stringResource(R.string.fast_anim_desc), settings.fastAnimations, onFast)
            ToggleRow(stringResource(R.string.vibration_title), stringResource(R.string.vibration_desc), settings.vibration, onVibration)
        }
    }
}

@Composable
private fun SelectableChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val borderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = if (selected) 0.6f else 0.2f)
    Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = if (selected) 0.06f else 0.02f))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
    ) {
        // Uniform content: icon + label, identical height whether selected or not.
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp))
            Text(label, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        }
        // Checkmark as a corner overlay so it doesn't affect the chip height.
        if (selected) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(18.dp),
            )
        }
    }
}

@Composable
private fun ToggleRow(title: String, desc: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(desc, style = MaterialTheme.typography.bodyMedium)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
