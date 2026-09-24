package com.quietgrid.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameCatalog
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.formatElapsed
import com.quietgrid.app.data.PlayRecord
import com.quietgrid.app.data.isLegacyMigratedTimestamp
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun LogsContent(records: List<PlayRecord>, selectedGame: GameId?, modifier: Modifier = Modifier) {
    var mode by remember { mutableStateOf(LogsMode.ALL) }
    var selectedRecord by remember { mutableStateOf<PlayRecord?>(null) }

    val filtered = remember(records, mode, selectedGame) { filterLogRecords(records, mode, selectedGame) }

    Column(modifier.fillMaxWidth()) {
        LogsModeRow(mode = mode, onModeChange = { mode = it })

        if (filtered.isEmpty()) {
            Text(stringResource(R.string.logs_empty), modifier = Modifier.padding(top = 32.dp))
        } else {
            LazyColumn(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                items(filtered) { record ->
                    LogRow(record, onClick = { selectedRecord = record })
                    HorizontalDivider()
                }
            }
        }
    }

    selectedRecord?.let { record ->
        LogDetailSheet(record, onDismiss = { selectedRecord = null })
    }
}

@Composable
private fun LogsModeRow(mode: LogsMode, onModeChange: (LogsMode) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(selected = mode == LogsMode.ALL, onClick = { onModeChange(LogsMode.ALL) }, label = { Text(stringResource(R.string.common_all)) })
        FilterChip(selected = mode == LogsMode.SOLO, onClick = { onModeChange(LogsMode.SOLO) }, label = { Text(stringResource(R.string.logs_mode_solo)) })
        FilterChip(selected = mode == LogsMode.CHALLENGER, onClick = { onModeChange(LogsMode.CHALLENGER) }, label = { Text(stringResource(R.string.logs_mode_challenger)) })
    }
}

@Composable
private fun LogRow(record: PlayRecord, onClick: () -> Unit) {
    val gameId = remember(record.gameId) { GameId.entries.first { it.key == record.gameId } }
    val meta = GameCatalog.get(gameId)

    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(stringResource(meta.titleRes), style = MaterialTheme.typography.titleSmall)
            val subtitleRes = if (record.isChallenger) R.string.logs_mode_challenger else difficultyLabelRes(gameId, Difficulty.fromKey(record.difficulty))
            Text(stringResource(subtitleRes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(horizontalAlignment = Alignment.End) {
            if (record.isChallenger) {
                Text(stringResource(R.string.logs_challenger_summary, record.score, record.puzzlesSolved ?: 0), style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(
                    stringResource(if (record.solved) R.string.logs_result_won else R.string.logs_result_lost),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(formatLogTimestamp(record.timestampMillis), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogDetailSheet(record: PlayRecord, onDismiss: () -> Unit) {
    val gameId = remember(record.gameId) { GameId.entries.first { it.key == record.gameId } }
    val meta = GameCatalog.get(gameId)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text(stringResource(R.string.logs_detail_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(meta.titleRes), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))

            if (record.isChallenger) {
                DetailRow(stringResource(R.string.logs_detail_tier_reached), stringResource(difficultyLabelRes(gameId, Difficulty.fromKey(record.difficulty))))
                DetailRow(stringResource(R.string.logs_challenger_summary, record.score, record.puzzlesSolved ?: 0), "")
                record.lossReason?.let { reason -> DetailRow(stringResource(R.string.logs_detail_ended_by), stringResource(logsReasonLabelRes(reason))) }
            } else {
                DetailRow(stringResource(difficultyLabelRes(gameId, Difficulty.fromKey(record.difficulty))), "")
                DetailRow(stringResource(if (record.solved) R.string.logs_result_won else R.string.logs_result_lost), formatElapsed(record.elapsedSeconds))
                if (!record.solved) {
                    record.lossReason?.let { reason -> DetailRow(stringResource(R.string.logs_detail_ended_by), stringResource(logsReasonLabelRes(reason))) }
                }
            }

            Text(formatLogTimestamp(record.timestampMillis), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        if (value.isNotEmpty()) Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun formatLogTimestamp(timestampMillis: Long): String =
    if (isLegacyMigratedTimestamp(timestampMillis)) {
        "-"
    } else {
        SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(timestampMillis)
    }
