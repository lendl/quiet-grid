package com.quietgrid.app.games.animaldoku

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.ui.components.CollectPuzzleResult
import com.quietgrid.app.ui.components.PuzzleBoardContainer
import com.quietgrid.app.ui.components.rememberHapticController
import com.quietgrid.app.ui.play.GamePlayScaffold
import com.quietgrid.app.ui.play.PlayHintAction
import com.quietgrid.app.ui.play.PlayHintMessage
import com.quietgrid.app.ui.play.PlayTimer
import com.quietgrid.app.ui.play.heartsInfo
import com.quietgrid.app.ui.seasonal.rememberHalloweenActive
import com.quietgrid.engine.animaldoku.AnimalDokuHint
import com.quietgrid.engine.animaldoku.AnimalDokuUnit
import com.quietgrid.engine.animaldoku.AnimalDokuUnitKind
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
    val emojiPalette = animalDokuEmojiPalette(rememberHalloweenActive())
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

    val hint = viewModel.hint
    GamePlayScaffold(
        gameId = GameId.ANIMALDOKU,
        timer = PlayTimer.Elapsed(viewModel.elapsedSeconds.toInt()),
        onBack = onBack,
        onEndPuzzle = { viewModel.endPuzzle() },
        finishing = finishing,
        foldRows = session?.puzzle?.size,
        foldCols = session?.puzzle?.size,
        difficulty = session?.let { Difficulty.fromKey(it.puzzle.difficulty) },
        hint = PlayHintAction(
            active = hint != null,
            busy = viewModel.isComputingHint,
            contentDescription = stringResource(if (hint != null) R.string.animaldoku_hint_hide else R.string.animaldoku_hint_show),
            onClick = viewModel::toggleHint,
        ),
        hintMessage = hint?.let { animalDokuHintMessage(it) },
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
                        hintHighlight = hint?.let { animalDokuHintHighlight(it, current.puzzle.regions) },
                        onCellTap = viewModel::onCellTap,
                        onCellDrag = viewModel::onCellDrag,
                        onCellDoubleTap = viewModel::onCellDoubleTap,
                        emojiPalette = emojiPalette,
                    )
                }
            }
        },
    )
}

@Composable
private fun unitsLabel(units: List<AnimalDokuUnit>): String {
    val kind = units.first().kind
    if (kind == AnimalDokuUnitKind.REGION) {
        return if (units.size == 1) {
            stringResource(R.string.animaldoku_hint_highlighted_region)
        } else {
            stringResource(R.string.animaldoku_hint_highlighted_regions, units.size)
        }
    }
    val numbers = units.map { (it.index + 1).toString() }
    if (numbers.size == 1) {
        return stringResource(if (kind == AnimalDokuUnitKind.ROW) R.string.animaldoku_hint_row else R.string.animaldoku_hint_column, units.first().index + 1)
    }
    val joined = stringResource(R.string.animaldoku_hint_list_and, numbers.dropLast(1).joinToString(", "), numbers.last())
    return stringResource(if (kind == AnimalDokuUnitKind.ROW) R.string.animaldoku_hint_rows else R.string.animaldoku_hint_columns, joined)
}

@Composable
private fun resolveAnimalDokuHintCopy(copy: AnimalDokuHintCopy): PlayHintMessage {
    val args = copy.unitArgs.map { unitsLabel(it) }
    return PlayHintMessage(stringResource(copy.titleRes), stringResource(copy.bodyRes, *args.toTypedArray()))
}

@Composable
internal fun animalDokuHintMessage(hint: AnimalDokuHint): PlayHintMessage = resolveAnimalDokuHintCopy(animalDokuHintCopy(hint))

@Composable
internal fun animalDokuChainFrameMessage(hint: AnimalDokuHint, frame: Int): PlayHintMessage =
    resolveAnimalDokuHintCopy(animalDokuChainFrameCopy(hint, frame))
