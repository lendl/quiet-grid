package com.quietgrid.app.games.chimptest

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.ui.components.CollectPuzzleResult
import com.quietgrid.app.ui.components.PuzzleBoardContainer
import com.quietgrid.app.ui.components.rememberHapticController
import com.quietgrid.app.ui.play.GamePlayScaffold
import com.quietgrid.app.ui.play.PlayTimer

@Composable
fun ChimpTestPlayScreen(
    difficulty: Difficulty,
    resume: Boolean,
    onBack: () -> Unit,
    onFinished: (ChimpTestResult) -> Unit,
) {
    val viewModel = hiltViewModel<ChimpTestPlayViewModel, ChimpTestPlayViewModel.Factory>(
        creationCallback = { factory -> factory.create(difficulty, resume) },
    )
    CollectPuzzleResult(viewModel.result, onFinished)
    val finishing by viewModel.finishing.collectAsState()

    val session = viewModel.session
    val haptics = rememberHapticController()
    LaunchedEffect(viewModel.correctTapTrigger) {
        if (viewModel.correctTapTrigger > 0) haptics.correctFeedback()
    }

    GamePlayScaffold(
        gameId = GameId.CHIMPTEST,
        timer = PlayTimer.Elapsed(viewModel.elapsedSeconds.toInt()),
        onBack = onBack,
        onEndPuzzle = { viewModel.endPuzzle() },
        finishing = finishing,
        difficulty = session?.let { Difficulty.fromKey(it.puzzle.difficulty) },
        board = {
            PuzzleBoardContainer(visible = session != null, playFresh = !resume, zoomable = false) {
                if (session != null) {
                    Box(Modifier.padding(24.dp)) {
                        ChimpTestGrid(
                            cells = session.cells,
                            revealAll = session.revealAll,
                            wrongTapCell = session.wrongTapCell,
                            gridSize = session.puzzle.gridSize,
                            nextExpected = session.nextExpected,
                            onCellTap = viewModel::onCellTap,
                        )
                    }
                }
            }
        },
    )
}
