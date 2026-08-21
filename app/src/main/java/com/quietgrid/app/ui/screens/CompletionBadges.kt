package com.quietgrid.app.ui.screens

import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.data.PlayRecord
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

val MILESTONE_THRESHOLDS = listOf(10, 25, 50, 100, 250, 500)

fun milestoneReached(solvedCount: Int): Int? = MILESTONE_THRESHOLDS.firstOrNull { it == solvedCount }

fun bouncedBackFromSkid(gameRecords: List<PlayRecord>, difficulty: Difficulty, excludingTimestampMillis: Long): Boolean {
    val priorLosses = gameRecords
        .filter { it.difficulty == difficulty.key && it.timestampMillis != excludingTimestampMillis }
        .sortedByDescending { it.timestampMillis }
        .take(2)
    return priorLosses.size == 2 && priorLosses.all { !it.solved }
}

private fun recordDate(record: PlayRecord): LocalDate =
    Instant.ofEpochMilli(record.timestampMillis).atZone(ZoneId.systemDefault()).toLocalDate()

fun distinctGamesToday(allRecords: List<PlayRecord>, today: LocalDate): Int =
    allRecords.filter { recordDate(it) == today }.map { it.gameId }.distinct().size

fun recordsToday(allRecords: List<PlayRecord>, today: LocalDate): Int =
    allRecords.count { recordDate(it) == today }
