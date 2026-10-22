package com.quietgrid.app.games.wordsearch

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.themes.themeLabelRes
import com.quietgrid.app.ui.components.CollectPuzzleResult
import com.quietgrid.app.ui.components.PuzzleBoardContainer
import com.quietgrid.app.ui.components.rememberHapticController
import com.quietgrid.app.ui.play.GamePlayScaffold
import com.quietgrid.app.ui.play.InfoItem
import com.quietgrid.app.ui.play.PlayHintAction
import com.quietgrid.app.ui.play.PlayHintMessage
import com.quietgrid.app.ui.play.PlayTimer
import com.quietgrid.app.ui.play.puzzleLanguageInfo
import java.time.LocalDate

@Composable
fun WordSearchPlayScreen(
    difficulty: Difficulty,
    resume: Boolean,
    dailyDate: LocalDate? = null,
    onBack: () -> Unit,
    onFinished: (WordSearchResult) -> Unit,
) {
    val viewModel = hiltViewModel<WordSearchPlayViewModel, WordSearchPlayViewModel.Factory>(
        creationCallback = { factory -> factory.create(difficulty, resume, dailyDate) },
    )
    CollectPuzzleResult(viewModel.result, onFinished)
    val finishing by viewModel.finishing.collectAsState()

    var isBoardZoomed by remember { mutableStateOf(false) }
    var resetZoomTrigger by remember { mutableStateOf(0) }
    val session = viewModel.session
    val haptics = rememberHapticController()
    var flashTrigger by remember { mutableStateOf(0) }
    LaunchedEffect(viewModel.selectionEvent) {
        val event = viewModel.selectionEvent
        if (event != null) {
            if (event.wasCorrect) {
                haptics.correctFeedback()
            } else {
                haptics.incorrectFeedback()
                flashTrigger++
            }
        }
    }
    LaunchedEffect(viewModel.wrongHiddenWordTapTrigger) {
        if (viewModel.wrongHiddenWordTapTrigger > 0) {
            haptics.incorrectFeedback()
            flashTrigger++
        }
    }

    val hint = viewModel.nextMoveHint
    GamePlayScaffold(
        gameId = GameId.WORDSEARCH,
        timer = PlayTimer.Elapsed(viewModel.elapsedSeconds.toInt()),
        onBack = onBack,
        onEndPuzzle = { viewModel.endPuzzle() },
        finishing = finishing,
        foldRows = session?.puzzle?.rows,
        foldCols = session?.puzzle?.cols,
        difficulty = session?.let { Difficulty.fromKey(it.puzzle.difficulty) },
        hint = PlayHintAction(
            active = hint != null,
            busy = viewModel.isComputingHint,
            contentDescription = stringResource(if (hint != null) R.string.wordsearch_next_move_hide else R.string.wordsearch_next_move_show),
            onClick = { viewModel.toggleNextMoveHint() },
        ),
        hintMessage = hint?.let { wordSearchHintMessage(it) },
        info = if (session == null) {
            emptyList()
        } else {
            listOfNotNull(
                InfoItem.Label(stringResource(themeLabelRes(session.puzzle.hiddenWord.clue)), emphasized = true),
                InfoItem.Label(stringResource(R.string.wordsearch_words_found, session.foundWordIds.size, session.puzzle.words.size)),
                puzzleLanguageInfo(session.puzzle.locale),
            )
        },
        dockHeight = ::wordSearchDockHeight,
        dock = {
            if (session != null) {
                WordSearchDock(
                    session = session,
                    onToggleHiddenWordMode = viewModel::onToggleHiddenWordMode,
                    wrongHiddenWordTap = viewModel.wrongHiddenWordTap,
                )
            }
        },
        board = {
            PuzzleBoardContainer(
                visible = session != null,
                playFresh = !resume,
                flashTrigger = flashTrigger,
                onZoomChange = { isBoardZoomed = it },
                resetTrigger = resetZoomTrigger,
            ) {
                if (session != null) {
                    Box(Modifier.padding(8.dp)) {
                        WordSearchGrid(
                            puzzle = session.puzzle,
                            foundWordIds = session.foundWordIds,
                            tempSelection = session.tempSelection,
                            hiddenWordMode = session.hiddenWordMode,
                            hiddenWordProgress = session.hiddenWordProgress,
                            onCellTap = viewModel::onCellTap,
                            onCellDragStart = viewModel::onCellDragStart,
                            onCellDragMove = viewModel::onCellDragMove,
                            onCellDragEnd = viewModel::onCellDragEnd,
                            onHiddenWordTap = viewModel::onHiddenWordCellTap,
                            nextMoveEvidenceCells = hint?.let {
                                when (it) {
                                    is WSNextMoveHint.FindWord -> it.evidenceCells
                                    is WSNextMoveHint.FindHiddenLetter -> it.evidenceCells
                                }
                            } ?: emptyList(),
                            nextMoveTargetCells = hint?.let {
                                when (it) {
                                    is WSNextMoveHint.FindWord -> it.targetCells
                                    is WSNextMoveHint.FindHiddenLetter -> it.targetCells
                                }
                            } ?: emptyList(),
                            wrongSelectionCells = viewModel.wrongSelectionCells,
                        )
                    }
                }
            }
            if (isBoardZoomed) {
                FilledTonalIconButton(
                    onClick = { resetZoomTrigger++ },
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(36.dp),
                ) {
                    Icon(Icons.Filled.ZoomOutMap, contentDescription = stringResource(R.string.wordsearch_reset_zoom), modifier = Modifier.size(18.dp))
                }
            }
        },
    )
}

@Composable
private fun wordSearchHintMessage(hint: WSNextMoveHint): PlayHintMessage {
    val title = when (hint) {
        is WSNextMoveHint.FindWord -> stringResource(R.string.wordsearch_next_move_title, hint.word)
        is WSNextMoveHint.FindHiddenLetter -> if (hint.revealedWord != null) {
            stringResource(R.string.wordsearch_hidden_word_next_letter_title, hint.revealedWord)
        } else {
            stringResource(R.string.wordsearch_hidden_word_theme_title, stringResource(themeLabelRes(hint.clue)))
        }
    }
    val body = when (hint) {
        is WSNextMoveHint.FindWord -> stringResource(R.string.wordsearch_next_move_body)
        is WSNextMoveHint.FindHiddenLetter -> stringResource(R.string.wordsearch_hidden_word_next_letter_body)
    }
    return PlayHintMessage(title, body)
}
