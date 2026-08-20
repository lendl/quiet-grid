package com.quietgrid.app.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quietgrid.app.R

data class QuickStartExample(@param:StringRes val wordRes: Int, @param:StringRes val hintRes: Int)

data class QuickStartContent(
    @param:StringRes val goalRes: Int,
    val bulletRes: List<Int>,
    val examples: List<QuickStartExample> = emptyList(),
    @param:StringRes val hookRes: Int,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickStartSheet(content: QuickStartContent, onDismiss: () -> Unit, onQuickPlay: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text(stringResource(R.string.quick_start_title), style = MaterialTheme.typography.titleMedium)

            Text(
                stringResource(content.goalRes),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
            )

            content.bulletRes.forEach { bulletRes ->
                Row(Modifier.padding(top = 10.dp)) {
                    Text("•", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        stringResource(bulletRes),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }
            }

            if (content.examples.isNotEmpty()) {
                HorizontalDivider(Modifier.padding(vertical = 16.dp))
                Text(stringResource(R.string.quick_start_examples_title), style = MaterialTheme.typography.titleSmall)
                content.examples.forEach { example ->
                    Column(Modifier.padding(top = 10.dp)) {
                        Text(stringResource(example.wordRes), style = MaterialTheme.typography.titleSmall)
                        Text(
                            stringResource(example.hintRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Text(
                stringResource(content.hookRes),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 20.dp, bottom = 20.dp),
            )

            Button(onClick = onQuickPlay, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.how_to_play_quick_play))
            }
        }
    }
}
