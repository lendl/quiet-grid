package com.quietgrid.app.games.wordsearch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.ui.components.CollectPuzzleResult
import com.quietgrid.app.ui.components.ElapsedTimerText
import com.quietgrid.app.ui.components.EndPuzzleDialog
import com.quietgrid.app.ui.components.EndPuzzleIconButton
import com.quietgrid.app.ui.components.GameBackButton
import com.quietgrid.app.ui.components.PuzzleBoardContainer
import com.quietgrid.app.ui.components.PuzzleLanguageFlag

@Composable
fun WordSearchPlayScreen(
    difficulty: Difficulty,
    resume: Boolean,
    onBack: () -> Unit,
    onFinished: (WordSearchResult) -> Unit,
) {
    val viewModel = hiltViewModel<WordSearchPlayViewModel, WordSearchPlayViewModel.Factory>(
        creationCallback = { factory -> factory.create(difficulty, resume) },
    )

    CollectPuzzleResult(viewModel.result, onFinished)

    var showEndDialog by remember { mutableStateOf(false) }
    var isBoardZoomed by remember { mutableStateOf(false) }
    var resetZoomTrigger by remember { mutableStateOf(0) }
    val session = viewModel.session

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            GameBackButton(onBack)
            Row(
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (session != null) {
                    if (!session.hiddenWordSolved) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(R.string.wordsearch_theme_label), style = MaterialTheme.typography.labelSmall)
                            Text(wordSearchThemeLabel(session.puzzle.hiddenWord.clue), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
                if (isBoardZoomed) {
                    IconButton(onClick = { resetZoomTrigger++ }) {
                        Icon(
                            imageVector = Icons.Filled.ZoomOutMap,
                            contentDescription = stringResource(R.string.wordsearch_reset_zoom),
                        )
                    }
                }
                IconButton(onClick = { viewModel.toggleNextMoveHint() }) {
                    Icon(
                        imageVector = Icons.Filled.Lightbulb,
                        contentDescription = stringResource(
                            if (viewModel.nextMoveHint != null) R.string.wordsearch_next_move_hide else R.string.wordsearch_next_move_show,
                        ),
                        tint = if (viewModel.nextMoveHint != null) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (session != null) {
                    PuzzleLanguageFlag(session.puzzle.locale)
                }
                EndPuzzleIconButton(onClick = { showEndDialog = true })
            }
        }

        val hint = viewModel.nextMoveHint
        if (hint != null) {
            Card(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Column(Modifier.padding(12.dp)) {
                    val title = when (hint) {
                        is WSNextMoveHint.FindWord -> stringResource(R.string.wordsearch_next_move_title, hint.word)
                        is WSNextMoveHint.FindHiddenLetter -> stringResource(R.string.wordsearch_hidden_word_next_letter_title, hint.clue)
                    }
                    val body = when (hint) {
                        is WSNextMoveHint.FindWord -> stringResource(R.string.wordsearch_next_move_body)
                        is WSNextMoveHint.FindHiddenLetter -> stringResource(R.string.wordsearch_hidden_word_next_letter_body)
                    }
                    Text(title, style = MaterialTheme.typography.titleSmall)
                    Text(body, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        ElapsedTimerText(
            viewModel.elapsedSeconds.toInt(),
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )

        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
            val sideBySide = shouldShowWordListSideBySide(maxWidth, maxHeight)
            if (sideBySide) {
                Row(Modifier.fillMaxSize()) {
                    PuzzleBoardContainer(
                        visible = session != null,
                        playFresh = !resume,
                        onZoomChange = { isBoardZoomed = it },
                        resetTrigger = resetZoomTrigger,
                    ) {
                        if (session != null) {
                            Box(Modifier.fillMaxSize().padding(8.dp)) {
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
                    if (session != null) {
                        Spacer(Modifier.width(12.dp))
                        WordSearchWordStrip(
                            session = session,
                            onToggleHiddenWordMode = viewModel::onToggleHiddenWordMode,
                            wrongHiddenWordTap = viewModel.wrongHiddenWordTap,
                            isSideColumn = true,
                            modifier = Modifier.width(160.dp).fillMaxHeight(),
                        )
                    }
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    PuzzleBoardContainer(
                        visible = session != null,
                        playFresh = !resume,
                        onZoomChange = { isBoardZoomed = it },
                        resetTrigger = resetZoomTrigger,
                    ) {
                        if (session != null) {
                            Box(Modifier.fillMaxSize().padding(8.dp)) {
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

                    if (session != null) {
                        WordSearchWordStrip(
                            session = session,
                            onToggleHiddenWordMode = viewModel::onToggleHiddenWordMode,
                            wrongHiddenWordTap = viewModel.wrongHiddenWordTap,
                        )
                    }
                }
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
