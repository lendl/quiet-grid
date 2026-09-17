package com.quietgrid.app.games.sudoku

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quietgrid.app.R
import com.quietgrid.app.core.formatElapsed
import com.quietgrid.app.ui.components.ChallengerHeartIcon
import com.quietgrid.app.ui.components.CollectPuzzleResult
import com.quietgrid.app.ui.components.EndPuzzleDialog
import com.quietgrid.app.ui.components.EndPuzzleIconButton
import com.quietgrid.app.ui.components.GameBackButton
import com.quietgrid.app.ui.components.PuzzleBoardContainer
import com.quietgrid.app.ui.components.rememberHapticController

@Composable
fun SudokuChallengerPlayScreen(
    onFinished: (SudokuChallengerResult) -> Unit,
) {
    val viewModel = hiltViewModel<SudokuChallengerViewModel>()

    CollectPuzzleResult(viewModel.result, onFinished)

    BackHandler { viewModel.endRun() }

    var showEndDialog by remember { mutableStateOf(false) }
    var selectedCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val session = viewModel.session
    val haptics = rememberHapticController()
    var boardFlashTrigger by remember { mutableStateOf(0) }
    LaunchedEffect(viewModel.wrongMoveTrigger) {
        if (viewModel.wrongMoveTrigger > 0) {
            haptics.incorrectFeedback()
            boardFlashTrigger++
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            GameBackButton(onBack = viewModel::endRun)
            Text(
                stringResource(R.string.sudoku_challenger_label),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 12.dp).weight(1f),
            )
            if (session != null) {
                Row(Modifier.padding(end = 8.dp)) {
                    repeat(SUDOKU_CHALLENGER_STARTING_LIVES) { index ->
                        ChallengerHeartIcon(
                            filled = index < session.livesRemaining,
                            modifier = Modifier.padding(end = 4.dp),
                        )
                    }
                }
            }
            EndPuzzleIconButton(onClick = { showEndDialog = true })
        }

        if (session != null) {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "${stringResource(R.string.sudoku_challenger_tier_label)}: ${stringResource(sudokuDifficultyLabelRes(session.tier))}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "${stringResource(R.string.sudoku_challenger_solved_label)}: ${session.puzzlesSolved}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                val secondsInt = maxOf(0, session.secondsRemaining.toInt())
                val lowTime = session.secondsRemaining <= 10
                val timePulse = remember { Animatable(1f) }
                LaunchedEffect(secondsInt) {
                    if (lowTime && secondsInt > 0) {
                        timePulse.snapTo(1f)
                        timePulse.animateTo(
                            targetValue = 1f,
                            animationSpec = keyframes {
                                durationMillis = 300
                                1f at 0
                                1.35f at 120
                                1f at 300
                            },
                        )
                    }
                }
                Text(
                    formatElapsed(secondsInt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (lowTime) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.graphicsLayer(scaleX = timePulse.value, scaleY = timePulse.value),
                )
            }
        }

        PuzzleBoardContainer(
            visible = session != null,
            playFresh = true,
            zoomable = false,
            flashTrigger = boardFlashTrigger,
        ) {
            session?.let { current ->
                SudokuGrid(
                    board = current.puzzleSession.board,
                    givens = current.puzzleSession.puzzle.givens,
                    notes = current.puzzleSession.notes,
                    finishedCells = current.puzzleSession.finishedCells,
                    selectedCell = selectedCell,
                    feedbackCorrectRows = emptySet(),
                    feedbackCorrectCols = emptySet(),
                    feedbackCorrectBoxes = emptySet(),
                    feedbackIncorrectRows = emptySet(),
                    feedbackIncorrectCols = emptySet(),
                    feedbackIncorrectBoxes = emptySet(),
                    onCellPress = { row, col -> selectedCell = row to col },
                )
            }
        }

        if (session != null) {
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                for (digit in 1..9) {
                    OutlinedButton(
                        onClick = { selectedCell?.let { (row, col) -> viewModel.onDigit(row, col, digit) } },
                        enabled = selectedCell != null,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(digit.toString())
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                OutlinedButton(
                    onClick = { selectedCell?.let { (row, col) -> viewModel.onErase(row, col) } },
                    enabled = selectedCell != null,
                ) {
                    Text(stringResource(R.string.sudoku_clear_cell))
                }
            }
        }
    }

    EndPuzzleDialog(
        visible = showEndDialog,
        onDismiss = { showEndDialog = false },
        onConfirm = {
            showEndDialog = false
            viewModel.endRun()
        },
    )
}
