package com.quietgrid.app.ui.screens

import com.quietgrid.app.R
import com.quietgrid.app.core.GameId
import com.quietgrid.app.data.PlayRecord
import com.quietgrid.app.data.isLegacyMigratedTimestamp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class LogsMode { ALL, SOLO, CHALLENGER }

fun filterLogRecords(records: List<PlayRecord>, mode: LogsMode, gameId: GameId?): List<PlayRecord> = records
    .filter { record ->
        when (mode) {
            LogsMode.ALL -> true
            LogsMode.SOLO -> !record.isChallenger
            LogsMode.CHALLENGER -> record.isChallenger
        }
    }
    .filter { record -> gameId == null || record.gameId == gameId.key }
    .sortedByDescending { it.timestampMillis }

data class LogDay(val date: LocalDate?, val records: List<PlayRecord>) {
    val key: String get() = date?.toString() ?: "undated"
    val solvedCount: Int get() = records.count { it.solved }
}

fun groupLogsByDay(records: List<PlayRecord>, zone: ZoneId): List<LogDay> {
    val dated = records
        .filterNot { isLegacyMigratedTimestamp(it.timestampMillis) }
        .sortedByDescending { it.timestampMillis }
        .groupBy { Instant.ofEpochMilli(it.timestampMillis).atZone(zone).toLocalDate() }
        .map { (date, dayRecords) -> LogDay(date, dayRecords) }
    val undated = records.filter { isLegacyMigratedTimestamp(it.timestampMillis) }
    return if (undated.isEmpty()) dated else dated + LogDay(null, undated)
}

fun logsReasonLabelRes(reason: String): Int = when (reason) {
    "time_up" -> R.string.logs_reason_time_up
    "lives_exhausted", "out_of_lives", "hearts_exhausted" -> R.string.logs_reason_lives_exhausted
    "bank_exhausted" -> R.string.logs_reason_bank_exhausted
    "abandoned" -> R.string.logs_reason_abandoned
    "rule-failure", "no-moves-left" -> R.string.logs_reason_rule_failure
    else -> R.string.logs_reason_generic
}
