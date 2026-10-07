package com.quietgrid.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
import com.quietgrid.app.games.game2048.Game2048QuickStart
import com.quietgrid.app.games.game2048.game2048DifficultyDescriptionRes
import com.quietgrid.app.games.game2048.game2048DifficultyLabelRes
import com.quietgrid.app.games.guessbynumbers.GuessByNumbersQuickStart
import com.quietgrid.app.games.guessbynumbers.guessByNumbersDifficultyDescriptionRes
import com.quietgrid.app.games.guessbynumbers.guessByNumbersDifficultyLabelRes
import com.quietgrid.app.games.minesweeper.MinesweeperQuickStart
import com.quietgrid.app.games.minesweeper.minesweeperDifficultyDescriptionRes
import com.quietgrid.app.games.minesweeper.minesweeperDifficultyLabelRes
import com.quietgrid.app.games.nback.NBackQuickStart
import com.quietgrid.app.games.nback.nbackDifficultyDescriptionRes
import com.quietgrid.app.games.nback.nbackDifficultyLabelRes
import com.quietgrid.app.games.nonogram.NonogramQuickStart
import com.quietgrid.app.games.nonogram.nonogramDifficultyDescriptionRes
import com.quietgrid.app.games.nonogram.nonogramDifficultyLabelRes
import com.quietgrid.app.games.starbattle.StarBattleQuickStart
import com.quietgrid.app.games.starbattle.starBattleDifficultyDescriptionRes
import com.quietgrid.app.games.starbattle.starBattleDifficultyLabelRes
import com.quietgrid.app.games.sudoku.SudokuQuickStart
import com.quietgrid.app.games.sudoku.sudokuDifficultyDescriptionRes
import com.quietgrid.app.games.sudoku.sudokuDifficultyLabelRes
import com.quietgrid.app.games.takuzu.TakuzuQuickStart
import com.quietgrid.app.games.takuzu.takuzuDifficultyDescriptionRes
import com.quietgrid.app.games.takuzu.takuzuDifficultyLabelRes
import com.quietgrid.app.games.themeclear.ThemeClearQuickStart
import com.quietgrid.app.games.themeclear.themeClearDifficultyDescriptionRes
import com.quietgrid.app.games.themeclear.themeClearDifficultyLabelRes
import com.quietgrid.app.games.wordguess.WordGuessQuickStart
import com.quietgrid.app.games.wordguess.wordGuessDifficultyDescriptionRes
import com.quietgrid.app.games.wordguess.wordGuessDifficultyLabelRes
import com.quietgrid.app.games.wordsearch.WordSearchQuickStart
import com.quietgrid.app.games.wordsearch.wordSearchDifficultyDescriptionRes
import com.quietgrid.app.games.wordsearch.wordSearchDifficultyLabelRes
import com.quietgrid.app.ui.components.QuickStartContent
import com.quietgrid.app.ui.components.StatGroup
import com.quietgrid.app.ui.components.StatItem
import com.quietgrid.app.ui.components.QuickStartSheet
import com.quietgrid.app.ui.components.ReplacePuzzleSheet
import kotlinx.coroutines.launch

private enum class GamePageTab { PLAY, RULES, STATS, SETTINGS }

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
    GameId.GAME_2048 -> Game2048QuickStart
    GameId.STARBATTLE -> StarBattleQuickStart
    GameId.GUESSBYNUMBERS -> GuessByNumbersQuickStart
    GameId.NBACK -> NBackQuickStart
    GameId.THEMECLEAR -> ThemeClearQuickStart
}

