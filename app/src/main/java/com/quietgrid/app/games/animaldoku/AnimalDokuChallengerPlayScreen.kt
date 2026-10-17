package com.quietgrid.app.games.animaldoku

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.quietgrid.app.R
import com.quietgrid.app.core.GameId
import com.quietgrid.app.session.ChallengerResult
import com.quietgrid.app.ui.components.CollectPuzzleResult
import com.quietgrid.app.ui.components.PuzzleBoardContainer
import com.quietgrid.app.ui.components.rememberHapticController
import com.quietgrid.app.ui.play.GamePlayScaffold
import com.quietgrid.app.ui.play.InfoItem
import com.quietgrid.app.ui.play.PlayTimer
import com.quietgrid.app.ui.play.heartsInfo

@Composable
fun AnimalDokuChallengerPlayScreen(
    onFinished: (ChallengerResult) -> Unit,
) {
    val viewModel = hiltViewModel<AnimalDokuChallengerViewModel>()

    CollectPuzzleResult(viewModel.result, onFinished)

    BackHandler { viewModel.endRun() }

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

    val info = if (session == null) {
        emptyList()
    } else {
        listOf(
            heartsInfo(session.puzzleSession.lives, ANIMALDOKU_STARTING_LIVES, wrongOpenTrigger),
            InfoItem.Label("${stringResource(R.string.animaldoku_challenger_tier_label)}: ${stringResource(animalDokuDifficultyLabelRes(session.tier))}"),
            InfoItem.Label("${stringResource(R.string.animaldoku_challenger_solved_label)}: ${session.puzzlesSolved}"),
        )
    }

    GamePlayScaffold(
        gameId = GameId.ANIMALDOKU,
        timer = session?.let { PlayTimer.Countdown(it.secondsRemaining.toInt()) },
        onBack = viewModel::endRun,
        onEndPuzzle = viewModel::endRun,
        info = info,
        board = {
            PuzzleBoardContainer(
                visible = session != null,
                playFresh = true,
                zoomable = false,
                flashTrigger = boardFlashTrigger,
            ) {
                session?.let { current ->
                    key(current.puzzleSession.puzzle.id) {
                        AnimalDokuGrid(
                            size = current.puzzleSession.puzzle.size,
                            regions = current.puzzleSession.puzzle.regions,
                            cells = current.puzzleSession.cells,
                            onCellTap = viewModel::onCellTap,
                            onCellDrag = viewModel::onCellDrag,
                            onCellDoubleTap = viewModel::onCellDoubleTap,
                        )
                    }
                }
            }
        },
    )
}
