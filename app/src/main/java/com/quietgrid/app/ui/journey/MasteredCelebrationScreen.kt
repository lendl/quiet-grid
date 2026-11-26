package com.quietgrid.app.ui.journey

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.journey.JourneyMilestone
import com.quietgrid.app.ui.components.rememberReduceMotion

@Composable
fun MasteredCelebrationScreen(
    gameId: GameId,
    onClose: () -> Unit,
    onDone: () -> Unit,
    onViewMap: (Int) -> Unit,
) {
    val viewModel = journeyViewModel()
    val state by remember(viewModel, gameId) { viewModel.state(gameId) }.collectAsStateWithLifecycle(initialValue = null)
    val playSeconds by produceState<Map<String, Int>?>(initialValue = null, viewModel, gameId) { value = viewModel.playSecondsByStop(gameId) }
    val rows = state?.rows ?: return
    val seconds = playSeconds ?: return
    if (rows.isEmpty()) {
        LaunchedEffect(Unit) { onDone() }
        return
    }
    JourneyCelebration(
        gameId = gameId,
        milestone = JourneyMilestone.Mastered,
        rows = rows,
        secondsByStop = seconds,
        reduceMotion = rememberReduceMotion(),
        actions = JourneyCelebrationActions(onClose = onClose, onPlay = {}, onMap = onViewMap, onDone = onDone),
    )
}
