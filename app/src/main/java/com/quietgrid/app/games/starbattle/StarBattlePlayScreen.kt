package com.quietgrid.app.games.starbattle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
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
import com.quietgrid.app.core.GameId
import com.quietgrid.app.ui.components.CollectPuzzleResult
import com.quietgrid.app.ui.components.PuzzleBoardContainer
import com.quietgrid.app.ui.components.rememberHapticController
import com.quietgrid.app.ui.play.GamePlayScaffold
import com.quietgrid.app.ui.play.InfoItem
import com.quietgrid.app.ui.play.PlayTimer
import com.quietgrid.app.ui.play.heartsInfo

@Composable
fun StarBattlePlayScreen(
    difficulty: Difficulty,
    resume: Boolean,
    onBack: () -> Unit,
    onFinished: (StarBattleResult) -> Unit,
) {
    val viewModel = hiltViewModel<StarBattlePlayViewModel, StarBattlePlayViewModel.Factory>(
        creationCallback = { factory -> factory.create(difficulty, resume) },
    )
    CollectPuzzleResult(viewModel.result, onFinished)

    val session = viewModel.session
    val haptics = rememberHapticController()
    var boardFlashTrigger by remember { mutableStateOf(0) }
    var wrongOpenTrigger by remember { mutableStateOf(0) }
    LaunchedEffect(viewModel.lastOpenEvent) {
        val event = viewModel.lastOpenEvent
        if (event != null) {
            if (event.wasCorrect) {
                haptics.correctFeedback()
            } else {
                haptics.incorrectFeedback()
                boardFlashTrigger++
                wrongOpenTrigger++
            }
        }
    }

    GamePlayScaffold(
        gameId = GameId.STARBATTLE,
        timer = PlayTimer.Elapsed(viewModel.elapsedSeconds.toInt()),
        onBack = onBack,
        onEndPuzzle = { viewModel.endPuzzle() },
        difficulty = session?.let { Difficulty.fromKey(it.puzzle.difficulty) },
        info = if (session == null) {
            emptyList()
        } else {
            listOf(
                starsPerGroupInfo(session.puzzle.k),
                heartsInfo(session.lives, STARBATTLE_STARTING_LIVES, wrongOpenTrigger),
            )
        },
        board = {
            PuzzleBoardContainer(
                visible = session != null,
                playFresh = !resume,
                zoomable = false,
                flashTrigger = boardFlashTrigger,
            ) {
                session?.let { current ->
                    StarBattleGrid(
                        size = current.puzzle.size,
                        regions = current.puzzle.regions,
                        cells = current.cells,
                        onCellTap = viewModel::onCellTap,
                        onCellDrag = viewModel::onCellDrag,
                        onCellDoubleTap = viewModel::onCellDoubleTap,
                    )
                }
            }
        },
    )
}

internal fun starsPerGroupInfo(k: Int): InfoItem = InfoItem.Custom {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(14.dp))
        Text(stringResource(R.string.starbattle_stars_per_group, k))
    }
}
