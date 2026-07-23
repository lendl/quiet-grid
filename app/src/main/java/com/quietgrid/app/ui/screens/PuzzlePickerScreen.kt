package com.quietgrid.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.difficultyColor
import com.quietgrid.app.data.AppContainer
import com.quietgrid.app.games.chimptest.chimpDifficultyDescriptionRes
import com.quietgrid.app.games.chimptest.chimpDifficultyLabelRes
import com.quietgrid.app.games.minesweeper.minesweeperDifficultyDescriptionRes
import com.quietgrid.app.games.minesweeper.minesweeperDifficultyLabelRes
import com.quietgrid.app.games.nonogram.nonogramDifficultyDescriptionRes
import com.quietgrid.app.games.nonogram.nonogramDifficultyLabelRes
import com.quietgrid.app.games.sudoku.sudokuDifficultyDescriptionRes
import com.quietgrid.app.games.sudoku.sudokuDifficultyLabelRes
import com.quietgrid.app.games.takuzu.takuzuDifficultyDescriptionRes
import com.quietgrid.app.games.takuzu.takuzuDifficultyLabelRes
import com.quietgrid.app.games.wordsearch.wordSearchDifficultyDescriptionRes
import com.quietgrid.app.games.wordsearch.wordSearchDifficultyLabelRes
import kotlinx.coroutines.flow.map

private enum class GamePageTab { PLAY, RULES, STATS }

@Composable
fun PuzzlePickerScreen(
    gameId: GameId,
    onPickDifficulty: (Difficulty) -> Unit,
    onResumeActiveGame: (GameId) -> Unit,
) {
    var selectedTab by remember { mutableStateOf(GamePageTab.PLAY) }

    Column(Modifier.fillMaxWidth()) {
        TabRow(
            selectedTabIndex = selectedTab.ordinal,
            containerColor = MaterialTheme.colorScheme.background,
        ) {
            Tab(
                selected = selectedTab == GamePageTab.PLAY,
                onClick = { selectedTab = GamePageTab.PLAY },
                text = { Text(stringResource(R.string.common_play)) },
            )
            Tab(
                selected = selectedTab == GamePageTab.RULES,
                onClick = { selectedTab = GamePageTab.RULES },
                text = { Text(stringResource(R.string.common_rules)) },
            )
            Tab(
                selected = selectedTab == GamePageTab.STATS,
                onClick = { selectedTab = GamePageTab.STATS },
                text = { Text(stringResource(R.string.common_stats)) },
            )
        }

        when (selectedTab) {
            GamePageTab.PLAY -> GamePlayPickerTab(gameId, onPickDifficulty, onResumeActiveGame)
            GamePageTab.RULES -> HowToPlayScreen(gameId)
            GamePageTab.STATS -> GameStatsTab(gameId)
        }
    }
}

@Composable
private fun GamePlayPickerTab(
    gameId: GameId,
    onPickDifficulty: (Difficulty) -> Unit,
    onResumeActiveGame: (GameId) -> Unit,
) {
    val activeGameKey by AppContainer.sessionRepository.activeSession
        .map { it?.gameId }
        .collectAsState(initial = null)
    var pendingDifficulty by remember { mutableStateOf<Difficulty?>(null) }

    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Column {
            Difficulty.entries.forEachIndexed { index, difficulty ->
                val labelRes = when (gameId) {
                    GameId.TAKUZU -> takuzuDifficultyLabelRes(difficulty)
                    GameId.NONOGRAM -> nonogramDifficultyLabelRes(difficulty)
                    GameId.MINESWEEPER -> minesweeperDifficultyLabelRes(difficulty)
                    GameId.SUDOKU -> sudokuDifficultyLabelRes(difficulty)
                    GameId.WORDSEARCH -> wordSearchDifficultyLabelRes(difficulty)
                    else -> chimpDifficultyLabelRes(difficulty)
                }
                val descriptionRes = when (gameId) {
                    GameId.CHIMPTEST -> chimpDifficultyDescriptionRes(difficulty)
                    GameId.TAKUZU -> takuzuDifficultyDescriptionRes(difficulty)
                    GameId.NONOGRAM -> nonogramDifficultyDescriptionRes(difficulty)
                    GameId.MINESWEEPER -> minesweeperDifficultyDescriptionRes(difficulty)
                    GameId.SUDOKU -> sudokuDifficultyDescriptionRes(difficulty)
                    GameId.WORDSEARCH -> wordSearchDifficultyDescriptionRes(difficulty)
                }
                if (index > 0) HorizontalDivider()
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (activeGameKey != null) pendingDifficulty = difficulty else onPickDifficulty(difficulty)
                        }
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(difficultyColor(difficulty)),
                    )
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(stringResource(labelRes), style = MaterialTheme.typography.titleSmall)
                        if (descriptionRes != null) {
                            Text(
                                stringResource(descriptionRes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }

    val difficultyToStart = pendingDifficulty
    if (difficultyToStart != null) {
        AlertDialog(
            onDismissRequest = { pendingDifficulty = null },
            title = { Text(stringResource(R.string.replace_dialog_title)) },
            text = { Text(stringResource(R.string.replace_dialog_message)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingDifficulty = null
                    onPickDifficulty(difficultyToStart)
                }) { Text(stringResource(R.string.common_start_new_puzzle)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingDifficulty = null
                    val activeGameId = activeGameKey?.let { key -> GameId.entries.firstOrNull { it.key == key } }
                    if (activeGameId != null) onResumeActiveGame(activeGameId)
                }) { Text(stringResource(R.string.common_continue_puzzle)) }
            },
        )
    }
}

@Composable
private fun GameStatsTab(gameId: GameId) {
    val stats by AppContainer.statsRepository.statsFor(gameId).collectAsState(initial = null)
    val currentStats = stats ?: return
    val overview = remember(currentStats) { buildStatsOverview(gameId, mapOf(gameId to currentStats)) }

    StatsOverviewContent(overview, modifier = Modifier.padding(16.dp))
}
