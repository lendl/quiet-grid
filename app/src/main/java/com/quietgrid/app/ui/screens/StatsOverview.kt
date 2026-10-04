package com.quietgrid.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameCatalog
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.difficultyColor
import com.quietgrid.app.data.GameStats
import com.quietgrid.app.ui.components.StatGroup
import com.quietgrid.app.ui.components.StatItem
import com.quietgrid.app.games.animaldoku.animalDokuDifficultyLabelRes
import com.quietgrid.app.games.arrowescape.arrowEscapeDifficultyLabelRes
import com.quietgrid.app.games.blockfill.blockFillDifficultyLabelRes
import com.quietgrid.app.games.chimptest.chimpDifficultyLabelRes
import com.quietgrid.app.games.game2048.game2048DifficultyLabelRes
import com.quietgrid.app.games.guessbynumbers.guessByNumbersDifficultyLabelRes
import com.quietgrid.app.games.minesweeper.minesweeperDifficultyLabelRes
import com.quietgrid.app.games.nback.nbackDifficultyLabelRes
import com.quietgrid.app.games.nonogram.nonogramDifficultyLabelRes
import com.quietgrid.app.games.starbattle.starBattleDifficultyLabelRes
import com.quietgrid.app.games.sudoku.sudokuDifficultyLabelRes
import com.quietgrid.app.games.takuzu.takuzuDifficultyLabelRes
import com.quietgrid.app.games.themeclear.themeClearDifficultyLabelRes
import com.quietgrid.app.games.wordguess.wordGuessDifficultyLabelRes
import com.quietgrid.app.games.wordsearch.wordSearchDifficultyLabelRes

fun difficultyLabelRes(gameId: GameId, difficulty: Difficulty): Int = when (gameId) {
    GameId.TAKUZU -> takuzuDifficultyLabelRes(difficulty)
    GameId.NONOGRAM -> nonogramDifficultyLabelRes(difficulty)
    GameId.MINESWEEPER -> minesweeperDifficultyLabelRes(difficulty)
    GameId.SUDOKU -> sudokuDifficultyLabelRes(difficulty)
    GameId.WORDSEARCH -> wordSearchDifficultyLabelRes(difficulty)
    GameId.CHIMPTEST -> chimpDifficultyLabelRes(difficulty)
    GameId.BLOCKFILL -> blockFillDifficultyLabelRes(difficulty)
    GameId.WORDGUESS -> wordGuessDifficultyLabelRes(difficulty)
    GameId.ANIMALDOKU -> animalDokuDifficultyLabelRes(difficulty)
    GameId.ARROWESCAPE -> arrowEscapeDifficultyLabelRes(difficulty)
    GameId.GAME_2048 -> game2048DifficultyLabelRes(difficulty)
    GameId.STARBATTLE -> starBattleDifficultyLabelRes(difficulty)
    GameId.GUESSBYNUMBERS -> guessByNumbersDifficultyLabelRes(difficulty)
    GameId.NBACK -> nbackDifficultyLabelRes(difficulty)
    GameId.THEMECLEAR -> themeClearDifficultyLabelRes(difficulty)
}

internal const val STATS_DIFFICULTY_TABLE_TAG = "stats-difficulty-table"

fun bestScoreCellText(bestScore: Int): String = if (bestScore == 0) "-" else bestScore.toString()

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
    val totalPlayed: Int,
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

    return StatsOverviewModel(totalSolved, totalPlayed, streak, overallWinRate, rows)
}

@Composable
fun StatsOverviewContent(overview: StatsOverviewModel, modifier: Modifier = Modifier) {
    Column(modifier) {
        StatGroup(
            listOf(
                StatItem(
                    stringResource(R.string.stats_solved),
                    overview.totalSolved.toString(),
                    stringResource(R.string.stat_desc_played, overview.totalPlayed),
                ),
                StatItem(
                    stringResource(R.string.stats_streak),
                    overview.streak.toString(),
                    stringResource(R.string.stat_desc_wins_in_row),
                ),
                StatItem(
                    stringResource(R.string.stats_win_rate),
                    "${overview.winRate}%",
                    stringResource(R.string.stat_desc_all_difficulties),
                ),
            ),
        )

        HorizontalDivider(Modifier.padding(top = 20.dp))

        Text(
            stringResource(R.string.stats_by_difficulty),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
        )

        StatsDifficultyTable(overview.rows)
    }
}

private val LABEL_DOT_SPACE = 20.dp
private val NUMBER_COLUMN_GAP = 8.dp
private const val NUMBER_COLUMN_COUNT = 3

