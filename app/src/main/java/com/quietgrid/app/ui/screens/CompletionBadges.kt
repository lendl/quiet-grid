package com.quietgrid.app.ui.screens

import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.data.PlayRecord

fun bouncedBackFromSkid(gameRecords: List<PlayRecord>, difficulty: Difficulty, excludingTimestampMillis: Long): Boolean {
    val priorLosses = gameRecords
        .filter { it.difficulty == difficulty.key && it.timestampMillis != excludingTimestampMillis }
        .sortedByDescending { it.timestampMillis }
        .take(2)
    return priorLosses.size == 2 && priorLosses.all { !it.solved }
}
