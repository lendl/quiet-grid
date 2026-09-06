package com.quietgrid.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.difficultyColor
import com.quietgrid.app.core.formatElapsed
import com.quietgrid.app.ui.screens.ChallengerExtras
import kotlin.math.roundToInt

data class ChallengerResultStrings(
    val titleRes: Int,
    val bodyRes: Int,
    val puzzlesSolvedLabelRes: Int,
    val scoreLabelRes: Int,
    val fastestSolveLabelRes: Int,
    val newHighScoreBadgeRes: Int,
    val offBestRes: Int,
    val playAgainRes: Int,
    val backToPuzzlesRes: Int,
    val tryAnotherGameRes: Int,
)

@Composable
fun ChallengerResultScreen(
    strings: ChallengerResultStrings,
    difficultyLabelRes: (Difficulty) -> Int,
    puzzlesSolved: Int,
    tierReached: Difficulty,
    score: Int,
    isNewHighScore: Boolean,
    previousBest: Int,
    fastestSolveSeconds: Double?,
    reason: String,
    startingLives: Int,
    solvesPerTier: Int,
    onPlayAgain: () -> Unit,
    onBackToPuzzles: () -> Unit,
    onTryAnotherGame: () -> Unit,
) {
    val context = LocalContext.current
    val reduceMotion = remember { systemAnimationsDisabled(context) }
    val runDetails = remember { ChallengerExtras.consume() }
    val puzzleHistory = runDetails.puzzleHistory

    val pageOpacity = remember { Animatable(if (reduceMotion) 1f else 0f) }
    val contentOffsetY = remember { Animatable(if (reduceMotion) 0f else 24f) }
    val badgeOpacity = remember { Animatable(if (reduceMotion) 1f else 0f) }
    var showConfetti by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (reduceMotion) {
            showConfetti = isNewHighScore
        } else {
            pageOpacity.animateTo(1f, animationSpec = tween(220))
            showConfetti = isNewHighScore
        }
    }
    LaunchedEffect(Unit) {
        if (!reduceMotion) {
            contentOffsetY.animateTo(0f, animationSpec = tween(320, easing = FastOutSlowInEasing))
        }
    }
    LaunchedEffect(Unit) {
        if (isNewHighScore) {
            if (reduceMotion) {
                badgeOpacity.snapTo(1f)
            } else {
                badgeOpacity.animateTo(1f, animationSpec = tween(250))
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (isNewHighScore) {
            Row(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .graphicsLayer { alpha = badgeOpacity.value },
                horizontalArrangement = Arrangement.End,
            ) {
                BadgePill(
                    emoji = "🏆",
                    text = stringResource(strings.newHighScoreBadgeRes),
                    borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                    textColor = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .graphicsLayer {
                        alpha = pageOpacity.value
                        translationY = contentOffsetY.value
                    }
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(strings.titleRes), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Text(
                    stringResource(strings.bodyRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp),
                )

                if (!isNewHighScore && previousBest > 0) {
                    Text(
                        stringResource(strings.offBestRes, previousBest - score),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                if (reason == "lives_exhausted") {
                    Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(startingLives) {
                            ChallengerHeartIcon(filled = false)
                        }
                    }
                }

                ChallengerTierProgressBar(
                    tierReached = tierReached,
                    solvesInTier = runDetails.solvesInTier,
                    solvesPerTier = solvesPerTier,
                    difficultyLabelRes = difficultyLabelRes,
                    reduceMotion = reduceMotion,
                    modifier = Modifier.padding(top = 20.dp),
                )

                Row(
                    Modifier.padding(top = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    ResultMetaItem(stringResource(strings.puzzlesSolvedLabelRes), puzzlesSolved.toString())
                    ResultMetaItem(stringResource(strings.scoreLabelRes), score.toString())
                    if (fastestSolveSeconds != null) {
                        ResultMetaItem(stringResource(strings.fastestSolveLabelRes), formatElapsed(fastestSolveSeconds.roundToInt()))
                    }
                }

                if (puzzleHistory.isNotEmpty()) {
                    Text(
                        stringResource(R.string.challenger_result_puzzle_breakdown_heading),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(top = 20.dp),
                    )
                    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        puzzleHistory.forEach { entry ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                                    DifficultyPill(
                                        difficulty = entry.difficulty,
                                        label = stringResource(difficultyLabelRes(entry.difficulty)),
                                    )
                                }
                                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                    Text(
                                        formatElapsed(entry.elapsedSeconds.roundToInt()),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }

                Button(onClick = onPlayAgain, modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
                    Text(stringResource(strings.playAgainRes))
                }

                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    TextButton(onClick = onBackToPuzzles) {
                        Text(stringResource(strings.backToPuzzlesRes), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = onTryAnotherGame) {
                        Text(stringResource(strings.tryAnotherGameRes), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        if (showConfetti) {
            ConfettiBurst(Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun ChallengerTierProgressBar(
    tierReached: Difficulty,
    solvesInTier: Int,
    solvesPerTier: Int,
    difficultyLabelRes: (Difficulty) -> Int,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val fills = remember(tierReached, solvesInTier, solvesPerTier) {
        Difficulty.entries.map { tier ->
            val target = challengerTierFillFraction(tier.ordinal, tierReached, solvesInTier, solvesPerTier)
            Animatable(if (reduceMotion) target else 0f)
        }
    }
    LaunchedEffect(tierReached, solvesInTier, solvesPerTier) {
        if (!reduceMotion) {
            Difficulty.entries.forEach { tier ->
                val target = challengerTierFillFraction(tier.ordinal, tierReached, solvesInTier, solvesPerTier)
                fills[tier.ordinal].animateTo(target, animationSpec = tween(500, easing = FastOutSlowInEasing))
            }
        }
    }

    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Difficulty.entries.forEach { tier ->
                val color = if (tier.ordinal <= tierReached.ordinal) difficultyColor(tier) else MaterialTheme.colorScheme.outlineVariant
                Box(
                    Modifier
                        .weight(1f)
                        .height(10.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(5.dp)),
                ) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fills[tier.ordinal].value)
                            .background(color, RoundedCornerShape(5.dp)),
                    )
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Difficulty.entries.forEach { tier ->
                val reached = tier.ordinal <= tierReached.ordinal
                Text(
                    stringResource(difficultyLabelRes(tier)),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (reached) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (reached) FontWeight.SemiBold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DifficultyPill(difficulty: Difficulty, label: String) {
    val color = difficultyColor(difficulty)
    Box(
        Modifier
            .background(color.copy(alpha = 0.18f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 3.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ResultMetaItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
    }
}
