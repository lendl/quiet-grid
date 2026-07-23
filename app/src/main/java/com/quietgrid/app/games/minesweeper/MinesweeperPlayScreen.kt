package com.quietgrid.app.games.minesweeper

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
fun MinesweeperPlayScreen(
    difficulty: Difficulty,
    resume: Boolean,
    onBack: () -> Unit,
    onFinished: (MinesweeperResult) -> Unit,
) {
    val viewModel: MinesweeperPlayViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                MinesweeperPlayViewModel(
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
                    val minesLeft = maxOf(0, session.board.mines - countFlaggedMinesweeperCells(session.board))
                    Column {
                        Text(stringResource(R.string.minesweeper_mines_left_label), style = MaterialTheme.typography.labelSmall)
                        Text(minesLeft.toString(), style = MaterialTheme.typography.titleMedium)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(stringResource(R.string.minesweeper_difficulty_label), style = MaterialTheme.typography.labelSmall)
                        Text(stringResource(minesweeperDifficultyLabelRes(displayDifficulty)), style = MaterialTheme.typography.titleMedium)
                    }
                }
                IconButton(onClick = { viewModel.toggleNextMoveHint() }) {
                    Icon(
                        imageVector = Icons.Filled.Lightbulb,
                        contentDescription = stringResource(
                            if (viewModel.nextMoveHint != null) R.string.minesweeper_hint_hide else R.string.minesweeper_hint_show,
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
        if (hint != null) {
            val (hintTitle, hintBody) = resolveMinesweeperHintText(hint)
            Card(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text(hintTitle, style = MaterialTheme.typography.titleSmall)
                    Text(hintBody, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (session != null) {
                BoardEntrance(playFresh = !resume, modifier = Modifier.fillMaxSize()) {
                    ZoomableBoardSurface(Modifier.fillMaxSize()) {
                        Box(Modifier.fillMaxSize().padding(8.dp)) {
                            MinesweeperGrid(
                                board = session.board,
                                onReveal = viewModel::onReveal,
                                onToggleFlag = viewModel::onToggleFlag,
                                hintEvidenceCells = hint?.evidenceCells?.toSet() ?: emptySet(),
                                hintTargetCells = hint?.targetCells?.toSet() ?: emptySet(),
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

    val title = if (pattern.pattern == MinesweeperHintPattern.ONLY_ONE_POSSIBLE_MINE) {
        stringResource(R.string.minesweeper_hint_title_compare_clues)
    } else {
        stringResource(R.string.minesweeper_hint_title_near_clue, clueLabel ?: "")
    }
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
    }
    return title to body
}
