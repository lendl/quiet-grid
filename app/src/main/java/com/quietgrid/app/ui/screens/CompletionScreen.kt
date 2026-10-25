package com.quietgrid.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.difficultyColor
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.quietgrid.app.core.formatElapsed
import com.quietgrid.app.data.RepositoriesViewModel
import com.quietgrid.app.ui.theme.LocalIsDarkTheme
import com.quietgrid.app.games.animaldoku.animalDokuDifficultyLabelRes
import com.quietgrid.app.games.arrowescape.arrowEscapeDifficultyLabelRes
import com.quietgrid.app.games.hashi.hashiDifficultyLabelRes
import com.quietgrid.app.games.links.linksDifficultyLabelRes
import com.quietgrid.app.games.blockfill.blockFillDifficultyLabelRes
import com.quietgrid.app.games.chimptest.chimpDifficultyLabelRes
import com.quietgrid.app.games.game2048.game2048DifficultyLabelRes
import com.quietgrid.app.games.guessbynumbers.guessByNumbersDifficultyLabelRes
import com.quietgrid.app.games.minesweeper.minesweeperDifficultyLabelRes
import com.quietgrid.app.games.nback.nbackDifficultyLabelRes
import com.quietgrid.app.games.nonogram.nonogramDifficultyLabelRes
import com.quietgrid.app.games.sequencememory.sequenceMemoryDifficultyLabelRes
import com.quietgrid.app.games.starbattle.starBattleDifficultyLabelRes
import com.quietgrid.app.games.whodidit.whoDidItDifficultyLabelRes
import com.quietgrid.app.games.minisudoku.miniSudokuDifficultyLabelRes
import com.quietgrid.app.games.sudoku.sudokuDifficultyLabelRes
import com.quietgrid.app.games.takuzu.takuzuDifficultyLabelRes
import com.quietgrid.app.games.themeclear.themeClearDifficultyLabelRes
import com.quietgrid.app.games.killersudoku.killerSudokuDifficultyLabelRes
import com.quietgrid.app.games.wordguess.wordGuessDifficultyLabelRes
import com.quietgrid.app.games.wordsearch.wordSearchDifficultyLabelRes
import com.quietgrid.app.ui.components.BadgePill
import com.quietgrid.app.ui.components.OutlinedGlowButton
import com.quietgrid.app.ui.components.rememberHapticController
import com.quietgrid.app.ui.seasonal.HalloweenCharacter
import com.quietgrid.app.ui.seasonal.HalloweenFigure
import com.quietgrid.app.ui.seasonal.rememberHalloweenCharacter
import java.time.LocalDate
import kotlin.math.roundToInt

