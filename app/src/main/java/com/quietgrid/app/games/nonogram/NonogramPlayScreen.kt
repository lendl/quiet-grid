package com.quietgrid.app.games.nonogram

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.ui.components.CollectPuzzleResult
import com.quietgrid.app.ui.components.ElapsedTimerText
import com.quietgrid.app.ui.components.EndPuzzleDialog
import com.quietgrid.app.ui.components.EndPuzzleIconButton
import com.quietgrid.app.ui.components.GameBackButton
import com.quietgrid.app.ui.components.PuzzleBoardContainer
import com.quietgrid.app.ui.components.rememberHapticController

@Composable
fun NonogramPlayScreen(
    difficulty: Difficulty,
    resume: Boolean,
    onBack: () -> Unit,
    onFinished: (NonogramResult) -> Unit,
) {
    val viewModel = hiltViewModel<NonogramPlayViewModel, NonogramPlayViewModel.Factory>(
        creationCallback = { factory -> factory.create(difficulty, resume) },
    )
    CollectPuzzleResult(viewModel.result, onFinished)

    var showEndDialog by remember { mutableStateOf(false) }
    val session = viewModel.session
    val haptics = rememberHapticController()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            GameBackButton(onBack)
            Row(
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { viewModel.toggleNextMoveHint() }, enabled = !viewModel.isComputingHint) {
                    if (viewModel.isComputingHint) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Lightbulb,
                            contentDescription = stringResource(
                                if (viewModel.nextMoveHintActive) R.string.nonogram_hint_hide else R.string.nonogram_hint_show,
                            ),
                            tint = if (viewModel.nextMoveHintActive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
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

        val hint = viewModel.nextMoveHint
        if (viewModel.nextMoveHintActive && hint != null) {
            val (hintTitle, hintBody) = resolveNonogramHintText(hint)
            Card(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text(hintTitle, style = MaterialTheme.typography.titleSmall)
                    Text(hintBody, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        PuzzleBoardContainer(visible = session != null, playFresh = !resume, zoomable = false) {
            if (session != null) {
                Box(Modifier.padding(12.dp)) {
                    NonogramBoard(
                        puzzle = session.puzzle,
                        board = session.board,
                        onTap = { row, col -> haptics.tapFeedback(); viewModel.onCellTap(row, col) },
                        onDragPaint = { cells -> haptics.tapFeedback(); viewModel.onDragPaint(cells) },
                        hintEvidenceCells = hint?.evidenceCells?.toSet() ?: emptySet(),
                        hintTargetCells = hint?.targetCells?.associate { (r, c, v) -> (r to c) to v } ?: emptyMap(),
                    )
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.nonogram_input_mode_fill), modifier = Modifier.padding(end = 8.dp))
            Switch(
                checked = viewModel.inputMode == NonogramInputMode.CROSS,
                onCheckedChange = { viewModel.inputMode = if (it) NonogramInputMode.CROSS else NonogramInputMode.FILL },
            )
            Text(stringResource(R.string.nonogram_input_mode_cross), modifier = Modifier.padding(start = 8.dp))
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

@Composable
private fun nonogramLineLabel(orientation: String, index: Int): String = stringResource(
    if (orientation == "row") R.string.nonogram_hint_line_row else R.string.nonogram_hint_line_col,
    index + 1,
)

@Composable
private fun resolveNonogramHintText(hint: NonogramNextMoveHint): Pair<String, String> {
    val label = nonogramLineLabel(hint.lineOrientation, hint.lineIndex)
    return when (hint) {
        is NonogramInvalidBoardHint -> stringResource(R.string.nonogram_hint_invalid_title) to
            stringResource(R.string.nonogram_hint_invalid_body, label)

        is NonogramProgressHint -> when (hint.kind) {
            NonogramHintKind.OVERLAP_FILL -> stringResource(R.string.nonogram_hint_overlap_fill_title) to
                if (hint.targetCount == 1) {
                    stringResource(R.string.nonogram_hint_overlap_fill_body_one, label)
                } else {
                    stringResource(R.string.nonogram_hint_overlap_fill_body_other, label, hint.targetCount)
                }
            NonogramHintKind.FORCED_EMPTY -> stringResource(R.string.nonogram_hint_forced_empty_title) to
                if (hint.targetCount == 1) {
                    stringResource(R.string.nonogram_hint_forced_empty_body_one, label)
                } else {
                    stringResource(R.string.nonogram_hint_forced_empty_body_other, label, hint.targetCount)
                }
            NonogramHintKind.COMPLETE_LINE -> stringResource(R.string.nonogram_hint_complete_line_title) to
                if (hint.targetCount == 1) {
                    stringResource(R.string.nonogram_hint_complete_line_body_one, label)
                } else {
                    stringResource(R.string.nonogram_hint_complete_line_body_other, label, hint.targetCount)
                }
        }

        is NonogramRevealFromSolution -> stringResource(R.string.nonogram_hint_reveal_title) to
            stringResource(if (hint.value == 1) R.string.nonogram_hint_reveal_fill_body else R.string.nonogram_hint_reveal_empty_body)
    }
}
