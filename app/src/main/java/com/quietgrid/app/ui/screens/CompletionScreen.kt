package com.quietgrid.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.quietgrid.app.core.formatElapsed
import com.quietgrid.app.data.AppContainer
import com.quietgrid.app.games.chimptest.chimpDifficultyLabelRes
import com.quietgrid.app.games.minesweeper.minesweeperDifficultyLabelRes
import com.quietgrid.app.games.nonogram.nonogramDifficultyLabelRes
import com.quietgrid.app.games.sudoku.sudokuDifficultyLabelRes
import com.quietgrid.app.games.takuzu.takuzuDifficultyLabelRes
import com.quietgrid.app.games.wordsearch.wordSearchDifficultyLabelRes
import com.quietgrid.app.ui.components.ConfettiBurst

private val CELEBRATION_ICONS = listOf("🎉", "🏆", "⭐", "✨", "🎈", "💜")

private fun pickCelebrationIcon(score: Int, accuracyPct: Int, variantSeed: Int): String {
    val seed = maxOf(0, score) + accuracyPct + variantSeed
    return CELEBRATION_ICONS[seed % CELEBRATION_ICONS.size]
}

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
    onTryAnotherGame: () -> Unit,
    onViewStats: () -> Unit,
) {
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
        else -> chimpDifficultyLabelRes(difficulty)
    }
    val accentColor = difficultyColor(difficulty)
    val icon = pickCelebrationIcon(score, accuracyPct, variantSeed)

    val stats by AppContainer.statsRepository.statsFor(gameId).collectAsState(initial = null)
    val streak = stats?.forDifficulty(difficulty)?.currentStreak ?: 0

    val pageOpacity = remember { Animatable(0f) }
    val contentOffsetY = remember { Animatable(24f) }
    var showConfetti by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        pageOpacity.animateTo(1f, animationSpec = tween(220))
        showConfetti = true
    }
    LaunchedEffect(Unit) {
        contentOffsetY.animateTo(0f, animationSpec = tween(320, easing = FastOutSlowInEasing))
    }

    val infinite = rememberInfiniteTransition(label = "completionAccents")
    val iconFloat by infinite.animateFloat(
        initialValue = 0f,
        targetValue = -14f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "iconFloat",
    )
    val accentPulse by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse),
        label = "accentPulse",
    )

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp)
                .graphicsLayer {
                    alpha = pageOpacity.value
                    translationY = contentOffsetY.value
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
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
                    Row(
                        Modifier
                            .padding(start = 8.dp)
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), CircleShape)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), CircleShape)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("🔥", style = MaterialTheme.typography.labelMedium)
                        Text(
                            stringResource(R.string.completion_streak_badge, streak),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(170.dp)
                        .graphicsLayer { alpha = 0.12f + accentPulse * 0.2f }
                        .border(1.dp, accentColor.copy(alpha = 0.4f), CircleShape),
                )
                Text(
                    "✦",
                    color = accentColor,
                    fontSize = 18.sp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 24.dp)
                        .graphicsLayer { alpha = 0.3f + accentPulse * 0.5f },
                )
                Text(
                    "✦",
                    color = MaterialTheme.colorScheme.tertiary,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(bottom = 16.dp, start = 20.dp)
                        .graphicsLayer { alpha = 0.3f + (1f - accentPulse) * 0.5f },
                )
                Box(
                    Modifier
                        .size(96.dp)
                        .graphicsLayer { translationY = iconFloat }
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(icon, fontSize = 40.sp)
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
                    score.toString(),
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
                    Box(Modifier.width(1.dp).height(28.dp).background(MaterialTheme.colorScheme.outlineVariant))
                    MetaItem(stringResource(R.string.completion_accuracy), "$accuracyPct%")
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
                TextButton(onClick = onTryAnotherGame) {
                    Text(stringResource(R.string.completion_try_another_game), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box(Modifier.width(1.dp).height(16.dp).background(MaterialTheme.colorScheme.outlineVariant))
                TextButton(onClick = onViewStats) {
                    Text(stringResource(R.string.completion_view_stats), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (showConfetti) {
            ConfettiBurst(Modifier.fillMaxSize())
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
