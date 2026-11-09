package com.quietgrid.app.games.wordswap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.ui.components.CollectPuzzleResult
import com.quietgrid.app.ui.components.PuzzleBoardContainer
import com.quietgrid.app.ui.components.rememberHapticController
import com.quietgrid.app.ui.play.GamePlayScaffold
import com.quietgrid.app.ui.play.InfoItem
import com.quietgrid.app.ui.play.PlayHintAction
import com.quietgrid.app.ui.play.PlayHintMessage
import com.quietgrid.app.ui.play.PlayTimer
import com.quietgrid.engine.wordswap.WordSwapHint
import com.quietgrid.engine.wordswap.WordSwapTechnique

@Composable
fun WordSwapPlayScreen(
    difficulty: Difficulty,
    resume: Boolean,
    onBack: () -> Unit,
    onFinished: (WordSwapResult) -> Unit,
) {
    val viewModel = hiltViewModel<WordSwapPlayViewModel, WordSwapPlayViewModel.Factory>(
        creationCallback = { factory -> factory.create(difficulty, resume) },
    )
    CollectPuzzleResult(viewModel.result, onFinished)
    val finishing by viewModel.finishing.collectAsState()
    val haptics = rememberHapticController()

    val session = viewModel.session
    GamePlayScaffold(
        gameId = GameId.WORDSWAP,
        timer = PlayTimer.Elapsed(viewModel.elapsedSeconds.toInt()),
        onBack = onBack,
        onEndPuzzle = { viewModel.endPuzzle() },
        finishing = finishing,
        foldRows = session?.puzzle?.rows,
        foldCols = session?.puzzle?.cols,
        difficulty = session?.let { Difficulty.fromKey(it.puzzle.difficulty) },
        hint = PlayHintAction(
            active = viewModel.hint != null,
            busy = viewModel.isComputingHint,
            contentDescription = stringResource(if (viewModel.hint != null) R.string.wordswap_hint_hide else R.string.wordswap_hint_show),
            onClick = viewModel::toggleHint,
        ),
        hintMessage = viewModel.hint?.let { wordSwapHintMessage(it) },
        info = if (session == null) {
            emptyList()
        } else {
            listOf(
                InfoItem.Label(stringResource(R.string.wordswap_swaps, session.swaps)),
                InfoItem.Label(stringResource(R.string.wordswap_fewest, session.puzzle.minSwaps)),
            )
        },
        dockHeight = ::wordSwapDockHeight,
        dock = { sizeClass ->
            if (session != null) {
                WordSwapDock(
                    sizeClass = sizeClass,
                    bank = wordSwapBank(session),
                    canUndo = viewModel.canUndo,
                    canRestart = session.board != session.puzzle.start,
                    onUndo = viewModel::onUndo,
                    onRestart = viewModel::onRestart,
                )
            }
        },
        board = {
            PuzzleBoardContainer(
                visible = session != null,
                playFresh = !resume,
                zoomable = false,
            ) {
                if (session != null) {
                    WordSwapGrid(
                        puzzle = session.puzzle,
                        board = session.board,
                        selectedCell = viewModel.selectedCell,
                        doneCells = wordSwapDoneCells(session),
                        hintCells = wordSwapHintCells(session.puzzle, viewModel.hint),
                        lastSwap = viewModel.lastSwap,
                        swapSerial = viewModel.swapSerial,
                        onTileTap = { cell ->
                            haptics.tapFeedback()
                            viewModel.onTileTap(cell)
                        },
                    )
                }
            }
        },
    )
}

@Composable
internal fun wordSwapHintMessage(hint: WordSwapHint): PlayHintMessage = when (hint) {
    is WordSwapHint.Placement -> PlayHintMessage(
        title = stringResource(R.string.wordswap_hint_place_title, hint.word.uppercase()),
        body = when (hint.technique) {
            WordSwapTechnique.LENGTH_SINGLE -> stringResource(R.string.wordswap_hint_length_single, hint.word.length)
            WordSwapTechnique.CROSSING_SINGLE -> stringResource(R.string.wordswap_hint_crossing_single)
            WordSwapTechnique.ONLY_SLOT -> stringResource(R.string.wordswap_hint_only_slot)
            WordSwapTechnique.CROSS_CHECK -> stringResource(R.string.wordswap_hint_cross_check)
        },
    )
    is WordSwapHint.Swap -> PlayHintMessage(
        title = stringResource(R.string.wordswap_hint_swap_title),
        body = stringResource(R.string.wordswap_hint_swap_body),
    )
}
