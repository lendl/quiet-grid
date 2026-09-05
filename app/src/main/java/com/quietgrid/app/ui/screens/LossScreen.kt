package com.quietgrid.app.ui.screens

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.quietgrid.app.core.formatElapsed
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
import com.quietgrid.app.nav.LocalAnimatedVisibilityScope
import com.quietgrid.app.nav.LocalSharedTransitionScope
import com.quietgrid.app.ui.components.SharedElementKeys
import com.quietgrid.app.ui.components.rememberHapticController

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun LossScreen(
    gameId: GameId,
    difficulty: Difficulty,
    elapsedSeconds: Int,
    reason: String,
    onRetry: () -> Unit,
    onOtherDifficulty: () -> Unit,
    onTryAnotherGame: () -> Unit,
    onWalkThroughSolve: () -> Unit,
) {
    val haptics = rememberHapticController()
    LaunchedEffect(Unit) { if (reason != "abandoned") haptics.incorrectFeedback() }
    val eyebrowRes: Int
    val titleRes: Int
    val bodyRes: Int
    when {
        gameId == GameId.TAKUZU -> {
            eyebrowRes = R.string.takuzu_loss_abandoned_eyebrow
            titleRes = R.string.takuzu_loss_abandoned_title
            bodyRes = R.string.takuzu_loss_abandoned_body
        }
        gameId == GameId.NONOGRAM -> {
            eyebrowRes = R.string.nonogram_loss_abandoned_eyebrow
            titleRes = R.string.nonogram_loss_abandoned_title
            bodyRes = R.string.nonogram_loss_abandoned_body
        }
        gameId == GameId.MINESWEEPER && reason == "abandoned" -> {
            eyebrowRes = R.string.minesweeper_loss_abandoned_eyebrow
            titleRes = R.string.minesweeper_loss_abandoned_title
            bodyRes = R.string.minesweeper_loss_abandoned_body
        }
        gameId == GameId.MINESWEEPER -> {
            eyebrowRes = R.string.minesweeper_loss_rule_failure_eyebrow
            titleRes = R.string.minesweeper_loss_rule_failure_title
            bodyRes = R.string.minesweeper_loss_rule_failure_body
        }
        gameId == GameId.SUDOKU -> {
            eyebrowRes = R.string.sudoku_loss_abandoned_eyebrow
            titleRes = R.string.sudoku_loss_abandoned_title
            bodyRes = R.string.sudoku_loss_abandoned_body
        }
        gameId == GameId.WORDSEARCH -> {
            eyebrowRes = R.string.wordsearch_loss_abandoned_eyebrow
            titleRes = R.string.wordsearch_loss_abandoned_title
            bodyRes = R.string.wordsearch_loss_abandoned_body
        }
        gameId == GameId.BLOCKFILL && reason == "abandoned" -> {
            eyebrowRes = R.string.blockfill_loss_abandoned_eyebrow
            titleRes = R.string.blockfill_loss_abandoned_title
            bodyRes = R.string.blockfill_loss_abandoned_body
        }
        gameId == GameId.BLOCKFILL -> {
            eyebrowRes = R.string.blockfill_loss_rule_failure_eyebrow
            titleRes = R.string.blockfill_loss_rule_failure_title
            bodyRes = R.string.blockfill_loss_rule_failure_body
        }
        gameId == GameId.WORDGUESS && reason == "abandoned" -> {
            eyebrowRes = R.string.wordguess_loss_abandoned_eyebrow
            titleRes = R.string.wordguess_loss_abandoned_title
            bodyRes = R.string.wordguess_loss_abandoned_body
        }
        gameId == GameId.WORDGUESS -> {
            eyebrowRes = R.string.wordguess_loss_rule_failure_eyebrow
            titleRes = R.string.wordguess_loss_rule_failure_title
            bodyRes = R.string.wordguess_loss_rule_failure_body
        }
        gameId == GameId.ANIMALDOKU && reason == "abandoned" -> {
            eyebrowRes = R.string.animaldoku_loss_abandoned_eyebrow
            titleRes = R.string.animaldoku_loss_abandoned_title
            bodyRes = R.string.animaldoku_loss_abandoned_body
        }
        gameId == GameId.ANIMALDOKU -> {
            eyebrowRes = R.string.animaldoku_loss_rule_failure_eyebrow
            titleRes = R.string.animaldoku_loss_rule_failure_title
            bodyRes = R.string.animaldoku_loss_rule_failure_body
        }
        gameId == GameId.ARROWESCAPE && reason == "abandoned" -> {
            eyebrowRes = R.string.arrowescape_loss_abandoned_eyebrow
            titleRes = R.string.arrowescape_loss_abandoned_title
            bodyRes = R.string.arrowescape_loss_abandoned_body
        }
        gameId == GameId.ARROWESCAPE -> {
            eyebrowRes = R.string.arrowescape_loss_rule_failure_eyebrow
            titleRes = R.string.arrowescape_loss_rule_failure_title
            bodyRes = R.string.arrowescape_loss_rule_failure_body
        }
        reason == "abandoned" -> {
            eyebrowRes = R.string.chimp_loss_abandoned_eyebrow
            titleRes = R.string.chimp_loss_abandoned_title
            bodyRes = R.string.chimp_loss_abandoned_body
        }
        else -> {
            eyebrowRes = R.string.chimp_loss_rule_failure_eyebrow
            titleRes = R.string.chimp_loss_rule_failure_title
            bodyRes = R.string.chimp_loss_rule_failure_body
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
    val icon = if (reason == "abandoned") "⏸" else "💥"
    val errorColor = MaterialTheme.colorScheme.error

    val highlight = remember { CompletionExtras.consume() }
    val revealWord = (highlight as? CompletionHighlight.RevealWord)?.word?.takeIf { it.isNotEmpty() }
    val analyzerSnapshot = rememberSaveable { mutableStateOf(AnalyzerHandoff.consume()) }.value

    val pageOpacity = remember { Animatable(0f) }
    val contentOffsetY = remember { Animatable(24f) }
    LaunchedEffect(Unit) {
        pageOpacity.animateTo(1f, animationSpec = tween(220))
    }
    LaunchedEffect(Unit) {
        contentOffsetY.animateTo(0f, animationSpec = tween(320, easing = FastOutSlowInEasing))
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = pageOpacity.value },
            horizontalArrangement = Arrangement.End,
        ) {
            Row(
                Modifier
                    .border(1.dp, errorColor, CircleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f), CircleShape)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(Modifier.size(10.dp).background(errorColor, CircleShape))
                Text(stringResource(eyebrowRes), style = MaterialTheme.typography.labelMedium, color = errorColor)
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Column(
                Modifier.graphicsLayer {
                    alpha = pageOpacity.value
                    translationY = contentOffsetY.value
                },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val sharedTransitionScope = LocalSharedTransitionScope.current
                val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
                val sharedModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                    with(sharedTransitionScope) {
                        Modifier.sharedBounds(
                            sharedContentState = rememberSharedContentState(key = SharedElementKeys.gameIdentity(gameId)),
                            animatedVisibilityScope = animatedVisibilityScope,
                        )
                    }
                } else {
                    Modifier
                }
                Box(
                    sharedModifier
                        .size(96.dp)
                        .background(errorColor.copy(alpha = 0.12f), CircleShape)
                        .border(1.dp, errorColor.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(icon, fontSize = 40.sp)
                }

                Text(stringResource(titleRes), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp))
                Text(
                    stringResource(bodyRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp),
                )

                if (revealWord != null) {
                    Column(Modifier.padding(top = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            stringResource(R.string.wordguess_reveal_word_label),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            revealWord.uppercase(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }

                Row(
                    Modifier.padding(top = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    MetaItem(stringResource(R.string.loss_difficulty), stringResource(difficultyLabelRes))
                    Box(Modifier.width(1.dp).height(28.dp).background(MaterialTheme.colorScheme.outlineVariant))
                    MetaItem(stringResource(R.string.loss_elapsed_time), formatElapsed(elapsedSeconds))
                }

                Button(onClick = onRetry, modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
                    Text(stringResource(R.string.loss_try_again))
                }

                if (analyzerSnapshot != null) {
                    TextButton(
                        onClick = {
                            AnalyzerHandoff.set(analyzerSnapshot)
                            onWalkThroughSolve()
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    ) {
                        Text(stringResource(R.string.walk_through_solve))
                    }
                }

                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onOtherDifficulty) {
                        Text(stringResource(R.string.loss_other_difficulty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Box(Modifier.width(1.dp).height(16.dp).background(MaterialTheme.colorScheme.outlineVariant))
                    TextButton(onClick = onTryAnotherGame) {
                        Text(stringResource(R.string.loss_try_another_game), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
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
