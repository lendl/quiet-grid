package com.quietgrid.app.games.sudoku

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.data.AppContainer
import com.quietgrid.app.ui.components.BoardEntrance
import com.quietgrid.app.ui.components.ElapsedTimerText
import com.quietgrid.app.ui.components.GameBackButton
import com.quietgrid.app.ui.components.ZoomableBoardSurface

/**
 * Material3's OutlinedButton hardcodes an internal Row with a 58dp minWidth that no external
 * modifier can shrink below — fine on its own, but fatal for 10 buttons sharing one row on a
 * phone width (each gets ~29dp), since the digit text centers inside that phantom 58dp box and
 * ends up clipped entirely outside the button's actual visible bounds. This is a bare [Surface]
 * instead, which enforces no minimum size at all, so it can actually shrink to its weighted share.
 */
@Composable
private fun SudokuPadButton(
    active: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val contentColor = when {
        active -> MaterialTheme.colorScheme.onPrimary
        enabled -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        contentColor = contentColor,
        border = if (active) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            content()
        }
    }
}

@Composable
fun SudokuPlayScreen(
    difficulty: Difficulty,
    resume: Boolean,
    onBack: () -> Unit,
    onFinished: (SudokuResult) -> Unit,
) {
    val context = LocalContext.current.applicationContext
    val viewModel: SudokuPlayViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                SudokuPlayViewModel(
                    appContext = context,
                    sessionRepository = AppContainer.sessionRepository,
                    statsRepository = AppContainer.statsRepository,
                    requestedDifficulty = difficulty,
                    resume = resume,
                )
            }
        },
    )

    LaunchedEffect(viewModel) {
        viewModel.result.collect { result -> onFinished(result) }
    }

    var showEndDialog by remember { mutableStateOf(false) }
    val session = viewModel.session

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            GameBackButton(onBack)
            Row(
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (session != null) {
                    val displayDifficulty = Difficulty.fromKey(session.puzzle.difficulty)
                    val filledCount = session.board.sumOf { row -> row.count { it != null } }
                    Column {
                        Text(stringResource(R.string.sudoku_filled_label), style = MaterialTheme.typography.labelSmall)
                        Text("$filledCount / 81", style = MaterialTheme.typography.titleMedium)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(stringResource(R.string.sudoku_difficulty_label), style = MaterialTheme.typography.labelSmall)
                        Text(stringResource(sudokuDifficultyLabelRes(displayDifficulty)), style = MaterialTheme.typography.titleMedium)
                    }
                }
                IconButton(onClick = { viewModel.toggleNextMoveHint() }) {
                    Icon(
                        imageVector = Icons.Filled.Lightbulb,
                        contentDescription = stringResource(
                            if (viewModel.nextMoveHintActive) R.string.sudoku_hint_hide else R.string.sudoku_hint_show,
                        ),
                        tint = if (viewModel.nextMoveHintActive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = { showEndDialog = true }) {
                    Text(stringResource(R.string.common_end_puzzle))
                }
            }
        }

        ElapsedTimerText(
            viewModel.elapsedSeconds.toInt(),
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )

        val hint = viewModel.nextMoveHint
        if (viewModel.nextMoveHintActive) {
            Card(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Column(Modifier.padding(12.dp)) {
                    val (hintTitle, hintBody) = if (hint != null) {
                        resolveSudokuHintText(hint)
                    } else {
                        stringResource(R.string.sudoku_hint_unsupported_title) to stringResource(R.string.sudoku_hint_unsupported_body)
                    }
                    Text(hintTitle, style = MaterialTheme.typography.titleSmall)
                    Text(hintBody, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (session != null) {
                BoardEntrance(playFresh = !resume, modifier = Modifier.fillMaxSize()) {
                    ZoomableBoardSurface(Modifier.fillMaxSize()) {
                        Box(Modifier.fillMaxSize().padding(8.dp)) {
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
            }
        }

        if (session != null) {
            val noteMode = session.inputMode == SudokuInputMode.NOTES
            val hasSelection = viewModel.selectedCell != null
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                SudokuPadButton(
                    active = noteMode,
                    enabled = hasSelection,
                    onClick = viewModel::onToggleNoteMode,
                    modifier = Modifier.weight(1f).height(52.dp),
                ) {
                    Icon(
                        if (noteMode) Icons.Filled.Create else Icons.Outlined.Create,
                        contentDescription = stringResource(R.string.sudoku_notes_mode_label),
                    )
                }
                for (digit in 1..9) {
                    val (selRow, selCol) = viewModel.selectedCell ?: (-1 to -1)
                    val selectedValue = if (hasSelection) session.board[selRow][selCol] else null
                    val active = if (noteMode) digit in session.notes.getOrElse(selRow) { emptyList() }.getOrElse(selCol) { emptySet() } else selectedValue == digit
                    val digitsDisabled = !hasSelection || (noteMode && selectedValue != null)
                    SudokuPadButton(
                        active = active,
                        enabled = !digitsDisabled,
                        onClick = { viewModel.onPressDigit(digit) },
                        modifier = Modifier.weight(1f).height(52.dp),
                    ) {
                        Text(digit.toString(), style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }

    if (showEndDialog) {
        AlertDialog(
            onDismissRequest = { showEndDialog = false },
            title = { Text(stringResource(R.string.puzzle_play_end_dialog_title)) },
            text = { Text(stringResource(R.string.puzzle_play_end_dialog_message)) },
            confirmButton = {
                Button(onClick = {
                    showEndDialog = false
                    viewModel.endPuzzle()
                }) { Text(stringResource(R.string.puzzle_play_end_dialog_confirm)) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEndDialog = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }
}

@Composable
private fun houseLabel(house: SudokuHouseRef): String = stringResource(
    when (house.kind) {
        "row" -> R.string.sudoku_hint_row
        "column" -> R.string.sudoku_hint_column
        else -> R.string.sudoku_hint_box
    },
    house.index + 1,
)

@Composable
private fun cellLabel(row: Int, col: Int): String = stringResource(R.string.sudoku_hint_cell, row + 1, col + 1)

private fun techniqueLabelRes(technique: SudokuTechnique): Int = when (technique) {
    SudokuTechnique.NAKED_SINGLE -> R.string.sudoku_hint_technique_naked_single
    SudokuTechnique.HIDDEN_SINGLE -> R.string.sudoku_hint_technique_hidden_single
    SudokuTechnique.NAKED_PAIR -> R.string.sudoku_hint_technique_naked_pair
    SudokuTechnique.HIDDEN_PAIR -> R.string.sudoku_hint_technique_hidden_pair
    SudokuTechnique.POINTING_PAIR_TRIPLE -> R.string.sudoku_hint_technique_pointing_pair_triple
    SudokuTechnique.BOX_LINE_REDUCTION -> R.string.sudoku_hint_technique_box_line_reduction
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
                houseLabel(hint.sourceHouse),
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
