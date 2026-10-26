package com.quietgrid.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameCatalog
import com.quietgrid.app.core.GameId
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.quietgrid.app.core.difficultyColor
import com.quietgrid.app.core.formatElapsed
import com.quietgrid.app.core.gameDifficultyLabelRes
import com.quietgrid.app.data.ActiveSessionEnvelope
import com.quietgrid.app.data.DifficultyStats
import com.quietgrid.app.data.RepositoriesViewModel
import com.quietgrid.app.data.SESSION_MODE_ENDLESS
import com.quietgrid.app.games.animaldoku.animalDokuDifficultyDescriptionRes
import com.quietgrid.app.games.hashi.hashiDifficultyDescriptionRes
import com.quietgrid.app.games.links.linksDifficultyDescriptionRes
import com.quietgrid.app.games.arrowescape.arrowEscapeDifficultyDescriptionRes
import com.quietgrid.app.games.blockfill.blockFillDifficultyDescriptionRes
import com.quietgrid.app.games.chimptest.chimpDifficultyDescriptionRes
import com.quietgrid.app.games.game2048.game2048DifficultyDescriptionRes
import com.quietgrid.app.games.guessbynumbers.guessByNumbersDifficultyDescriptionRes
import com.quietgrid.app.games.minesweeper.minesweeperDifficultyDescriptionRes
import com.quietgrid.app.games.minisudoku.miniSudokuDifficultyDescriptionRes
import com.quietgrid.app.games.nback.nbackDifficultyDescriptionRes
import com.quietgrid.app.games.nonogram.nonogramDifficultyDescriptionRes
import com.quietgrid.app.games.sequencememory.sequenceMemoryDifficultyDescriptionRes
import com.quietgrid.app.games.starbattle.starBattleDifficultyDescriptionRes
import com.quietgrid.app.games.whodidit.whoDidItDifficultyDescriptionRes
import com.quietgrid.app.games.sudoku.sudokuDifficultyDescriptionRes
import com.quietgrid.app.games.takuzu.takuzuDifficultyDescriptionRes
import com.quietgrid.app.games.killersudoku.killerSudokuDifficultyDescriptionRes
import com.quietgrid.app.games.themeclear.themeClearDifficultyDescriptionRes
import com.quietgrid.app.games.wordguess.wordGuessDifficultyDescriptionRes
import com.quietgrid.app.games.wordsearch.wordSearchDifficultyDescriptionRes
import com.quietgrid.app.ui.components.pressScale
import com.quietgrid.app.ui.components.quickStartFor
import com.quietgrid.app.ui.components.StatGroup
import com.quietgrid.app.ui.components.StatItem
import com.quietgrid.app.ui.components.QuickStartSheet
import com.quietgrid.app.ui.components.ReplacePuzzleSheet
import kotlinx.coroutines.launch

private enum class GamePageTab { PLAY, RULES, STATS, SETTINGS }

@Composable
fun PuzzlePickerScreen(
    gameId: GameId,
    onPickDifficulty: (Difficulty) -> Unit,
    onResumeActiveGame: (GameId) -> Unit,
    onStartChallenger: () -> Unit,
    onStartEndless: () -> Unit = {},
) {
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

    LaunchedEffect(gameId, settings) {
        val newBadgeSeen = settings?.newBadgeSeenGameIds ?: return@LaunchedEffect
        if (GameCatalog.get(gameId).isNew && gameId.key !in newBadgeSeen) repositories.settingsRepository.markNewBadgeSeen(gameId)
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

    val tabs = GamePageTab.entries
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val endlessCopy = endlessModeCopyFor(gameId)

    Column(Modifier.fillMaxSize()) {
        GamePageTabs(
            tabs = tabs,
            selectedIndex = pagerState.currentPage,
            onSelect = { index -> coroutineScope.launch { pagerState.animateScrollToPage(index) } },
        )
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalAlignment = Alignment.Top,
        ) { page ->
            when (tabs[page]) {
                GamePageTab.PLAY -> GamePlayPickerTab(
                    gameId,
                    activeSession = activeSession?.takeIf { it.gameId == gameId.key && it.dailyDate == null },
                    onResume = resumeActiveGame,
                    requestStartDifficulty = requestStartDifficulty,
                    requestStartChallenger = requestStartChallenger,
                    requestStartEndless = requestStartEndless,
                    endlessCopy = endlessCopy,
                )
                GamePageTab.RULES -> HowToPlayScreen(gameId)
                GamePageTab.STATS -> GameStatsTab(gameId, endlessCopy)
                GamePageTab.SETTINGS -> GameSettingsTab(gameId)
            }
        }
    }
}