private const val CHECK_SIZE_FRACTION = 0.46f

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CompletionScreen(
    gameId: GameId,
    difficulty: Difficulty,
    score: Int,
    accuracyPct: Int,
    elapsedSeconds: Int,
    bestTile: Int,
    isFirstSolve: Boolean,
    isNewHighScore: Boolean,
    isMixActive: Boolean,
    onAddToMix: () -> Unit,
    onPlayAgain: () -> Unit,
    onOtherDifficulty: () -> Unit,
    onTryAnotherGame: () -> Unit,
    dailyDate: String? = null,
    onShareDaily: () -> Unit = {},
    onBackToDaily: () -> Unit = {},
    hasNextDaily: Boolean = false,
    onPlayNextDaily: () -> Unit = {},
) {
    val haptics = rememberHapticController()
    LaunchedEffect(Unit) { haptics.correctFeedback() }
    val eyebrowRes: Int
    val titleRes: Int
    val bodyRes: Int
    when {
        isNewHighScore -> {
            eyebrowRes = R.string.completion_new_high_score_eyebrow
            titleRes = R.string.completion_new_high_score_title
            bodyRes = R.string.completion_new_high_score_body
        }
        isFirstSolve -> {
            eyebrowRes = R.string.completion_first_score_eyebrow
            titleRes = R.string.completion_first_score_title
            bodyRes = R.string.completion_first_score_body
        }
        else -> {
            eyebrowRes = R.string.completion_solved_eyebrow
            titleRes = R.string.completion_solved_title
            bodyRes = R.string.completion_solved_body
        }
    }
    val difficultyLabelRes = when (gameId) {
        GameId.TAKUZU -> takuzuDifficultyLabelRes(difficulty)
        GameId.NONOGRAM -> nonogramDifficultyLabelRes(difficulty)
        GameId.MINESWEEPER -> minesweeperDifficultyLabelRes(difficulty)
        GameId.SUDOKU -> sudokuDifficultyLabelRes(difficulty)
        GameId.WORDSEARCH -> wordSearchDifficultyLabelRes(difficulty)
        GameId.BLOCKFILL -> blockFillDifficultyLabelRes(difficulty)
        GameId.WORDGUESS -> wordGuessDifficultyLabelRes(difficulty)
        GameId.ANIMALDOKU -> animalDokuDifficultyLabelRes(difficulty)
        GameId.ARROWESCAPE -> arrowEscapeDifficultyLabelRes(difficulty)
        GameId.GAME_2048 -> game2048DifficultyLabelRes(difficulty)
        GameId.STARBATTLE -> starBattleDifficultyLabelRes(difficulty)
        GameId.WHODIDIT -> whoDidItDifficultyLabelRes(difficulty)
        GameId.GUESSBYNUMBERS -> guessByNumbersDifficultyLabelRes(difficulty)
        GameId.NBACK -> nbackDifficultyLabelRes(difficulty)
        GameId.THEMECLEAR -> themeClearDifficultyLabelRes(difficulty)
        GameId.SEQUENCEMEMORY -> sequenceMemoryDifficultyLabelRes(difficulty)
        GameId.HASHI -> hashiDifficultyLabelRes(difficulty)
        GameId.LINKS -> linksDifficultyLabelRes(difficulty)
        GameId.KILLERSUDOKU -> killerSudokuDifficultyLabelRes(difficulty)
        GameId.MINISUDOKU -> miniSudokuDifficultyLabelRes(difficulty)
        else -> chimpDifficultyLabelRes(difficulty)
    }
    val accentColor = difficultyColor(difficulty)
    val isFlawless = gameId in FLAWLESS_ELIGIBLE_GAMES && accuracyPct == 100

    val highlight = remember { CompletionExtras.consume() }
    val picture = (highlight as? CompletionHighlight.Picture)?.solution?.takeIf { it.isNotEmpty() }
    val themeIcon = (highlight as? CompletionHighlight.ThemeIcon)?.icon
    val nbackBreakdown = highlight as? CompletionHighlight.NBackBreakdown
    val caseSolvedText = (highlight as? CompletionHighlight.CaseSolved)?.text?.takeIf { it.isNotEmpty() }

    val repositories: RepositoriesViewModel = hiltViewModel()
    val halloween = rememberHalloweenCharacter()?.takeIf { picture == null }
    val bodyText = stringResource(halloween?.lineRes?.takeIf { !isNewHighScore && !isFirstSolve } ?: bodyRes)
    val stats by remember(repositories, gameId) { repositories.statsRepository.statsFor(gameId) }.collectAsState(initial = null)
    val streak = stats?.forDifficulty(difficulty)?.currentStreak ?: 0

    val gameRecords by remember(repositories, gameId) { repositories.playHistoryRepository.recordsFor(gameId) }
        .collectAsState(initial = emptyList())
    val allHistoryRecords by remember(repositories) { repositories.playHistoryRepository.allRecords() }
        .collectAsState(initial = emptyList())
    val currentWinTimestamp = remember(gameRecords) { gameRecords.maxOfOrNull { it.timestampMillis } ?: 0L }
    val gameMilestone = remember(gameRecords) { milestoneReached(gameRecords.count { it.solved }) }
    val difficultyMilestone = remember(gameRecords, difficulty) {
        milestoneReached(gameRecords.count { it.solved && it.difficulty == difficulty.key })
    }
    val totalMilestone = remember(allHistoryRecords) { milestoneReached(allHistoryRecords.count { it.solved }) }
    val didBounceBack = remember(gameRecords, difficulty, currentWinTimestamp) {
        bouncedBackFromSkid(gameRecords, difficulty, currentWinTimestamp)
    }
    val today = remember { LocalDate.now() }
    val gamesToday = remember(allHistoryRecords, today) { distinctGamesToday(allHistoryRecords, today) }
    val puzzlesToday = remember(allHistoryRecords, today) { recordsToday(allHistoryRecords, today) }

    val entrance = rememberResultEntrance()

    val links = if (dailyDate != null) {
        dailyResultLinks(hasNextDaily, onShareDaily, onBackToDaily, onTryAnotherGame)
    } else {
        buildList {
            if (!isMixActive) add(ResultLink(R.string.completion_other_difficulty, onOtherDifficulty))
            add(ResultLink(R.string.completion_try_another_game, onTryAnotherGame))
        }
    }
    val primaryAction: @Composable () -> Unit = {
        if (dailyDate != null) {
            DailyResultPrimaryButton(hasNextDaily, onPlayNextDaily, onShareDaily)
        } else {
            OutlinedGlowButton(onClick = onPlayAgain) {
                Text(
                    stringResource(mixAwarePrimaryLabel(isMixActive, R.string.completion_play_again)),
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
    val scoreDescription = "${stringResource(R.string.completion_score)} $score"

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val sideways = isSidewaysPhoneResult(maxWidth, maxHeight)
        val heroSize = if (sideways) WinHeroSize(96.dp, 72.dp, 30.sp) else WinHeroSize(130.dp, 96.dp, 40.sp)
        val heroFigure: @Composable (Boolean) -> Unit = { shared ->
            WinHeroFigure(picture, halloween, themeIcon, gameId, heroSize, entrance, shared)
        }
        val heroGlow = MaterialTheme.colorScheme.primary.takeIf { halloween == null }
        PinnedHeaderScrollLayout(
            header = {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 8.dp, end = 4.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    FlowRow(
                        Modifier
                            .weight(1f)
                            .padding(top = 8.dp)
                            .badgesFadeIn(entrance),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            Modifier
                                .border(1.dp, accentColor, CircleShape)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), CircleShape)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(Modifier.size(10.dp).background(accentColor, CircleShape))
                            Text(stringResource(eyebrowRes), style = MaterialTheme.typography.labelMedium, color = accentColor)
                        }
                        if (streak >= 2) {
                            BadgePill(
                                borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                textColor = MaterialTheme.colorScheme.primary,
                                emoji = "🔥",
                                text = stringResource(R.string.completion_streak_badge, streak),
                            )
                        }
                        if (isFlawless) {
                            BadgePill(
                                borderColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f),
                                textColor = MaterialTheme.colorScheme.tertiary,
                                emoji = "💯",
                                text = stringResource(R.string.completion_flawless_badge),
                            )
                        }
                        if (gameMilestone != null) {
                            BadgePill(
                                borderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f),
                                textColor = MaterialTheme.colorScheme.secondary,
                                emoji = "🏅",
                                text = stringResource(R.string.completion_milestone_game_badge, gameMilestone),
                            )
                        }
                        if (difficultyMilestone != null) {
                            BadgePill(
                                borderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f),
                                textColor = MaterialTheme.colorScheme.secondary,
                                emoji = "🎯",
                                text = stringResource(R.string.completion_milestone_difficulty_badge, difficultyMilestone, stringResource(difficultyLabelRes)),
                            )
                        }
                        if (totalMilestone != null) {
                            BadgePill(
                                borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                textColor = MaterialTheme.colorScheme.primary,
                                emoji = "🌟",
                                text = stringResource(R.string.completion_milestone_total_badge, totalMilestone),
                            )
                        }
                        if (didBounceBack) {
                            BadgePill(
                                borderColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f),
                                textColor = MaterialTheme.colorScheme.tertiary,
                                emoji = "💪",
                                text = stringResource(R.string.completion_bounced_back_badge),
                            )
                        }
                        if (gamesToday >= 3) {
                            BadgePill(
                                borderColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f),
                                textColor = MaterialTheme.colorScheme.secondary,
                                emoji = "🎲",
                                text = stringResource(R.string.completion_variety_badge, gamesToday),
                            )
                        }
                        if (puzzlesToday >= 5) {
                            BadgePill(
                                borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                textColor = MaterialTheme.colorScheme.primary,
                                emoji = "☀️",
                                text = stringResource(R.string.completion_daily_total_badge, puzzlesToday),
                            )
                        }
                    }
                    ResultOverflowMenu(
                        gameId = gameId,
                        difficulty = difficulty,
                        onAddToMix = onAddToMix,
                        modifier = Modifier.badgesFadeIn(entrance),
                    )
                }
            },
        ) {
            if (sideways) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    WinHero(
                        glowSize = 112.dp,
                        glowColor = heroGlow,
                        entrance = entrance,
                        modifier = Modifier.size(112.dp),
                    ) { heroFigure(true) }
                    Column(Modifier.widthIn(max = 420.dp)) {
                        Text(stringResource(titleRes), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.riseIn(entrance, 0))
                        Text(
                            bodyText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp).riseIn(entrance, 1),
                        )
                        Text(
                            stringResource(difficultyLabelRes),
                            style = MaterialTheme.typography.labelLarge,
                            color = accentColor,
                            modifier = Modifier.padding(top = 4.dp).riseIn(entrance, 2),
                        )
                    }
                }

                Row(
                    Modifier.padding(top = 18.dp).riseIn(entrance, 3),
                    horizontalArrangement = Arrangement.spacedBy(22.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CountUpScore(
                        score = score,
                        progress = { entrance.scoreProgress() },
                        modifier = Modifier.semantics { contentDescription = scoreDescription },
                    )
                    ResultMetaDivider(height = 40.dp)
                    WinMetaItems(gameId, elapsedSeconds, accuracyPct, bestTile)
                }
                Column(Modifier.riseIn(entrance, 4), horizontalAlignment = Alignment.CenterHorizontally) {
                    WinHighlights(caseSolvedText, nbackBreakdown)
                }

                CompactResultActions(links, Modifier.padding(top = 20.dp).riseIn(entrance, 5), primaryAction)
            } else {
                WinHero(
                    glowSize = 180.dp,
                    glowColor = heroGlow,
                    entrance = entrance,
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                ) { heroFigure(true) }

                Text(
                    stringResource(titleRes),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp).riseIn(entrance, 0),
                )
                Text(
                    bodyText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp).riseIn(entrance, 1),
                )
                Text(
                    stringResource(difficultyLabelRes),
                    style = MaterialTheme.typography.labelLarge,
                    color = accentColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp).riseIn(entrance, 2),
                )

                Column(Modifier.padding(top = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(R.string.completion_score),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.riseIn(entrance, 3),
                    )
                    CountUpScore(score = score, progress = { entrance.scoreProgress() }, modifier = Modifier.riseIn(entrance, 4))

                    Row(
                        Modifier.padding(top = 14.dp).riseIn(entrance, 5),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        WinMetaItems(gameId, elapsedSeconds, accuracyPct, bestTile)
                    }
                    Column(Modifier.riseIn(entrance, 6), horizontalAlignment = Alignment.CenterHorizontally) {
                        WinHighlights(caseSolvedText, nbackBreakdown)
                    }
                }

                Box(Modifier.padding(top = 24.dp).riseIn(entrance, 7)) { primaryAction() }
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp).riseIn(entrance, 8),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ResultLinkGroup(links)
                }
            }
        }
        ResultHeroFlight(entrance) { heroFigure(false) }
    }
}

