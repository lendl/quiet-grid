package com.quietgrid.app.games.nback

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.ui.components.ChunkyPressButton
import com.quietgrid.app.ui.components.CollectPuzzleResult
import com.quietgrid.app.ui.components.PuzzleBoardContainer
import com.quietgrid.app.ui.components.rememberHapticController
import com.quietgrid.app.ui.play.GamePlayScaffold
import com.quietgrid.app.ui.play.InfoItem
import com.quietgrid.app.ui.play.PlaySizeClass
import com.quietgrid.app.ui.play.PlayTimer
import com.quietgrid.app.ui.play.isTablet
import kotlinx.coroutines.delay

private fun nbackDockHeight(sizeClass: PlaySizeClass) = if (sizeClass.isTablet) 96.dp else 76.dp

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
    val finishing by viewModel.finishing.collectAsState()

    val session = viewModel.session
    val haptics = rememberHapticController()
    var tapPulse by remember { mutableIntStateOf(0) }
    LaunchedEffect(tapPulse) {
        if (tapPulse > 0) {
            delay(NBACK_TAP_FEEDBACK_MS)
            tapPulse = 0
        }
    }
    val tapped = session?.trials?.getOrNull(session.currentIndex)?.responded == true
    val buttonColor by animateColorAsState(
        targetValue = if (tapPulse > 0 || tapped) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
        label = "nbackMatchButtonColor",
    )

    GamePlayScaffold(
        gameId = GameId.NBACK,
        timer = PlayTimer.Elapsed(viewModel.elapsedSeconds.toInt()),
        onBack = onBack,
        onEndPuzzle = { viewModel.endPuzzle() },
        finishing = finishing,
        difficulty = session?.difficulty,
        info = if (session == null) {
            emptyList()
        } else {
            listOf(
                InfoItem.Label(stringResource(R.string.nback_n_label, session.config.n), emphasized = true),
                InfoItem.Label(
                    stringResource(
                        R.string.nback_progress_label,
                        (session.currentIndex + 1).coerceAtLeast(0).coerceAtMost(session.trials.size),
                        session.trials.size,
                    ),
                ),
            )
        },
        dockHeight = ::nbackDockHeight,
        dock = {
            ChunkyPressButton(
                onClick = {
                    haptics.tapFeedback()
                    viewModel.onMatchTap()
                    tapPulse++
                },
                enabled = session != null,
                containerColor = buttonColor,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Text(stringResource(R.string.nback_match_button))
            }
        },
        board = {
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
        },
    )
}
