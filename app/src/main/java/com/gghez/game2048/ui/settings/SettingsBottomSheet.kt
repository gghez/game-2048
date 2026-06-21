package com.gghez.game2048.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gghez.game2048.R
import com.gghez.game2048.data.settings.GameSettings
import com.gghez.game2048.data.settings.OrientationMode
import com.gghez.game2048.data.settings.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    settings: GameSettings,
    onTheme: (ThemeMode) -> Unit,
    onFast: (Boolean) -> Unit,
    onVibration: (Boolean) -> Unit,
    onOrientation: (OrientationMode) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                stringResource(R.string.settings_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SelectableChip(stringResource(R.string.theme_light), settings.theme == ThemeMode.LIGHT, Modifier.weight(1f)) { onTheme(ThemeMode.LIGHT) }
                SelectableChip(stringResource(R.string.theme_dark), settings.theme == ThemeMode.DARK, Modifier.weight(1f)) { onTheme(ThemeMode.DARK) }
            }
            ToggleRow(stringResource(R.string.fast_anim_title), stringResource(R.string.fast_anim_desc), settings.fastAnimations, onFast)
            ToggleRow(stringResource(R.string.vibration_title), stringResource(R.string.vibration_desc), settings.vibration, onVibration)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SelectableChip(stringResource(R.string.orientation_portrait), settings.orientation == OrientationMode.PORTRAIT, Modifier.weight(1f)) { onOrientation(OrientationMode.PORTRAIT) }
                SelectableChip(stringResource(R.string.orientation_auto), settings.orientation == OrientationMode.AUTO, Modifier.weight(1f)) { onOrientation(OrientationMode.AUTO) }
                SelectableChip(stringResource(R.string.orientation_landscape), settings.orientation == OrientationMode.LANDSCAPE, Modifier.weight(1f)) { onOrientation(OrientationMode.LANDSCAPE) }
            }
        }
    }
}

@Composable
private fun SelectableChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val borderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = if (selected) 0.6f else 0.2f)
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.04f))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (selected) Icon(Icons.Default.Check, contentDescription = null)
        Text(label, fontWeight = FontWeight.SemiBold)
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
