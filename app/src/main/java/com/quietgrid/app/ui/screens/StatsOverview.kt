package com.quietgrid.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameCatalog
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.difficultyColor
import com.quietgrid.app.data.GameStats
import com.quietgrid.app.games.chimptest.chimpDifficultyLabelRes
import com.quietgrid.app.games.minesweeper.minesweeperDifficultyLabelRes
import com.quietgrid.app.games.nonogram.nonogramDifficultyLabelRes
import com.quietgrid.app.games.sudoku.sudokuDifficultyLabelRes
import com.quietgrid.app.games.takuzu.takuzuDifficultyLabelRes
import com.quietgrid.app.games.wordsearch.wordSearchDifficultyLabelRes

fun difficultyLabelRes(gameId: GameId, difficulty: Difficulty): Int = when (gameId) {
    GameId.TAKUZU -> takuzuDifficultyLabelRes(difficulty)
    GameId.NONOGRAM -> nonogramDifficultyLabelRes(difficulty)
    GameId.MINESWEEPER -> minesweeperDifficultyLabelRes(difficulty)
    GameId.SUDOKU -> sudokuDifficultyLabelRes(difficulty)
    GameId.WORDSEARCH -> wordSearchDifficultyLabelRes(difficulty)
    GameId.CHIMPTEST -> chimpDifficultyLabelRes(difficulty)
}

private fun gameStreak(stats: GameStats): Int =
    Difficulty.entries.maxOf { stats.forDifficulty(it).currentStreak }

data class StatsDifficultyRow(
    val difficulty: Difficulty,
    val labelRes: Int,
    val played: Int,
    val solved: Int,
    val bestScore: Int,
    val winRate: Int,
)

data class StatsOverviewModel(
    val totalSolved: Int,
    val streak: Int,
    val winRate: Int,
    val rows: List<StatsDifficultyRow>,
)

fun buildStatsOverview(
    scope: GameId?,
    statsByGame: Map<GameId, GameStats>,
): StatsOverviewModel {
    val gameIds = scope?.let { listOf(it) } ?: GameCatalog.games.map { it.id }
    val labelGameId = scope ?: GameId.TAKUZU

    var totalPlayed = 0
    var totalSolved = 0
    val rows = Difficulty.entries.map { difficulty ->
        var played = 0
        var solved = 0
        var bestScore = 0
        for (gameId in gameIds) {
            val diff = statsByGame[gameId]?.forDifficulty(difficulty) ?: continue
            played += diff.played
            solved += diff.solved
            bestScore = maxOf(bestScore, diff.bestScore)
        }
        totalPlayed += played
        totalSolved += solved
        val winRate = if (played > 0) Math.round(solved * 100f / played) else 0
        StatsDifficultyRow(difficulty, difficultyLabelRes(labelGameId, difficulty), played, solved, bestScore, winRate)
    }

    val streak = gameIds.sumOf { gameId -> statsByGame[gameId]?.let(::gameStreak) ?: 0 }
    val overallWinRate = if (totalPlayed > 0) Math.round(totalSolved * 100f / totalPlayed) else 0

    return StatsOverviewModel(totalSolved, streak, overallWinRate, rows)
}

@Composable
fun StatsOverviewContent(overview: StatsOverviewModel, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatsSummaryStat(stringResource(R.string.stats_solved), overview.totalSolved.toString())
            VerticalDivider(Modifier.height(32.dp))
            StatsSummaryStat(stringResource(R.string.stats_streak), overview.streak.toString())
            VerticalDivider(Modifier.height(32.dp))
            StatsSummaryStat(stringResource(R.string.stats_win_rate), "${overview.winRate}%")
        }

        HorizontalDivider(Modifier.padding(top = 20.dp))

        Text(
            stringResource(R.string.stats_by_difficulty),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
        )

        Column {
            overview.rows.forEachIndexed { index, row ->
                if (index > 0) HorizontalDivider()
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(difficultyColor(row.difficulty)),
                    )
                    Column(Modifier.padding(start = 10.dp).weight(1f)) {
                        Text(stringResource(row.labelRes), style = MaterialTheme.typography.titleSmall)
                        Text(
                            "${stringResource(R.string.stats_solved)}: ${row.solved}/${row.played}  ·  ${stringResource(R.string.stats_win_rate)}: ${row.winRate}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        "${stringResource(R.string.stats_best_score)}: ${row.bestScore}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsSummaryStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
