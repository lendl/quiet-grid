package com.quietgrid.app.games.hashi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.quietgrid.app.ui.play.PlayTimer

private const val HASHI_ZOOM_MIN_COLUMNS = 10

@Composable
fun HashiPlayScreen(
    difficulty: Difficulty,
    resume: Boolean,
    onBack: () -> Unit,
    onFinished: (HashiResult) -> Unit,
) {
    val viewModel = hiltViewModel<HashiPlayViewModel, HashiPlayViewModel.Factory>(
        creationCallback = { factory -> factory.create(difficulty, resume) },
    )
    CollectPuzzleResult(viewModel.result, onFinished)
    val finishing by viewModel.finishing.collectAsState()

    val session = viewModel.session
    val haptics = rememberHapticController()
    LaunchedEffect(viewModel.lastTapEvent) {
        val event = viewModel.lastTapEvent ?: return@LaunchedEffect
        if (event.blocked) haptics.incorrectFeedback() else haptics.tapFeedback()
    }

    val islandStates = session?.let { hashiIslandStates(it) }
    GamePlayScaffold(
        gameId = GameId.HASHI,
        timer = PlayTimer.Elapsed(viewModel.elapsedSeconds.toInt()),
        onBack = onBack,
        onEndPuzzle = { viewModel.endPuzzle() },
        finishing = finishing,
        foldRows = session?.puzzle?.rows,
        foldCols = session?.puzzle?.cols,
        difficulty = session?.let { Difficulty.fromKey(it.puzzle.difficulty) },
        hint = PlayHintAction(
            active = false,
            busy = viewModel.isComputingHint,
            contentDescription = stringResource(R.string.hashi_hint_content_description),
            onClick = viewModel::onHint,
        ),
        info = if (islandStates == null) {
            emptyList()
        } else {
            listOf(
                InfoItem.Label(
                    stringResource(R.string.hashi_islands_done, islandStates.count { it == HashiIslandState.COMPLETE }, islandStates.size),
                ),
            )
        },
        board = {
            PuzzleBoardContainer(
                visible = session != null,
                playFresh = !resume,
                zoomable = (session?.puzzle?.cols ?: 0) >= HASHI_ZOOM_MIN_COLUMNS,
            ) {
                session?.let { current ->
                    HashiGrid(
                        board = current.board,
                        bridges = current.bridges,
                        islandStates = islandStates.orEmpty(),
                        highlight = current.highlight,
                        lastTap = viewModel.lastTapEvent,
                        onEdgeTap = viewModel::onEdgeTap,
                    )
                }
            }
        },
    )
}
