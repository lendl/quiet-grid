package com.quietgrid.app.ui.screens

import androidx.annotation.StringRes
import com.quietgrid.app.R
import com.quietgrid.app.core.GameId

internal data class EndlessModeCopy(
    @StringRes val label: Int,
    @StringRes val description: Int,
    @StringRes val bestValue: Int,
    @StringRes val statsTitle: Int,
    @StringRes val statsBestScore: Int,
    @StringRes val statsBestLevel: Int,
    @StringRes val bestLevelFormat: Int?,
)

internal fun endlessModeCopyFor(gameId: GameId): EndlessModeCopy? = when (gameId) {
    GameId.BLOCKFILL -> EndlessModeCopy(
        label = R.string.blockfill_endless_label,
        description = R.string.blockfill_endless_description,
        bestValue = R.string.blockfill_endless_best_value,
        statsTitle = R.string.blockfill_endless_stats_title,
        statsBestScore = R.string.blockfill_endless_stats_best_score,
        statsBestLevel = R.string.blockfill_endless_stats_best_level,
        bestLevelFormat = R.string.blockfill_multiplier_value,
    )
    GameId.MINESWEEPER -> EndlessModeCopy(
        label = R.string.minesweeper_endless_label,
        description = R.string.minesweeper_endless_description,
        bestValue = R.string.minesweeper_endless_best_value,
        statsTitle = R.string.minesweeper_endless_stats_title,
        statsBestScore = R.string.minesweeper_endless_stats_best_score,
        statsBestLevel = R.string.minesweeper_endless_stats_best_ring,
        bestLevelFormat = null,
    )
    else -> null
}
