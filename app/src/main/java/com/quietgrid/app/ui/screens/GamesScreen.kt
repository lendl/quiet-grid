package com.quietgrid.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quietgrid.app.R
import com.quietgrid.app.core.GameCatalog
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.GameMeta
import com.quietgrid.app.data.AppContainer
import com.quietgrid.app.data.AppSettings

@Composable
fun GamesScreen(onOpenGame: (GameId) -> Unit, onResumeGame: (GameId) -> Unit) {
    val activeEnvelope by AppContainer.sessionRepository.activeSession.collectAsState(initial = null)
    val activeSummary = activeEnvelope?.let { buildActivePuzzleSummary(it) }
    val settings by AppContainer.settingsRepository.settings.collectAsState(initial = AppSettings())

    @Composable
    fun sortedBy(list: List<GameMeta>) = list
        .map { it to stringResource(it.titleRes) }
        .sortedBy { it.second }
        .map { it.first }

    val readyGames = sortedBy(GameCatalog.games.filter { !it.beta })
    val betaGames = sortedBy(GameCatalog.games.filter { it.beta })

    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        if (activeSummary != null) {
            val activeMeta = GameCatalog.games.first { it.id == activeSummary.gameId }
            Card(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        stringResource(activeMeta.titleRes),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        stringResource(R.string.games_active_puzzle_waiting),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(top = 2.dp, bottom = 10.dp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val difficultyLabelRes = activeSummary.difficulty?.let { difficultyLabelRes(activeSummary.gameId, it) }
                        if (difficultyLabelRes != null) ActivePuzzleChip(stringResource(difficultyLabelRes))
                        if (activeSummary.dimensions != null) ActivePuzzleChip(activeSummary.dimensions)
                        ActivePuzzleChip(activeSummary.elapsedLabel)
                    }
                    Button(
                        onClick = { onResumeGame(activeSummary.gameId) },
                        modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                        Text(stringResource(R.string.common_continue_puzzle))
                    }
                }
            }
        }

        LazyColumn(contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp)) {
            itemsIndexed(readyGames) { index, meta ->
                GameRow(meta, enabled = true, showDivider = index > 0, onClick = { onOpenGame(meta.id) })
            }

            if (betaGames.isNotEmpty()) {
                item {
                    Text(
                        stringResource(R.string.games_coming_soon),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 20.dp, bottom = if (settings.betaGamesEnabled) 4.dp else 8.dp),
                    )
                    if (settings.betaGamesEnabled) {
                        Text(
                            stringResource(R.string.games_beta_disclaimer),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                }
                items(betaGames) { meta ->
                    GameRow(meta, enabled = settings.betaGamesEnabled, onClick = { onOpenGame(meta.id) })
                }
            }
        }
    }
}

@Composable
private fun GameRow(meta: GameMeta, enabled: Boolean, showDivider: Boolean = true, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().alpha(if (enabled) 1f else 0.5f)) {
        if (showDivider) HorizontalDivider()
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = onClick)
                .padding(vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(meta.titleRes), style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(meta.taglineRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun ActivePuzzleChip(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}
