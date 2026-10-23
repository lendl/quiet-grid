package com.quietgrid.app.games.sudoku

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.quietgrid.app.R
import java.time.LocalDate
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.ui.components.CollectPuzzleResult
import com.quietgrid.app.ui.components.PuzzleBoardContainer
import com.quietgrid.app.ui.components.rememberHapticController
import com.quietgrid.app.ui.play.GamePlayScaffold
import com.quietgrid.app.ui.play.LocalGamePlayPreferences
import com.quietgrid.app.ui.play.PlayHintAction
import com.quietgrid.app.ui.play.PlayHintMessage
import com.quietgrid.app.ui.play.PlayTimer
import com.quietgrid.engine.sudoku.SudokuTechnique

@Composable
fun SudokuPlayScreen(
    difficulty: Difficulty,
    resume: Boolean,
    dailyDate: LocalDate? = null,
    variant: SudokuVariant = SudokuVariant.CLASSIC,
    onBack: () -> Unit,
    onFinished: (SudokuResult) -> Unit,
) {
    val viewModel = hiltViewModel<SudokuPlayViewModel, SudokuPlayViewModel.Factory>(
        creationCallback = { factory -> factory.create(difficulty, resume, dailyDate, variant) },
    )
    CollectPuzzleResult(viewModel.result, onFinished)
    val finishing by viewModel.finishing.collectAsState()

    val session = viewModel.session

    val haptics = rememberHapticController()
    val showMistakes = LocalGamePlayPreferences.current.showMistakes
    SideEffect { viewModel.showMistakes = showMistakes }
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

    val hint = viewModel.nextMoveHint
    val hintActive = viewModel.nextMoveHintActive && !viewModel.isComputingHint
    GamePlayScaffold(
        gameId = variant.gameId,
        timer = PlayTimer.Elapsed(viewModel.elapsedSeconds.toInt()),
        onBack = onBack,
        onEndPuzzle = { viewModel.endPuzzle() },
        difficulty = session?.let { Difficulty.fromKey(it.puzzle.difficulty) },
        hint = PlayHintAction(
            active = hintActive,
            busy = viewModel.isComputingHint,
            contentDescription = stringResource(if (hintActive) R.string.sudoku_hint_hide else R.string.sudoku_hint_show),
            onClick = { viewModel.toggleNextMoveHint() },
        ),
        hintMessage = if (hintActive) sudokuHintMessage(hint) else null,
        finishing = finishing,
        foldRows = session?.board?.size,
        foldCols = session?.board?.firstOrNull()?.size,
        dockHeight = ::sudokuDockHeight,
        dock = { sizeClass ->
            if (session != null) {
                SudokuDock(
                    sizeClass,
                    sudokuDockModel(
                        session = session,
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
                        SudokuGrid(
                            board = session.board,
                            givens = session.puzzle.givens,
                            notes = session.notes,
                            finishedCells = session.finishedCells,
                            selectedCell = viewModel.selectedCell,
                            feedbackCorrectRows = viewModel.feedbackCorrectRows,
                            feedbackCorrectCols = viewModel.feedbackCorrectCols,
                            feedbackCorrectBoxes = viewModel.feedbackCorrectBoxes,
                            feedbackIncorrectRows = viewModel.feedbackIncorrectRows,
                            feedbackIncorrectCols = viewModel.feedbackIncorrectCols,
                            feedbackIncorrectBoxes = viewModel.feedbackIncorrectBoxes,
                            onCellPress = viewModel::onCellPress,
                            hintEvidenceCells = hint?.evidenceCells?.toSet() ?: emptySet(),
                            hintTargetCells = hint?.targetCells?.toSet() ?: emptySet(),
                            hintPlacementTarget = (hint as? SudokuPlacementHint)?.let { Triple(it.row, it.col, it.digit) },
                            hintHighlightRows = hint?.highlightRows?.toSet() ?: emptySet(),
                            hintHighlightCols = hint?.highlightCols?.toSet() ?: emptySet(),
                            hintHighlightBoxes = hint?.highlightBoxes?.toSet() ?: emptySet(),
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun sudokuHintMessage(hint: SudokuNextMoveHint?): PlayHintMessage {
    val (title, body) = if (hint != null) {
        resolveSudokuHintText(hint)
    } else {
        stringResource(R.string.sudoku_hint_unsupported_title) to stringResource(R.string.sudoku_hint_unsupported_body)
    }
    return PlayHintMessage(title, body)
}

@Composable
internal fun houseLabel(house: SudokuHouseRefUi): String = stringResource(
    when (house.kind) {
        "row" -> R.string.sudoku_hint_row
        "column" -> R.string.sudoku_hint_column
        else -> R.string.sudoku_hint_box
    },
    house.index + 1,
)

@Composable
internal fun cellLabel(row: Int, col: Int): String = stringResource(R.string.sudoku_hint_cell, row + 1, col + 1)

internal fun techniqueLabelRes(technique: SudokuTechnique): Int = when (technique) {
    SudokuTechnique.NAKED_SINGLE -> R.string.sudoku_hint_technique_naked_single
    SudokuTechnique.HIDDEN_SINGLE -> R.string.sudoku_hint_technique_hidden_single
    SudokuTechnique.NAKED_PAIR -> R.string.sudoku_hint_technique_naked_pair
    SudokuTechnique.HIDDEN_PAIR -> R.string.sudoku_hint_technique_hidden_pair
    SudokuTechnique.POINTING_PAIR_TRIPLE -> R.string.sudoku_hint_technique_pointing_pair_triple
    SudokuTechnique.BOX_LINE_REDUCTION -> R.string.sudoku_hint_technique_box_line_reduction
    SudokuTechnique.X_WING -> R.string.sudoku_hint_technique_x_wing
    SudokuTechnique.SWORDFISH -> R.string.sudoku_hint_technique_swordfish
    SudokuTechnique.XY_WING -> R.string.sudoku_hint_technique_xy_wing
    SudokuTechnique.XYZ_WING -> R.string.sudoku_hint_technique_xyz_wing
    SudokuTechnique.COLORING -> R.string.sudoku_hint_technique_coloring
    SudokuTechnique.CHAINS -> R.string.sudoku_hint_technique_chains
}

@Composable
private fun resolveSudokuHintText(hint: SudokuNextMoveHint): Pair<String, String> = when (hint) {
    is SudokuInvalidConflict -> stringResource(R.string.sudoku_hint_invalid_conflict_title) to
        stringResource(R.string.sudoku_hint_invalid_conflict_body, houseLabel(hint.house), hint.digit)

    is SudokuInvalidDeadCell -> stringResource(R.string.sudoku_hint_invalid_dead_cell_title) to
        stringResource(R.string.sudoku_hint_invalid_dead_cell_body, cellLabel(hint.row, hint.col))

    is SudokuPlacementHint -> {
        val techniqueLabel = stringResource(techniqueLabelRes(hint.technique))
        val cell = cellLabel(hint.row, hint.col)
        val title = stringResource(R.string.sudoku_hint_placement_title, techniqueLabel, hint.digit)
        val body = when (hint.technique) {
            SudokuTechnique.NAKED_SINGLE -> stringResource(R.string.sudoku_hint_naked_single_body, hint.digit, cell)
            SudokuTechnique.HIDDEN_SINGLE -> stringResource(
                R.string.sudoku_hint_hidden_single_body, hint.digit, hint.house?.let { houseLabel(it) } ?: "", cell,
            )
            else -> stringResource(R.string.sudoku_hint_naked_single_body, hint.digit, cell)
        }
        title to body
    }

    is SudokuEliminationHint -> {
        val techniqueLabel = stringResource(techniqueLabelRes(hint.technique))
        val digitsLabel = hint.digits.joinToString(", ")
        val title = stringResource(R.string.sudoku_hint_elimination_title, techniqueLabel, digitsLabel)
        val body = if (hint.technique == SudokuTechnique.POINTING_PAIR_TRIPLE || hint.technique == SudokuTechnique.BOX_LINE_REDUCTION) {
            stringResource(
                R.string.sudoku_hint_locked_candidates_body,
                digitsLabel,
                hint.sourceHouse?.let { houseLabel(it) } ?: "",
                hint.targetHouse?.let { houseLabel(it) } ?: "",
            )
        } else {
            val targetLabelList = mutableListOf<String>()
            for (cell in hint.targetCells) {
                targetLabelList.add(cellLabel(cell.first, cell.second))
            }
            val targetLabels = targetLabelList.joinToString(", ")
            stringResource(R.string.sudoku_hint_elimination_body, techniqueLabel, digitsLabel, targetLabels)
        }
        title to body
    }
}
