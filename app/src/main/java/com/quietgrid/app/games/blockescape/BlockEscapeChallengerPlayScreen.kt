package com.quietgrid.app.games.blockescape

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.res.stringResource
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

@Composable
fun BlockEscapeChallengerPlayScreen(
    onFinished: (ChallengerResult) -> Unit,
) {
    val viewModel = hiltViewModel<BlockEscapeChallengerViewModel>()

    CollectPuzzleResult(viewModel.result, onFinished)

    BackHandler { viewModel.endRun() }

    val session = viewModel.session
    val haptics = rememberHapticController()

    val info = if (session == null) {
        emptyList()
    } else {
        listOf(
            InfoItem.Label("${stringResource(R.string.blockescape_challenger_tier_label)}: ${stringResource(blockEscapeDifficultyLabelRes(session.tier))}"),
            InfoItem.Label("${stringResource(R.string.blockescape_challenger_solved_label)}: ${session.puzzlesSolved}"),
            InfoItem.Label(stringResource(R.string.blockescape_moves, session.puzzleSession.moves)),
        )
    }

    GamePlayScaffold(
        gameId = GameId.BLOCKESCAPE,
        timer = session?.let { PlayTimer.Countdown(it.secondsRemaining.toInt()) },
        onBack = viewModel::endRun,
        onEndPuzzle = viewModel::endRun,
        info = info,
        dockHeight = ::blockEscapeDockHeight,
        dock = { sizeClass ->
            if (session != null) {
                BlockEscapeDock(
                    sizeClass = sizeClass,
                    canUndo = viewModel.canUndo,
                    onUndo = viewModel::onUndo,
                )
            }
        },
        board = {
            PuzzleBoardContainer(
                visible = session != null,
                playFresh = true,
                zoomable = false,
            ) {
                session?.let { current ->
                    key(current.puzzleSession.puzzle.id) {
                        BlockEscapeGrid(
                            blocks = current.puzzleSession.blocks,
                            hint = null,
                            onMove = viewModel::onMove,
                            onPickUp = { haptics.tapFeedback() },
                        )
                    }
                }
            }
        },
    )
}