@Composable
fun PuzzlePickerScreen(
    gameId: GameId,
    onPickDifficulty: (Difficulty) -> Unit,
    onResumeActiveGame: (GameId) -> Unit,
    onStartChallenger: () -> Unit,
    onStartEndless: () -> Unit = {},
) {
    var selectedTab by remember { mutableStateOf(GamePageTab.PLAY) }

    val repositories: RepositoriesViewModel = hiltViewModel()
    val settings by repositories.settingsRepository.settings.collectAsState(initial = null)
    val activeSession by repositories.sessionRepository.activeSession.collectAsState(initial = null)
    val activeGameKey = activeSession?.gameId
    val coroutineScope = rememberCoroutineScope()
    var showQuickStart by remember(gameId) { mutableStateOf(false) }
    var pendingDifficulty by remember(gameId) { mutableStateOf<Difficulty?>(null) }
    var pendingStart by remember(gameId) { mutableStateOf<(() -> Unit)?>(null) }

    val requestStartDifficulty: (Difficulty) -> Unit = { difficulty ->
        if (activeGameKey != null) pendingDifficulty = difficulty else onPickDifficulty(difficulty)
    }
    val requestStartChallenger: () -> Unit = {
        if (activeGameKey != null) pendingStart = onStartChallenger else onStartChallenger()
    }
    val requestStartEndless: () -> Unit = {
        if (activeGameKey != null) pendingStart = onStartEndless else onStartEndless()
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

    val resumeActiveGame: () -> Unit = {
        val activeGameId = activeGameKey?.let { key -> GameId.entries.firstOrNull { it.key == key } }
        if (activeGameId != null) onResumeActiveGame(activeGameId)
    }
    val difficultyToStart = pendingDifficulty
    if (difficultyToStart != null) {
        ReplacePuzzleSheet(
            onContinue = {
                pendingDifficulty = null
                resumeActiveGame()
            },
            onStartNew = {
                pendingDifficulty = null
                onPickDifficulty(difficultyToStart)
            },
            onDismiss = { pendingDifficulty = null },
        )
    }
    pendingStart?.let { startMode ->
        ReplacePuzzleSheet(
            onContinue = {
                pendingStart = null
                resumeActiveGame()
            },
            onStartNew = {
                pendingStart = null
                startMode()
            },
            onDismiss = { pendingStart = null },
        )
    }

    Column(Modifier.fillMaxWidth()) {
        val visibleTabs = remember(gameId) {
            listOf(GamePageTab.PLAY, GamePageTab.RULES, GamePageTab.STATS) +
                if (gameHasSettings(gameId)) listOf(GamePageTab.SETTINGS) else emptyList()
        }
        SecondaryTabRow(
            selectedTabIndex = visibleTabs.indexOf(selectedTab).coerceAtLeast(0),
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = TabRowDefaults.primaryContentColor,
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
            if (GamePageTab.SETTINGS in visibleTabs) {
                Tab(
                    selected = selectedTab == GamePageTab.SETTINGS,
                    onClick = { selectedTab = GamePageTab.SETTINGS },
                    text = { Text(stringResource(R.string.common_settings)) },
                )
            }
        }

        when (selectedTab) {
            GamePageTab.PLAY -> GamePlayPickerTab(
                gameId,
                requestStartDifficulty,
                requestStartChallenger,
                requestStartEndless,
            )
            GamePageTab.RULES -> HowToPlayScreen(gameId)
            GamePageTab.STATS -> GameStatsTab(gameId)
            GamePageTab.SETTINGS -> GameSettingsTab(gameId)
        }
    }
}

private fun pickableDifficultiesFor(gameId: GameId): List<Difficulty> = Difficulty.entries

@Composable
private fun GamePlayPickerTab(
    gameId: GameId,
    requestStartDifficulty: (Difficulty) -> Unit,
    requestStartChallenger: () -> Unit,
    requestStartEndless: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
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
                    GameId.GAME_2048 -> game2048DifficultyLabelRes(difficulty)
                    GameId.STARBATTLE -> starBattleDifficultyLabelRes(difficulty)
                    GameId.GUESSBYNUMBERS -> guessByNumbersDifficultyLabelRes(difficulty)
                    GameId.NBACK -> nbackDifficultyLabelRes(difficulty)
                    GameId.THEMECLEAR -> themeClearDifficultyLabelRes(difficulty)
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
                    GameId.GAME_2048 -> game2048DifficultyDescriptionRes(difficulty)
                    GameId.STARBATTLE -> starBattleDifficultyDescriptionRes(difficulty)
                    GameId.GUESSBYNUMBERS -> guessByNumbersDifficultyDescriptionRes(difficulty)
                    GameId.NBACK -> nbackDifficultyDescriptionRes(difficulty)
                    GameId.THEMECLEAR -> themeClearDifficultyDescriptionRes(difficulty)
                }
                if (index > 0) HorizontalDivider()
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { requestStartDifficulty(difficulty) }
                        .padding(vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DifficultyLevelBlock(difficulty)
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
                GameId.STARBATTLE -> R.string.starbattle_challenger_label
                GameId.SUDOKU -> R.string.sudoku_challenger_label
                GameId.TAKUZU -> R.string.takuzu_challenger_label
                GameId.NONOGRAM -> R.string.nonogram_challenger_label
                else -> null
            }
            val challengerDescriptionRes = when (gameId) {
                GameId.ANIMALDOKU -> R.string.animaldoku_challenger_description
                GameId.WORDGUESS -> R.string.wordguess_challenger_description
                GameId.CHIMPTEST -> R.string.chimp_challenger_description
                GameId.STARBATTLE -> R.string.starbattle_challenger_description
                GameId.SUDOKU -> R.string.sudoku_challenger_description
                GameId.TAKUZU -> R.string.takuzu_challenger_description
                GameId.NONOGRAM -> R.string.nonogram_challenger_description
                else -> null
            }
            if (challengerLabelRes != null && challengerDescriptionRes != null) {
                PickerModeRow(stringResource(challengerLabelRes), stringResource(challengerDescriptionRes), detail = null, onClick = requestStartChallenger)
            }
            if (gameId == GameId.BLOCKFILL) {
                val repositories: RepositoriesViewModel = hiltViewModel()
                val endlessStats by remember(repositories, gameId) { repositories.statsRepository.endlessStatsFor(gameId) }
                    .collectAsState(initial = null)
                val bestScore = endlessStats?.bestScore ?: 0
                PickerModeRow(
                    stringResource(R.string.blockfill_endless_label),
                    stringResource(R.string.blockfill_endless_description),
                    detail = if (bestScore > 0) stringResource(R.string.blockfill_endless_best_value, bestScore) else null,
                    onClick = requestStartEndless,
                )
            }
        }
    }
}

