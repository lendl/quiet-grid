package com.quietgrid.app.ui.screens

import com.quietgrid.app.core.daily.DailyShareCopy
import android.content.Context
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameCatalog
import com.quietgrid.app.core.GameId
import com.quietgrid.app.games.gameDifficultyLabelRes

fun dailyShareCopy(context: Context, gameId: GameId, difficulty: Difficulty): DailyShareCopy = DailyShareCopy(
    header = context.getString(
        R.string.daily_share_header,
        context.getString(GameCatalog.get(gameId).titleRes),
        context.getString(gameDifficultyLabelRes(gameId, difficulty)),
    ),
    solved = { date, elapsed -> context.getString(R.string.daily_share_solved, date, elapsed) },
    lost = { date -> context.getString(R.string.daily_share_lost, date) },
    streak = { count -> context.getString(R.string.daily_share_streak, count) },
)
