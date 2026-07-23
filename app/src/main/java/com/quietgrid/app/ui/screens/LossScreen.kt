package com.quietgrid.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.formatElapsed
import com.quietgrid.app.games.chimptest.chimpDifficultyLabelRes
import com.quietgrid.app.games.nonogram.nonogramDifficultyLabelRes
import com.quietgrid.app.games.minesweeper.minesweeperDifficultyLabelRes
import com.quietgrid.app.games.sudoku.sudokuDifficultyLabelRes
import com.quietgrid.app.games.takuzu.takuzuDifficultyLabelRes
import com.quietgrid.app.games.wordsearch.wordSearchDifficultyLabelRes

@Composable
fun LossScreen(
    gameId: GameId,
    difficulty: Difficulty,
    elapsedSeconds: Int,
    reason: String,
    onTryAnotherGame: () -> Unit,
) {
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
        else -> chimpDifficultyLabelRes(difficulty)
    }

    val visibleState = remember { MutableTransitionState(false) }
    LaunchedEffect(Unit) { visibleState.targetState = true }

    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visibleState = visibleState,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 8 }),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(stringResource(eyebrowRes), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error)
                Text(stringResource(titleRes), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
                Text(stringResource(bodyRes), style = MaterialTheme.typography.bodyMedium)

                Card(Modifier.fillMaxWidth().padding(top = 24.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.loss_difficulty))
                            Text(stringResource(difficultyLabelRes))
                        }
                        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(stringResource(R.string.loss_elapsed_time))
                            Text(formatElapsed(elapsedSeconds))
                        }
                    }
                }

                Button(onClick = onTryAnotherGame, modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
                    Text(stringResource(R.string.loss_try_another_game))
                }
            }
        }
    }
}
