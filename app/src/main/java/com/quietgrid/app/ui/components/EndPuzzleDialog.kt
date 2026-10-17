package com.quietgrid.app.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.quietgrid.app.R

@Composable
fun EndPuzzleDialog(visible: Boolean, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    if (!visible) return
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.puzzle_play_end_dialog_title)) },
        text = { Text(stringResource(R.string.puzzle_play_end_dialog_message)) },
        confirmButton = {
            Button(onClick = onConfirm) { Text(stringResource(R.string.puzzle_play_end_dialog_confirm)) }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
    )
}
