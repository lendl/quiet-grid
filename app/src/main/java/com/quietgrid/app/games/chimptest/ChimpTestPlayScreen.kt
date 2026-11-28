package com.quietgrid.app.games.chimptest

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quietgrid.app.appViewModel
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
    val viewModel = appViewModel { graph ->
        ChimpTestPlayViewModel(
            graph.sessionRepository,
            graph.statsRepository,
            graph.playHistoryRepository,
            graph.badgeRepository,
            graph.appForeground,
            graph.motionSettings,
            difficulty,
            resume,
        )
    }
    CollectPuzzleResult(viewModel.result, onFinished)
    val finishing by viewModel.finishing.collectAsStateWithLifecycle()

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
