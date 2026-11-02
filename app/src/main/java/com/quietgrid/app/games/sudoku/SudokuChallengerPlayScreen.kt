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
                SudokuDock(
                    sizeClass,
                    sudokuDockModel(
                        session = session.puzzleSession,
                        selectedCell = viewModel.selectedCell,
                        canUndo = viewModel.canUndo,
                        onDigit = viewModel::onPressDigit,
                        onClear = viewModel::onClearCell,
                        onToggleNoteMode = viewModel::onToggleNoteMode,
                        onToggleAutoCandidateMode = viewModel::onToggleAutoCandidateMode,
                        onUndo = viewModel::onUndo,
                    ),
                )
            }
        },
        board = {
            PuzzleBoardContainer(
                visible = session != null,
                playFresh = true,
                flashTrigger = boardFlashTrigger,
                resetTrigger = session?.puzzlesSolved ?: 0,
            ) {
                session?.let { current ->
                    SudokuGrid(
                        board = current.puzzleSession.board,
                        givens = current.puzzleSession.puzzle.givens,
                        notes = current.puzzleSession.notes,
                        finishedCells = current.puzzleSession.finishedCells,
                        selectedCell = viewModel.selectedCell,
                        feedbackCorrectRows = emptySet(),
                        feedbackCorrectCols = emptySet(),
                        feedbackCorrectBoxes = emptySet(),
                        feedbackIncorrectRows = emptySet(),
                        feedbackIncorrectCols = emptySet(),
                        feedbackIncorrectBoxes = emptySet(),
                        onCellPress = viewModel::onCellPress,
                    )
                }
            }
        },
    )
}
