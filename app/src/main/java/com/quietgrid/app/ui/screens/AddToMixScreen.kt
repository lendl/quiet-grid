package com.quietgrid.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameCatalog
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.difficultyColor
import com.quietgrid.app.core.gameDifficultyLabelRes
import com.quietgrid.app.core.mix.Mix
import com.quietgrid.app.core.mix.MixEntry
import com.quietgrid.app.core.mix.MixEntryMode
import com.quietgrid.app.core.mix.mixIdsContainingPuzzle
import com.quietgrid.app.core.mix.nextMixName
import com.quietgrid.app.data.RepositoriesViewModel
import com.quietgrid.app.ui.components.OutlinedGlowButton
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun AddToMixScreen(gameId: GameId, difficulty: Difficulty, onDone: () -> Unit, onNewMix: () -> Unit) {
    val repositories: RepositoriesViewModel = hiltViewModel()
    val scope = rememberCoroutineScope()
    val mixes by repositories.mixRepository.mixes.collectAsState(initial = null)
    var selectedIds by rememberSaveable { mutableStateOf<List<String>?>(null) }
    var saving by remember { mutableStateOf(false) }
    val loadedMixes = mixes ?: return
    val selected = selectedIds?.toSet() ?: mixIdsContainingPuzzle(loadedMixes, gameId, difficulty)

    fun saveThen(next: () -> Unit) {
        if (saving) return
        saving = true
        scope.launch {
            repositories.mixRepository.setPuzzleMembership(gameId, difficulty, selected)
            next()
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        PuzzleSummaryCard(gameId, difficulty, Modifier.padding(top = 8.dp))

        OutlinedButton(
            onClick = { saveThen(onNewMix) },
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 16.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
            Text(stringResource(R.string.add_to_mix_new_mix))
        }

        if (loadedMixes.isEmpty()) {
            Text(
                stringResource(R.string.mix_empty_state),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp).align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.weight(1f))
        } else {
            LazyColumn(Modifier.weight(1f).padding(top = 8.dp)) {
                items(loadedMixes, key = { it.id }) { mix ->
                    MixToggleRow(
                        mix = mix,
                        checked = mix.id in selected,
                        onCheckedChange = { checked ->
                            selectedIds = (if (checked) selected + mix.id else selected - mix.id).toList()
                        },
                    )
                }
            }
        }

        OutlinedGlowButton(
            onClick = { saveThen(onDone) },
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 16.dp),
        ) {
            Text(stringResource(R.string.add_to_mix_done), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun NewMixScreen(gameId: GameId, difficulty: Difficulty, onCreated: () -> Unit) {
    val repositories: RepositoriesViewModel = hiltViewModel()
    val scope = rememberCoroutineScope()
    val mixes by repositories.mixRepository.mixes.collectAsState(initial = null)
    var name by rememberSaveable { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val loadedMixes = mixes ?: return
    val defaultName = nextMixName(loadedMixes.size)

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    fun create() {
        if (creating) return
        creating = true
        val mix = Mix(
            id = UUID.randomUUID().toString(),
            name = name.trim().ifEmpty { defaultName },
            entries = listOf(MixEntry(gameId = gameId.key, mode = MixEntryMode.PUZZLE, difficulty = difficulty.key, weight = 1)),
        )
        scope.launch {
            repositories.mixRepository.saveMix(mix)
            onCreated()
        }
    }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 16.dp).imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PuzzleSummaryCard(gameId, difficulty, Modifier.padding(top = 8.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.mix_name_label)) },
            placeholder = { Text(defaultName) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { create() }),
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp).focusRequester(focusRequester),
        )

        OutlinedGlowButton(onClick = { create() }, modifier = Modifier.padding(top = 24.dp)) {
            Text(stringResource(R.string.add_to_mix_create), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PuzzleSummaryCard(gameId: GameId, difficulty: Difficulty, modifier: Modifier = Modifier) {
    val accent = difficultyColor(difficulty)
    Row(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(10.dp).background(accent, CircleShape))
        Text(
            stringResource(GameCatalog.get(gameId).titleRes),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            stringResource(gameDifficultyLabelRes(gameId, difficulty)),
            style = MaterialTheme.typography.labelLarge,
            color = accent,
        )
    }
}

@Composable
private fun MixToggleRow(mix: Mix, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(mix.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(R.string.mix_card_entry_count, mix.entries.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            if (checked) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
            contentDescription = null,
            tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        )
    }
}
