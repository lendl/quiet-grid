package com.quietgrid.app.games.blockescape

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quietgrid.app.R
import com.quietgrid.app.appViewModel
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.journey.JourneyRequest
import com.quietgrid.app.ui.components.CollectPuzzleResult
import com.quietgrid.app.ui.components.PuzzleBoardContainer
import com.quietgrid.app.ui.components.rememberHapticController
import com.quietgrid.app.ui.play.GamePlayScaffold
import com.quietgrid.app.ui.play.InfoItem
import com.quietgrid.app.ui.play.PlayHintAction
import com.quietgrid.app.ui.play.PlayTimer
import com.quietgrid.engine.blockescape.BLOCKESCAPE_SIZE

@Composable
fun BlockEscapePlayScreen(
    difficulty: Difficulty,
    resume: Boolean,
    onBack: () -> Unit,
    onFinished: (BlockEscapeResult) -> Unit,
    journey: JourneyRequest? = null,
    onRestarted: () -> Unit = {},
) {
    val viewModel = appViewModel { graph ->
        BlockEscapePlayViewModel(
            graph.bundledFiles,
            graph.sessionRepository,
            graph.statsRepository,
            graph.playHistoryRepository,
            graph.journeyProgressRepository,
            graph.badgeRepository,
            graph.appForeground,
            graph.motionSettings,
            difficulty,
            resume,
            journey,
        )
    }
    CollectPuzzleResult(viewModel.result, onFinished)
    CollectPuzzleResult(viewModel.restarts) { onRestarted() }
    val finishing by viewModel.finishing.collectAsStateWithLifecycle()
    val haptics = rememberHapticController()

    val session = viewModel.session
    GamePlayScaffold(
        gameId = GameId.BLOCKESCAPE,
        timer = PlayTimer.Elapsed(viewModel.elapsedSeconds.toInt()),
        onBack = onBack,
        onEndPuzzle = { viewModel.endPuzzle() },
        onRestartPuzzle = if (journey != null) viewModel::restartPuzzle else null,
        restarting = viewModel.restarting,
        finishing = finishing,
        foldRows = BLOCKESCAPE_SIZE,
        foldCols = BLOCKESCAPE_SIZE,
        difficulty = session?.let { Difficulty.fromKey(it.puzzle.difficulty) },
        hint = PlayHintAction(
            active = viewModel.hint != null,
            busy = viewModel.isComputingHint,
            contentDescription = stringResource(R.string.blockescape_hint_content_description),
            onClick = viewModel::onHint,
        ),
        info = if (session == null) {
            emptyList()
        } else {
            listOf(
                InfoItem.Label(stringResource(R.string.blockescape_moves, session.moves)),
                InfoItem.Label(stringResource(R.string.blockescape_fewest, session.puzzle.minMoves)),
            )
        },
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
                playFresh = !resume,
                zoomable = false,
            ) {
                if (session != null) {
                    BlockEscapeGrid(
                        blocks = session.blocks,
                        hint = viewModel.hint,
                        onMove = viewModel::onMove,
                        onPickUp = { haptics.tapFeedback() },
                    )
                }
            }
        },
    )
}
