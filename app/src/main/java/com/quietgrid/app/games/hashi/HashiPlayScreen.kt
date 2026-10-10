package com.quietgrid.app.games.hashi

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lightbulb
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.ui.components.CollectPuzzleResult
import com.quietgrid.app.ui.components.ElapsedTimerText
import com.quietgrid.app.ui.components.EndPuzzleDialog
import com.quietgrid.app.ui.components.EndPuzzleIconButton
import com.quietgrid.app.ui.components.GameBackButton
import com.quietgrid.app.ui.components.LogoGridLoader
import com.quietgrid.app.ui.components.PuzzleBoardContainer
import com.quietgrid.app.ui.components.rememberHapticController

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

    var showEndDialog by remember { mutableStateOf(false) }
    val session = viewModel.session
    val haptics = rememberHapticController()
    LaunchedEffect(viewModel.lastTapEvent) {
        val event = viewModel.lastTapEvent ?: return@LaunchedEffect
        if (event.blocked) haptics.incorrectFeedback() else haptics.tapFeedback()
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            GameBackButton(onBack)
            Spacer(Modifier.weight(1f))
            if (session != null) {
                val states = hashiIslandStates(session)
                Text(
                    stringResource(
                        R.string.hashi_progress_label,
                        states.count { it == HashiIslandState.COMPLETE },
                        states.size,
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(end = 8.dp),
                )
                IconButton(onClick = viewModel::onHint, enabled = !viewModel.isComputingHint) {
                    if (viewModel.isComputingHint) {
                        LogoGridLoader(size = 20.dp)
                    } else {
                        Icon(Icons.Outlined.Lightbulb, contentDescription = stringResource(R.string.hashi_hint_content_description))
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
            zoomable = (session?.puzzle?.cols ?: 0) >= HASHI_ZOOM_MIN_COLUMNS,
        ) {
            session?.let { current ->
                HashiGrid(
                    board = current.board,
                    bridges = current.bridges,
                    islandStates = hashiIslandStates(current),
                    highlight = current.highlight,
                    lastTap = viewModel.lastTapEvent,
                    onEdgeTap = viewModel::onEdgeTap,
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
