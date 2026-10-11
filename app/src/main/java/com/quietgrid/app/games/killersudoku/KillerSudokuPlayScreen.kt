package com.quietgrid.app.games.killersudoku

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.games.sudoku.SudokuHouseRefUi
import com.quietgrid.app.games.sudoku.SudokuInputMode
import com.quietgrid.app.games.sudoku.SudokuInvalidConflict
import com.quietgrid.app.games.sudoku.SudokuInvalidDeadCell
import com.quietgrid.app.games.sudoku.SudokuPadButton
import com.quietgrid.app.games.sudoku.cellLabel
import com.quietgrid.app.games.sudoku.houseLabel
import com.quietgrid.app.games.sudoku.techniqueLabelRes
import com.quietgrid.app.ui.components.CollectPuzzleResult
import com.quietgrid.app.ui.components.ElapsedTimerText
import com.quietgrid.app.ui.components.EndPuzzleDialog
import com.quietgrid.app.ui.components.EndPuzzleIconButton
import com.quietgrid.app.ui.components.GameBackButton
import com.quietgrid.app.ui.components.LogoGridLoader
import com.quietgrid.app.ui.components.PuzzleBoardContainer
import com.quietgrid.app.ui.components.rememberHapticController
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

    var showEndDialog by remember { mutableStateOf(false) }
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

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            GameBackButton(onBack)
            Row(
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { viewModel.toggleHint() }, enabled = !viewModel.isComputingHint) {
                    if (viewModel.isComputingHint) {
                        LogoGridLoader(size = 20.dp)
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Lightbulb,
                            contentDescription = stringResource(if (viewModel.hintActive) R.string.sudoku_hint_hide else R.string.sudoku_hint_show),
                            tint = if (viewModel.hintActive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                EndPuzzleIconButton(onClick = { showEndDialog = true })
            }
        }

        ElapsedTimerText(
            viewModel.elapsedSeconds.toInt(),
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )

        val hint = viewModel.hint
        if (viewModel.hintActive && !viewModel.isComputingHint) {
            Card(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Column(Modifier.padding(12.dp)) {
                    val (hintTitle, hintBody) = if (hint != null) {
                        resolveKillerHintText(hint)
                    } else {
                        stringResource(R.string.sudoku_hint_unsupported_title) to stringResource(R.string.killersudoku_hint_unsupported_body)
                    }
                    Text(hintTitle, style = MaterialTheme.typography.titleSmall)
                    Text(hintBody, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

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

        if (session != null) {
            val grid = session.grid
            val noteMode = grid.inputMode == SudokuInputMode.NOTES
            val hasSelection = viewModel.selectedCell != null
            val (selRow, selCol) = viewModel.selectedCell ?: (-1 to -1)
            val selectedValue = if (hasSelection) grid.board[selRow][selCol] else null

            @Composable
            fun digitButton(digit: Int, modifier: Modifier) {
                val active = if (noteMode) digit in grid.notes.getOrElse(selRow) { emptyList() }.getOrElse(selCol) { emptySet() } else selectedValue == digit
                val digitsDisabled = !hasSelection || (noteMode && selectedValue != null)
                SudokuPadButton(
                    active = active,
                    enabled = !digitsDisabled,
                    onClick = { viewModel.onPressDigit(digit) },
                    modifier = modifier,
                ) {
                    Text(digit.toString(), style = MaterialTheme.typography.titleMedium)
                }
            }

            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                SudokuPadButton(
                    active = !noteMode,
                    enabled = true,
                    onClick = { if (noteMode) viewModel.onToggleNoteMode() },
                    modifier = Modifier.weight(2f).height(44.dp),
                ) {
                    Text(stringResource(R.string.sudoku_normal_mode_label), style = MaterialTheme.typography.labelLarge)
                }
                SudokuPadButton(
                    active = noteMode,
                    enabled = !grid.autoCandidateMode,
                    onClick = { if (!noteMode) viewModel.onToggleNoteMode() },
                    modifier = Modifier.weight(2f).height(44.dp),
                ) {
                    Text(stringResource(R.string.sudoku_candidate_mode_label), style = MaterialTheme.typography.labelLarge)
                }
                SudokuPadButton(
                    active = false,
                    enabled = viewModel.canUndo,
                    onClick = viewModel::onUndo,
                    modifier = Modifier.weight(1.5f).height(44.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = stringResource(R.string.sudoku_undo))
                }
            }

            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (digit in 1..5) digitButton(digit, Modifier.weight(1f).height(52.dp))
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (digit in 6..9) digitButton(digit, Modifier.weight(1f).height(52.dp))
                SudokuPadButton(
                    active = false,
                    enabled = hasSelection,
                    onClick = viewModel::onClearCell,
                    modifier = Modifier.weight(1f).height(52.dp),
                ) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.sudoku_clear_cell))
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = grid.autoCandidateMode, onCheckedChange = { viewModel.onToggleAutoCandidateMode() })
                Text(stringResource(R.string.sudoku_auto_candidate_mode_label), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    EndPuzzleDialog(
        visible = showEndDialog,
        onDismiss = { showEndDialog = false },
        onConfirm = {
            showEndDialog = false
            viewModel.endPuzzle()
        },
    )
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