@Composable
private fun DifficultyLevelBlock(difficulty: Difficulty) {
    val filledCount = difficulty.ordinal + 1
    val fillColor = difficultyColor(difficulty)
    val emptyColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val squareShape = RoundedCornerShape(percent = 30)
    Column(Modifier.size(22.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(2) { row ->
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(2) { col ->
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(squareShape)
                            .background(if (row * 2 + col < filledCount) fillColor else emptyColor),
                    )
                }
            }
        }
    }
}

@Composable
private fun PickerModeRow(label: String, description: String, detail: String?, onClick: () -> Unit) {
    HorizontalDivider()
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Outlined.Timer,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Column(Modifier.padding(start = 14.dp)) {
            Text(label, style = MaterialTheme.typography.titleLarge)
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
            if (detail != null) {
                Text(
                    detail,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun GameStatsTab(gameId: GameId) {
    val repositories: RepositoriesViewModel = hiltViewModel()
    val stats by remember(repositories, gameId) { repositories.statsRepository.statsFor(gameId) }.collectAsState(initial = null)
    val currentStats = stats ?: return
    val overview = remember(currentStats) { buildStatsOverview(gameId, mapOf(gameId to currentStats)) }

    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
        StatsOverviewContent(overview)

        val challengerStatsTitleRes = when (gameId) {
            GameId.ANIMALDOKU -> R.string.animaldoku_challenger_stats_title
            GameId.WORDGUESS -> R.string.wordguess_challenger_stats_title
            GameId.CHIMPTEST -> R.string.chimp_challenger_stats_title
            GameId.STARBATTLE -> R.string.starbattle_challenger_stats_title
            GameId.SUDOKU -> R.string.sudoku_challenger_stats_title
            GameId.TAKUZU -> R.string.takuzu_challenger_stats_title
            GameId.NONOGRAM -> R.string.nonogram_challenger_stats_title
            else -> null
        }
        val challengerStatsBestRunRes = when (gameId) {
            GameId.ANIMALDOKU -> R.string.animaldoku_challenger_stats_best_run
            GameId.WORDGUESS -> R.string.wordguess_challenger_stats_best_run
            GameId.CHIMPTEST -> R.string.chimp_challenger_stats_best_run
            GameId.STARBATTLE -> R.string.starbattle_challenger_stats_best_run
            GameId.SUDOKU -> R.string.sudoku_challenger_stats_best_run
            GameId.TAKUZU -> R.string.takuzu_challenger_stats_best_run
            GameId.NONOGRAM -> R.string.nonogram_challenger_stats_best_run
            else -> null
        }
        val challengerSolvedLabelRes = when (gameId) {
            GameId.ANIMALDOKU -> R.string.animaldoku_challenger_result_puzzles_solved
            GameId.WORDGUESS -> R.string.wordguess_challenger_result_puzzles_solved
            GameId.CHIMPTEST -> R.string.chimp_challenger_result_puzzles_solved
            GameId.STARBATTLE -> R.string.starbattle_challenger_result_puzzles_solved
            GameId.SUDOKU -> R.string.sudoku_challenger_result_puzzles_solved
            GameId.TAKUZU -> R.string.takuzu_challenger_result_puzzles_solved
            GameId.NONOGRAM -> R.string.nonogram_challenger_result_puzzles_solved
            else -> null
        }
        val challengerScoreLabelRes = when (gameId) {
            GameId.ANIMALDOKU -> R.string.animaldoku_challenger_result_score
            GameId.WORDGUESS -> R.string.wordguess_challenger_result_score
            GameId.CHIMPTEST -> R.string.chimp_challenger_result_score
            GameId.STARBATTLE -> R.string.starbattle_challenger_result_score
            GameId.SUDOKU -> R.string.sudoku_challenger_result_score
            GameId.TAKUZU -> R.string.takuzu_challenger_result_score
            GameId.NONOGRAM -> R.string.nonogram_challenger_result_score
            else -> null
        }
        if (challengerStatsTitleRes != null && challengerStatsBestRunRes != null && challengerSolvedLabelRes != null && challengerScoreLabelRes != null) {
            val challengerStats by remember(repositories, gameId) { repositories.statsRepository.challengerStatsFor(gameId) }
                .collectAsState(initial = null)
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
        if (gameId == GameId.BLOCKFILL) {
            val endlessStats by remember(repositories, gameId) { repositories.statsRepository.endlessStatsFor(gameId) }
                .collectAsState(initial = null)
            endlessStats?.let { current ->
                HorizontalDivider(Modifier.padding(top = 20.dp))
                Text(
                    stringResource(R.string.blockfill_endless_stats_title),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
                )
                StatGroup(
                    listOf(
                        StatItem(stringResource(R.string.blockfill_endless_stats_best_score), current.bestScore.toString()),
                        StatItem(
                            stringResource(R.string.blockfill_endless_stats_best_level),
                            if (current.bestLevel > 0) stringResource(R.string.blockfill_multiplier_value, current.bestLevel) else "-",
                        ),
                    ),
                )
            }
        }
    }
}
