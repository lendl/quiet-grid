package com.quietgrid.app.games.nonogram

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.ui.components.CollectPuzzleResult
import com.quietgrid.app.ui.components.PuzzleBoardContainer
import com.quietgrid.app.ui.components.rememberHapticController
import com.quietgrid.app.ui.play.GamePlayScaffold
import com.quietgrid.app.ui.play.PlayHintAction
import com.quietgrid.app.ui.play.PlayHintMessage
import com.quietgrid.app.ui.play.PlayTimer

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
    val finishing by viewModel.finishing.collectAsState()

    val session = viewModel.session
    val haptics = rememberHapticController()

    val hint = viewModel.nextMoveHint
    val hintActive = viewModel.nextMoveHintActive && !viewModel.isComputingHint
    GamePlayScaffold(
        gameId = GameId.NONOGRAM,
        timer = PlayTimer.Elapsed(viewModel.elapsedSeconds.toInt()),
        onBack = onBack,
        onEndPuzzle = { viewModel.endPuzzle() },
        finishing = finishing,
        foldRows = session?.puzzle?.rows,
        foldCols = session?.puzzle?.cols,
        difficulty = session?.let { Difficulty.fromKey(it.puzzle.difficulty) },
        hint = PlayHintAction(
            active = hintActive,
            busy = viewModel.isComputingHint,
            contentDescription = stringResource(if (hintActive) R.string.nonogram_hint_hide else R.string.nonogram_hint_show),
            onClick = { viewModel.toggleNextMoveHint() },
        ),
        hintMessage = if (hintActive && hint != null) {
            val (title, body) = resolveNonogramHintText(hint)
            PlayHintMessage(title, body)
        } else {
            null
        },
        dockHeight = ::nonogramDockHeight,
        dock = { sizeClass ->
            NonogramModeDock(sizeClass, viewModel.inputMode) { viewModel.inputMode = it }
        },
        board = {
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