private class WinHeroSize(val pictureSize: Dp, val medallionSize: Dp, val symbolSize: TextUnit)

@Composable
private fun WinHero(
    glowSize: Dp,
    glowColor: Color?,
    entrance: ResultEntrance,
    modifier: Modifier = Modifier,
    figure: @Composable () -> Unit,
) {
    Box(modifier, contentAlignment = Alignment.Center) {
        if (glowColor != null && LocalIsDarkTheme.current) {
            Box(
                Modifier
                    .size(glowSize)
                    .graphicsLayer { alpha = entrance.glowAlpha() }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(glowColor.copy(alpha = 0.35f), Color.Transparent),
                        ),
                        CircleShape,
                    ),
            )
        }
        Box(Modifier.heroSlot(entrance)) { figure() }
    }
}

@Composable
private fun WinHeroFigure(
    picture: List<List<Boolean>>?,
    halloween: HalloweenCharacter?,
    themeIcon: String?,
    gameId: GameId,
    size: WinHeroSize,
    entrance: ResultEntrance,
    shared: Boolean,
) {
    val tint = MaterialTheme.colorScheme.primary
    if (picture != null) {
        NonogramMiniPicture(solution = picture, fillColor = tint, modifier = Modifier.size(size.pictureSize))
    } else if (halloween != null) {
        HalloweenFigure(halloween, size.pictureSize, clockMs = { entrance.elapsedMs() })
    } else {
        ResultMedallion(gameId = gameId, tint = tint, size = size.medallionSize, shared = shared) {
            if (themeIcon != null) {
                MedallionSymbol(themeIcon, size.symbolSize)
            } else {
                DrawnCheck(
                    progress = { entrance.checkProgress() },
                    tint = tint,
                    modifier = Modifier.size(size.medallionSize * CHECK_SIZE_FRACTION),
                )
            }
        }
    }
}

