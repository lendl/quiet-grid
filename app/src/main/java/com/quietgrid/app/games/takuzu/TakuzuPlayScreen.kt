package com.quietgrid.app.games.takuzu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.data.AppContainer
import com.quietgrid.app.ui.components.BoardEntrance
import com.quietgrid.app.ui.components.ElapsedTimerText
import com.quietgrid.app.ui.components.GameBackButton
import com.quietgrid.app.ui.components.ZoomableBoardSurface

@Composable
fun TakuzuPlayScreen(
    difficulty: Difficulty,
    resume: Boolean,
    onBack: () -> Unit,
    onFinished: (TakuzuResult) -> Unit,
) {
    val context = LocalContext.current.applicationContext
    val viewModel: TakuzuPlayViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                TakuzuPlayViewModel(
                    appContext = context,
                    sessionRepository = AppContainer.sessionRepository,
                    statsRepository = AppContainer.statsRepository,
                    requestedDifficulty = difficulty,
                    resume = resume,
                )
            }
        },
    )

    LaunchedEffect(viewModel) {
        viewModel.result.collect { result -> onFinished(result) }
    }

    var showEndDialog by remember { mutableStateOf(false) }
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
                    val displayDifficulty = Difficulty.fromKey(session.puzzle.difficulty)
                    Column {
                        Text(stringResource(R.string.takuzu_size_label), style = MaterialTheme.typography.labelSmall)
                        Text("${session.puzzle.size}x${session.puzzle.size}", style = MaterialTheme.typography.titleMedium)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(stringResource(R.string.takuzu_difficulty_label), style = MaterialTheme.typography.labelSmall)
                        Text(stringResource(takuzuDifficultyLabelRes(displayDifficulty)), style = MaterialTheme.typography.titleMedium)
                    }
                }
                IconButton(onClick = { viewModel.toggleNextMoveHint() }) {
                    Icon(
                        imageVector = Icons.Filled.Lightbulb,
                        contentDescription = stringResource(
                            if (viewModel.nextMoveHint != null) R.string.takuzu_hint_hide else R.string.takuzu_hint_show,
                        ),
                        tint = if (viewModel.nextMoveHint != null) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = { showEndDialog = true }) {
                    Text(stringResource(R.string.common_end_puzzle))
                }
            }
        }

        ElapsedTimerText(
            viewModel.elapsedSeconds.toInt(),
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )

        val hint = viewModel.nextMoveHint
        if (hint != null && hint !is TakuzuNextMoveHint.Paused) {
            val (hintTitle, hintBody) = resolveTakuzuHintText(hint)
            Card(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text(hintTitle, style = MaterialTheme.typography.titleSmall)
                    Text(hintBody, style = MaterialTheme.typography.bodySmall)
                }
            }
        } else if (hint is TakuzuNextMoveHint.Paused) {
            Card(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text(stringResource(R.string.takuzu_hint_paused_title), style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(R.string.takuzu_hint_paused_body), style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (session != null) {
                BoardEntrance(playFresh = !resume, modifier = Modifier.fillMaxSize()) {
                    ZoomableBoardSurface(Modifier.fillMaxSize()) {
                        Box(Modifier.fillMaxSize().padding(12.dp)) {
                            TakuzuBoard(
                                board = session.board,
                                isGiven = session.isGiven,
                                finishedCells = session.finishedCells,
                                feedbackCorrectRows = viewModel.feedbackCorrectRows,
                                feedbackCorrectCols = viewModel.feedbackCorrectCols,
                                feedbackIncorrectRows = viewModel.feedbackIncorrectRows,
                                feedbackIncorrectCols = viewModel.feedbackIncorrectCols,
                                size = session.puzzle.size,
                                onCellPress = viewModel::onCellPress,
                                hintEvidenceCells = hint?.evidenceCells?.toSet() ?: emptySet(),
                                hintTargetCells = hint?.targetCells?.associate { (r, c, v) -> (r to c) to v } ?: emptyMap(),
                                hintHighlightRows = hint?.highlightRows?.toSet() ?: emptySet(),
                                hintHighlightCols = hint?.highlightCols?.toSet() ?: emptySet(),
                            )
                        }
                    }
                }
            }
        }
    }

    if (showEndDialog) {
        AlertDialog(
            onDismissRequest = { showEndDialog = false },
            title = { Text(stringResource(R.string.puzzle_play_end_dialog_title)) },
            text = { Text(stringResource(R.string.puzzle_play_end_dialog_message)) },
            confirmButton = {
                Button(onClick = {
                    showEndDialog = false
                    viewModel.endPuzzle()
                }) { Text(stringResource(R.string.puzzle_play_end_dialog_confirm)) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEndDialog = false }) { Text(stringResource(R.string.common_cancel)) }
            },
        )
    }
}

