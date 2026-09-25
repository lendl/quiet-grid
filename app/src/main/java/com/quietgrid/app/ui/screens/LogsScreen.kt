package com.quietgrid.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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

    val days = remember(records, mode, selectedGame) {
        groupLogsByDay(filterLogRecords(records, mode, selectedGame), ZoneId.systemDefault())
    }
    val toggled = remember { mutableStateMapOf<String, Boolean>() }
    val today = LocalDate.now()

    Column(modifier.fillMaxWidth()) {
        LogsModeRow(mode = mode, onModeChange = { mode = it })

        if (days.isEmpty()) {
            Text(stringResource(R.string.logs_empty), modifier = Modifier.padding(top = 32.dp))
        } else {
            LazyColumn(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                days.forEachIndexed { dayIndex, day ->
                    val expanded = toggled[day.key] ?: (dayIndex == 0)
                    val lastDay = dayIndex == days.lastIndex
                    item(key = "day_${day.key}") {
                        LogDayHeader(
                            day = day,
                            today = today,
                            expanded = expanded,
                            lineAbove = dayIndex > 0,
                            lineBelow = expanded || !lastDay,
                            onToggle = { toggled[day.key] = !expanded },
                        )
                    }
                    if (expanded) {
                        itemsIndexed(day.records, key = { index, record -> "log_${day.key}_${record.timestampMillis}_$index" }) { index, record ->
                            LogTimelineEntry(
                                record = record,
                                lineBelow = index < day.records.lastIndex || !lastDay,
                                onClick = { selectedRecord = record },
                            )
                        }
                    }
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

private val RAIL_WIDTH = 28.dp
private val RAIL_LINE_WIDTH = 2.dp

@Composable
private fun TimelineRail(dotSize: Dp, dotColor: Color, dotTop: Dp, lineAbove: Boolean, lineBelow: Boolean, filled: Boolean) {
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    Column(
        Modifier.width(RAIL_WIDTH).fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .width(RAIL_LINE_WIDTH)
                .height(dotTop)
                .background(if (lineAbove) lineColor else Color.Transparent),
        )
        Box(
            Modifier
                .size(dotSize)
                .clip(CircleShape)
                .then(if (filled) Modifier.background(dotColor) else Modifier.border(RAIL_LINE_WIDTH, dotColor, CircleShape)),
        )
        Box(
            Modifier
                .width(RAIL_LINE_WIDTH)
                .weight(1f)
                .background(if (lineBelow) lineColor else Color.Transparent),
        )
    }
}

@Composable
private fun LogDayHeader(
    day: LogDay,
    today: LocalDate,
    expanded: Boolean,
    lineAbove: Boolean,
    lineBelow: Boolean,
    onToggle: () -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val label = when (day.date) {
        null -> stringResource(R.string.logs_day_undated)
        today -> stringResource(R.string.logs_day_today)
        today.minusDays(1) -> stringResource(R.string.logs_day_yesterday)
        else -> day.date.format(DateTimeFormatter.ofPattern("EEE d MMM yyyy", locale))
    }
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clickable(onClick = onToggle),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TimelineRail(
            dotSize = 14.dp,
            dotColor = MaterialTheme.colorScheme.primary,
            dotTop = 21.dp,
            lineAbove = lineAbove,
            lineBelow = lineBelow,
            filled = true,
        )
        Column(Modifier.weight(1f).padding(start = 8.dp, top = 12.dp, bottom = 12.dp)) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.logs_day_summary, day.records.size, day.solvedCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LogTimelineEntry(record: PlayRecord, lineBelow: Boolean, onClick: () -> Unit) {
    val gameId = remember(record.gameId) { GameId.entries.first { it.key == record.gameId } }
    val meta = GameCatalog.get(gameId)
    val dotColor = when {
        record.isChallenger -> MaterialTheme.colorScheme.tertiary
        record.solved -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.error
    }

    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        TimelineRail(
            dotSize = 10.dp,
            dotColor = dotColor,
            dotTop = 24.dp,
            lineAbove = true,
            lineBelow = lineBelow,
            filled = record.solved || record.isChallenger,
        )
        OutlinedCard(
            onClick = onClick,
            modifier = Modifier.weight(1f).padding(start = 8.dp, top = 4.dp, bottom = 4.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
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
                            color = dotColor,
                        )
                    }
                    Text(formatLogTime(record.timestampMillis), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private fun formatLogTime(timestampMillis: Long): String =
    if (isLegacyMigratedTimestamp(timestampMillis)) {
        "-"
    } else {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(timestampMillis)
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
