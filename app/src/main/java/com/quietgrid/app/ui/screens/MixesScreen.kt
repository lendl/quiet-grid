package com.quietgrid.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quietgrid.app.R
import com.quietgrid.app.core.mix.Mix
import com.quietgrid.app.ui.components.AccountIconButton
import com.quietgrid.app.ui.components.pressScale

@Composable
fun MixesScreen(
    mixes: List<Mix>,
    onPlay: (Mix) -> Unit,
    onEdit: (String) -> Unit,
    onNewMix: () -> Unit,
    onOpenAccount: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            AccountIconButton(onOpenAccount)
            val interactionSource = remember { MutableInteractionSource() }
            IconButton(
                onClick = onNewMix,
                interactionSource = interactionSource,
                modifier = Modifier.pressScale(interactionSource),
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.mix_new_content_description))
            }
        }

        LazyColumn(contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)) {
            if (mixes.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.mix_empty_state),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                    )
                }
            } else {
                itemsIndexed(mixes) { index, mix ->
                    MixListRow(mix = mix, showDivider = index > 0, onPlay = { onPlay(mix) }, onEdit = { onEdit(mix.id) })
                }
            }
        }
    }
}

@Composable
private fun MixListRow(mix: Mix, showDivider: Boolean, onPlay: () -> Unit, onEdit: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        if (showDivider) HorizontalDivider()
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onEdit)
                .padding(vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(mix.name, style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(R.string.mix_card_entry_count, mix.entries.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            if (mix.entries.isNotEmpty()) {
                Text(
                    stringResource(R.string.mix_play_button),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = onPlay).padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}