private fun GamePageTab.icon(): ImageVector = when (this) {
    GamePageTab.PLAY -> Icons.Outlined.PlayArrow
    GamePageTab.RULES -> Icons.AutoMirrored.Outlined.MenuBook
    GamePageTab.STATS -> Icons.Outlined.BarChart
    GamePageTab.SETTINGS -> Icons.Outlined.Settings
}

private fun GamePageTab.labelRes(): Int = when (this) {
    GamePageTab.PLAY -> R.string.common_play
    GamePageTab.RULES -> R.string.common_rules
    GamePageTab.STATS -> R.string.common_stats
    GamePageTab.SETTINGS -> R.string.common_settings
}

@Composable
private fun GamePageTabs(tabs: List<GamePageTab>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        tabs.forEachIndexed { index, tab ->
            val selected = index == selectedIndex
            val containerColor by animateColorAsState(
                if (selected) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent,
                label = "gamePageTabContainer",
            )
            val label = stringResource(tab.labelRes())
            Row(
                Modifier
                    .weight(if (selected) 2.2f else 1f)
                    .heightIn(min = 40.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(containerColor)
                    .semantics { if (!selected) contentDescription = label }
                    .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(index) }),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val contentColor = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                Icon(tab.icon(), contentDescription = null, tint = contentColor, modifier = Modifier.size(20.dp))
                if (selected) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}

private fun pickableDifficultiesFor(gameId: GameId): List<Difficulty> = Difficulty.entries

private fun difficultyDescriptionRes(gameId: GameId, difficulty: Difficulty): Int = when (gameId) {
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
    GameId.WHODIDIT -> whoDidItDifficultyDescriptionRes(difficulty)
    GameId.GUESSBYNUMBERS -> guessByNumbersDifficultyDescriptionRes(difficulty)
    GameId.NBACK -> nbackDifficultyDescriptionRes(difficulty)
    GameId.THEMECLEAR -> themeClearDifficultyDescriptionRes(difficulty)
    GameId.SEQUENCEMEMORY -> sequenceMemoryDifficultyDescriptionRes(difficulty)
    GameId.HASHI -> hashiDifficultyDescriptionRes(difficulty)
    GameId.LINKS -> linksDifficultyDescriptionRes(difficulty)
    GameId.KILLERSUDOKU -> killerSudokuDifficultyDescriptionRes(difficulty)
    GameId.MINISUDOKU -> miniSudokuDifficultyDescriptionRes(difficulty)
}

@Composable
private fun GamePlayPickerTab(
    gameId: GameId,
    activeSession: ActiveSessionEnvelope?,
    onResume: () -> Unit,
    requestStartDifficulty: (Difficulty) -> Unit,
    requestStartChallenger: () -> Unit,
    requestStartEndless: () -> Unit,
    endlessCopy: EndlessModeCopy?,
) {
    val repositories: RepositoriesViewModel = hiltViewModel()
    val stats by remember(repositories, gameId) { repositories.statsRepository.statsFor(gameId) }.collectAsState(initial = null)

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                stringResource(GameCatalog.get(gameId).taglineRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 4.dp),
            )
            if (activeSession != null) {
                ContinueButton(gameId, activeSession, endlessCopy, onResume)
            }
            pickableDifficultiesFor(gameId).chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { difficulty ->
                        DifficultyTile(
                            label = stringResource(gameDifficultyLabelRes(gameId, difficulty)),
                            description = stringResource(difficultyDescriptionRes(gameId, difficulty)),
                            difficulty = difficulty,
                            stats = stats?.forDifficulty(difficulty),
                            onClick = { requestStartDifficulty(difficulty) },
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
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
                val challengerStats by remember(repositories, gameId) { repositories.statsRepository.challengerStatsFor(gameId) }
                    .collectAsState(initial = null)
                val bestScore = challengerStats?.bestScore ?: 0
                PickerModeTile(
                    stringResource(challengerLabelRes),
                    stringResource(challengerDescriptionRes),
                    detail = if (bestScore > 0) stringResource(R.string.game_page_mode_best, bestScore) else null,
                    onClick = requestStartChallenger,
                )
            }
            if (endlessCopy != null) {
                val endlessStats by remember(repositories, gameId) { repositories.statsRepository.endlessStatsFor(gameId) }
                    .collectAsState(initial = null)
                val bestScore = endlessStats?.bestScore ?: 0
                PickerModeTile(
                    stringResource(endlessCopy.label),
                    stringResource(endlessCopy.description),
                    detail = if (bestScore > 0) stringResource(endlessCopy.bestValue, bestScore) else null,
                    onClick = requestStartEndless,
                )
            }
        }
    }
}