@Composable
private fun WinMetaItems(gameId: GameId, elapsedSeconds: Int, accuracyPct: Int, bestTile: Int) {
    MetaItem(stringResource(R.string.completion_elapsed_time), formatElapsed(elapsedSeconds))
    if (gameId in ACCURACY_TRACKED_GAMES) {
        ResultMetaDivider()
        MetaItem(stringResource(R.string.completion_accuracy), "$accuracyPct%")
    }
    if (gameId in BEST_TILE_TRACKED_GAMES) {
        ResultMetaDivider()
        MetaItem(stringResource(R.string.completion_best_tile), bestTile.toString())
    }
}

@Composable
private fun WinHighlights(caseSolvedText: String?, nbackBreakdown: CompletionHighlight.NBackBreakdown?) {
    if (caseSolvedText != null) {
        Column(Modifier.padding(top = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(R.string.whodidit_completion_case_label),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                caseSolvedText,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
    if (nbackBreakdown != null) {
        Row(
            Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MetaItem(stringResource(R.string.nback_completion_hits), nbackBreakdown.hits.toString())
            MetaItem(stringResource(R.string.nback_completion_misses), nbackBreakdown.misses.toString())
            MetaItem(stringResource(R.string.nback_completion_false_positives), nbackBreakdown.falsePositives.toString())
            MetaItem(stringResource(R.string.nback_completion_avg_reaction_time), "${nbackBreakdown.avgReactionTimeMs}ms")
        }
    }
}


private val FLAWLESS_ELIGIBLE_GAMES = setOf(GameId.TAKUZU, GameId.SUDOKU, GameId.KILLERSUDOKU, GameId.MINISUDOKU, GameId.LINKS)
private val ACCURACY_TRACKED_GAMES = setOf(GameId.TAKUZU, GameId.SUDOKU, GameId.KILLERSUDOKU, GameId.MINISUDOKU, GameId.WORDSEARCH, GameId.NBACK, GameId.LINKS)
private val BEST_TILE_TRACKED_GAMES = setOf(GameId.GAME_2048)

@Composable
private fun NonogramMiniPicture(solution: List<List<Boolean>>, fillColor: Color, modifier: Modifier = Modifier) {
    val rows = solution.size
    val cols = solution.firstOrNull()?.size ?: 0
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier) {
        if (rows == 0 || cols == 0) return@Canvas
        val cellSize = minOf(size.width / cols, size.height / rows)
        val gridWidth = cellSize * cols
        val gridHeight = cellSize * rows
        val offsetX = (size.width - gridWidth) / 2f
        val offsetY = (size.height - gridHeight) / 2f
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val topLeft = Offset(offsetX + col * cellSize, offsetY + row * cellSize)
                val cellSizePx = Size(cellSize, cellSize)
                drawRect(
                    color = if (solution[row][col]) fillColor else outlineColor.copy(alpha = 0.25f),
                    topLeft = topLeft,
                    size = cellSizePx,
                )
            }
        }
    }
}

@Composable
private fun CountUpScore(score: Int, progress: () -> Float, modifier: Modifier = Modifier) {
    Text(
        (score * progress()).roundToInt().toString(),
        style = MaterialTheme.typography.displayMedium,
        fontWeight = FontWeight.Black,
        modifier = modifier.padding(top = 2.dp),
    )
}

@Composable
private fun MetaItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
internal fun PinnedHeaderScrollLayout(
    header: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    var headerHeightPx by remember { mutableIntStateOf(0) }
    val headerHeight = with(LocalDensity.current) { headerHeightPx.toDp() }
    Box(modifier.fillMaxSize()) {
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .onSizeChanged { headerHeightPx = it.height },
        ) {
            header()
        }
        Box(
            Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(top = headerHeight),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                contentModifier
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = headerHeight),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = content,
            )
        }
    }
}
