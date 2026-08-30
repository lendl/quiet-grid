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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import androidx.hilt.navigation.compose.hiltViewModel
import com.quietgrid.app.core.difficultyColor
import com.quietgrid.app.data.RepositoriesViewModel
import com.quietgrid.app.games.animaldoku.AnimalDokuQuickStart
import com.quietgrid.app.games.animaldoku.animalDokuDifficultyDescriptionRes
import com.quietgrid.app.games.animaldoku.animalDokuDifficultyLabelRes
import com.quietgrid.app.games.arrowescape.ArrowEscapeQuickStart
import com.quietgrid.app.games.arrowescape.arrowEscapeDifficultyDescriptionRes
import com.quietgrid.app.games.arrowescape.arrowEscapeDifficultyLabelRes
import com.quietgrid.app.games.blockfill.BlockFillQuickStart
import com.quietgrid.app.games.blockfill.blockFillDifficultyDescriptionRes
import com.quietgrid.app.games.blockfill.blockFillDifficultyLabelRes
import com.quietgrid.app.games.chimptest.ChimpTestQuickStart
import com.quietgrid.app.games.chimptest.chimpDifficultyDescriptionRes
import com.quietgrid.app.games.chimptest.chimpDifficultyLabelRes
import com.quietgrid.app.games.minesweeper.MinesweeperQuickStart
import com.quietgrid.app.games.minesweeper.minesweeperDifficultyDescriptionRes
import com.quietgrid.app.games.minesweeper.minesweeperDifficultyLabelRes
import com.quietgrid.app.games.nonogram.NonogramQuickStart
import com.quietgrid.app.games.nonogram.nonogramDifficultyDescriptionRes
import com.quietgrid.app.games.nonogram.nonogramDifficultyLabelRes
import com.quietgrid.app.games.sudoku.SudokuQuickStart
import com.quietgrid.app.games.sudoku.sudokuDifficultyDescriptionRes
import com.quietgrid.app.games.sudoku.sudokuDifficultyLabelRes
import com.quietgrid.app.games.takuzu.TakuzuQuickStart
import com.quietgrid.app.games.takuzu.takuzuDifficultyDescriptionRes
import com.quietgrid.app.games.takuzu.takuzuDifficultyLabelRes
import com.quietgrid.app.games.wordguess.WordGuessQuickStart
import com.quietgrid.app.games.wordguess.wordGuessDifficultyDescriptionRes
import com.quietgrid.app.games.wordguess.wordGuessDifficultyLabelRes
import com.quietgrid.app.games.wordsearch.WordSearchQuickStart
import com.quietgrid.app.games.wordsearch.wordSearchDifficultyDescriptionRes
import com.quietgrid.app.games.wordsearch.wordSearchDifficultyLabelRes
import com.quietgrid.app.ui.components.QuickStartContent
import com.quietgrid.app.ui.components.QuickStartSheet
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private enum class GamePageTab { PLAY, RULES, STATS }

private fun quickStartFor(gameId: GameId): QuickStartContent = when (gameId) {
    GameId.CHIMPTEST -> ChimpTestQuickStart
    GameId.TAKUZU -> TakuzuQuickStart
    GameId.NONOGRAM -> NonogramQuickStart
    GameId.MINESWEEPER -> MinesweeperQuickStart
    GameId.SUDOKU -> SudokuQuickStart
    GameId.WORDSEARCH -> WordSearchQuickStart
    GameId.BLOCKFILL -> BlockFillQuickStart
    GameId.WORDGUESS -> WordGuessQuickStart
    GameId.ANIMALDOKU -> AnimalDokuQuickStart
    GameId.ARROWESCAPE -> ArrowEscapeQuickStart
}

