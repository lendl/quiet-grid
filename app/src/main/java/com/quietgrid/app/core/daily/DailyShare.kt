package com.quietgrid.app.core.daily

import android.content.Context
import android.content.Intent
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameCatalog
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.formatElapsed
import com.quietgrid.app.core.gameDifficultyLabelRes
import com.quietgrid.app.data.PlayHistoryStore
import kotlinx.coroutines.flow.first
import java.time.LocalDate

suspend fun shareDailyResult(context: Context, history: PlayHistoryStore, gameId: GameId, difficulty: Difficulty, dateKey: String) {
    val records = history.allRecords().first()
    val record = records
        .filter { it.gameId == gameId.key && it.dailyDate == dateKey && it.difficulty == difficulty.key }
        .maxByOrNull { it.timestampMillis } ?: return
    val date = runCatching { LocalDate.parse(dateKey) }.getOrNull() ?: return
    val streak = dailyStreak(records, setOf(gameId), date)
    val header = context.getString(
        R.string.daily_share_header,
        context.getString(GameCatalog.get(gameId).titleRes),
        context.getString(gameDifficultyLabelRes(gameId, difficulty)),
    )
    val resultLine = if (record.solved) {
        context.getString(R.string.daily_share_solved, dateKey, formatElapsed(record.elapsedSeconds))
    } else {
        context.getString(R.string.daily_share_lost, dateKey)
    }
    val streakLine = if (streak >= 2) context.getString(R.string.daily_share_streak, streak) else null
    val text = joinDailyShareLines(header, resultLine, record.shareDetail, streakLine)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    val chooser = Intent.createChooser(send, context.getString(R.string.daily_share_chooser))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(chooser)
}
