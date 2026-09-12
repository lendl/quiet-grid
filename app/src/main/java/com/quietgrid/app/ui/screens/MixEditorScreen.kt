package com.quietgrid.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameCatalog
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.difficultyColor
import com.quietgrid.app.core.gameDifficultyLabelRes
import com.quietgrid.app.core.mix.Mix
import com.quietgrid.app.core.mix.MixEntry
import com.quietgrid.app.core.mix.MixEntryMode
import com.quietgrid.app.core.mix.MixEntryOption
import com.quietgrid.app.core.mix.missingModeOptionsFor
import com.quietgrid.app.data.AppSettings
import com.quietgrid.app.data.RepositoriesViewModel
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.roundToInt

@Composable
fun MixEditorScreen(mixId: String?, onDone: () -> Unit) {
    val repositories: RepositoriesViewModel = hiltViewModel()
    val settings by repositories.settingsRepository.settings.collectAsState(initial = AppSettings())
    val mixes by repositories.mixRepository.mixes.collectAsState(initial = emptyList())
    val existing = remember(mixId, mixes) { mixes.firstOrNull { it.id == mixId } }
    val coroutineScope = rememberCoroutineScope()

    var name by remember(existing) { mutableStateOf(existing?.name ?: "") }
    var entries by remember(existing) {
        mutableStateOf(
            (existing?.entries ?: emptyList())
                .distinctBy { Triple(it.gameId, it.mode, it.difficulty) }
                .map { it.copy(weight = it.weight.coerceIn(1, 10)) },
        )
    }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val groupedEntries = remember(entries) {
        entries.groupBy { it.gameId }.map { (gameIdKey, groupEntries) ->
            GameId.entries.first { it.key == gameIdKey } to groupEntries
        }
    }
    val orderedEntries = remember(groupedEntries) { groupedEntries.flatMap { it.second } }

    val availableGames = remember(entries, settings.betaGamesEnabled) {
        val usedGameIds = entries.map { it.gameId }.toSet()
        GameCatalog.games.filter { (!it.beta || settings.betaGamesEnabled) && it.id.key !in usedGameIds }
    }

    fun addEntry(gameId: GameId, option: MixEntryOption) {
        val alreadyPresent = entries.any { it.gameId == gameId.key && it.mode == option.mode && it.difficulty == option.difficulty }
        if (alreadyPresent) return
        entries = entries + MixEntry(gameId = gameId.key, mode = option.mode, difficulty = option.difficulty, weight = 1)
    }

    fun updateWeight(gameId: String, mode: MixEntryMode, difficulty: String?, newWeight: Int) {
        entries = entries.map {
            if (it.gameId == gameId && it.mode == mode && it.difficulty == difficulty) it.copy(weight = newWeight) else it
        }
    }

    fun removeEntry(gameId: String, mode: MixEntryMode, difficulty: String?) {
        entries = entries.filterNot { it.gameId == gameId && it.mode == mode && it.difficulty == difficulty }
    }

    Column(Modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState())) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.mix_name_label)) },
            modifier = Modifier.fillMaxWidth(),
        )

        Box(Modifier.fillMaxWidth().padding(top = 20.dp), contentAlignment = Alignment.Center) {
            MixPie(entries = orderedEntries, modifier = Modifier.size(160.dp))
        }

        Text(
            stringResource(R.string.mix_entries_heading),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 20.dp),
        )

        if (entries.isEmpty()) {
            Text(
                stringResource(R.string.mix_editor_no_entries_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        } else {
            Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                groupedEntries.forEachIndexed { index, (gameId, groupEntries) ->
                    if (index > 0) HorizontalDivider(Modifier.padding(vertical = 12.dp))
                    MixGameGroup(
                        gameId = gameId,
                        entries = groupEntries,
                        onWeightChange = ::updateWeight,
                        onRemove = ::removeEntry,
                        onAddMode = { option -> addEntry(gameId, option) },
                    )
                }
            }
        }

        Text(
            stringResource(R.string.mix_available_games_heading),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 24.dp),
        )

        if (availableGames.isEmpty()) {
            Text(
                stringResource(R.string.mix_available_games_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        } else {
            Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                availableGames.forEachIndexed { index, meta ->
                    if (index > 0) HorizontalDivider()
                    Text(
                        stringResource(meta.titleRes),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { addEntry(meta.id, MixEntryOption(MixEntryMode.PUZZLE, Difficulty.EASY.key)) }
                            .padding(vertical = 14.dp),
                    )
                }
            }
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
private fun MixPie(entries: List<MixEntry>, modifier: Modifier = Modifier) {
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    val challengerColor = MaterialTheme.colorScheme.primary
    val separatorColor = MaterialTheme.colorScheme.surface
    val wedges = entries.map { entry ->
        val color = if (entry.mode == MixEntryMode.CHALLENGER) {
            challengerColor
        } else {
            val difficulty = Difficulty.entries.firstOrNull { it.key == entry.difficulty } ?: Difficulty.EASY
            difficultyColor(difficulty)
        }
        entry.weight to color
    }
    val totalWeight = wedges.sumOf { it.first }.coerceAtLeast(1)

    Canvas(modifier) {
        if (wedges.isEmpty()) {
            drawArc(color = outlineColor, startAngle = 0f, sweepAngle = 360f, useCenter = true)
            return@Canvas
        }
        val separatorStroke = Stroke(width = 2.dp.toPx())
        var startAngle = -90f
        wedges.forEach { (weight, color) ->
            val sweep = 360f * weight / totalWeight
            drawArc(color = color, startAngle = startAngle, sweepAngle = sweep, useCenter = true)
            drawArc(color = separatorColor, startAngle = startAngle, sweepAngle = sweep, useCenter = true, style = separatorStroke)
            startAngle += sweep
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MixGameGroup(
    gameId: GameId,
    entries: List<MixEntry>,
    onWeightChange: (String, MixEntryMode, String?, Int) -> Unit,
    onRemove: (String, MixEntryMode, String?) -> Unit,
    onAddMode: (MixEntryOption) -> Unit,
) {
    val gameTitleRes = remember(gameId) { GameCatalog.games.first { it.id == gameId }.titleRes }
    val missingOptions = remember(gameId, entries) { missingModeOptionsFor(gameId, entries) }

    Column(Modifier.fillMaxWidth()) {
        Text(stringResource(gameTitleRes), style = MaterialTheme.typography.titleMedium)

        entries.forEach { entry ->
            MixEntryRow(
                gameId = gameId,
                entry = entry,
                onWeightChange = { newWeight -> onWeightChange(entry.gameId, entry.mode, entry.difficulty, newWeight) },
                onRemove = { onRemove(entry.gameId, entry.mode, entry.difficulty) },
            )
        }

        if (missingOptions.isNotEmpty()) {
            FlowRow(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                missingOptions.forEach { option ->
                    val label = if (option.mode == MixEntryMode.CHALLENGER) {
                        stringResource(R.string.mix_entry_challenger_chip)
                    } else {
                        val difficulty = Difficulty.entries.first { it.key == option.difficulty }
                        stringResource(gameDifficultyLabelRes(gameId, difficulty))
                    }
                    ModeChip(
                        label = stringResource(R.string.mix_add_mode_chip_format, label),
                        selected = false,
                        onClick = { onAddMode(option) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MixEntryRow(gameId: GameId, entry: MixEntry, onWeightChange: (Int) -> Unit, onRemove: () -> Unit) {
    val color = if (entry.mode == MixEntryMode.CHALLENGER) {
        MaterialTheme.colorScheme.primary
    } else {
        val difficulty = Difficulty.entries.firstOrNull { it.key == entry.difficulty } ?: Difficulty.EASY
        difficultyColor(difficulty)
    }
    val modeLabel = if (entry.mode == MixEntryMode.CHALLENGER) {
        stringResource(R.string.mix_entry_challenger_chip)
    } else {
        val difficulty = Difficulty.entries.first { it.key == entry.difficulty }
        stringResource(gameDifficultyLabelRes(gameId, difficulty))
    }

    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(color))
            Text(
                modeLabel,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 8.dp).weight(1f),
            )
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.mix_entry_remove_content_description),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.mix_entry_weight_label),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelSmall,
            )
            Slider(
                value = entry.weight.toFloat().coerceIn(1f, 10f),
                onValueChange = { onWeightChange(it.roundToInt().coerceIn(1, 10)) },
                valueRange = 1f..10f,
                steps = 8,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            )
            Text(entry.weight.toString(), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(start = 4.dp))
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
