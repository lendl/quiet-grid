package com.quietgrid.app.games.sequencememory

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.quietgrid.app.ui.play.InfoItem
import com.quietgrid.app.ui.play.PlayTimer

@Composable
fun SequenceMemoryPlayScreen(
    difficulty: Difficulty,
    resume: Boolean,
    onBack: () -> Unit,
    onFinished: (SequenceMemoryResult) -> Unit,
) {
    val viewModel = hiltViewModel<SequenceMemoryPlayViewModel, SequenceMemoryPlayViewModel.Factory>(
        creationCallback = { factory -> factory.create(difficulty, resume) },
    )
    CollectPuzzleResult(viewModel.result, onFinished)

    val session = viewModel.session
    val haptics = rememberHapticController()
    LaunchedEffect(viewModel.roundCompleteTrigger) {
        if (viewModel.roundCompleteTrigger > 0) haptics.correctFeedback()
    }
    LaunchedEffect(viewModel.wrongTapTrigger) {
        if (viewModel.wrongTapTrigger > 0) haptics.incorrectFeedback()
    }

    GamePlayScaffold(
        gameId = GameId.SEQUENCEMEMORY,
        timer = PlayTimer.Elapsed(viewModel.elapsedSeconds.toInt()),
        onBack = onBack,
        onEndPuzzle = { viewModel.endPuzzle() },
        difficulty = session?.let { Difficulty.fromKey(it.difficulty) },
        info = if (session == null) {
            emptyList()
        } else {
            val statusRes = when (session.phase) {
                SequenceMemoryPhase.SHOWING -> R.string.sequencememory_status_watch
                SequenceMemoryPhase.INPUT, SequenceMemoryPhase.WON -> R.string.sequencememory_status_repeat
                SequenceMemoryPhase.FAILED -> R.string.sequencememory_status_wrong
            }
            listOf(
                InfoItem.Label(stringResource(R.string.sequencememory_round_label, session.sequence.size, sequenceMemoryConfig(session).targetLength)),
                InfoItem.Label(stringResource(statusRes), emphasized = true),
            )
        },
        board = {
            PuzzleBoardContainer(visible = session != null, playFresh = !resume, zoomable = false) {
                if (session != null) {
                    val config = sequenceMemoryConfig(session)
                    Box(Modifier.padding(24.dp)) {
                        SequenceMemoryGrid(
                            padCount = config.padCount,
                            columns = config.columns,
                            litPad = viewModel.litPad,
                            wrongPad = session.wrongPad,
                            enabled = session.phase == SequenceMemoryPhase.INPUT,
                            onPadTap = { pad ->
                                haptics.tapFeedback()
                                viewModel.onPadTap(pad)
                            },
                        )
                    }
                }
            }
        },
    )
}
