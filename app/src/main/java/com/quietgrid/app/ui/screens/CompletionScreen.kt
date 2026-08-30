package com.quietgrid.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.difficultyColor
import androidx.hilt.navigation.compose.hiltViewModel
import com.quietgrid.app.core.formatElapsed
import com.quietgrid.app.data.RepositoriesViewModel
import com.quietgrid.app.games.animaldoku.animalDokuDifficultyLabelRes
import com.quietgrid.app.games.arrowescape.arrowEscapeDifficultyLabelRes
import com.quietgrid.app.games.blockfill.blockFillDifficultyLabelRes
import com.quietgrid.app.games.chimptest.chimpDifficultyLabelRes
import com.quietgrid.app.games.minesweeper.minesweeperDifficultyLabelRes
import com.quietgrid.app.games.nonogram.nonogramDifficultyLabelRes
import com.quietgrid.app.games.sudoku.sudokuDifficultyLabelRes
import com.quietgrid.app.games.takuzu.takuzuDifficultyLabelRes
import com.quietgrid.app.games.wordguess.wordGuessDifficultyLabelRes
import com.quietgrid.app.games.wordsearch.wordSearchDifficultyLabelRes
import com.quietgrid.app.ui.components.ConfettiBurst
import com.quietgrid.app.ui.components.rememberHapticController
import kotlinx.coroutines.delay
import java.time.LocalDate
import kotlin.math.roundToInt

private val CELEBRATION_ICONS = listOf("🎉", "🏆", "⭐", "✨", "🎈", "💜")

private fun pickCelebrationIcon(score: Int, accuracyPct: Int, variantSeed: Int): String {
    val seed = maxOf(0, score) + accuracyPct + variantSeed
    return CELEBRATION_ICONS[seed % CELEBRATION_ICONS.size]
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CompletionScreen(
    gameId: GameId,
    difficulty: Difficulty,
    score: Int,
    accuracyPct: Int,
    elapsedSeconds: Int,
    isFirstSolve: Boolean,
    isNewHighScore: Boolean,
    onPlayAgain: () -> Unit,
    onOtherDifficulty: () -> Unit,
    onTryAnotherGame: () -> Unit,
) {
    val haptics = rememberHapticController()
    LaunchedEffect(Unit) { haptics.correctFeedback() }
    val eyebrowRes: Int
    val titleRes: Int
    val bodyRes: Int
    val variantSeed: Int
    when {
        isNewHighScore -> {
            eyebrowRes = R.string.completion_new_high_score_eyebrow
            titleRes = R.string.completion_new_high_score_title
            bodyRes = R.string.completion_new_high_score_body
            variantSeed = 14
        }
        isFirstSolve -> {
            eyebrowRes = R.string.completion_first_score_eyebrow
            titleRes = R.string.completion_first_score_title
            bodyRes = R.string.completion_first_score_body
            variantSeed = 11
        }
        else -> {
            eyebrowRes = R.string.completion_solved_eyebrow
            titleRes = R.string.completion_solved_title
            bodyRes = R.string.completion_solved_body
            variantSeed = 6
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
        else -> chimpDifficultyLabelRes(difficulty)
    }
    val accentColor = difficultyColor(difficulty)
    val icon = pickCelebrationIcon(score, accuracyPct, variantSeed)
    val isFlawless = gameId in FLAWLESS_ELIGIBLE_GAMES && accuracyPct == 100

    val highlight = remember { CompletionExtras.consume() }
    val picture = (highlight as? CompletionHighlight.Picture)?.solution?.takeIf { it.isNotEmpty() }
    val themeIcon = (highlight as? CompletionHighlight.ThemeIcon)?.icon

    val repositories: RepositoriesViewModel = hiltViewModel()
    val stats by repositories.statsRepository.statsFor(gameId).collectAsState(initial = null)
    val streak = stats?.forDifficulty(difficulty)?.currentStreak ?: 0

    val gameRecords by repositories.playHistoryRepository.recordsFor(gameId).collectAsState(initial = emptyList())
    val allHistoryRecords by repositories.playHistoryRepository.allRecords().collectAsState(initial = emptyList())
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

    val pageOpacity = remember { Animatable(0f) }
    val contentOffsetY = remember { Animatable(24f) }
    val pictureScale = remember { Animatable(0.6f) }
    val scoreProgress = remember { Animatable(0f) }
    var showConfetti by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        pageOpacity.animateTo(1f, animationSpec = tween(220))
        showConfetti = true
    }
    LaunchedEffect(Unit) {
        contentOffsetY.animateTo(0f, animationSpec = tween(320, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        pictureScale.animateTo(
            1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        )
    }
    LaunchedEffect(score) {
        delay(200)
        scoreProgress.animateTo(1f, animationSpec = tween(650, easing = FastOutSlowInEasing))
    }

    Box(Modifier.fillMaxSize()) {
        FlowRow(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(16.dp)
                .graphicsLayer { alpha = pageOpacity.value },
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

        Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Column(
                    Modifier.graphicsLayer {
                        alpha = pageOpacity.value
                        translationY = contentOffsetY.value
                    },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                        if (picture != null) {
                            NonogramMiniPicture(
                                solution = picture,
                                fillColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(130.dp)
                                    .graphicsLayer { scaleX = pictureScale.value; scaleY = pictureScale.value },
                            )
                        } else {
                            Box(
                                Modifier
                                    .size(96.dp)
                                    .graphicsLayer { scaleX = pictureScale.value; scaleY = pictureScale.value }
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(themeIcon ?: icon, fontSize = 40.sp)
                            }
                        }
                    }

                    Text(stringResource(titleRes), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                    Text(
                        stringResource(bodyRes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    Text(
                        stringResource(difficultyLabelRes),
                        style = MaterialTheme.typography.labelLarge,
                        color = accentColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp),
                    )

                    Column(Modifier.padding(top = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            stringResource(R.string.completion_score),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            (score * scoreProgress.value).roundToInt().toString(),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(top = 2.dp),
                        )

                        Row(
                            Modifier.padding(top = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            MetaItem(stringResource(R.string.completion_elapsed_time), formatElapsed(elapsedSeconds))
                            if (gameId in ACCURACY_TRACKED_GAMES) {
                                Box(Modifier.width(1.dp).height(28.dp).background(MaterialTheme.colorScheme.outlineVariant))
                                MetaItem(stringResource(R.string.completion_accuracy), "$accuracyPct%")
                            }
                        }
                    }

                    Button(onClick = onPlayAgain, modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
                        Text(stringResource(R.string.completion_play_again))
                    }

                    Row(
                        Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = onOtherDifficulty) {
                            Text(stringResource(R.string.completion_other_difficulty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Box(Modifier.width(1.dp).height(16.dp).background(MaterialTheme.colorScheme.outlineVariant))
                        TextButton(onClick = onTryAnotherGame) {
                            Text(stringResource(R.string.completion_try_another_game), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        if (showConfetti) {
            ConfettiBurst(Modifier.fillMaxSize())
        }
    }
}


private val FLAWLESS_ELIGIBLE_GAMES = setOf(GameId.TAKUZU, GameId.SUDOKU)
private val ACCURACY_TRACKED_GAMES = setOf(GameId.TAKUZU, GameId.SUDOKU, GameId.WORDSEARCH)

@Composable
private fun BadgePill(emoji: String, text: String, borderColor: Color, textColor: Color, modifier: Modifier = Modifier) {
    Row(
        modifier
            .border(1.dp, borderColor, CircleShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(emoji, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        Text(text, style = MaterialTheme.typography.labelMedium, color = textColor, maxLines = 1)
    }
}

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
private fun MetaItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
    }
}