fun statsTableTextScale(availableWidth: Float, requiredWidthAt: (Float) -> Float): Float {
    var scale = (availableWidth / requiredWidthAt(1f)).coerceAtMost(1f)
    while (scale > MIN_TABLE_TEXT_SCALE && requiredWidthAt(scale) > availableWidth) {
        scale *= TABLE_TEXT_SCALE_STEP
    }
    return scale.coerceAtLeast(MIN_TABLE_TEXT_SCALE)
}

private const val MIN_TABLE_TEXT_SCALE = 0.5f
private const val TABLE_TEXT_SCALE_STEP = 0.97f

private fun TextUnit.scaledBy(scale: Float): TextUnit = if (isSpecified) this * scale else this

private fun TextStyle.scaledBy(scale: Float): TextStyle =
    if (scale >= 1f) {
        this
    } else {
        copy(
            fontSize = fontSize.scaledBy(scale),
            lineHeight = lineHeight.scaledBy(scale),
            letterSpacing = letterSpacing.scaledBy(scale),
        )
    }

@Composable
private fun StatsDifficultyTable(rows: List<StatsDifficultyRow>) {
    val solvedHeader = stringResource(R.string.stats_solved)
    val winRateHeader = stringResource(R.string.stats_win_rate)
    val bestScoreHeader = stringResource(R.string.stats_best_score)
    val headers = listOf(solvedHeader, winRateHeader, bestScoreHeader)
    val labels = rows.map { stringResource(it.labelRes) }
    val cells = rows.map { listOf("${it.solved}/${it.played}", "${it.winRate}%", bestScoreCellText(it.bestScore)) }
    val baseLabelStyle = MaterialTheme.typography.bodyLarge
    val baseHeaderStyle = MaterialTheme.typography.labelSmall
    val baseValueStyle = MaterialTheme.typography.bodyMedium
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    BoxWithConstraints(Modifier.fillMaxWidth().testTag(STATS_DIFFICULTY_TABLE_TAG)) {
        val availableWidthPx = with(density) { (maxWidth - LABEL_DOT_SPACE).toPx() }
        val (labelWidthPx, scale) = remember(labels, cells, headers, baseLabelStyle, baseHeaderStyle, baseValueStyle, availableWidthPx, density) {
            fun widthOf(text: String, style: TextStyle) =
                textMeasurer.measure(text, style, softWrap = false, maxLines = 1).size.width.toFloat()
            val gapPx = with(density) { NUMBER_COLUMN_GAP.toPx() }
            val headerWords = headers.flatMap { it.split(' ') }
            fun labelWidthAt(scale: Float) = labels.maxOfOrNull { widthOf(it, baseLabelStyle.scaledBy(scale)) } ?: 0f
            fun numberColumnWidthAt(scale: Float) = maxOf(
                headerWords.maxOfOrNull { widthOf(it, baseHeaderStyle.scaledBy(scale)) } ?: 0f,
                cells.flatten().maxOfOrNull { widthOf(it, baseValueStyle.scaledBy(scale)) } ?: 0f,
            ) + gapPx
            val fittedScale = statsTableTextScale(availableWidthPx) { scale ->
                labelWidthAt(scale) + numberColumnWidthAt(scale) * NUMBER_COLUMN_COUNT
            }
            labelWidthAt(fittedScale) to fittedScale
        }
        val labelColumnWidth = with(density) { labelWidthPx.toDp() } + LABEL_DOT_SPACE
        val labelStyle = baseLabelStyle.scaledBy(scale)
        val headerStyle = baseHeaderStyle.scaledBy(scale)
        val valueStyle = baseValueStyle.scaledBy(scale)

        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) { hideFromAccessibility() }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Spacer(Modifier.width(labelColumnWidth))
                headers.forEach { StatsTableHeaderCell(it, headerStyle) }
            }
            rows.forEachIndexed { index, row ->
                val label = labels[index]
                val (solvedText, winRateText, bestScoreText) = cells[index]
                HorizontalDivider()
                Row(
                    Modifier
                        .fillMaxWidth()
                        .semantics(mergeDescendants = true) {
                            contentDescription = "$label, $solvedHeader: $solvedText, $winRateHeader: $winRateText, $bestScoreHeader: $bestScoreText"
                        }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(Modifier.width(labelColumnWidth), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(difficultyColor(row.difficulty)),
                        )
                        Text(label, style = labelStyle, modifier = Modifier.padding(start = 10.dp))
                    }
                    cells[index].forEach { StatsTableValueCell(it, valueStyle) }
                }
            }
        }
    }
}

@Composable
private fun RowScope.StatsTableHeaderCell(text: String, style: TextStyle) {
    Text(
        text,
        style = style,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.End,
        modifier = Modifier.weight(1f),
    )
}

@Composable
private fun RowScope.StatsTableValueCell(text: String, style: TextStyle) {
    Text(
        text,
        style = style,
        textAlign = TextAlign.End,
        modifier = Modifier.weight(1f),
    )
}