@Composable
fun PuzzlePickerScreen(
    gameId: GameId,
    onPickDifficulty: (Difficulty) -> Unit,
    onResumeActiveGame: (GameId) -> Unit,
    onStartChallenger: () -> Unit,
) {
    var selectedTab by remember { mutableStateOf(GamePageTab.PLAY) }

    val repositories: RepositoriesViewModel = hiltViewModel()
    val settings by repositories.settingsRepository.settings.collectAsState(initial = null)
    val activeGameKey by repositories.sessionRepository.activeSession
        .map { it?.gameId }
        .collectAsState(initial = null)
    val coroutineScope = rememberCoroutineScope()
    var showQuickStart by remember(gameId) { mutableStateOf(false) }
    var pendingDifficulty by remember(gameId) { mutableStateOf<Difficulty?>(null) }
    var pendingChallenger by remember(gameId) { mutableStateOf(false) }

    val requestStartDifficulty: (Difficulty) -> Unit = { difficulty ->
        if (activeGameKey != null) pendingDifficulty = difficulty else onPickDifficulty(difficulty)
    }
    val requestStartChallenger: () -> Unit = {
        if (activeGameKey != null) pendingChallenger = true else onStartChallenger()
    }

    LaunchedEffect(gameId, settings) {
        val seen = settings?.quickStartSeenGameIds ?: return@LaunchedEffect
        if (gameId.key !in seen) showQuickStart = true
    }

    if (showQuickStart) {
        QuickStartSheet(
            content = quickStartFor(gameId),
            onDismiss = {
                showQuickStart = false
                coroutineScope.launch { repositories.settingsRepository.markQuickStartSeen(gameId) }
            },
            onQuickPlay = {
                showQuickStart = false
                coroutineScope.launch { repositories.settingsRepository.markQuickStartSeen(gameId) }
                requestStartDifficulty(Difficulty.EASY)
            },
        )
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
    if (pendingChallenger) {
        AlertDialog(
            onDismissRequest = { pendingChallenger = false },
            title = { Text(stringResource(R.string.replace_dialog_title)) },
            text = { Text(stringResource(R.string.replace_dialog_message)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingChallenger = false
                    onStartChallenger()
                }) { Text(stringResource(R.string.common_start_new_puzzle)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingChallenger = false
                    val activeGameId = activeGameKey?.let { key -> GameId.entries.firstOrNull { it.key == key } }
                    if (activeGameId != null) onResumeActiveGame(activeGameId)
                }) { Text(stringResource(R.string.common_continue_puzzle)) }
            },
        )
    }

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
            GamePageTab.PLAY -> GamePlayPickerTab(gameId, requestStartDifficulty, requestStartChallenger)
            GamePageTab.RULES -> HowToPlayScreen(gameId)
            GamePageTab.STATS -> GameStatsTab(gameId)
        }
    }
}

private fun pickableDifficultiesFor(gameId: GameId): List<Difficulty> = Difficulty.entries

