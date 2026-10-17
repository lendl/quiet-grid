package com.quietgrid.app.games.game2048

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
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
import com.quietgrid.app.ui.play.InfoItem
import com.quietgrid.app.ui.play.PlayTimer
import kotlin.math.abs
import kotlin.math.max

private val SWIPE_THRESHOLD_DP = 24.dp

@Composable
fun Game2048PlayScreen(
    difficulty: Difficulty,
    resume: Boolean,
    onBack: () -> Unit,
    onFinished: (Game2048Result) -> Unit,
) {
    val viewModel = hiltViewModel<Game2048PlayViewModel, Game2048PlayViewModel.Factory>(
        creationCallback = { factory -> factory.create(difficulty, resume) },
    )
    CollectPuzzleResult(viewModel.result, onFinished)

    val session = viewModel.session
    val haptics = rememberHapticController()
    val density = LocalDensity.current
    val thresholdPx = with(density) { SWIPE_THRESHOLD_DP.toPx() }

    GamePlayScaffold(
        gameId = GameId.GAME_2048,
        timer = PlayTimer.Elapsed(viewModel.elapsedSeconds.toInt()),
        onBack = onBack,
        onEndPuzzle = { viewModel.endPuzzle() },
        modifier = Modifier.pointerInput(Unit) {
            var totalDrag = Offset.Zero
            detectDragGestures(
                onDragStart = { totalDrag = Offset.Zero },
                onDrag = { change, dragAmount ->
                    totalDrag += dragAmount
                    change.consume()
                },
                onDragEnd = {
                    val dx = totalDrag.x
                    val dy = totalDrag.y
                    if (max(abs(dx), abs(dy)) > thresholdPx) {
                        val direction = if (abs(dx) > abs(dy)) {
                            if (dx > 0) Game2048Direction.RIGHT else Game2048Direction.LEFT
                        } else {
                            if (dy > 0) Game2048Direction.DOWN else Game2048Direction.UP
                        }
                        haptics.tapFeedback()
                        viewModel.onSwipe(direction)
                    }
                },
            )
        },
        difficulty = session?.let { Difficulty.fromKey(it.puzzle.difficulty) },
        info = if (session == null) {
            emptyList()
        } else {
            listOf(InfoItem.Label(stringResource(R.string.game2048_score_value, session.board.score), emphasized = true))
        },
        board = {
            PuzzleBoardContainer(visible = session != null, playFresh = !resume, zoomable = false) {
                if (session != null) {
                    Game2048Grid(board = session.board, lastMove = viewModel.lastMove)
                }
            }
        },
    )
}
