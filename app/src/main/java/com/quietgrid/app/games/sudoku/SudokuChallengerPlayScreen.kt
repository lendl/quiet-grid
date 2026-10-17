package com.quietgrid.app.games.sudoku

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.quietgrid.app.R
import com.quietgrid.app.core.GameId
import com.quietgrid.app.session.ChallengerResult
import com.quietgrid.app.ui.components.CollectPuzzleResult
import com.quietgrid.app.ui.components.PuzzleBoardContainer
import com.quietgrid.app.ui.components.rememberHapticController
import com.quietgrid.app.ui.play.GamePlayScaffold
import com.quietgrid.app.ui.play.InfoItem
import com.quietgrid.app.ui.play.PlayTimer
import com.quietgrid.app.ui.play.heartsInfo

@Composable
fun SudokuChallengerPlayScreen(
    onFinished: (ChallengerResult) -> Unit,
) {
    val viewModel = hiltViewModel<SudokuChallengerViewModel>()

    CollectPuzzleResult(viewModel.result, onFinished)

    BackHandler { viewModel.endRun() }

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

    val info = if (session == null) {
        emptyList()
    } else {
        listOf(
            heartsInfo(session.livesRemaining, SUDOKU_CHALLENGER_STARTING_LIVES),
            InfoItem.Label("${stringResource(R.string.sudoku_challenger_tier_label)}: ${stringResource(sudokuDifficultyLabelRes(session.tier))}"),
            InfoItem.Label("${stringResource(R.string.sudoku_challenger_solved_label)}: ${session.puzzlesSolved}"),
        )
    }

    GamePlayScaffold(
        gameId = GameId.SUDOKU,
        timer = session?.let { PlayTimer.Countdown(it.secondsRemaining.toInt()) },
        onBack = viewModel::endRun,
        onEndPuzzle = viewModel::endRun,
        info = info,
        dockHeight = ::sudokuDockHeight,
        dock = { sizeClass ->
            if (session != null) {
                val cell = selectedCell
                SudokuDock(
                    sizeClass,
                    SudokuDockModel(
                        digitCount = session.puzzleSession.board.size,
                        digitsEnabled = cell != null,
                        clearEnabled = cell != null,
                        isDigitActive = { false },
                        onDigit = { digit -> cell?.let { (row, col) -> viewModel.onDigit(row, col, digit) } },
                        onClear = { cell?.let { (row, col) -> viewModel.onErase(row, col) } },
                    ),
                )
            }
        },
        board = {
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
        },
    )
}