@Composable
private fun GamePlayPickerTab(
    gameId: GameId,
    requestStartDifficulty: (Difficulty) -> Unit,
    requestStartChallenger: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Column {
            pickableDifficultiesFor(gameId).forEachIndexed { index, difficulty ->
                val labelRes = when (gameId) {
                    GameId.TAKUZU -> takuzuDifficultyLabelRes(difficulty)
                    GameId.NONOGRAM -> nonogramDifficultyLabelRes(difficulty)
                    GameId.MINESWEEPER -> minesweeperDifficultyLabelRes(difficulty)
                    GameId.SUDOKU -> sudokuDifficultyLabelRes(difficulty)
                    GameId.WORDSEARCH -> wordSearchDifficultyLabelRes(difficulty)
                    GameId.BLOCKFILL -> blockFillDifficultyLabelRes(difficulty)
                    GameId.WORDGUESS -> wordGuessDifficultyLabelRes(difficulty)
                    GameId.ANIMALDOKU -> animalDokuDifficultyLabelRes(difficulty)
                    GameId.ARROWESCAPE -> arrowEscapeDifficultyLabelRes(difficulty)
                    else -> chimpDifficultyLabelRes(difficulty)
                }
                val descriptionRes = when (gameId) {
                    GameId.CHIMPTEST -> chimpDifficultyDescriptionRes(difficulty)
                    GameId.TAKUZU -> takuzuDifficultyDescriptionRes(difficulty)
                    GameId.NONOGRAM -> nonogramDifficultyDescriptionRes(difficulty)
                    GameId.MINESWEEPER -> minesweeperDifficultyDescriptionRes(difficulty)
                    GameId.SUDOKU -> sudokuDifficultyDescriptionRes(difficulty)
                    GameId.WORDSEARCH -> wordSearchDifficultyDescriptionRes(difficulty)
                    GameId.BLOCKFILL -> blockFillDifficultyDescriptionRes(difficulty)
                    GameId.WORDGUESS -> wordGuessDifficultyDescriptionRes(difficulty)
                    GameId.ANIMALDOKU -> animalDokuDifficultyDescriptionRes(difficulty)
                    GameId.ARROWESCAPE -> arrowEscapeDifficultyDescriptionRes(difficulty)
                }
                if (index > 0) HorizontalDivider()
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { requestStartDifficulty(difficulty) }
                        .padding(vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(difficultyColor(difficulty)),
                    )
                    Column(Modifier.padding(start = 14.dp)) {
                        Text(stringResource(labelRes), style = MaterialTheme.typography.titleLarge)
                        Text(
                            stringResource(descriptionRes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }
            val challengerLabelRes = when (gameId) {
                GameId.ANIMALDOKU -> R.string.animaldoku_challenger_label
                GameId.WORDGUESS -> R.string.wordguess_challenger_label
                GameId.CHIMPTEST -> R.string.chimp_challenger_label
                else -> null
            }
            val challengerDescriptionRes = when (gameId) {
                GameId.ANIMALDOKU -> R.string.animaldoku_challenger_description
                GameId.WORDGUESS -> R.string.wordguess_challenger_description
                GameId.CHIMPTEST -> R.string.chimp_challenger_description
                else -> null
            }
            if (challengerLabelRes != null && challengerDescriptionRes != null) {
                HorizontalDivider()
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { requestStartChallenger() }
                        .padding(vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                    Column(Modifier.padding(start = 14.dp)) {
                        Text(stringResource(challengerLabelRes), style = MaterialTheme.typography.titleLarge)
                        Text(
                            stringResource(challengerDescriptionRes),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GameStatsTab(gameId: GameId) {
    val repositories: RepositoriesViewModel = hiltViewModel()
    val stats by repositories.statsRepository.statsFor(gameId).collectAsState(initial = null)
    val currentStats = stats ?: return
    val overview = remember(currentStats) { buildStatsOverview(gameId, mapOf(gameId to currentStats)) }

    Column(Modifier.padding(16.dp)) {
        StatsOverviewContent(overview)

        val challengerStatsTitleRes = when (gameId) {
            GameId.ANIMALDOKU -> R.string.animaldoku_challenger_stats_title
            GameId.WORDGUESS -> R.string.wordguess_challenger_stats_title
            GameId.CHIMPTEST -> R.string.chimp_challenger_stats_title
            else -> null
        }
        val challengerStatsBestRunRes = when (gameId) {
            GameId.ANIMALDOKU -> R.string.animaldoku_challenger_stats_best_run
            GameId.WORDGUESS -> R.string.wordguess_challenger_stats_best_run
            GameId.CHIMPTEST -> R.string.chimp_challenger_stats_best_run
            else -> null
        }
        val challengerSolvedLabelRes = when (gameId) {
            GameId.ANIMALDOKU -> R.string.animaldoku_challenger_result_puzzles_solved
            GameId.WORDGUESS -> R.string.wordguess_challenger_result_puzzles_solved
            GameId.CHIMPTEST -> R.string.chimp_challenger_result_puzzles_solved
            else -> null
        }
        val challengerScoreLabelRes = when (gameId) {
            GameId.ANIMALDOKU -> R.string.animaldoku_challenger_result_score
            GameId.WORDGUESS -> R.string.wordguess_challenger_result_score
            GameId.CHIMPTEST -> R.string.chimp_challenger_result_score
            else -> null
        }
        if (challengerStatsTitleRes != null && challengerStatsBestRunRes != null && challengerSolvedLabelRes != null && challengerScoreLabelRes != null) {
            val challengerStats by repositories.statsRepository.challengerStatsFor(gameId).collectAsState(initial = null)
            challengerStats?.let { current ->
                HorizontalDivider(Modifier.padding(top = 20.dp))
                Text(
                    stringResource(challengerStatsTitleRes),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
                )
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(challengerStatsBestRunRes), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "${stringResource(challengerSolvedLabelRes)}: ${current.solved}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        "${stringResource(challengerScoreLabelRes)}: ${current.bestScore}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}
