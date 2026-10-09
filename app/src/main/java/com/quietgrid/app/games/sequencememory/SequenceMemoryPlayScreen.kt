package com.quietgrid.app.games.sequencememory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.ui.components.CollectPuzzleResult
import com.quietgrid.app.ui.components.ElapsedTimerText
import com.quietgrid.app.ui.components.EndPuzzleDialog
import com.quietgrid.app.ui.components.EndPuzzleIconButton
import com.quietgrid.app.ui.components.GameBackButton
import com.quietgrid.app.ui.components.PuzzleBoardContainer
import com.quietgrid.app.ui.components.rememberHapticController

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

    var showEndDialog by remember { mutableStateOf(false) }
    val session = viewModel.session
    val haptics = rememberHapticController()
    LaunchedEffect(viewModel.roundCompleteTrigger) {
        if (viewModel.roundCompleteTrigger > 0) haptics.correctFeedback()
    }
    LaunchedEffect(viewModel.wrongTapTrigger) {
        if (viewModel.wrongTapTrigger > 0) haptics.incorrectFeedback()
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            GameBackButton(onBack)
            Row(
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (session != null) {
                    Text(
                        stringResource(R.string.sequencememory_round_label, session.sequence.size, sequenceMemoryConfig(session).targetLength),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                EndPuzzleIconButton(onClick = { showEndDialog = true })
            }
        }

        ElapsedTimerText(
            viewModel.elapsedSeconds.toInt(),
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )

        if (session != null) {
            val statusRes = when (session.phase) {
                SequenceMemoryPhase.SHOWING -> R.string.sequencememory_status_watch
                SequenceMemoryPhase.INPUT, SequenceMemoryPhase.WON -> R.string.sequencememory_status_repeat
                SequenceMemoryPhase.FAILED -> R.string.sequencememory_status_wrong
            }
            Text(
                stringResource(statusRes),
                style = MaterialTheme.typography.titleMedium,
                color = if (session.phase == SequenceMemoryPhase.FAILED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            )
        }

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
    }

    EndPuzzleDialog(
        visible = showEndDialog,
        onDismiss = { showEndDialog = false },
        onConfirm = {
            showEndDialog = false
            viewModel.endPuzzle()
        },
    )
}
