package com.quietgrid.app.games.takuzu

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.quietgrid.app.R
import java.time.LocalDate
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.ui.components.CollectPuzzleResult
import com.quietgrid.app.ui.components.PuzzleBoardContainer
import com.quietgrid.app.ui.components.rememberHapticController
import com.quietgrid.app.ui.play.GamePlayScaffold
import com.quietgrid.app.ui.play.PlayHintAction
import com.quietgrid.app.ui.play.PlayHintMessage
import com.quietgrid.app.ui.play.PlayTimer

@Composable
fun TakuzuPlayScreen(
    difficulty: Difficulty,
    resume: Boolean,
    dailyDate: LocalDate? = null,
    onBack: () -> Unit,
    onFinished: (TakuzuResult) -> Unit,
) {
    val viewModel = hiltViewModel<TakuzuPlayViewModel, TakuzuPlayViewModel.Factory>(
        creationCallback = { factory -> factory.create(difficulty, resume, dailyDate) },
    )
    CollectPuzzleResult(viewModel.result, onFinished)

    val session = viewModel.session

    val haptics = rememberHapticController()
    var flashTrigger by remember { mutableStateOf(0) }
    LaunchedEffect(viewModel.feedbackEvent) {
        val event = viewModel.feedbackEvent
        if (event != null) {
            if (event.incorrect) {
                haptics.incorrectFeedback()
                flashTrigger++
            } else if (event.correct) {
                haptics.correctFeedback()
            }
        }
    }

    val hint = viewModel.nextMoveHint
    GamePlayScaffold(
        gameId = GameId.TAKUZU,
        timer = PlayTimer.Elapsed(viewModel.elapsedSeconds.toInt()),
        onBack = onBack,
        onEndPuzzle = { viewModel.endPuzzle() },
        difficulty = session?.let { Difficulty.fromKey(it.puzzle.difficulty) },
        hint = PlayHintAction(
            active = hint != null,
            busy = viewModel.isComputingHint,
            contentDescription = stringResource(if (hint != null) R.string.takuzu_hint_hide else R.string.takuzu_hint_show),
            onClick = { viewModel.toggleNextMoveHint() },
        ),
        hintMessage = hint?.let { current ->
            val (title, body) = resolveTakuzuHintText(current)
            PlayHintMessage(title, body)
        },
        board = {
            PuzzleBoardContainer(
                visible = session != null,
                playFresh = !resume,
                flashTrigger = flashTrigger,
            ) {
                if (session != null) {
                    Box(Modifier.padding(12.dp)) {
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
        },
    )
}

@Composable
internal fun lineLabel(kind: TakuzuLineKind, index: Int): String = stringResource(
    if (kind == TakuzuLineKind.ROW) R.string.takuzu_hint_line_row else R.string.takuzu_hint_line_column,
    index + 1,
)

@Composable
internal fun lineKindWord(kind: TakuzuLineKind, plural: Boolean): String = stringResource(
    when {
        kind == TakuzuLineKind.ROW && !plural -> R.string.takuzu_hint_kind_row
        kind == TakuzuLineKind.ROW && plural -> R.string.takuzu_hint_kind_rows
        kind == TakuzuLineKind.COLUMN && !plural -> R.string.takuzu_hint_kind_column
        else -> R.string.takuzu_hint_kind_columns
    },
)

@Composable
internal fun resolveTakuzuHintText(hint: TakuzuNextMoveHint): Pair<String, String> = when (hint) {
    is TakuzuNextMoveHint.Paused -> stringResource(R.string.takuzu_hint_paused_title) to stringResource(R.string.takuzu_hint_paused_body)

    is TakuzuNextMoveHint.FindPairs -> {
        val label = lineLabel(hint.lineKind, hint.lineIndex)
        stringResource(R.string.takuzu_rule_1_title) to
            stringResource(R.string.takuzu_hint_find_pairs_body, hint.targetValue, hint.repeatedValue, label)
    }

    is TakuzuNextMoveHint.AvoidTrios -> {
        val label = lineLabel(hint.lineKind, hint.lineIndex)
        stringResource(R.string.takuzu_rule_1_title) to
            stringResource(R.string.takuzu_hint_avoid_trios_body, hint.targetValue, label, hint.repeatedValue)
    }

    is TakuzuNextMoveHint.CompleteLines -> {
        val label = lineLabel(hint.lineKind, hint.lineIndex)
        val suffix = if (hint.filledCount == 1) "" else stringResource(R.string.takuzu_hint_digit_plural_suffix)
        stringResource(R.string.takuzu_rule_2_title) to
            stringResource(R.string.takuzu_hint_complete_lines_body, hint.targetValue, label, hint.filledCount, hint.filledValue, suffix)
    }

    is TakuzuNextMoveHint.EliminateFilledLines -> {
        val label = lineLabel(hint.lineKind, hint.lineIndex)
        val matchingLabel = lineLabel(hint.lineKind, hint.matchingLineIndex)
        val kindWord = lineKindWord(hint.lineKind, plural = false)
        stringResource(R.string.takuzu_rule_3_title) to
            stringResource(R.string.takuzu_hint_eliminate_filled_lines_body, hint.targetValue, label, matchingLabel, kindWord)
    }

    is TakuzuNextMoveHint.AvoidTriosRepair -> {
        val label = lineLabel(hint.lineKind, hint.lineIndex)
        stringResource(R.string.takuzu_hint_repair_title, stringResource(R.string.takuzu_rule_1_title)) to
            stringResource(R.string.takuzu_hint_repair_avoid_trios_body, label, hint.repeatedValue)
    }

    is TakuzuNextMoveHint.CompleteLinesRepair -> {
        val label = lineLabel(hint.lineKind, hint.lineIndex)
        val suffix = if (hint.filledCount == 1) "" else stringResource(R.string.takuzu_hint_digit_plural_suffix)
        stringResource(R.string.takuzu_hint_repair_title, stringResource(R.string.takuzu_rule_2_title)) to
            stringResource(R.string.takuzu_hint_repair_complete_lines_body, label, hint.filledCount, hint.filledValue, suffix, hint.limit)
    }

    is TakuzuNextMoveHint.EliminateFilledLinesRepair -> {
        val firstLabel = lineLabel(hint.lineKind, hint.firstLineIndex)
        val secondLabel = lineLabel(hint.lineKind, hint.secondLineIndex)
        val kindWord = lineKindWord(hint.lineKind, plural = true)
        stringResource(R.string.takuzu_hint_repair_title, stringResource(R.string.takuzu_rule_3_title)) to
            stringResource(R.string.takuzu_hint_repair_eliminate_filled_lines_body, kindWord, firstLabel, secondLabel)
    }

    is TakuzuNextMoveHint.RevealFromSolution ->
        stringResource(R.string.takuzu_hint_reveal_title) to stringResource(R.string.takuzu_hint_reveal_body, hint.value)
}
