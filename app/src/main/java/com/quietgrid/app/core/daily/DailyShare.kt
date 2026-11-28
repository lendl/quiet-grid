package com.quietgrid.app.core.daily

import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.formatElapsed
import com.quietgrid.app.data.PlayHistoryStore
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate

class DailyShareCopy(
    val header: String,
    val solved: (date: String, elapsed: String) -> String,
    val lost: (date: String) -> String,
    val streak: (Int) -> String,
)

suspend fun dailyShareText(history: PlayHistoryStore, gameId: GameId, difficulty: Difficulty, dateKey: String, copy: DailyShareCopy): String? {
    val records = history.allRecords().first()
    val record = records
        .filter { it.gameId == gameId.key && it.dailyDate == dateKey && it.difficulty == difficulty.key }
        .maxByOrNull { it.timestampMillis } ?: return null
    val date = runCatching { LocalDate.parse(dateKey) }.getOrNull() ?: return null
    val streak = dailyStreak(records, setOf(gameId), date)
    val resultLine = if (record.solved) {
        copy.solved(dateKey, formatElapsed(record.elapsedSeconds))
    } else {
        copy.lost(dateKey)
    }
    val streakLine = if (streak >= 2) copy.streak(streak) else null
    return joinDailyShareLines(copy.header, resultLine, record.shareDetail, streakLine)
}
