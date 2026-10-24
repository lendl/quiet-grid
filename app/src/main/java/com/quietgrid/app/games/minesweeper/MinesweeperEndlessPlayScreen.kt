package com.quietgrid.app.games.minesweeper

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.quietgrid.app.R
import com.quietgrid.app.core.GameId
import com.quietgrid.app.ui.components.CollectPuzzleResult
import com.quietgrid.app.ui.components.LogoGridLoader
import com.quietgrid.app.ui.components.PuzzleBoardContainer
import com.quietgrid.app.ui.components.rememberHapticController
import com.quietgrid.app.ui.play.GamePlayScaffold
import com.quietgrid.app.ui.play.InfoItem
import com.quietgrid.app.ui.play.PlayTimer
import com.quietgrid.app.ui.play.heartsInfo
import com.quietgrid.engine.minesweeper.endless.WorldCell
import kotlinx.coroutines.delay

private const val BUILD_LOADER_DELAY_MS = 300L

@Composable
fun MinesweeperEndlessPlayScreen(
    resume: Boolean,
    onBack: () -> Unit,
    onFinished: (MinesweeperEndlessResult) -> Unit,
) {
    val viewModel = hiltViewModel<MinesweeperEndlessViewModel, MinesweeperEndlessViewModel.Factory>(
        creationCallback = { factory -> factory.create(resume) },
    )
    CollectPuzzleResult(viewModel.result, onFinished)
    val haptics = rememberHapticController()

    MinesweeperEndlessPlayContent(
        session = viewModel.session,
        timer = PlayTimer.Elapsed(viewModel.elapsedSeconds),
        playFresh = !resume,
        building = viewModel.building,
        revealMines = viewModel.revealMines,
        onBack = onBack,
        onEndRun = { viewModel.endRun() },
        onReveal = { cell -> haptics.tapFeedback(); viewModel.onReveal(cell) },
        onToggleFlag = { cell -> haptics.tapFeedback(); viewModel.onToggleFlag(cell) },
        onCameraChanged = { viewModel.onCameraChanged(it) },
    )
}

@Composable
internal fun MinesweeperEndlessPlayContent(
    session: MinesweeperEndlessSession?,
    timer: PlayTimer?,
    playFresh: Boolean,
    building: Boolean,
    revealMines: Boolean,
    onBack: () -> Unit,
    onEndRun: () -> Unit,
    onReveal: (WorldCell) -> Unit,
    onToggleFlag: (WorldCell) -> Unit,
    onCameraChanged: (MinesweeperEndlessCamera) -> Unit,
) {
    GamePlayScaffold(
        gameId = GameId.MINESWEEPER,
        timer = timer,
        onBack = onBack,
        onEndPuzzle = onEndRun,
        boardFillsSlot = true,
        rulesContent = MinesweeperEndlessQuickStart,
        info = if (session == null) {
            emptyList()
        } else {
            buildList {
                add(heartsInfo(session.hearts, MINESWEEPER_ENDLESS_HEARTS, session.heartShake))
                add(InfoItem.Label(stringResource(R.string.minesweeper_endless_cleared_count, session.score), emphasized = true))
                if (session.bestAtStart > 0) {
                    add(InfoItem.Label(stringResource(R.string.minesweeper_endless_best_value, session.bestAtStart)))
                }
            }
        },
        board = {
            PuzzleBoardContainer(visible = session != null, playFresh = playFresh, zoomable = false, showFrame = false) {
                if (session != null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        MinesweeperEndlessGrid(
                            world = session.world,
                            camera = session.camera,
                            revealMines = revealMines,
                            onCameraChanged = onCameraChanged,
                            onReveal = onReveal,
                            onToggleFlag = onToggleFlag,
                            modifier = Modifier.fillMaxSize(),
                        )
                        if (building) DelayedBuildLoader()
                    }
                }
            }
        },
    )
}

@Composable
private fun DelayedBuildLoader() {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(BUILD_LOADER_DELAY_MS)
        visible = true
    }
    if (visible) LogoGridLoader()
}
