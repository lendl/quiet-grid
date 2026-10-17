package com.quietgrid.app.games.killersudoku

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.games.sudoku.SudokuDock
import com.quietgrid.app.games.sudoku.SudokuHouseRefUi
import com.quietgrid.app.games.sudoku.SudokuInvalidConflict
import com.quietgrid.app.games.sudoku.SudokuInvalidDeadCell
import com.quietgrid.app.games.sudoku.cellLabel
import com.quietgrid.app.games.sudoku.houseLabel
import com.quietgrid.app.games.sudoku.sudokuDockHeight
import com.quietgrid.app.games.sudoku.sudokuDockModel
import com.quietgrid.app.games.sudoku.techniqueLabelRes
import com.quietgrid.app.ui.components.CollectPuzzleResult
import com.quietgrid.app.ui.components.PuzzleBoardContainer
import com.quietgrid.app.ui.components.rememberHapticController
import com.quietgrid.app.ui.play.GamePlayScaffold
import com.quietgrid.app.ui.play.PlayHintAction
import com.quietgrid.app.ui.play.PlayHintMessage
import com.quietgrid.app.ui.play.PlayTimer
import com.quietgrid.engine.killersudoku.KillerTechnique

@Composable
fun KillerSudokuPlayScreen(
    difficulty: Difficulty,
    resume: Boolean,
    onBack: () -> Unit,
    onFinished: (KillerSudokuResult) -> Unit,
) {
    val viewModel = hiltViewModel<KillerSudokuPlayViewModel, KillerSudokuPlayViewModel.Factory>(
        creationCallback = { factory -> factory.create(difficulty, resume) },
    )
    CollectPuzzleResult(viewModel.result, onFinished)

    val session = viewModel.session

    val haptics = rememberHapticController()
    var flashTrigger by remember { mutableStateOf(0) }
    LaunchedEffect(viewModel.feedbackEvent) {
        val event = viewModel.feedbackEvent
        if (event != null) {
            if (event.incorrect) {
                haptics.incorrectFeedback()
                flashTrigger++
            } else if (event.correct) {
                haptics.correctFeedback()
            }
        }
    }

    val hint = viewModel.hint
    val hintActive = viewModel.hintActive && !viewModel.isComputingHint
    GamePlayScaffold(
        gameId = GameId.KILLERSUDOKU,
        timer = PlayTimer.Elapsed(viewModel.elapsedSeconds.toInt()),
        onBack = onBack,
        onEndPuzzle = { viewModel.endPuzzle() },
        difficulty = session?.let { Difficulty.fromKey(it.puzzle.difficulty) },
        hint = PlayHintAction(
            active = hintActive,
            busy = viewModel.isComputingHint,
            contentDescription = stringResource(if (hintActive) R.string.sudoku_hint_hide else R.string.sudoku_hint_show),
            onClick = { viewModel.toggleHint() },
        ),
        hintMessage = if (hintActive) killerHintMessage(hint) else null,
        dockHeight = ::sudokuDockHeight,
        dock = { sizeClass ->
            if (session != null) {
                SudokuDock(
                    sizeClass,
                    sudokuDockModel(
                        session = session.grid,
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
                playFresh = !resume,
                flashTrigger = flashTrigger,
            ) {
                if (session != null) {
                    Box(Modifier.padding(8.dp)) {
                        KillerSudokuGrid(
                            layout = session.layout,
                            board = session.grid.board,
                            notes = session.grid.notes,
                            finishedCells = session.grid.finishedCells,
                            selectedCell = viewModel.selectedCell,
                            feedbackCorrectRows = viewModel.feedbackCorrectRows,
                            feedbackCorrectCols = viewModel.feedbackCorrectCols,
                            feedbackCorrectBoxes = viewModel.feedbackCorrectBoxes,
                            feedbackIncorrectRows = viewModel.feedbackIncorrectRows,
                            feedbackIncorrectCols = viewModel.feedbackIncorrectCols,
                            feedbackIncorrectBoxes = viewModel.feedbackIncorrectBoxes,
                            onCellPress = viewModel::onCellPress,
                            hint = hint,
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun killerHintMessage(hint: KillerSudokuHint?): PlayHintMessage {
    val (title, body) = if (hint != null) {
        resolveKillerHintText(hint)
    } else {
        stringResource(R.string.sudoku_hint_unsupported_title) to stringResource(R.string.killersudoku_hint_unsupported_body)
    }
    return PlayHintMessage(title, body)
}

private fun killerTechniqueLabelRes(technique: KillerTechnique): Int = when (technique) {
    KillerTechnique.CAGE_LAST_CELL -> R.string.killersudoku_hint_technique_cage_last_cell
    KillerTechnique.CAGE_COMBINATION -> R.string.killersudoku_hint_technique_cage_combination
    KillerTechnique.CAGE_PERMUTATION -> R.string.killersudoku_hint_technique_cage_permutation
    KillerTechnique.CAGE_HOUSE_INTERACTION -> R.string.killersudoku_hint_technique_cage_house_interaction
    KillerTechnique.INNIE_OUTIE -> R.string.killersudoku_hint_technique_innie_outie
    KillerTechnique.RULE_OF_45 -> R.string.killersudoku_hint_technique_rule_of_45
    KillerTechnique.RULE_OF_45_MULTI_HOUSE -> R.string.killersudoku_hint_technique_rule_of_45_multi_house
    else -> techniqueLabelRes(requireNotNull(technique.classic))
}

@Composable
private fun housesLabel(hint: KillerSudokuPlacementHint): String =
    hint.houses.map { houseLabel(SudokuHouseRefUi(it.kind, it.index)) }.joinToString(", ")

@Composable
private fun resolveKillerHintText(hint: KillerSudokuHint): Pair<String, String> = when (hint) {
    is KillerSudokuGridConflictHint -> when (val conflict = hint.conflict) {
        is SudokuInvalidConflict -> stringResource(R.string.sudoku_hint_invalid_conflict_title) to
            stringResource(R.string.sudoku_hint_invalid_conflict_body, houseLabel(conflict.house), conflict.digit)
        is SudokuInvalidDeadCell -> stringResource(R.string.sudoku_hint_invalid_dead_cell_title) to
            stringResource(R.string.sudoku_hint_invalid_dead_cell_body, cellLabel(conflict.row, conflict.col))
        else -> stringResource(R.string.sudoku_hint_invalid_conflict_title) to stringResource(R.string.killersudoku_hint_unsupported_body)
    }

    is KillerSudokuCageConflictHint -> stringResource(R.string.sudoku_hint_invalid_conflict_title) to
        stringResource(R.string.killersudoku_hint_cage_conflict_body, hint.sum)

    is KillerSudokuPlacementHint -> {
        val cell = cellLabel(hint.row, hint.col)
        val title = stringResource(R.string.sudoku_hint_placement_title, stringResource(killerTechniqueLabelRes(hint.technique)), hint.digit)
        val sum = hint.cageSum ?: 0
        val size = hint.cageSize ?: 0
        val body = when (hint.technique) {
            KillerTechnique.CAGE_LAST_CELL -> stringResource(R.string.killersudoku_hint_cage_last_cell_body, sum, cell, hint.digit)
            KillerTechnique.CAGE_COMBINATION -> stringResource(R.string.killersudoku_hint_cage_combination_body, sum, size, hint.digit, cell)
            KillerTechnique.CAGE_PERMUTATION -> stringResource(R.string.killersudoku_hint_cage_permutation_body, sum, hint.digit, cell)
            KillerTechnique.CAGE_HOUSE_INTERACTION -> stringResource(R.string.killersudoku_hint_cage_house_interaction_body, sum, hint.digit, cell)
            KillerTechnique.INNIE_OUTIE, KillerTechnique.RULE_OF_45 ->
                stringResource(R.string.killersudoku_hint_rule_of_45_body, housesLabel(hint), hint.digit, cell)
            KillerTechnique.RULE_OF_45_MULTI_HOUSE ->
                stringResource(R.string.killersudoku_hint_rule_of_45_multi_house_body, housesLabel(hint), hint.houses.size * 45, hint.digit, cell)
            else -> stringResource(R.string.killersudoku_hint_classic_body, stringResource(killerTechniqueLabelRes(hint.technique)), hint.digit, cell)
        }
        title to body
    }
}
