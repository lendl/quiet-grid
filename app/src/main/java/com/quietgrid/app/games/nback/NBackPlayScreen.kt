package com.quietgrid.app.games.nback

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.hilt.navigation.compose.hiltViewModel
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.ui.components.CollectPuzzleResult
import com.quietgrid.app.ui.components.ElapsedTimerText
import com.quietgrid.app.ui.components.EndPuzzleDialog
import com.quietgrid.app.ui.components.EndPuzzleIconButton
import com.quietgrid.app.ui.components.ChunkyPressButton
import com.quietgrid.app.ui.components.GameBackButton
import com.quietgrid.app.ui.components.PuzzleBoardContainer
import com.quietgrid.app.ui.components.rememberHapticController

@Composable
fun NBackPlayScreen(
    difficulty: Difficulty,
    resume: Boolean,
    onBack: () -> Unit,
    onFinished: (NBackResult) -> Unit,
) {
    val viewModel = hiltViewModel<NBackPlayViewModel, NBackPlayViewModel.Factory>(
        creationCallback = { factory -> factory.create(difficulty, resume) },
    )
    CollectPuzzleResult(viewModel.result, onFinished)

    var showEndDialog by remember { mutableStateOf(false) }
    val session = viewModel.session
    val haptics = rememberHapticController()
    var tapPulse by remember { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            GameBackButton(onBack)
            Row(
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (session != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.nback_n_label, session.config.n),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Text(
                            stringResource(
                                R.string.nback_progress_label,
                                (session.currentIndex + 1).coerceAtLeast(0).coerceAtMost(session.trials.size),
                                session.trials.size,
                            ),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
                EndPuzzleIconButton(onClick = { showEndDialog = true })
            }
        }

        ElapsedTimerText(
            viewModel.elapsedSeconds.toInt(),
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )

        PuzzleBoardContainer(visible = session != null, playFresh = !resume, zoomable = false) {
            if (session != null) {
                val litPosition = if (session.showStimulus) {
                    session.trials.getOrNull(session.currentIndex)?.position
                } else {
                    null
                }
                NBackGrid(litPosition = litPosition)
            }
        }

        val tapped = session?.trials?.getOrNull(session.currentIndex)?.responded == true
        LaunchedEffect(tapPulse) {
            if (tapPulse > 0) {
                delay(NBACK_TAP_FEEDBACK_MS)
                tapPulse = 0
            }
        }
        val buttonColor by animateColorAsState(
            targetValue = if (tapPulse > 0 || tapped) {
                MaterialTheme.colorScheme.tertiary
            } else {
                MaterialTheme.colorScheme.primary
            },
            label = "nbackMatchButtonColor",
        )
        ChunkyPressButton(
            onClick = {
                haptics.tapFeedback()
                viewModel.onMatchTap()
                tapPulse++
            },
            enabled = session != null,
            containerColor = buttonColor,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        ) {
            Text(stringResource(R.string.nback_match_button))
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
