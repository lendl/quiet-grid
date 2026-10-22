package com.quietgrid.app.games.animaldoku

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.ui.components.CollectPuzzleResult
import com.quietgrid.app.ui.components.PuzzleBoardContainer
import com.quietgrid.app.ui.components.rememberHapticController
import com.quietgrid.app.ui.play.GamePlayScaffold
import com.quietgrid.app.ui.play.PlayTimer
import com.quietgrid.app.ui.play.heartsInfo
import java.time.LocalDate

@Composable
fun AnimalDokuPlayScreen(
    difficulty: Difficulty,
    resume: Boolean,
    dailyDate: LocalDate? = null,
    onBack: () -> Unit,
    onFinished: (AnimalDokuResult) -> Unit,
) {
    val viewModel = hiltViewModel<AnimalDokuPlayViewModel, AnimalDokuPlayViewModel.Factory>(
        creationCallback = { factory -> factory.create(difficulty, resume, dailyDate) },
    )
    CollectPuzzleResult(viewModel.result, onFinished)
    val finishing by viewModel.finishing.collectAsState()

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
        gameId = GameId.ANIMALDOKU,
        timer = PlayTimer.Elapsed(viewModel.elapsedSeconds.toInt()),
        onBack = onBack,
        onEndPuzzle = { viewModel.endPuzzle() },
        finishing = finishing,
        foldRows = session?.puzzle?.size,
        foldCols = session?.puzzle?.size,
        difficulty = session?.let { Difficulty.fromKey(it.puzzle.difficulty) },
        info = if (session == null) emptyList() else listOf(heartsInfo(session.lives, ANIMALDOKU_STARTING_LIVES, wrongOpenTrigger)),
        board = {
            PuzzleBoardContainer(
                visible = session != null,
                playFresh = !resume,
                zoomable = false,
                flashTrigger = boardFlashTrigger,
            ) {
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
        },
    )
}
