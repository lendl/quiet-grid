package com.quietgrid.app.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameCatalog
import com.quietgrid.app.core.GameId
import com.quietgrid.app.data.AppContainer
import com.quietgrid.app.data.AppSettings
import com.quietgrid.app.games.chimptest.ChimpTestPlayScreen
import com.quietgrid.app.games.minesweeper.MinesweeperPlayScreen
import com.quietgrid.app.games.nonogram.NonogramPlayScreen
import com.quietgrid.app.games.sudoku.SudokuPlayScreen
import com.quietgrid.app.games.takuzu.TakuzuPlayScreen
import com.quietgrid.app.games.wordsearch.WordSearchPlayScreen
import com.quietgrid.app.ui.components.AppTab
import com.quietgrid.app.ui.components.AppTopBar
import com.quietgrid.app.ui.components.BottomNavBar
import com.quietgrid.app.ui.components.GlobalMenu
import com.quietgrid.app.ui.screens.CompletionScreen
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.quietgrid.app.ui.screens.GamesScreen
import com.quietgrid.app.ui.screens.LossScreen
import com.quietgrid.app.ui.screens.PuzzlePickerScreen
import com.quietgrid.app.ui.screens.SettingsScreen
import com.quietgrid.app.ui.screens.StatsScreen
import com.quietgrid.app.ui.screens.SupportInfoScreen
import com.quietgrid.app.ui.screens.supportInfoTitleRes
import com.quietgrid.app.ui.screens.SupportScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isTabsRoute = currentRoute == Routes.TABS || currentRoute == null
    var selectedTab by remember { mutableStateOf(AppTab.GAMES) }
    val scope = rememberCoroutineScope()
    val settings by AppContainer.settingsRepository.settings.collectAsState(initial = AppSettings())
    val activeGameKey by AppContainer.sessionRepository.activeSession
        .map { it?.gameId }
        .collectAsState(initial = null)

    Scaffold(
        topBar = {
            when {
                isTabsRoute -> GlobalMenu(
                    themeMode = settings.themeMode,
                    onThemeModeChange = { mode -> scope.launch { AppContainer.settingsRepository.setThemeMode(mode) } },
                    hasActiveSession = activeGameKey != null,
                    onContinueSession = {
                        val gameId = GameId.entries.firstOrNull { it.key == activeGameKey } ?: return@GlobalMenu
                        navController.navigate(Routes.play(gameId, Difficulty.EASY, resume = true))
                    },
                )
                currentRoute == Routes.PICKER -> {
                    val pickerGameId = GameId.entries.firstOrNull {
                        it.key == backStackEntry?.arguments?.getString("gameId")
                    }
                    GlobalMenu(
                        themeMode = settings.themeMode,
                        onThemeModeChange = { mode -> scope.launch { AppContainer.settingsRepository.setThemeMode(mode) } },
                        hasActiveSession = activeGameKey != null,
                        onContinueSession = {
                            val gameId = GameId.entries.firstOrNull { it.key == activeGameKey } ?: return@GlobalMenu
                            navController.navigate(Routes.play(gameId, Difficulty.EASY, resume = true))
                        },
                        subtitle = pickerGameId?.let { stringResource(GameCatalog.games.first { meta -> meta.id == it }.titleRes) },
                        showBottomDivider = false,
                    )
                }
                currentRoute == Routes.PLAY -> Unit
                currentRoute == Routes.COMPLETION || currentRoute == Routes.LOSS -> Unit
                currentRoute == Routes.SUPPORT_INFO -> {
                    val infoKey = backStackEntry?.arguments?.getString("key")
                    val infoTitleRes = infoKey?.let { supportInfoTitleRes(it) }
                    GlobalMenu(
                        themeMode = settings.themeMode,
                        onThemeModeChange = { mode -> scope.launch { AppContainer.settingsRepository.setThemeMode(mode) } },
                        hasActiveSession = activeGameKey != null,
                        onContinueSession = {
                            val gameId = GameId.entries.firstOrNull { it.key == activeGameKey } ?: return@GlobalMenu
                            navController.navigate(Routes.play(gameId, Difficulty.EASY, resume = true))
                        },
                        subtitle = infoTitleRes?.let { stringResource(it) },
                    )
                }
                else -> AppTopBar(onBack = { navController.popBackStack() })
            }
        },
        bottomBar = {
            if (isTabsRoute || currentRoute == Routes.PICKER || currentRoute == Routes.SUPPORT_INFO) {
                BottomNavBar(
                    selectedTab = selectedTab,
                    onSelectTab = { tab ->
                        selectedTab = tab
                        if (currentRoute != Routes.TABS) {
                            navController.popBackStack(Routes.TABS, inclusive = false)
                        }
                    },
                )
            }
        },
    ) { padding ->
        NavHost(navController = navController, startDestination = Routes.TABS, modifier = Modifier.padding(padding)) {
            composable(Routes.TABS) {
                when (selectedTab) {
                    AppTab.GAMES -> GamesScreen(
                        onOpenGame = { gameId -> navController.navigate(Routes.picker(gameId)) },
                        onResumeGame = { gameId -> navController.navigate(Routes.play(gameId, Difficulty.EASY, resume = true)) },
                    )
                    AppTab.STATS -> StatsScreen()
                    AppTab.SETTINGS -> SettingsScreen()
                    AppTab.SUPPORT -> SupportScreen(onOpenInfo = { key -> navController.navigate(Routes.supportInfo(key)) })
                }
            }

            composable(
                Routes.PICKER,
                arguments = listOf(navArgument("gameId") { type = NavType.StringType }),
            ) { entry ->
                val gameId = GameId.entries.first { it.key == entry.arguments?.getString("gameId") }
                PuzzlePickerScreen(
                    gameId = gameId,
                    onPickDifficulty = { difficulty -> navController.navigate(Routes.play(gameId, difficulty, resume = false)) },
                    onResumeActiveGame = { activeGameId -> navController.navigate(Routes.play(activeGameId, Difficulty.EASY, resume = true)) },
                )
            }

            composable(
                Routes.PLAY,
                arguments = listOf(
                    navArgument("gameId") { type = NavType.StringType },
                    navArgument("difficulty") { type = NavType.StringType },
                    navArgument("resume") { type = NavType.BoolType },
                ),
            ) { entry ->
                val difficulty = Difficulty.fromKey(entry.arguments?.getString("difficulty") ?: "easy")
                val resume = entry.arguments?.getBoolean("resume") ?: false
                val gameId = GameId.entries.first { it.key == entry.arguments?.getString("gameId") }

                fun goToCompletion(resultDifficulty: Difficulty, score: Int, accuracyPct: Int, elapsedSeconds: Int, isFirstSolve: Boolean, isNewHighScore: Boolean) {
                    navController.navigate(
                        Routes.completion(gameId, resultDifficulty, score, accuracyPct, elapsedSeconds, isFirstSolve, isNewHighScore),
                    ) { popUpTo(Routes.TABS) { inclusive = false } }
                }

                fun goToLoss(resultDifficulty: Difficulty, elapsedSeconds: Int, reason: String) {
                    navController.navigate(
                        Routes.loss(gameId, resultDifficulty, elapsedSeconds, reason),
                    ) { popUpTo(Routes.TABS) { inclusive = false } }
                }

                when (gameId) {
                    GameId.TAKUZU -> TakuzuPlayScreen(
                        difficulty = difficulty,
                        resume = resume,
                        onBack = { navController.popBackStack() },
                        onFinished = { result ->
                            if (result.solved) {
                                goToCompletion(result.difficulty, result.score, result.accuracyPct, result.elapsedSeconds, result.isFirstSolve, result.isNewHighScore)
                            } else {
                                goToLoss(result.difficulty, result.elapsedSeconds, result.lossReason ?: "abandoned")
                            }
                        },
                    )
                    GameId.NONOGRAM -> NonogramPlayScreen(
                        difficulty = difficulty,
                        resume = resume,
                        onBack = { navController.popBackStack() },
                        onFinished = { result ->
                            if (result.solved) {
                                goToCompletion(result.difficulty, result.score, 100, result.elapsedSeconds, result.isFirstSolve, result.isNewHighScore)
                            } else {
                                goToLoss(result.difficulty, result.elapsedSeconds, result.lossReason ?: "abandoned")
                            }
                        },
                    )
                    GameId.MINESWEEPER -> MinesweeperPlayScreen(
                        difficulty = difficulty,
                        resume = resume,
                        onBack = { navController.popBackStack() },
                        onFinished = { result ->
                            if (result.solved) {
                                goToCompletion(result.difficulty, result.score, 100, result.elapsedSeconds, result.isFirstSolve, result.isNewHighScore)
                            } else {
                                goToLoss(result.difficulty, result.elapsedSeconds, result.lossReason ?: "abandoned")
                            }
                        },
                    )
                    GameId.SUDOKU -> SudokuPlayScreen(
                        difficulty = difficulty,
                        resume = resume,
                        onBack = { navController.popBackStack() },
                        onFinished = { result ->
                            if (result.solved) {
                                goToCompletion(result.difficulty, result.score, result.accuracyPct, result.elapsedSeconds, result.isFirstSolve, result.isNewHighScore)
                            } else {
                                goToLoss(result.difficulty, result.elapsedSeconds, result.lossReason ?: "abandoned")
                            }
                        },
                    )
                    GameId.WORDSEARCH -> WordSearchPlayScreen(
                        difficulty = difficulty,
                        resume = resume,
                        onBack = { navController.popBackStack() },
                        onFinished = { result ->
                            if (result.solved) {
                                goToCompletion(result.difficulty, result.score, 100, result.elapsedSeconds, result.isFirstSolve, result.isNewHighScore)
                            } else {
                                goToLoss(result.difficulty, result.elapsedSeconds, result.lossReason ?: "abandoned")
                            }
                        },
                    )
                    else -> ChimpTestPlayScreen(
                        difficulty = difficulty,
                        resume = resume,
                        onBack = { navController.popBackStack() },
                        onFinished = { result ->
                            if (result.solved) {
                                goToCompletion(result.difficulty, result.score, 100, result.elapsedSeconds, result.isFirstSolve, result.isNewHighScore)
                            } else {
                                goToLoss(result.difficulty, result.elapsedSeconds, result.lossReason ?: "abandoned")
                            }
                        },
                    )
                }
            }

            composable(
                Routes.COMPLETION,
                arguments = listOf(
                    navArgument("gameId") { type = NavType.StringType },
                    navArgument("difficulty") { type = NavType.StringType },
                    navArgument("score") { type = NavType.IntType },
                    navArgument("accuracyPct") { type = NavType.IntType },
                    navArgument("elapsedSeconds") { type = NavType.IntType },
                    navArgument("isFirstSolve") { type = NavType.BoolType },
                    navArgument("isNewHighScore") { type = NavType.BoolType },
                ),
            ) { entry ->
                CompletionScreen(
                    gameId = GameId.entries.first { it.key == entry.arguments?.getString("gameId") },
                    difficulty = Difficulty.fromKey(entry.arguments?.getString("difficulty") ?: "easy"),
                    score = entry.arguments?.getInt("score") ?: 0,
                    accuracyPct = entry.arguments?.getInt("accuracyPct") ?: 100,
                    elapsedSeconds = entry.arguments?.getInt("elapsedSeconds") ?: 0,
                    isFirstSolve = entry.arguments?.getBoolean("isFirstSolve") ?: false,
                    isNewHighScore = entry.arguments?.getBoolean("isNewHighScore") ?: false,
                    onPlayAgain = {
                        val playGameId = GameId.entries.first { it.key == entry.arguments?.getString("gameId") }
                        val playDifficulty = Difficulty.fromKey(entry.arguments?.getString("difficulty") ?: "easy")
                        navController.navigate(Routes.play(playGameId, playDifficulty, resume = false)) {
                            popUpTo(Routes.TABS) { inclusive = false }
                        }
                    },
                    onTryAnotherGame = {
                        selectedTab = AppTab.GAMES
                        navController.popBackStack(Routes.TABS, inclusive = false)
                    },
                    onViewStats = {
                        selectedTab = AppTab.STATS
                        navController.popBackStack(Routes.TABS, inclusive = false)
                    },
                )
            }

            composable(
                Routes.LOSS,
                arguments = listOf(
                    navArgument("gameId") { type = NavType.StringType },
                    navArgument("difficulty") { type = NavType.StringType },
                    navArgument("elapsedSeconds") { type = NavType.IntType },
                    navArgument("reason") { type = NavType.StringType },
                ),
            ) { entry ->
                LossScreen(
                    gameId = GameId.entries.first { it.key == entry.arguments?.getString("gameId") },
                    difficulty = Difficulty.fromKey(entry.arguments?.getString("difficulty") ?: "easy"),
                    elapsedSeconds = entry.arguments?.getInt("elapsedSeconds") ?: 0,
                    reason = entry.arguments?.getString("reason") ?: "abandoned",
                    onTryAnotherGame = {
                        selectedTab = AppTab.GAMES
                        navController.popBackStack(Routes.TABS, inclusive = false)
                    },
                )
            }

            composable(
                Routes.SUPPORT_INFO,
                arguments = listOf(navArgument("key") { type = NavType.StringType }),
            ) { entry ->
                SupportInfoScreen(entry.arguments?.getString("key") ?: "about")
            }
        }
    }
}
