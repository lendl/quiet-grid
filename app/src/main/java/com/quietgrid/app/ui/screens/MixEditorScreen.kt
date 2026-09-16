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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
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
import com.quietgrid.app.core.mix.allModeOptionsFor
import com.quietgrid.app.core.mix.groupEntriesByKnownGame
import com.quietgrid.app.core.mix.missingModeOptionsFor
import com.quietgrid.app.data.AppSettings
import com.quietgrid.app.data.RepositoriesViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

internal const val MIX_NAME_FIELD_TEST_TAG = "mix_name_field"

@Composable
fun MixEditorScreen(mixId: String, onDone: () -> Unit, renameTrigger: Int = 0, deleteTrigger: Int = 0) {
    val repositories: RepositoriesViewModel = hiltViewModel()
    MixEditorContent(
        mixId = mixId,
        onDone = onDone,
        repositories = repositories,
        renameTrigger = renameTrigger,
        deleteTrigger = deleteTrigger,
    )
}

@Composable
internal fun MixEditorContent(
    mixId: String,
    onDone: () -> Unit,
    repositories: RepositoriesViewModel,
    renameTrigger: Int = 0,
    deleteTrigger: Int = 0,
) {
    val settings by repositories.settingsRepository.settings.collectAsState(initial = AppSettings())
    val mixes by repositories.mixRepository.mixes.collectAsState(initial = emptyList())
    val existing = remember(mixId, mixes) { mixes.firstOrNull { it.id == mixId } }
    val coroutineScope = rememberCoroutineScope()

    var name by remember(existing?.id) { mutableStateOf(existing?.name ?: "") }
    var entries by remember(existing?.id) {
        mutableStateOf(
            (existing?.entries ?: emptyList())
                .distinctBy { Triple(it.gameId, it.mode, it.difficulty) }
                .map { it.copy(weight = it.weight.coerceIn(1, 10)) },
        )
    }
    var isEditingName by remember(existing?.id) { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var expandedAvailableGameId by remember(existing?.id) { mutableStateOf<GameId?>(null) }

    LaunchedEffect(renameTrigger, existing?.id) {
        if (existing != null && renameTrigger > 0) isEditingName = true
    }
    LaunchedEffect(deleteTrigger, existing?.id) {
        if (existing != null && deleteTrigger > 0) showDeleteConfirm = true
    }

    fun persist(newName: String = name, newEntries: List<MixEntry> = entries) {
        repositories.viewModelScope.launch {
            repositories.mixRepository.saveMix(Mix(id = mixId, name = newName, entries = newEntries))
        }
    }

    val groupedEntries = remember(entries) { groupEntriesByKnownGame(entries) }
    val orderedEntries = remember(groupedEntries) { groupedEntries.flatMap { it.second } }

    val availableGames = remember(entries, settings.betaGamesEnabled) {
        val usedGameIds = entries.map { it.gameId }.toSet()
        GameCatalog.games.filter { (!it.beta || settings.betaGamesEnabled) && it.id.key !in usedGameIds }
    }

    fun addEntry(gameId: GameId, option: MixEntryOption) {
        val alreadyPresent = entries.any { it.gameId == gameId.key && it.mode == option.mode && it.difficulty == option.difficulty }
        if (!alreadyPresent) {
            entries = entries + MixEntry(gameId = gameId.key, mode = option.mode, difficulty = option.difficulty, weight = 1)
            persist(newEntries = entries)
        }
        expandedAvailableGameId = null
    }

    fun updateWeight(gameId: String, mode: MixEntryMode, difficulty: String?, newWeight: Int) {
        entries = entries.map {
            if (it.gameId == gameId && it.mode == mode && it.difficulty == difficulty) it.copy(weight = newWeight) else it
        }
        persist(newEntries = entries)
    }

    fun removeEntry(gameId: String, mode: MixEntryMode, difficulty: String?) {
        entries = entries.filterNot { it.gameId == gameId && it.mode == mode && it.difficulty == difficulty }
        persist(newEntries = entries)
    }

    fun commitRename() {
        if (!isEditingName) return
        isEditingName = false
        val resolvedName = name.ifBlank { existing?.name.orEmpty() }
        name = resolvedName
        persist(newName = resolvedName)
    }

    if (existing == null) return

    Column(Modifier.fillMaxWidth().padding(16.dp).verticalScroll(rememberScrollState())) {
        if (isEditingName) {
            MixNameEditField(
                name = name,
                onNameChange = { name = it },
                onCommit = ::commitRename,
            )
        }

        Box(Modifier.fillMaxWidth().padding(top = 20.dp), contentAlignment = Alignment.Center) {
            MixPie(entries = orderedEntries, modifier = Modifier.size(160.dp))
        }

        if (orderedEntries.isNotEmpty()) {
            MixPieLegend(groupedEntries = groupedEntries, modifier = Modifier.padding(top = 12.dp))
        }

        if (entries.isEmpty()) {
            AvailableGamesSection(
                availableGames = availableGames,
                expandedGameId = expandedAvailableGameId,
                onToggleExpand = { gameId -> expandedAvailableGameId = if (expandedAvailableGameId == gameId) null else gameId },
                onAddMode = ::addEntry,
            )
        } else {
            Text(
                stringResource(R.string.mix_entries_heading),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 20.dp),
            )
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

            AvailableGamesSection(
                availableGames = availableGames,
                expandedGameId = expandedAvailableGameId,
                onToggleExpand = { gameId -> expandedAvailableGameId = if (expandedAvailableGameId == gameId) null else gameId },
                onAddMode = ::addEntry,
                topPadding = 24.dp,
            )
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
                    coroutineScope.launch {
                        repositories.mixRepository.deleteMix(mixId)
                        onDone()
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
private fun MixNameEditField(
    name: String,
    onNameChange: (String) -> Unit,
    onCommit: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    var hasBeenFocused by remember { mutableStateOf(false) }
    var fieldValue by remember { mutableStateOf(TextFieldValue(name, selection = TextRange(0, name.length))) }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    OutlinedTextField(
        value = fieldValue,
        onValueChange = { newValue ->
            fieldValue = newValue
            onNameChange(newValue.text)
        },
        label = { Text(stringResource(R.string.mix_name_label)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onCommit() }),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(MIX_NAME_FIELD_TEST_TAG)
            .focusRequester(focusRequester)
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    hasBeenFocused = true
                } else if (hasBeenFocused) {
                    onCommit()
                }
            },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AvailableGamesSection(
    availableGames: List<com.quietgrid.app.core.GameMeta>,
    expandedGameId: GameId?,
    onToggleExpand: (GameId) -> Unit,
    onAddMode: (GameId, MixEntryOption) -> Unit,
    topPadding: androidx.compose.ui.unit.Dp = 20.dp,
) {
    Text(
        stringResource(R.string.mix_available_games_heading),
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.padding(top = topPadding),
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
                        .clickable { onToggleExpand(meta.id) }
                        .padding(vertical = 14.dp),
                )
                if (expandedGameId == meta.id) {
                    FlowRow(
                        Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        allModeOptionsFor(meta.id).forEach { option ->
                            val label = if (option.mode == MixEntryMode.CHALLENGER) {
                                stringResource(R.string.mix_entry_challenger_chip)
                            } else {
                                val difficulty = Difficulty.entries.first { it.key == option.difficulty }
                                stringResource(gameDifficultyLabelRes(meta.id, difficulty))
                            }
                            ModeChip(label = label, onClick = { onAddMode(meta.id, option) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun mixEntryColor(entry: MixEntry): Color {
    val base = if (entry.mode == MixEntryMode.CHALLENGER) {
        MaterialTheme.colorScheme.primary
    } else {
        val difficulty = Difficulty.entries.firstOrNull { it.key == entry.difficulty } ?: Difficulty.EASY
        difficultyColor(difficulty)
    }
    return shadeForGame(base, entry.gameId)
}

private fun shadeForGame(base: Color, gameId: String): Color {
    val bucket = (gameId.hashCode() and 0x7fffffff) % 5
    val amount = (bucket - 2) * 0.08f
    return if (amount >= 0) lerp(base, Color.Black, amount) else lerp(base, Color.White, -amount)
}

@Composable
private fun mixEntryModeLabel(gameId: GameId, entry: MixEntry): String = if (entry.mode == MixEntryMode.CHALLENGER) {
    stringResource(R.string.mix_entry_challenger_chip)
} else {
    val difficulty = Difficulty.entries.first { it.key == entry.difficulty }
    stringResource(gameDifficultyLabelRes(gameId, difficulty))
}

@Composable
private fun MixPie(entries: List<MixEntry>, modifier: Modifier = Modifier) {
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    val separatorColor = MaterialTheme.colorScheme.surface
    val wedges = entries.map { entry -> entry.weight to mixEntryColor(entry) }
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
private fun MixPieLegend(groupedEntries: List<Pair<GameId, List<MixEntry>>>, modifier: Modifier = Modifier) {
    FlowRow(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        groupedEntries.forEach { (gameId, groupEntries) ->
            val gameTitle = stringResource(GameCatalog.games.first { it.id == gameId }.titleRes)
            groupEntries.forEach { entry ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(mixEntryColor(entry)))
                    Text(
                        "$gameTitle · ${mixEntryModeLabel(gameId, entry)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
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
                        onClick = { onAddMode(option) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MixEntryRow(gameId: GameId, entry: MixEntry, onWeightChange: (Int) -> Unit, onRemove: () -> Unit) {
    val color = mixEntryColor(entry)
    val modeLabel = mixEntryModeLabel(gameId, entry)

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
private fun ModeChip(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
    }
}
