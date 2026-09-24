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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quietgrid.app.R
import com.quietgrid.app.core.GameCatalog
import com.quietgrid.app.core.GameId
import androidx.hilt.navigation.compose.hiltViewModel
import com.quietgrid.app.data.GameStats
import com.quietgrid.app.data.RepositoriesViewModel
import com.quietgrid.app.ui.components.AccountIconButton
import kotlinx.coroutines.launch

private enum class StatsView { OVERVIEW, LOGS }

@Composable
fun StatsScreen(onOpenAccount: () -> Unit) {
    val repositories: RepositoriesViewModel = hiltViewModel()
    val gameIds = remember { GameCatalog.games.map { it.id } }
    val statsByGame by repositories.statsRepository.statsForGames(gameIds)
        .collectAsState(initial = emptyMap())
    val records by repositories.playHistoryRepository.allRecords().collectAsState(initial = emptyList())
    val filterGames = remember(statsByGame, records) { statsFilterGames(statsByGame, records) }

    var view by rememberSaveable { mutableStateOf(StatsView.OVERVIEW) }
    var selectedGame by remember { mutableStateOf<GameId?>(null) }

    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            AccountIconButton(onOpenAccount)
        }

        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(top = 12.dp)) {
            StatsView.entries.forEachIndexed { index, entry ->
                SegmentedButton(
                    selected = view == entry,
                    onClick = { view = entry },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = StatsView.entries.size),
                ) {
                    Text(
                        stringResource(
                            when (entry) {
                                StatsView.OVERVIEW -> R.string.stats_view_overview
                                StatsView.LOGS -> R.string.tab_logs
                            },
                        ),
                    )
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = selectedGame == null,
                onClick = { selectedGame = null },
                label = { Text(stringResource(R.string.common_all)) },
            )
            filterGames.forEach { gameId ->
                FilterChip(
                    selected = selectedGame == gameId,
                    onClick = { selectedGame = gameId },
                    label = { Text(stringResource(GameCatalog.get(gameId).titleRes)) },
                )
            }
        }

        when (view) {
            StatsView.OVERVIEW -> OverviewContent(statsByGame, selectedGame, repositories)
            StatsView.LOGS -> LogsContent(records, selectedGame)
        }
    }
}

@Composable
private fun OverviewContent(
    statsByGame: Map<GameId, GameStats>,
    selectedGame: GameId?,
    repositories: RepositoriesViewModel,
) {
    var showClearDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val overview = remember(statsByGame, selectedGame) { buildStatsOverview(selectedGame, statsByGame) }

    Column(Modifier.fillMaxWidth()) {
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
                        repositories.statsRepository.clearAll()
                        repositories.sessionRepository.clear()
                        repositories.playHistoryRepository.clear()
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