@Composable
private fun lineLabel(kind: TakuzuLineKind, index: Int): String = stringResource(
    if (kind == TakuzuLineKind.ROW) R.string.takuzu_hint_line_row else R.string.takuzu_hint_line_column,
    index + 1,
)

@Composable
private fun lineKindWord(kind: TakuzuLineKind, plural: Boolean): String = stringResource(
    when {
        kind == TakuzuLineKind.ROW && !plural -> R.string.takuzu_hint_kind_row
        kind == TakuzuLineKind.ROW && plural -> R.string.takuzu_hint_kind_rows
        kind == TakuzuLineKind.COLUMN && !plural -> R.string.takuzu_hint_kind_column
        else -> R.string.takuzu_hint_kind_columns
    },
)

@Composable
private fun resolveTakuzuHintText(hint: TakuzuNextMoveHint): Pair<String, String> = when (hint) {
    is TakuzuNextMoveHint.Paused -> stringResource(R.string.takuzu_hint_paused_title) to stringResource(R.string.takuzu_hint_paused_body)

    is TakuzuNextMoveHint.FindPairs -> {
        val label = lineLabel(hint.lineKind, hint.lineIndex)
        stringResource(R.string.takuzu_hint_next_move_title, label) to
            stringResource(R.string.takuzu_hint_find_pairs_body, hint.targetValue, hint.repeatedValue, label)
    }

    is TakuzuNextMoveHint.AvoidTrios -> {
        val label = lineLabel(hint.lineKind, hint.lineIndex)
        stringResource(R.string.takuzu_hint_next_move_title, label) to
            stringResource(R.string.takuzu_hint_avoid_trios_body, hint.targetValue, label, hint.repeatedValue)
    }

    is TakuzuNextMoveHint.CompleteLines -> {
        val label = lineLabel(hint.lineKind, hint.lineIndex)
        val suffix = if (hint.filledCount == 1) "" else stringResource(R.string.takuzu_hint_digit_plural_suffix)
        stringResource(R.string.takuzu_hint_next_move_title, label) to
            stringResource(R.string.takuzu_hint_complete_lines_body, hint.targetValue, label, hint.filledCount, hint.filledValue, suffix)
    }

    is TakuzuNextMoveHint.EliminateFilledLines -> {
        val label = lineLabel(hint.lineKind, hint.lineIndex)
        val matchingLabel = lineLabel(hint.lineKind, hint.matchingLineIndex)
        val kindWord = lineKindWord(hint.lineKind, plural = false)
        stringResource(R.string.takuzu_hint_next_move_title, label) to
            stringResource(R.string.takuzu_hint_eliminate_filled_lines_body, hint.targetValue, label, matchingLabel, kindWord)
    }

    is TakuzuNextMoveHint.AvoidTriosRepair -> {
        val label = lineLabel(hint.lineKind, hint.lineIndex)
        stringResource(R.string.takuzu_hint_repair_avoid_trios_title, label) to
            stringResource(R.string.takuzu_hint_repair_avoid_trios_body, label, hint.repeatedValue)
    }

    is TakuzuNextMoveHint.CompleteLinesRepair -> {
        val label = lineLabel(hint.lineKind, hint.lineIndex)
        val suffix = if (hint.filledCount == 1) "" else stringResource(R.string.takuzu_hint_digit_plural_suffix)
        stringResource(R.string.takuzu_hint_repair_complete_lines_title, label) to
            stringResource(R.string.takuzu_hint_repair_complete_lines_body, label, hint.filledCount, hint.filledValue, suffix, hint.limit)
    }

    is TakuzuNextMoveHint.EliminateFilledLinesRepair -> {
        val firstLabel = lineLabel(hint.lineKind, hint.firstLineIndex)
        val secondLabel = lineLabel(hint.lineKind, hint.secondLineIndex)
        val kindWord = lineKindWord(hint.lineKind, plural = true)
        stringResource(R.string.takuzu_hint_repair_eliminate_filled_lines_title, kindWord) to
            stringResource(R.string.takuzu_hint_repair_eliminate_filled_lines_body, kindWord, firstLabel, secondLabel)
    }
}
