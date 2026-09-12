package com.quietgrid.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quietgrid.app.R
import com.quietgrid.app.core.CHALLENGER_CAPABLE_GAMES
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameCatalog
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.gameDifficultyLabelRes
import com.quietgrid.app.core.mix.Mix
import com.quietgrid.app.core.mix.MixEntry
import com.quietgrid.app.core.mix.MixEntryMode
import com.quietgrid.app.data.AppSettings
import com.quietgrid.app.data.RepositoriesViewModel
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun MixEditorScreen(mixId: String?, onDone: () -> Unit) {
    val repositories: RepositoriesViewModel = hiltViewModel()
    val settings by repositories.settingsRepository.settings.collectAsState(initial = AppSettings())
    val mixes by repositories.mixRepository.mixes.collectAsState(initial = emptyList())
    val existing = remember(mixId, mixes) { mixes.firstOrNull { it.id == mixId } }
    val coroutineScope = rememberCoroutineScope()

    var name by remember(existing) { mutableStateOf(existing?.name ?: "") }
    var entries by remember(existing) { mutableStateOf(existing?.entries ?: emptyList()) }
    var showAddGameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val selectableGames = remember(settings.betaGamesEnabled) {
        GameCatalog.games.filter { !it.beta || settings.betaGamesEnabled }
    }

    fun totalWeight() = entries.sumOf { it.weight }.coerceAtLeast(1)

    Column(Modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState())) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.mix_name_label)) },
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            stringResource(R.string.mix_entries_heading),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 20.dp),
        )

        Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            entries.forEachIndexed { index, entry ->
                if (index > 0) HorizontalDivider()
                MixEntryRow(
                    entry = entry,
                    sharePercent = entry.weight * 100 / totalWeight(),
                    gameTitleRes = GameCatalog.games.first { it.id.key == entry.gameId }.titleRes,
                    onModeChange = { newMode, newDifficulty ->
                        entries = entries.mapIndexed { i, e -> if (i == index) e.copy(mode = newMode, difficulty = newDifficulty) else e }
                    },
                    onWeightChange = { delta ->
                        entries = entries.mapIndexed { i, e -> if (i == index) e.copy(weight = (e.weight + delta).coerceIn(1, 20)) else e }
                    },
                    onRemove = { entries = entries.filterIndexed { i, _ -> i != index } },
                )
            }
        }

        TextButton(onClick = { showAddGameDialog = true }, modifier = Modifier.padding(top = 8.dp)) {
            Text(stringResource(R.string.mix_add_game_button))
        }

        Button(
            onClick = {
                coroutineScope.launch {
                    repositories.mixRepository.saveMix(
                        Mix(id = existing?.id ?: UUID.randomUUID().toString(), name = name, entries = entries),
                    )
                    onDone()
                }
            },
            enabled = name.isNotBlank() && entries.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        ) {
            Text(stringResource(R.string.mix_save_button))
        }

        if (existing != null) {
            TextButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Text(stringResource(R.string.mix_delete_button), color = MaterialTheme.colorScheme.error)
            }
        }
    }

    if (showAddGameDialog) {
        AlertDialog(
            onDismissRequest = { showAddGameDialog = false },
            title = { Text(stringResource(R.string.mix_add_game_dialog_title)) },
            text = {
                LazyColumn {
                    items(selectableGames) { meta ->
                        Text(
                            stringResource(meta.titleRes),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    entries = entries + MixEntry(
                                        gameId = meta.id.key,
                                        mode = MixEntryMode.PUZZLE,
                                        difficulty = Difficulty.EASY.key,
                                        weight = 1,
                                    )
                                    showAddGameDialog = false
                                }
                                .padding(vertical = 12.dp),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddGameDialog = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.mix_delete_confirm_title)) },
            text = { Text(stringResource(R.string.mix_delete_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    val idToDelete = existing?.id
                    if (idToDelete != null) {
                        coroutineScope.launch {
                            repositories.mixRepository.deleteMix(idToDelete)
                            onDone()
                        }
                    }
                }) { Text(stringResource(R.string.mix_delete_confirm_button), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }
}

@Composable
private fun MixEntryRow(
    entry: MixEntry,
    sharePercent: Int,
    gameTitleRes: Int,
    onModeChange: (MixEntryMode, String?) -> Unit,
    onWeightChange: (Int) -> Unit,
    onRemove: () -> Unit,
) {
    val gameId = remember(entry.gameId) { GameId.entries.first { it.key == entry.gameId } }

    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(gameTitleRes), style = MaterialTheme.typography.titleMedium)
            Text(
                "✕",
                modifier = Modifier.clickable(onClick = onRemove),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Difficulty.entries.forEach { difficulty ->
                val selected = entry.mode == MixEntryMode.PUZZLE && entry.difficulty == difficulty.key
                ModeChip(
                    label = stringResource(gameDifficultyLabelRes(gameId, difficulty)),
                    selected = selected,
                    onClick = { onModeChange(MixEntryMode.PUZZLE, difficulty.key) },
                )
            }
            if (gameId in CHALLENGER_CAPABLE_GAMES) {
                ModeChip(
                    label = stringResource(R.string.mix_entry_challenger_chip),
                    selected = entry.mode == MixEntryMode.CHALLENGER,
                    onClick = { onModeChange(MixEntryMode.CHALLENGER, null) },
                )
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.mix_entry_weight_label), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("−", modifier = Modifier.clickable { onWeightChange(-1) }.padding(horizontal = 8.dp))
            Text(entry.weight.toString(), style = MaterialTheme.typography.titleMedium)
            Text("+", modifier = Modifier.clickable { onWeightChange(1) }.padding(horizontal = 8.dp))
            Text(
                stringResource(R.string.mix_entry_share_format, sharePercent),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun ModeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val background = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(background, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(label, color = textColor, style = MaterialTheme.typography.labelMedium)
    }
}