@Composable
private fun ContinueButton(gameId: GameId, session: ActiveSessionEnvelope, endlessCopy: EndlessModeCopy?, onResume: () -> Unit) {
    val difficulty = session.difficulty?.let { key -> Difficulty.entries.firstOrNull { it.key == key } }
    val elapsed = formatElapsed(session.elapsedSeconds.toInt())
    val detail = when {
        session.mode == SESSION_MODE_ENDLESS ->
            if (endlessCopy != null) stringResource(R.string.game_page_continue_detail, stringResource(endlessCopy.label), elapsed) else elapsed
        difficulty != null -> stringResource(R.string.game_page_continue_detail, stringResource(gameDifficultyLabelRes(gameId, difficulty)), elapsed)
        else -> elapsed
    }
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            .pressScale(interactionSource)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.primary)
            .clickable(interactionSource = interactionSource, indication = LocalIndication.current, onClick = onResume)
            .heightIn(min = 60.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
        Column(Modifier.padding(start = 12.dp)) {
            Text(
                stringResource(R.string.common_continue_puzzle),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun DifficultyTile(
    label: String,
    description: String,
    difficulty: Difficulty,
    stats: DifficultyStats?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier
            .pressScale(interactionSource)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(interactionSource = interactionSource, indication = LocalIndication.current, onClick = onClick)
            .heightIn(min = 132.dp)
            .padding(horizontal = 14.dp, vertical = 16.dp),
    ) {
        DifficultyLevelBar(difficulty)
        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 10.dp))
        Text(
            description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        Spacer(Modifier.weight(1f))
        val solved = stats?.solved ?: 0
        Text(
            if (solved > 0) {
                stringResource(R.string.game_page_tile_progress, solved, stats?.bestScore ?: 0)
            } else {
                stringResource(R.string.game_page_tile_not_played)
            },
            style = MaterialTheme.typography.labelMedium,
            color = if (solved > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun DifficultyLevelBar(difficulty: Difficulty) {
    val filledCount = difficulty.ordinal + 1
    val fillColor = difficultyColor(difficulty)
    val emptyColor = MaterialTheme.colorScheme.surfaceContainerHighest
    Row(Modifier.width(44.dp).height(6.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(Difficulty.entries.size) { index ->
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (index < filledCount) fillColor else emptyColor),
            )
        }
    }
}

@Composable
private fun PickerModeTile(label: String, description: String, detail: String?, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            .pressScale(interactionSource)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(interactionSource = interactionSource, indication = LocalIndication.current, onClick = onClick)
            .heightIn(min = 72.dp)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Outlined.Timer,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (detail != null) {
            Text(
                detail,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun GameStatsTab(gameId: GameId, endlessCopy: EndlessModeCopy?) {
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
        if (endlessCopy != null) {
            val endlessStats by remember(repositories, gameId) { repositories.statsRepository.endlessStatsFor(gameId) }
                .collectAsState(initial = null)
            endlessStats?.let { current ->
                HorizontalDivider(Modifier.padding(top = 20.dp))
                Text(
                    stringResource(endlessCopy.statsTitle),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 20.dp, bottom = 4.dp),
                )
                val bestLevelText = when {
                    endlessCopy.bestLevelFormat != null ->
                        if (current.bestLevel > 0) stringResource(endlessCopy.bestLevelFormat, current.bestLevel) else "-"
                    current.bestScore > 0 -> current.bestLevel.toString()
                    else -> "-"
                }
                StatGroup(
                    listOf(
                        StatItem(stringResource(endlessCopy.statsBestScore), current.bestScore.toString()),
                        StatItem(stringResource(endlessCopy.statsBestLevel), bestLevelText),
                    ),
                )
            }
        }
    }
}
