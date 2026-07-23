package com.quietgrid.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameCatalog
import com.quietgrid.app.core.GameId
import com.quietgrid.app.data.AppContainer
import kotlinx.coroutines.launch

@Composable
fun StatsScreen() {
    val gameIds = remember { GameCatalog.games.map { it.id } }
    val statsByGame by AppContainer.statsRepository.statsForGames(gameIds)
        .collectAsState(initial = emptyMap())
    val playedGameIds = remember(statsByGame) {
        GameCatalog.games
            .filter { meta -> (statsByGame[meta.id]?.let { s -> Difficulty.entries.sumOf { s.forDifficulty(it).played } } ?: 0) > 0 }
            .sortedByDescending { meta -> statsByGame[meta.id]?.let { s -> Difficulty.entries.sumOf { s.forDifficulty(it).played } } ?: 0 }
            .map { it.id }
    }

    var selectedScope by remember { mutableStateOf<GameId?>(null) }
    var showClearDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = selectedScope == null,
                onClick = { selectedScope = null },
                label = { Text(stringResource(R.string.common_all)) },
            )
            playedGameIds.forEach { gameId ->
                val meta = GameCatalog.games.first { it.id == gameId }
                FilterChip(
                    selected = selectedScope == gameId,
                    onClick = { selectedScope = gameId },
                    label = { Text(stringResource(meta.titleRes)) },
                )
            }
        }

        val overview = remember(statsByGame, selectedScope) { buildStatsOverview(selectedScope, statsByGame) }

        StatsOverviewContent(overview, modifier = Modifier.padding(top = 16.dp))

        TextButton(
            onClick = { showClearDialog = true },
            modifier = Modifier.padding(top = 16.dp),
        ) {
            Text(stringResource(R.string.stats_clear_data))
        }

        Text(
            stringResource(R.string.stats_privacy_text),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.stats_clear_data_title)) },
            text = { Text(stringResource(R.string.stats_clear_data_message)) },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        AppContainer.statsRepository.clearAll()
                        AppContainer.sessionRepository.clear()
                    }
                    showClearDialog = false
                }) { Text(stringResource(R.string.stats_clear_data)) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearDialog = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }
}
