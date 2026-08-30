// app/src/main/java/com/quietgrid/app/games/animaldoku/AnimalDokuPlayScreen.kt
package com.quietgrid.app.games.animaldoku

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.ui.components.CollectPuzzleResult
import com.quietgrid.app.ui.components.ElapsedTimerText
import com.quietgrid.app.ui.components.EndPuzzleDialog
import com.quietgrid.app.ui.components.EndPuzzleIconButton
import com.quietgrid.app.ui.components.GameBackButton
import com.quietgrid.app.ui.components.PuzzleBoardContainer
import com.quietgrid.app.ui.components.rememberHapticController

@Composable
fun AnimalDokuPlayScreen(
    difficulty: Difficulty,
    resume: Boolean,
    onBack: () -> Unit,
    onFinished: (AnimalDokuResult) -> Unit,
) {
    val viewModel = hiltViewModel<AnimalDokuPlayViewModel, AnimalDokuPlayViewModel.Factory>(
        creationCallback = { factory -> factory.create(difficulty, resume) },
    )

    CollectPuzzleResult(viewModel.result, onFinished)

    var showEndDialog by remember { mutableStateOf(false) }
    val session = viewModel.session
    val haptics = rememberHapticController()
    LaunchedEffect(viewModel.lastOpenEvent) {
        val event = viewModel.lastOpenEvent
        if (event != null) {
            if (event.wasCorrect) haptics.correctFeedback() else haptics.incorrectFeedback()
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            GameBackButton(onBack)
            Text(
                stringResource(animalDokuDifficultyLabelRes(session?.let { Difficulty.fromKey(it.puzzle.difficulty) } ?: difficulty)),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 12.dp).weight(1f),
            )
            if (session != null) {
                var wrongOpenTrigger by remember { mutableStateOf(0) }
                LaunchedEffect(viewModel.lastOpenEvent) {
                    val event = viewModel.lastOpenEvent
                    if (event != null && !event.wasCorrect) wrongOpenTrigger++
                }
                Row(Modifier.padding(end = 8.dp)) {
                    repeat(ANIMALDOKU_STARTING_LIVES) { index ->
                        AnimalDokuHeartIcon(
                            filled = index < session.lives,
                            shakeTrigger = if (index == session.lives) wrongOpenTrigger else 0,
                            modifier = Modifier.padding(end = 4.dp),
                        )
                    }
                }
            }
            EndPuzzleIconButton(onClick = { showEndDialog = true })
        }

        ElapsedTimerText(
            viewModel.elapsedSeconds.toInt(),
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )

        PuzzleBoardContainer(visible = session != null, playFresh = !resume, zoomable = false) {
            session?.let { current ->
                AnimalDokuGrid(
                    size = current.puzzle.size,
                    regions = current.puzzle.regions,
                    cells = current.cells,
                    onCellTap = viewModel::onCellTap,
                    onCellDrag = viewModel::onCellDrag,
                    onCellDoubleTap = viewModel::onCellDoubleTap,
                )
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

@Composable
private fun AnimalDokuHeartIcon(filled: Boolean, shakeTrigger: Int, modifier: Modifier = Modifier) {
    val shakeX = remember { Animatable(0f) }
    LaunchedEffect(shakeTrigger) {
        if (shakeTrigger > 0) {
            shakeX.snapTo(0f)
            shakeX.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 350
                    0f at 0
                    -6f at 60
                    6f at 120
                    -4f at 190
                    4f at 260
                    0f at 350
                },
            )
        }
    }
    Icon(
        if (filled) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.error,
        modifier = modifier.graphicsLayer(translationX = shakeX.value),
    )
}
