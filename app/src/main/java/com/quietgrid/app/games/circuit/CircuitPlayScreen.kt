package com.quietgrid.app.games.circuit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.quietgrid.app.ui.play.PlayHintMessage
import com.quietgrid.app.ui.play.PlayTimer

private const val CIRCUIT_ZOOM_MIN_COLUMNS = 8

@Composable
fun CircuitPlayScreen(
    difficulty: Difficulty,
    resume: Boolean,
    onBack: () -> Unit,
    onFinished: (CircuitResult) -> Unit,
    journey: JourneyRequest? = null,
    onRestarted: () -> Unit = {},
) {
    val viewModel = appViewModel { graph ->
        CircuitPlayViewModel(
            graph.appContext,
            graph.sessionRepository,
            graph.statsRepository,
            graph.playHistoryRepository,
            graph.journeyProgressRepository,
            graph.badgeRepository,
            difficulty,
            resume,
            journey,
        )
    }
    CollectPuzzleResult(viewModel.result, onFinished)
    CollectPuzzleResult(viewModel.restarts) { onRestarted() }
    val finishing by viewModel.finishing.collectAsStateWithLifecycle()

    val session = viewModel.session
    val haptics = rememberHapticController()
    LaunchedEffect(viewModel.lastTapEvent) {
        val event = viewModel.lastTapEvent ?: return@LaunchedEffect
        if (event.blocked) haptics.incorrectFeedback() else haptics.tapFeedback()
    }

    val lit = session?.let { circuitLit(it) }
    GamePlayScaffold(
        gameId = GameId.CIRCUIT,
        timer = PlayTimer.Elapsed(viewModel.elapsedSeconds.toInt()),
        onBack = onBack,
        onEndPuzzle = { viewModel.endPuzzle() },
        onRestartPuzzle = if (journey != null) viewModel::restartPuzzle else null,
        restarting = viewModel.restarting,
        finishing = finishing,
        foldRows = session?.puzzle?.rows,
        foldCols = session?.puzzle?.cols,
        difficulty = session?.let { Difficulty.fromKey(it.puzzle.difficulty) },
        hint = PlayHintAction(
            active = viewModel.hint != null,
            busy = viewModel.isComputingHint,
            contentDescription = stringResource(if (viewModel.hint != null) R.string.circuit_hint_hide else R.string.circuit_hint_show),
            onClick = viewModel::toggleHint,
        ),
        hintMessage = viewModel.hint?.let {
            PlayHintMessage(stringResource(circuitHintTitleRes(it)), stringResource(circuitHintBodyRes(it)))
        },
        info = if (session == null || lit == null) {
            emptyList()
        } else {
            listOf(
                InfoItem.Label(stringResource(R.string.circuit_tiles_lit, lit.count { it }, lit.size)),
                InfoItem.Label(stringResource(R.string.circuit_turns, session.moves)),
            )
        },
        board = {
            PuzzleBoardContainer(
                visible = session != null,
                playFresh = !resume,
                zoomable = (session?.puzzle?.cols ?: 0) >= CIRCUIT_ZOOM_MIN_COLUMNS,
            ) {
                if (session != null && lit != null) {
                    CircuitGrid(
                        board = session.board,
                        startMasks = session.puzzle.start,
                        turns = session.turns,
                        locked = session.locked,
                        lit = lit,
                        hintCell = viewModel.hint?.cell,
                        lastTap = viewModel.lastTapEvent,
                        onTileTap = viewModel::onTileTap,
                        onTileLongPress = viewModel::onTileLongPress,
                    )
                }
            }
        },
    )
}
