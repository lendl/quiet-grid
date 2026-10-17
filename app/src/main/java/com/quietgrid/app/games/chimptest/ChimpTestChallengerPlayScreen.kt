package com.quietgrid.app.games.chimptest

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
fun ChimpTestChallengerPlayScreen(
    onFinished: (ChallengerResult) -> Unit,
) {
    val viewModel = hiltViewModel<ChimpTestChallengerViewModel>()

    CollectPuzzleResult(viewModel.result, onFinished)

    BackHandler { viewModel.endRun() }

    val session = viewModel.session
    val haptics = rememberHapticController()
    LaunchedEffect(viewModel.correctTapTrigger) {
        if (viewModel.correctTapTrigger > 0) haptics.correctFeedback()
    }
    LaunchedEffect(viewModel.wrongTapTrigger) {
        if (viewModel.wrongTapTrigger > 0) haptics.incorrectFeedback()
    }

    val info = if (session == null) {
        emptyList()
    } else {
        listOf(
            heartsInfo(session.livesRemaining, CHIMPTEST_CHALLENGER_STARTING_LIVES, viewModel.wrongTapTrigger),
            InfoItem.Label("${stringResource(R.string.chimp_challenger_tier_label)}: ${stringResource(chimpDifficultyLabelRes(session.tier))}"),
            InfoItem.Label("${stringResource(R.string.chimp_challenger_solved_label)}: ${session.puzzlesSolved}"),
        )
    }

    GamePlayScaffold(
        gameId = GameId.CHIMPTEST,
        timer = session?.let { PlayTimer.Countdown(it.secondsRemaining.toInt()) },
        onBack = viewModel::endRun,
        onEndPuzzle = viewModel::endRun,
        info = info,
        board = {
            PuzzleBoardContainer(
                visible = session != null,
                playFresh = true,
                zoomable = false,
                flashTrigger = viewModel.wrongTapTrigger,
            ) {
                session?.let { current ->
                    Box(Modifier.padding(24.dp)) {
                        ChimpTestGrid(
                            cells = current.puzzleSession.cells,
                            revealAll = current.puzzleSession.revealAll,
                            wrongTapCell = current.puzzleSession.wrongTapCell,
                            gridSize = current.puzzleSession.puzzle.gridSize,
                            nextExpected = current.puzzleSession.nextExpected,
                            onCellTap = viewModel::onCellTap,
                        )
                    }
                }
            }
        },
    )
}
