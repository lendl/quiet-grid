package com.quietgrid.app.games.takuzu

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
fun TakuzuChallengerPlayScreen(
    onFinished: (ChallengerResult) -> Unit,
) {
    val viewModel = hiltViewModel<TakuzuChallengerViewModel>()

    CollectPuzzleResult(viewModel.result, onFinished)

    BackHandler { viewModel.endRun() }

    val session = viewModel.session
    val haptics = rememberHapticController()
    var boardFlashTrigger by remember { mutableStateOf(0) }
    LaunchedEffect(viewModel.wrongMoveTrigger) {
        if (viewModel.wrongMoveTrigger > 0) {
            haptics.incorrectFeedback()
            boardFlashTrigger++
        }
    }

    val info = if (session == null) {
        emptyList()
    } else {
        listOf(
            heartsInfo(session.livesRemaining, TAKUZU_CHALLENGER_STARTING_LIVES),
            InfoItem.Label("${stringResource(R.string.takuzu_challenger_tier_label)}: ${stringResource(takuzuDifficultyLabelRes(session.tier))}"),
            InfoItem.Label("${stringResource(R.string.takuzu_challenger_solved_label)}: ${session.puzzlesSolved}"),
        )
    }

    GamePlayScaffold(
        gameId = GameId.TAKUZU,
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
                    TakuzuBoard(
                        board = current.puzzleSession.board,
                        isGiven = current.puzzleSession.isGiven,
                        finishedCells = current.puzzleSession.finishedCells,
                        feedbackCorrectRows = emptySet(),
                        feedbackCorrectCols = emptySet(),
                        feedbackIncorrectRows = emptySet(),
                        feedbackIncorrectCols = emptySet(),
                        size = current.puzzleSession.puzzle.size,
                        onCellPress = viewModel::onCellPress,
                    )
                }
            }
        },
    )
}
