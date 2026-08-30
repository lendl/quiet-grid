// app/src/main/java/com/quietgrid/app/games/arrowescape/ArrowEscapePlayScreen.kt
package com.quietgrid.app.games.arrowescape

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
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
fun ArrowEscapePlayScreen(
    difficulty: Difficulty,
    resume: Boolean,
    onBack: () -> Unit,
    onFinished: (ArrowEscapeResult) -> Unit,
) {
    val viewModel = hiltViewModel<ArrowEscapePlayViewModel, ArrowEscapePlayViewModel.Factory>(
        creationCallback = { factory -> factory.create(difficulty, resume) },
    )

    CollectPuzzleResult(viewModel.result, onFinished)

    var showEndDialog by remember { mutableStateOf(false) }
    var visibleBounds by remember { mutableStateOf<Rect?>(null) }
    var panTarget by remember { mutableStateOf<Offset?>(null) }
    val session = viewModel.session

    val haptics = rememberHapticController()
    LaunchedEffect(viewModel.lastTapEvent) {
        val event = viewModel.lastTapEvent
        if (event != null) {
            if (event.removed) haptics.correctFeedback() else haptics.incorrectFeedback()
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            GameBackButton(onBack)
            Text(
                stringResource(arrowEscapeDifficultyLabelRes(session?.let { Difficulty.fromKey(it.puzzle.difficulty) } ?: difficulty)),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 12.dp).weight(1f),
            )
            if (session != null) {
                Row(Modifier.padding(end = 8.dp)) {
                    repeat(ARROW_ESCAPE_STARTING_LIVES) { index ->
                        Icon(
                            if (index < session.lives) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(end = 4.dp),
                        )
                    }
                }
                IconButton(onClick = viewModel::onHint, enabled = !viewModel.isComputingHint) {
                    if (viewModel.isComputingHint) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Outlined.Lightbulb, contentDescription = null)
                    }
                }
            }
            EndPuzzleIconButton(onClick = { showEndDialog = true })
        }

        ElapsedTimerText(
            viewModel.elapsedSeconds.toInt(),
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )

        PuzzleBoardContainer(
            visible = session != null,
            playFresh = !resume,
            zoomable = true,
            panTarget = panTarget,
            onVisibleBoundsChange = { visibleBounds = it },
        ) {
            session?.let { current ->
                ArrowEscapeGrid(
                    puzzle = current.puzzle,
                    removedIndices = current.removedIndices,
                    selectedIndex = current.selectedIndex,
                    blockedIndex = viewModel.lastBlockedIndex,
                    visibleBounds = visibleBounds,
                    onRequestPan = { panTarget = it },
                    onPieceTap = viewModel::onPieceTap,
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
