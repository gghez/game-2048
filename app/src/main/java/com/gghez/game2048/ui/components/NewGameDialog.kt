package com.gghez.game2048.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.gghez.game2048.R

@Composable
fun NewGameDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(
                Modifier.padding(24.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(stringResource(R.string.new_game_title), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.new_game_body), textAlign = TextAlign.Center)
                Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.start))
                }
                OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
}
