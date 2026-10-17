package com.quietgrid.app.games.minesweeper

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
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
import com.quietgrid.app.ui.play.PlayHintAction
import com.quietgrid.app.ui.play.PlayHintMessage
import com.quietgrid.app.ui.play.PlayTimer

@Composable
fun MinesweeperPlayScreen(
    difficulty: Difficulty,
    resume: Boolean,
    onBack: () -> Unit,
    onFinished: (MinesweeperResult) -> Unit,
) {
    val viewModel = hiltViewModel<MinesweeperPlayViewModel, MinesweeperPlayViewModel.Factory>(
        creationCallback = { factory -> factory.create(difficulty, resume) },
    )
    CollectPuzzleResult(viewModel.result, onFinished)

    val session = viewModel.session
    val haptics = rememberHapticController()

    val hint = viewModel.nextMoveHint
    GamePlayScaffold(
        gameId = GameId.MINESWEEPER,
        timer = PlayTimer.Elapsed(viewModel.elapsedSeconds.toInt()),
        onBack = onBack,
        onEndPuzzle = { viewModel.endPuzzle() },
        difficulty = session?.let { Difficulty.fromKey(it.puzzle.difficulty) },
        boardAspectRatio = session?.let { minesweeperBoardAspectRatio(it.puzzle) } ?: 1f,
        hint = PlayHintAction(
            active = hint != null,
            busy = viewModel.isComputingHint,
            contentDescription = stringResource(if (hint != null) R.string.minesweeper_hint_hide else R.string.minesweeper_hint_show),
            onClick = { viewModel.toggleNextMoveHint() },
        ),
        hintMessage = hint?.let { current ->
            val (title, body) = resolveMinesweeperHintText(current)
            PlayHintMessage(title, body)
        },
        info = if (session == null) {
            emptyList()
        } else {
            val minesLeft = maxOf(0, session.board.mines - countFlaggedMinesweeperCells(session.board))
            listOf(InfoItem.Label(stringResource(R.string.minesweeper_mines_left_count, minesLeft), emphasized = true))
        },
        board = {
            PuzzleBoardContainer(visible = session != null, playFresh = !resume) {
                if (session != null) {
                    Box(Modifier.padding(8.dp)) {
                        MinesweeperGrid(
                            board = session.board,
                            onReveal = { row, col -> haptics.tapFeedback(); viewModel.onReveal(row, col) },
                            onToggleFlag = { row, col -> haptics.tapFeedback(); viewModel.onToggleFlag(row, col) },
                            hintEvidenceCells = hint?.evidenceCells?.toSet() ?: emptySet(),
                            hintTargetCells = hint?.targetCells?.toSet() ?: emptySet(),
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun minesweeperCellLabel(cell: Pair<Int, Int>): String =
    stringResource(R.string.minesweeper_hint_cell, cell.first + 1, cell.second + 1)

@Composable
private fun resolveMinesweeperHintText(hint: MinesweeperNextMoveHint): Pair<String, String> {
    if (hint is MinesweeperGuessHint) {
        return stringResource(R.string.minesweeper_hint_guess_title) to stringResource(R.string.minesweeper_hint_guess_body)
    }
    val pattern = hint as MinesweeperPatternHint
    val tileLabel = stringResource(if (pattern.targetCells.size == 1) R.string.minesweeper_hint_tile_one else R.string.minesweeper_hint_tile_other)
    val mineLabel = stringResource(if (pattern.mineCount == 1) R.string.minesweeper_hint_mine_one else R.string.minesweeper_hint_mine_other)
    val clueLabel = pattern.clueCell?.let { minesweeperCellLabel(it) }
    val secondaryClueLabel = pattern.secondaryClueCell?.let { minesweeperCellLabel(it) }

    val title = stringResource(
        when (pattern.pattern) {
            MinesweeperHintPattern.ONLY_ONE_POSSIBLE_MINE -> R.string.minesweeper_hint_title_only_one_possible_mine
            MinesweeperHintPattern.GUARANTEED_SAFE_TILE -> R.string.minesweeper_hint_title_guaranteed_safe_tile
            MinesweeperHintPattern.SINGLE_MINE_LOGIC -> R.string.minesweeper_hint_title_single_mine_logic
            MinesweeperHintPattern.FULL_CLUE_RESOLUTION -> R.string.minesweeper_hint_title_full_clue_resolution
            MinesweeperHintPattern.ALL_MINES_ACCOUNTED_FOR -> R.string.minesweeper_hint_title_all_mines_accounted_for
            MinesweeperHintPattern.CLUE_PAIR -> R.string.minesweeper_hint_title_clue_pair
            MinesweeperHintPattern.MINES_LEFT -> R.string.minesweeper_hint_title_mines_left
        },
    )
    val body = when (pattern.pattern) {
        MinesweeperHintPattern.SINGLE_MINE_LOGIC -> stringResource(R.string.minesweeper_hint_single_mine_logic_body, tileLabel)
        MinesweeperHintPattern.ALL_MINES_ACCOUNTED_FOR -> stringResource(
            R.string.minesweeper_hint_all_mines_accounted_for_body, tileLabel, clueLabel ?: "", pattern.mineCount, mineLabel,
        )
        MinesweeperHintPattern.FULL_CLUE_RESOLUTION -> stringResource(R.string.minesweeper_hint_full_clue_resolution_body, tileLabel)
        MinesweeperHintPattern.ONLY_ONE_POSSIBLE_MINE -> stringResource(
            R.string.minesweeper_hint_only_one_possible_mine_body, tileLabel, clueLabel ?: "", secondaryClueLabel ?: "",
        )
        MinesweeperHintPattern.GUARANTEED_SAFE_TILE -> stringResource(R.string.minesweeper_hint_guaranteed_safe_tile_body, tileLabel)
        MinesweeperHintPattern.CLUE_PAIR -> stringResource(
            R.string.minesweeper_hint_clue_pair_body, tileLabel, clueLabel ?: "", secondaryClueLabel ?: "",
        )
        MinesweeperHintPattern.MINES_LEFT -> stringResource(R.string.minesweeper_hint_mines_left_body, tileLabel)
    }
    return title to body
}
