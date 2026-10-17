package com.quietgrid.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
import com.quietgrid.app.games.hashi.hashiDifficultyLabelRes
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
import com.quietgrid.app.ui.components.OutlinedGlowButton
import com.quietgrid.app.ui.components.rememberHapticController

@Composable
fun LossScreen(
    gameId: GameId,
    difficulty: Difficulty,
    elapsedSeconds: Int,
    reason: String,
    score: Int,
    bestTile: Int,
    isMixActive: Boolean,
    onAddToMix: () -> Unit,
    onRetry: () -> Unit,
    onOtherDifficulty: () -> Unit,
    onTryAnotherGame: () -> Unit,
    onWalkThroughSolve: () -> Unit,
    dailyDate: String? = null,
    onShareDaily: () -> Unit = {},
    onBackToDaily: () -> Unit = {},
    hasNextDaily: Boolean = false,
    onPlayNextDaily: () -> Unit = {},
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
        gameId == GameId.MINISUDOKU -> {
            eyebrowRes = R.string.sudoku_loss_abandoned_eyebrow
            titleRes = R.string.minisudoku_loss_abandoned_title
            bodyRes = R.string.minisudoku_loss_abandoned_body
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
        gameId == GameId.STARBATTLE && reason == "abandoned" -> {
            eyebrowRes = R.string.starbattle_loss_abandoned_eyebrow
            titleRes = R.string.starbattle_loss_abandoned_title
            bodyRes = R.string.starbattle_loss_abandoned_body
        }
        gameId == GameId.STARBATTLE -> {
            eyebrowRes = R.string.starbattle_loss_rule_failure_eyebrow
            titleRes = R.string.starbattle_loss_rule_failure_title
            bodyRes = R.string.starbattle_loss_rule_failure_body
        }
        gameId == GameId.WHODIDIT && reason == "abandoned" -> {
            eyebrowRes = R.string.whodidit_loss_abandoned_eyebrow
            titleRes = R.string.whodidit_loss_abandoned_title
            bodyRes = R.string.whodidit_loss_abandoned_body
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
        gameId == GameId.GAME_2048 && reason == "abandoned" -> {
            eyebrowRes = R.string.game2048_loss_abandoned_eyebrow
            titleRes = R.string.game2048_loss_abandoned_title
            bodyRes = R.string.game2048_loss_abandoned_body
        }
        gameId == GameId.GAME_2048 -> {
            eyebrowRes = R.string.game2048_loss_rule_failure_eyebrow
            titleRes = R.string.game2048_loss_rule_failure_title
            bodyRes = R.string.game2048_loss_rule_failure_body
        }
        gameId == GameId.GUESSBYNUMBERS && reason == "abandoned" -> {
            eyebrowRes = R.string.guessbynumbers_loss_abandoned_eyebrow
            titleRes = R.string.guessbynumbers_loss_abandoned_title
            bodyRes = R.string.guessbynumbers_loss_abandoned_body
        }
        gameId == GameId.GUESSBYNUMBERS -> {
            eyebrowRes = R.string.guessbynumbers_loss_rule_failure_eyebrow
            titleRes = R.string.guessbynumbers_loss_rule_failure_title
            bodyRes = R.string.guessbynumbers_loss_rule_failure_body
        }
        gameId == GameId.HASHI -> {
            eyebrowRes = R.string.hashi_loss_abandoned_eyebrow
            titleRes = R.string.hashi_loss_abandoned_title
            bodyRes = R.string.hashi_loss_abandoned_body
        }
        gameId == GameId.KILLERSUDOKU -> {
            eyebrowRes = R.string.killersudoku_loss_abandoned_eyebrow
            titleRes = R.string.killersudoku_loss_abandoned_title
            bodyRes = R.string.killersudoku_loss_abandoned_body
        }
        gameId == GameId.THEMECLEAR -> {
            eyebrowRes = R.string.themeclear_loss_abandoned_eyebrow
            titleRes = R.string.themeclear_loss_abandoned_title
            bodyRes = R.string.themeclear_loss_abandoned_body
        }
        gameId == GameId.NBACK -> {
            eyebrowRes = R.string.nback_loss_abandoned_eyebrow
            titleRes = R.string.nback_loss_abandoned_title
            bodyRes = R.string.nback_loss_abandoned_body
        }
        gameId == GameId.SEQUENCEMEMORY && reason == "abandoned" -> {
            eyebrowRes = R.string.sequencememory_loss_abandoned_eyebrow
            titleRes = R.string.sequencememory_loss_abandoned_title
            bodyRes = R.string.sequencememory_loss_abandoned_body
        }
        gameId == GameId.SEQUENCEMEMORY -> {
            eyebrowRes = R.string.sequencememory_loss_rule_failure_eyebrow
            titleRes = R.string.sequencememory_loss_rule_failure_title
            bodyRes = R.string.sequencememory_loss_rule_failure_body
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
        GameId.GAME_2048 -> game2048DifficultyLabelRes(difficulty)
        GameId.STARBATTLE -> starBattleDifficultyLabelRes(difficulty)
        GameId.WHODIDIT -> whoDidItDifficultyLabelRes(difficulty)
        GameId.GUESSBYNUMBERS -> guessByNumbersDifficultyLabelRes(difficulty)
        GameId.NBACK -> nbackDifficultyLabelRes(difficulty)
        GameId.THEMECLEAR -> themeClearDifficultyLabelRes(difficulty)
        GameId.SEQUENCEMEMORY -> sequenceMemoryDifficultyLabelRes(difficulty)
        GameId.HASHI -> hashiDifficultyLabelRes(difficulty)
        GameId.KILLERSUDOKU -> killerSudokuDifficultyLabelRes(difficulty)
        GameId.MINISUDOKU -> miniSudokuDifficultyLabelRes(difficulty)
        else -> chimpDifficultyLabelRes(difficulty)
    }
    val icon = if (reason == "abandoned") "⏸" else "💥"
    val errorColor = MaterialTheme.colorScheme.error

    val highlight = remember { CompletionExtras.consume() }
    val revealWord = (highlight as? CompletionHighlight.RevealWord)?.word?.takeIf { it.isNotEmpty() }
    val revealWordList = (highlight as? CompletionHighlight.RevealWordList)?.words?.takeIf { it.isNotEmpty() }
    val analyzerSnapshot = rememberSaveable { mutableStateOf(AnalyzerHandoff.consume()) }.value

    val pageOpacity = remember { Animatable(0f) }
    val contentOffsetY = remember { Animatable(24f) }
    LaunchedEffect(Unit) {
        pageOpacity.animateTo(1f, animationSpec = tween(220))
    }
    LaunchedEffect(Unit) {
        contentOffsetY.animateTo(0f, animationSpec = tween(320, easing = FastOutSlowInEasing))
    }

    val links = if (dailyDate != null) {
        dailyResultLinks(hasNextDaily, onShareDaily, onBackToDaily, onTryAnotherGame)
    } else {
        buildList {
            if (!isMixActive) add(ResultLink(R.string.loss_other_difficulty, onOtherDifficulty))
            add(ResultLink(R.string.loss_try_another_game, onTryAnotherGame))
        }
    }
    val primaryAction: @Composable () -> Unit = {
        if (dailyDate != null) {
            DailyResultPrimaryButton(hasNextDaily, onPlayNextDaily, onShareDaily)
        } else {
            OutlinedGlowButton(onClick = onRetry) {
                Text(
                    stringResource(mixAwarePrimaryLabel(isMixActive, R.string.loss_try_again)),
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
    val walkThrough: (() -> Unit)? = analyzerSnapshot?.let { snapshot ->
        {
            AnalyzerHandoff.set(snapshot)
            onWalkThroughSolve()
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val sideways = isSidewaysPhoneResult(maxWidth, maxHeight)
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = pageOpacity.value },
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
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
                ResultOverflowMenu(
                    gameId = gameId,
                    difficulty = difficulty,
                    onAddToMix = onAddToMix,
                    modifier = Modifier.offset(x = 12.dp),
                )
            }

            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(
                    Modifier
                        .graphicsLayer {
                            alpha = pageOpacity.value
                            translationY = contentOffsetY.value
                        }
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (sideways) {
                        Row(
                            Modifier.widthIn(max = 560.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                        ) {
                            ResultMedallion(gameId, icon, errorColor, size = 72.dp, symbolSize = 30.sp)
                            Column {
                                Text(stringResource(titleRes), style = MaterialTheme.typography.headlineSmall)
                                Text(
                                    stringResource(bodyRes),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                                LossReveals(revealWord, revealWordList, centered = false)
                            }
                        }

                        Row(
                            Modifier.padding(top = 22.dp),
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            LossMetaItems(gameId, difficultyLabelRes, elapsedSeconds, score, bestTile)
                        }

                        CompactResultActions(links, Modifier.padding(top = 22.dp), primaryAction)
                        if (walkThrough != null) {
                            TextButton(onClick = walkThrough, modifier = Modifier.padding(top = 4.dp)) {
                                Text(stringResource(R.string.walk_through_solve))
                            }
                        }
                    } else {
                        ResultMedallion(gameId, icon, errorColor, size = 96.dp, symbolSize = 40.sp)

                        Text(stringResource(titleRes), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 12.dp))
                        Text(
                            stringResource(bodyRes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                        LossReveals(revealWord, revealWordList, centered = true)

                        Row(
                            Modifier.padding(top = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            LossMetaItems(gameId, difficultyLabelRes, elapsedSeconds, score, bestTile)
                        }

                        Box(Modifier.padding(top = 24.dp)) { primaryAction() }

                        if (walkThrough != null) {
                            TextButton(onClick = walkThrough, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                Text(stringResource(R.string.walk_through_solve))
                            }
                        }

                        Row(
                            Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ResultLinkGroup(links)
                        }
                    }
                }
            }
        }
    }
}

private val SCORE_ON_LOSS_TRACKED_GAMES = setOf(GameId.GAME_2048)

@Composable
private fun LossReveals(revealWord: String?, revealWordList: List<String>?, centered: Boolean) {
    if (revealWord != null) {
        RevealedAnswer(R.string.wordguess_reveal_word_label, revealWord.uppercase(), centered)
    }
    if (revealWordList != null) {
        RevealedAnswer(R.string.themeclear_reveal_words_label, revealWordList.joinToString(" · "), centered)
    }
}

@Composable
private fun RevealedAnswer(labelRes: Int, answer: String, centered: Boolean) {
    Column(
        Modifier.padding(top = if (centered) 10.dp else 6.dp),
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        Text(
            stringResource(labelRes),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            answer,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun LossMetaItems(gameId: GameId, difficultyLabelRes: Int, elapsedSeconds: Int, score: Int, bestTile: Int) {
    MetaItem(stringResource(R.string.loss_difficulty), stringResource(difficultyLabelRes))
    ResultMetaDivider()
    MetaItem(stringResource(R.string.loss_elapsed_time), formatElapsed(elapsedSeconds))
    if (gameId in SCORE_ON_LOSS_TRACKED_GAMES) {
        ResultMetaDivider()
        MetaItem(stringResource(R.string.completion_score), score.toString())
        ResultMetaDivider()
        MetaItem(stringResource(R.string.completion_best_tile), bestTile.toString())
    }
}

@Composable
private fun MetaItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
    }
}
