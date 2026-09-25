package com.quietgrid.app.nav

import com.quietgrid.app.ui.screens.DailyScreen
import com.quietgrid.app.ui.screens.DailyViewModel
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.platform.LocalContext
import com.quietgrid.app.ui.components.DailyPlayBanner
import com.quietgrid.app.core.daily.nextUnplayedDaily
import com.quietgrid.app.core.daily.shareDailyResult
import java.time.LocalDate
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameCatalog
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.mix.Mix
import com.quietgrid.app.core.mix.MixMode
import com.quietgrid.app.core.mix.drawWeighted
import com.quietgrid.app.core.mix.nextMixName
import com.quietgrid.app.core.mix.MixEntry
import com.quietgrid.app.core.mix.MixEntryMode
import com.quietgrid.app.core.mix.mixesEligibleForQuickAdd
import com.quietgrid.app.core.mix.resolvedCandidates
import com.quietgrid.app.data.AppSettings
import com.quietgrid.app.data.RepositoriesViewModel
import com.quietgrid.app.games.animaldoku.AnimalDokuChallengerPlayScreen
import com.quietgrid.app.games.animaldoku.AnimalDokuChallengerResultScreen
import com.quietgrid.app.games.animaldoku.AnimalDokuPlayScreen
import com.quietgrid.app.games.chimptest.ChimpTestChallengerPlayScreen
import com.quietgrid.app.games.chimptest.ChimpTestChallengerResultScreen
import com.quietgrid.app.games.arrowescape.ArrowEscapePlayScreen
import com.quietgrid.app.games.blockfill.BlockFillPlayScreen
import com.quietgrid.app.games.chimptest.ChimpTestPlayScreen
import com.quietgrid.app.games.flowfree.FlowFreePlayScreen
import com.quietgrid.app.games.game2048.Game2048PlayScreen
import com.quietgrid.app.games.nback.NBackPlayScreen
import com.quietgrid.app.games.guessbynumbers.GuessByNumbersPlayScreen
import com.quietgrid.app.games.minesweeper.MinesweeperPlayScreen
import com.quietgrid.app.games.nonogram.NonogramChallengerPlayScreen
import com.quietgrid.app.games.nonogram.NonogramChallengerResultScreen
import com.quietgrid.app.games.nonogram.NonogramPlayScreen
import com.quietgrid.app.games.starbattle.StarBattleChallengerPlayScreen
import com.quietgrid.app.games.starbattle.StarBattleChallengerResultScreen
import com.quietgrid.app.games.starbattle.StarBattlePlayScreen
import com.quietgrid.app.games.sudoku.SudokuChallengerPlayScreen
import com.quietgrid.app.games.sudoku.SudokuChallengerResultScreen
import com.quietgrid.app.games.sudoku.SudokuPlayScreen
import com.quietgrid.app.games.takuzu.TakuzuAnalyzerScreen
import com.quietgrid.app.games.takuzu.TakuzuChallengerPlayScreen
import com.quietgrid.app.games.takuzu.TakuzuChallengerResultScreen
import com.quietgrid.app.games.takuzu.TakuzuPlayScreen
import com.quietgrid.app.games.wordguess.WordGuessChallengerPlayScreen
import com.quietgrid.app.games.wordguess.WordGuessChallengerResultScreen
import com.quietgrid.app.games.wordguess.WordGuessPlayScreen
import com.quietgrid.app.games.wordsearch.WordSearchPlayScreen
import com.quietgrid.app.games.wordsearch.wordSearchThemeIcon
import com.quietgrid.app.ui.components.AppTab
import com.quietgrid.app.ui.components.AppTopBar
import com.quietgrid.app.ui.components.BottomNavBar
import com.quietgrid.app.ui.components.ContinueSessionMiniBar
import com.quietgrid.app.ui.components.pressScale
import com.quietgrid.app.ui.screens.AccountDrawerContent
import com.quietgrid.app.ui.screens.AnalyzerHandoff
import com.quietgrid.app.ui.screens.ChallengerExtras
import com.quietgrid.app.ui.screens.ChallengerRunDetails
import com.quietgrid.app.ui.screens.CompletionExtras
import com.quietgrid.app.ui.screens.CompletionHighlight
import com.quietgrid.app.ui.screens.CompletionScreen
import kotlinx.coroutines.launch
import com.quietgrid.app.ui.screens.GamesScreen
import com.quietgrid.app.ui.screens.LossScreen
import com.quietgrid.app.ui.screens.MixEditorScreen
import com.quietgrid.app.ui.screens.MixesScreen
import com.quietgrid.app.ui.screens.AboutPageScreen
import com.quietgrid.app.ui.screens.PuzzlePickerScreen
import com.quietgrid.app.ui.screens.SettingsPageScreen
import com.quietgrid.app.ui.screens.StatsScreen
import com.quietgrid.app.ui.screens.SupportInfoScreen
import com.quietgrid.app.ui.screens.SupportPageScreen
import com.quietgrid.app.ui.screens.TrustPageScreen
import com.quietgrid.app.ui.screens.supportInfoTitleRes
import java.util.UUID

val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }
val LocalAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost(openDailyTab: Boolean = false, onOpenDailyTabHandled: () -> Unit = {}) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isTabsRoute = currentRoute == Routes.TABS || currentRoute == null
    var selectedTab by remember { mutableStateOf(AppTab.GAMES) }
    val scope = rememberCoroutineScope()
    val repositories: RepositoriesViewModel = hiltViewModel()
    val settings by repositories.settingsRepository.settings.collectAsState(initial = AppSettings())
    val activeSession by repositories.sessionRepository.activeSession.collectAsState(initial = null)
    val activeGameKey = activeSession?.gameId

    val mixes by repositories.mixRepository.mixes.collectAsState(initial = emptyList())
    val activeMixId by repositories.mixRepository.activeMixId.collectAsState(initial = null)
    val activeMix = mixes.firstOrNull { it.id == activeMixId }

    fun endMixAndGoToGames() {
        scope.launch { repositories.mixRepository.clearActiveMix() }
        selectedTab = AppTab.GAMES
        navController.popBackStack(Routes.TABS, inclusive = false)
    }

    fun resumeActiveRoute(gameId: GameId): String =
        Routes.play(gameId, Difficulty.EASY, resume = true, daily = activeSession?.dailyDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() })

    fun playNextDaily(next: Pair<GameId, Difficulty>?, dailyKey: String?) {
        val date = dailyKey?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return
        val (gameId, difficulty) = next ?: return
        navController.navigate(Routes.play(gameId, difficulty, resume = false, daily = date)) {
            popUpTo(Routes.TABS) { inclusive = false }
        }
    }

    fun goToDailyTab() {
        selectedTab = AppTab.DAILY
        navController.popBackStack(Routes.TABS, inclusive = false)
    }

    LaunchedEffect(openDailyTab) {
        if (openDailyTab) {
            goToDailyTab()
            onOpenDailyTabHandled()
        }
    }

    var pendingMixToStart by remember { mutableStateOf<Mix?>(null) }
    var pendingDailyRoute by remember { mutableStateOf<String?>(null) }
    val accountDrawerState = rememberDrawerState(DrawerValue.Closed)
    var mixEditorRenameTrigger by remember { mutableStateOf(0) }
    var mixEditorDeleteTrigger by remember { mutableStateOf(0) }

    fun goToMixDraw(mix: Mix) {
        scope.launch {
            val enabledGameIds = GameCatalog.games.filter { !it.beta || settings.betaGamesEnabled }.map { it.id }.toSet()
            val candidate = drawWeighted(mix.resolvedCandidates().filter { it.gameId in enabledGameIds })
            if (candidate == null) {
                repositories.mixRepository.clearActiveMix()
                selectedTab = AppTab.GAMES
                navController.popBackStack(Routes.TABS, inclusive = false)
                return@launch
            }
            val route = when (val mode = candidate.mode) {
                is MixMode.Puzzle -> Routes.play(candidate.gameId, mode.difficulty, resume = false)
                MixMode.Challenger -> Routes.challenger(candidate.gameId)
            }
            navController.navigate(route) { popUpTo(Routes.TABS) { inclusive = false } }
        }
    }

    fun startMix(mix: Mix) {
        scope.launch { repositories.mixRepository.setActiveMix(mix.id) }
        goToMixDraw(mix)
    }

    fun playAgainOrDrawMix(fallback: () -> Unit) {
        val mix = activeMix
        if (mix != null) goToMixDraw(mix) else fallback()
    }

    ModalNavigationDrawer(
        drawerState = accountDrawerState,
        gesturesEnabled = currentRoute != Routes.PLAY,
        drawerContent = {
            ModalDrawerSheet {
                AccountDrawerContent(
                    onOpenSettings = {
                        scope.launch { accountDrawerState.close() }
                        navController.navigate(Routes.SETTINGS)
                    },
                    onOpenSupport = {
                        scope.launch { accountDrawerState.close() }
                        navController.navigate(Routes.SUPPORT)
                    },
                    onOpenTrust = {
                        scope.launch { accountDrawerState.close() }
                        navController.navigate(Routes.TRUST)
                    },
                    onOpenAbout = {
                        scope.launch { accountDrawerState.close() }
                        navController.navigate(Routes.ABOUT)
                    },
                )
            }
        },
    ) {
    Scaffold(
        topBar = {
            when {
                isTabsRoute -> Unit
                currentRoute == Routes.PICKER -> {
                    val pickerGameId = GameId.entries.firstOrNull {
                        it.key == backStackEntry?.arguments?.getString("gameId")
                    }
                    AppTopBar(
                        title = pickerGameId?.let { stringResource(GameCatalog.games.first { meta -> meta.id == it }.titleRes) },
                        onBack = { navController.popBackStack() },
                    )
                }
                currentRoute == Routes.PLAY -> Unit
                currentRoute == Routes.COMPLETION || currentRoute == Routes.LOSS -> Unit
                currentRoute == Routes.CHALLENGER || currentRoute == Routes.CHALLENGER_RESULT -> Unit
                currentRoute == Routes.SUPPORT_INFO -> {
                    val infoKey = backStackEntry?.arguments?.getString("key")
                    val infoTitleRes = infoKey?.let { supportInfoTitleRes(it) }
                    AppTopBar(
                        title = infoTitleRes?.let { stringResource(it) },
                        onBack = { navController.popBackStack() },
                    )
                }
                currentRoute == Routes.MIX_EDITOR -> {
                    val editingMixId = backStackEntry?.arguments?.getString("mixId")
                    val editingMixName = mixes.firstOrNull { it.id == editingMixId }?.name
                    AppTopBar(
                        title = editingMixName ?: stringResource(R.string.mix_editor_title_edit),
                        onBack = { navController.popBackStack() },
                        actions = {
                            val renameInteractionSource = remember { MutableInteractionSource() }
                            IconButton(
                                onClick = { mixEditorRenameTrigger++ },
                                interactionSource = renameInteractionSource,
                                modifier = Modifier.pressScale(renameInteractionSource),
                            ) {
                                Icon(Icons.Filled.Create, contentDescription = stringResource(R.string.mix_rename_content_description))
                            }
                            val deleteInteractionSource = remember { MutableInteractionSource() }
                            IconButton(
                                onClick = { mixEditorDeleteTrigger++ },
                                interactionSource = deleteInteractionSource,
                                modifier = Modifier.pressScale(deleteInteractionSource),
                            ) {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = stringResource(R.string.mix_delete_button),
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        },
                    )
                }
                currentRoute == Routes.SETTINGS -> {
                    AppTopBar(
                        title = stringResource(R.string.account_preferences_section),
                        onBack = { navController.popBackStack() },
                    )
                }
                currentRoute == Routes.SUPPORT -> {
                    AppTopBar(
                        title = stringResource(R.string.support_support_section),
                        onBack = { navController.popBackStack() },
                    )
                }
                currentRoute == Routes.TRUST -> {
                    AppTopBar(
                        title = stringResource(R.string.support_trust_section),
                        onBack = { navController.popBackStack() },
                    )
                }
                currentRoute == Routes.ABOUT -> {
                    AppTopBar(
                        title = stringResource(R.string.support_about_section),
                        onBack = { navController.popBackStack() },
                    )
                }
                else -> AppTopBar(onBack = { navController.popBackStack() })
            }
        },
        bottomBar = {
            if (isTabsRoute || currentRoute == Routes.PICKER || currentRoute == Routes.SUPPORT_INFO) {
                Column {
                    if (isTabsRoute && activeGameKey != null) {
                        val activeGameId = GameId.entries.firstOrNull { it.key == activeGameKey }
                        val activeGameTitle = activeGameId
                            ?.let { stringResource(GameCatalog.games.first { meta -> meta.id == it }.titleRes) }
                        if (activeGameId != null && activeGameTitle != null) {
                            ContinueSessionMiniBar(
                                gameTitle = activeGameTitle,
                                onClick = {
                                    navController.navigate(resumeActiveRoute(activeGameId))
                                },
                            )
                        }
                    }
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
            }
        },
    ) { padding ->
        SharedTransitionLayout {
            NavHost(navController = navController, startDestination = Routes.TABS, modifier = Modifier.padding(padding)) {
            composable(
                Routes.TABS,
                enterTransition = { fadeIn(animationSpec = tween(250)) },
                exitTransition = { fadeOut(animationSpec = tween(200)) },
            ) {
                CompositionLocalProvider(
                    LocalSharedTransitionScope provides this@SharedTransitionLayout,
                    LocalAnimatedVisibilityScope provides this,
                ) {
                    AnimatedContent(
                        targetState = selectedTab,
                        label = "bottomNavTabCrossfade",
                        transitionSpec = { fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(150)) },
                    ) { tab ->
                        when (tab) {
                            AppTab.DAILY -> DailyScreen(
                                onOpenAccount = { scope.launch { accountDrawerState.open() } },
                                onStartDaily = { gameId, difficulty, date ->
                                    val route = Routes.play(gameId, difficulty, resume = false, daily = date)
                                    if (activeGameKey != null) pendingDailyRoute = route else navController.navigate(route)
                                },
                                onResumeDaily = { gameId -> navController.navigate(resumeActiveRoute(gameId)) },
                            )
                            AppTab.GAMES -> GamesScreen(
                                onOpenGame = { gameId -> navController.navigate(Routes.picker(gameId)) },
                                onOpenAccount = { scope.launch { accountDrawerState.open() } },
                            )
                            AppTab.MIXES -> MixesScreen(
                                mixes = mixes,
                                onPlay = { mix -> if (activeGameKey != null) pendingMixToStart = mix else startMix(mix) },
                                onEdit = { mixId -> navController.navigate(Routes.mixEditor(mixId)) },
                                onNewMix = {
                                    scope.launch {
                                        val newMix = Mix(
                                            id = UUID.randomUUID().toString(),
                                            name = nextMixName(mixes.size),
                                            entries = emptyList(),
                                        )
                                        repositories.mixRepository.saveMix(newMix)
                                        navController.navigate(Routes.mixEditor(newMix.id))
                                    }
                                },
                                onOpenAccount = { scope.launch { accountDrawerState.open() } },
                            )
                            AppTab.STATS -> StatsScreen(onOpenAccount = { scope.launch { accountDrawerState.open() } })
                        }
                    }
                }
            }

            composable(
                Routes.PICKER,
                arguments = listOf(navArgument("gameId") { type = NavType.StringType }),
                enterTransition = { fadeIn(animationSpec = tween(250)) },
                exitTransition = { fadeOut(animationSpec = tween(200)) },
            ) { entry ->
                CompositionLocalProvider(
                    LocalSharedTransitionScope provides this@SharedTransitionLayout,
                    LocalAnimatedVisibilityScope provides this,
                ) {
                    val gameId = GameId.entries.first { it.key == entry.arguments?.getString("gameId") }
                    PuzzlePickerScreen(
                        gameId = gameId,
                        onPickDifficulty = { difficulty ->
                            scope.launch { repositories.mixRepository.clearActiveMix() }
                            navController.navigate(Routes.play(gameId, difficulty, resume = false))
                        },
                        onResumeActiveGame = { activeGameId -> navController.navigate(resumeActiveRoute(activeGameId)) },
                        onStartChallenger = {
                            scope.launch { repositories.mixRepository.clearActiveMix() }
                            navController.navigate(Routes.challenger(gameId))
                        },
                    )
                }
            }

            composable(
                Routes.MIX_EDITOR,
                arguments = listOf(navArgument("mixId") { type = NavType.StringType }),
                enterTransition = { fadeIn(animationSpec = tween(250)) },
                exitTransition = { fadeOut(animationSpec = tween(200)) },
            ) { entry ->
                val mixId = entry.arguments?.getString("mixId") ?: return@composable
                LaunchedEffect(mixId) {
                    mixEditorRenameTrigger = 0
                    mixEditorDeleteTrigger = 0
                }
                MixEditorScreen(
                    mixId = mixId,
                    onDone = { navController.popBackStack() },
                    renameTrigger = mixEditorRenameTrigger,
                    deleteTrigger = mixEditorDeleteTrigger,
                )
            }

            composable(
                Routes.PLAY,
                arguments = listOf(
                    navArgument("gameId") { type = NavType.StringType },
                    navArgument("difficulty") { type = NavType.StringType },
                    navArgument("resume") { type = NavType.BoolType },
                    navArgument("daily") { type = NavType.StringType; nullable = true; defaultValue = null },
                ),
                enterTransition = { fadeIn(animationSpec = tween(250)) },
                exitTransition = { fadeOut(animationSpec = tween(200)) },
            ) { entry ->
                CompositionLocalProvider(
                    LocalSharedTransitionScope provides this@SharedTransitionLayout,
                    LocalAnimatedVisibilityScope provides this,
                ) {
                    val difficulty = Difficulty.fromKey(entry.arguments?.getString("difficulty") ?: "easy")
                    val resume = entry.arguments?.getBoolean("resume") ?: false
                    val gameId = GameId.entries.first { it.key == entry.arguments?.getString("gameId") }
                    val dailyKey = entry.arguments?.getString("daily")
                    val dailyDate = dailyKey?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

                    fun goToCompletion(resultDifficulty: Difficulty, score: Int, accuracyPct: Int, elapsedSeconds: Int, isFirstSolve: Boolean, isNewHighScore: Boolean, bestTile: Int) {
                        navController.navigate(
                            Routes.completion(gameId, resultDifficulty, score, accuracyPct, elapsedSeconds, isFirstSolve, isNewHighScore, bestTile, daily = dailyKey),
                        ) { popUpTo(Routes.TABS) { inclusive = false } }
                    }

                    fun goToLoss(resultDifficulty: Difficulty, elapsedSeconds: Int, reason: String, score: Int, bestTile: Int) {
                        navController.navigate(
                            Routes.loss(gameId, resultDifficulty, elapsedSeconds, reason, score, bestTile, daily = dailyKey),
                        ) { popUpTo(Routes.TABS) { inclusive = false } }
                    }

                    Column(Modifier.fillMaxSize()) {
                        if (dailyKey != null) DailyPlayBanner(dailyKey)
                        Box(Modifier.weight(1f)) {
                            when (gameId) {
                                GameId.TAKUZU -> TakuzuPlayScreen(
                                    difficulty = difficulty,
                                    resume = resume,
                                    dailyDate = dailyDate,
                                    onBack = { navController.popBackStack() },
                                    onFinished = { result ->
                                        AnalyzerHandoff.set(result.analyzerSnapshot)
                                        if (result.solved) {
                                            goToCompletion(result.difficulty, result.score, result.accuracyPct, result.elapsedSeconds, result.isFirstSolve, result.isNewHighScore, 0)
                                        } else {
                                            goToLoss(result.difficulty, result.elapsedSeconds, result.lossReason ?: "abandoned", result.score, 0)
                                        }
                                    },
                                )
                                GameId.NONOGRAM -> NonogramPlayScreen(
                                    difficulty = difficulty,
                                    resume = resume,
                                    onBack = { navController.popBackStack() },
                                    onFinished = { result ->
                                        if (result.solved) {
                                            CompletionExtras.set(CompletionHighlight.Picture(result.solution))
                                            goToCompletion(result.difficulty, result.score, 100, result.elapsedSeconds, result.isFirstSolve, result.isNewHighScore, 0)
                                        } else {
                                            goToLoss(result.difficulty, result.elapsedSeconds, result.lossReason ?: "abandoned", result.score, 0)
                                        }
                                    },
                                )
                                GameId.MINESWEEPER -> MinesweeperPlayScreen(
                                    difficulty = difficulty,
                                    resume = resume,
                                    onBack = { navController.popBackStack() },
                                    onFinished = { result ->
                                        if (result.solved) {
                                            goToCompletion(result.difficulty, result.score, 100, result.elapsedSeconds, result.isFirstSolve, result.isNewHighScore, 0)
                                        } else {
                                            goToLoss(result.difficulty, result.elapsedSeconds, result.lossReason ?: "abandoned", result.score, 0)
                                        }
                                    },
                                )
                                GameId.SUDOKU -> SudokuPlayScreen(
                                    difficulty = difficulty,
                                    resume = resume,
                                    dailyDate = dailyDate,
                                    onBack = { navController.popBackStack() },
                                    onFinished = { result ->
                                        if (result.solved) {
                                            goToCompletion(result.difficulty, result.score, result.accuracyPct, result.elapsedSeconds, result.isFirstSolve, result.isNewHighScore, 0)
                                        } else {
                                            goToLoss(result.difficulty, result.elapsedSeconds, result.lossReason ?: "abandoned", result.score, 0)
                                        }
                                    },
                                )
                                GameId.WORDSEARCH -> WordSearchPlayScreen(
                                    difficulty = difficulty,
                                    resume = resume,
                                    dailyDate = dailyDate,
                                    onBack = { navController.popBackStack() },
                                    onFinished = { result ->
                                        if (result.solved) {
                                            wordSearchThemeIcon(result.themeId)?.let { CompletionExtras.set(CompletionHighlight.ThemeIcon(it)) }
                                            goToCompletion(result.difficulty, result.score, result.accuracyPct, result.elapsedSeconds, result.isFirstSolve, result.isNewHighScore, 0)
                                        } else {
                                            goToLoss(result.difficulty, result.elapsedSeconds, result.lossReason ?: "abandoned", result.score, 0)
                                        }
                                    },
                                )
                                GameId.BLOCKFILL -> BlockFillPlayScreen(
                                    difficulty = difficulty,
                                    resume = resume,
                                    onBack = { navController.popBackStack() },
                                    onFinished = { result ->
                                        if (result.solved) {
                                            goToCompletion(result.difficulty, result.score, 100, result.elapsedSeconds, result.isFirstSolve, result.isNewHighScore, 0)
                                        } else {
                                            goToLoss(result.difficulty, result.elapsedSeconds, result.lossReason ?: "abandoned", result.score, 0)
                                        }
                                    },
                                )
                                GameId.WORDGUESS -> WordGuessPlayScreen(
                                    difficulty = difficulty,
                                    resume = resume,
                                    dailyDate = dailyDate,
                                    onBack = { navController.popBackStack() },
                                    onFinished = { result ->
                                        if (result.solved) {
                                            goToCompletion(result.difficulty, result.score, 100, result.elapsedSeconds, result.isFirstSolve, result.isNewHighScore, 0)
                                        } else {
                                            if (result.targetWord.isNotEmpty()) {
                                                CompletionExtras.set(CompletionHighlight.RevealWord(result.targetWord))
                                            }
                                            goToLoss(result.difficulty, result.elapsedSeconds, result.lossReason ?: "abandoned", result.score, 0)
                                        }
                                    },
                                )
                                GameId.GUESSBYNUMBERS -> GuessByNumbersPlayScreen(
                                    difficulty = difficulty,
                                    resume = resume,
                                    onBack = { navController.popBackStack() },
                                    onFinished = { result ->
                                        if (result.solved) {
                                            goToCompletion(result.difficulty, result.score, 100, result.elapsedSeconds, result.isFirstSolve, result.isNewHighScore, 0)
                                        } else {
                                            if (result.targetWord.isNotEmpty()) {
                                                CompletionExtras.set(CompletionHighlight.RevealWord(result.targetWord))
                                            }
                                            goToLoss(result.difficulty, result.elapsedSeconds, result.lossReason ?: "abandoned", result.score, 0)
                                        }
                                    },
                                )
                                GameId.ANIMALDOKU -> AnimalDokuPlayScreen(
                                    difficulty = difficulty,
                                    resume = resume,
                                    dailyDate = dailyDate,
                                    onBack = { navController.popBackStack() },
                                    onFinished = { result ->
                                        if (result.solved) {
                                            goToCompletion(result.difficulty, result.score, 100, result.elapsedSeconds, result.isFirstSolve, result.isNewHighScore, 0)
                                        } else {
                                            goToLoss(result.difficulty, result.elapsedSeconds, result.lossReason ?: "abandoned", result.score, 0)
                                        }
                                    },
                                )
                                GameId.ARROWESCAPE -> ArrowEscapePlayScreen(
                                    difficulty = difficulty,
                                    resume = resume,
                                    onBack = { navController.popBackStack() },
                                    onFinished = { result ->
                                        if (result.solved) {
                                            goToCompletion(result.difficulty, result.score, 100, result.elapsedSeconds, result.isFirstSolve, result.isNewHighScore, 0)
                                        } else {
                                            goToLoss(result.difficulty, result.elapsedSeconds, result.lossReason ?: "abandoned", result.score, 0)
                                        }
                                    },
                                )
                                GameId.STARBATTLE -> StarBattlePlayScreen(
                                    difficulty = difficulty,
                                    resume = resume,
                                    onBack = { navController.popBackStack() },
                                    onFinished = { result ->
                                        if (result.solved) {
                                            goToCompletion(result.difficulty, result.score, 100, result.elapsedSeconds, result.isFirstSolve, result.isNewHighScore, 0)
                                        } else {
                                            goToLoss(result.difficulty, result.elapsedSeconds, result.lossReason ?: "abandoned", result.score, 0)
                                        }
                                    },
                                )
                                GameId.GAME_2048 -> Game2048PlayScreen(
                                    difficulty = difficulty,
                                    resume = resume,
                                    onBack = { navController.popBackStack() },
                                    onFinished = { result ->
                                        if (result.solved) {
                                            goToCompletion(result.difficulty, result.score, 100, result.elapsedSeconds, result.isFirstSolve, result.isNewHighScore, result.bestTile)
                                        } else {
                                            goToLoss(result.difficulty, result.elapsedSeconds, result.lossReason ?: "abandoned", result.score, result.bestTile)
                                        }
                                    },
                                )
                                GameId.NBACK -> NBackPlayScreen(
                                    difficulty = difficulty,
                                    resume = resume,
                                    onBack = { navController.popBackStack() },
                                    onFinished = { result ->
                                        if (result.solved) {
                                            CompletionExtras.set(
                                                CompletionHighlight.NBackBreakdown(
                                                    hits = result.hits,
                                                    misses = result.misses,
                                                    falsePositives = result.falsePositives,
                                                    avgReactionTimeMs = result.averageReactionTimeMs,
                                                ),
                                            )
                                            goToCompletion(result.difficulty, result.score, result.accuracyPct, result.elapsedSeconds, result.isFirstSolve, result.isNewHighScore, 0)
                                        } else {
                                            goToLoss(result.difficulty, result.elapsedSeconds, result.lossReason ?: "abandoned", result.score, 0)
                                        }
                                    },
                                )
                                GameId.FLOWFREE -> FlowFreePlayScreen(
                                    difficulty = difficulty,
                                    resume = resume,
                                    onBack = { navController.popBackStack() },
                                    onFinished = { result ->
                                        if (result.solved) {
                                            goToCompletion(result.difficulty, result.score, 100, result.elapsedSeconds, result.isFirstSolve, result.isNewHighScore, 0)
                                        } else {
                                            goToLoss(result.difficulty, result.elapsedSeconds, result.lossReason ?: "abandoned", result.score, 0)
                                        }
                                    },
                                )
                                else -> ChimpTestPlayScreen(
                                    difficulty = difficulty,
                                    resume = resume,
                                    onBack = { navController.popBackStack() },
                                    onFinished = { result ->
                                        if (result.solved) {
                                            goToCompletion(result.difficulty, result.score, 100, result.elapsedSeconds, result.isFirstSolve, result.isNewHighScore, 0)
                                        } else {
                                            goToLoss(result.difficulty, result.elapsedSeconds, result.lossReason ?: "abandoned", result.score, 0)
                                        }
                                    },
                                )
                            }
                        }
                    }
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
                    navArgument("bestTile") { type = NavType.IntType },
                    navArgument("daily") { type = NavType.StringType; nullable = true; defaultValue = null },
                ),
                enterTransition = { fadeIn(animationSpec = tween(250)) },
                exitTransition = { fadeOut(animationSpec = tween(200)) },
            ) { entry ->
                CompositionLocalProvider(
                    LocalSharedTransitionScope provides this@SharedTransitionLayout,
                    LocalAnimatedVisibilityScope provides this,
                ) {
                    val completionGameId = GameId.entries.first { it.key == entry.arguments?.getString("gameId") }
                    val completionDifficulty = Difficulty.fromKey(entry.arguments?.getString("difficulty") ?: "easy")
                    val resultDailyKey = entry.arguments?.getString("daily")
                    val nextDaily = rememberNextDaily(resultDailyKey, completionGameId, completionDifficulty)
                    val resultContext = LocalContext.current
                    val completionEligibleMixes = remember(mixes, completionGameId, completionDifficulty) {
                        mixesEligibleForQuickAdd(mixes, completionGameId, completionDifficulty)
                    }
                    CompletionScreen(
                        gameId = completionGameId,
                        difficulty = completionDifficulty,
                        score = entry.arguments?.getInt("score") ?: 0,
                        accuracyPct = entry.arguments?.getInt("accuracyPct") ?: 100,
                        elapsedSeconds = entry.arguments?.getInt("elapsedSeconds") ?: 0,
                        bestTile = entry.arguments?.getInt("bestTile") ?: 0,
                        isFirstSolve = entry.arguments?.getBoolean("isFirstSolve") ?: false,
                        isNewHighScore = entry.arguments?.getBoolean("isNewHighScore") ?: false,
                        isMixActive = activeMix != null,
                        eligibleMixes = completionEligibleMixes,
                        onAddToMix = { mix ->
                            scope.launch {
                                repositories.mixRepository.addEntryToMix(
                                    mix.id,
                                    MixEntry(gameId = completionGameId.key, mode = MixEntryMode.PUZZLE, difficulty = completionDifficulty.key, weight = 1),
                                )
                            }
                        },
                        onPlayAgain = {
                            playAgainOrDrawMix {
                                navController.navigate(Routes.play(completionGameId, completionDifficulty, resume = false)) {
                                    popUpTo(Routes.TABS) { inclusive = false }
                                }
                            }
                        },
                        onOtherDifficulty = {
                            navController.navigate(Routes.picker(completionGameId)) {
                                popUpTo(Routes.TABS) { inclusive = false }
                            }
                        },
                        onTryAnotherGame = { endMixAndGoToGames() },
                        dailyDate = resultDailyKey,
                        onShareDaily = {
                            if (resultDailyKey != null) {
                                scope.launch {
                                    shareDailyResult(resultContext, repositories.playHistoryRepository, completionGameId, completionDifficulty, resultDailyKey)
                                }
                            }
                        },
                        onBackToDaily = { goToDailyTab() },
                        hasNextDaily = nextDaily != null,
                        onPlayNextDaily = { playNextDaily(nextDaily, resultDailyKey) },
                    )
                }
            }

            composable(
                Routes.LOSS,
                arguments = listOf(
                    navArgument("gameId") { type = NavType.StringType },
                    navArgument("difficulty") { type = NavType.StringType },
                    navArgument("elapsedSeconds") { type = NavType.IntType },
                    navArgument("reason") { type = NavType.StringType },
                    navArgument("score") { type = NavType.IntType },
                    navArgument("bestTile") { type = NavType.IntType },
                    navArgument("daily") { type = NavType.StringType; nullable = true; defaultValue = null },
                ),
                enterTransition = { fadeIn(animationSpec = tween(250)) },
                exitTransition = { fadeOut(animationSpec = tween(200)) },
            ) { entry ->
                CompositionLocalProvider(
                    LocalSharedTransitionScope provides this@SharedTransitionLayout,
                    LocalAnimatedVisibilityScope provides this,
                ) {
                    val lossGameId = GameId.entries.first { it.key == entry.arguments?.getString("gameId") }
                    val lossDifficulty = Difficulty.fromKey(entry.arguments?.getString("difficulty") ?: "easy")
                    val resultDailyKey = entry.arguments?.getString("daily")
                    val nextDaily = rememberNextDaily(resultDailyKey, lossGameId, lossDifficulty)
                    val resultContext = LocalContext.current
                    val lossEligibleMixes = remember(mixes, lossGameId, lossDifficulty) {
                        mixesEligibleForQuickAdd(mixes, lossGameId, lossDifficulty)
                    }
                    LossScreen(
                        gameId = lossGameId,
                        difficulty = lossDifficulty,
                        elapsedSeconds = entry.arguments?.getInt("elapsedSeconds") ?: 0,
                        reason = entry.arguments?.getString("reason") ?: "abandoned",
                        score = entry.arguments?.getInt("score") ?: 0,
                        bestTile = entry.arguments?.getInt("bestTile") ?: 0,
                        isMixActive = activeMix != null,
                        eligibleMixes = lossEligibleMixes,
                        onAddToMix = { mix ->
                            scope.launch {
                                repositories.mixRepository.addEntryToMix(
                                    mix.id,
                                    MixEntry(gameId = lossGameId.key, mode = MixEntryMode.PUZZLE, difficulty = lossDifficulty.key, weight = 1),
                                )
                            }
                        },
                        onRetry = {
                            playAgainOrDrawMix {
                                navController.navigate(Routes.play(lossGameId, lossDifficulty, resume = false)) {
                                    popUpTo(Routes.TABS) { inclusive = false }
                                }
                            }
                        },
                        onOtherDifficulty = {
                            navController.navigate(Routes.picker(lossGameId)) {
                                popUpTo(Routes.TABS) { inclusive = false }
                            }
                        },
                        onTryAnotherGame = { endMixAndGoToGames() },
                        onWalkThroughSolve = {
                            navController.navigate(Routes.analyzer(lossGameId))
                        },
                        dailyDate = resultDailyKey,
                        onShareDaily = {
                            if (resultDailyKey != null) {
                                scope.launch {
                                    shareDailyResult(resultContext, repositories.playHistoryRepository, lossGameId, lossDifficulty, resultDailyKey)
                                }
                            }
                        },
                        onBackToDaily = { goToDailyTab() },
                        hasNextDaily = nextDaily != null,
                        onPlayNextDaily = { playNextDaily(nextDaily, resultDailyKey) },
                    )
                }
            }

            composable(
                Routes.CHALLENGER,
                arguments = listOf(navArgument("gameId") { type = NavType.StringType }),
                enterTransition = { fadeIn(animationSpec = tween(250)) },
                exitTransition = { fadeOut(animationSpec = tween(200)) },
            ) { entry ->
                val challengerGameId = GameId.entries.first { it.key == entry.arguments?.getString("gameId") }
                when (challengerGameId) {
                    GameId.ANIMALDOKU -> AnimalDokuChallengerPlayScreen(
                        onFinished = { result ->
                            ChallengerExtras.set(ChallengerRunDetails(result.puzzleHistory, result.solvesInTier))
                            navController.navigate(
                                Routes.challengerResult(challengerGameId, result.puzzlesSolved, result.tierReached, result.score, result.isNewHighScore, result.reason, result.previousBest, result.fastestSolveSeconds),
                            ) { popUpTo(Routes.TABS) { inclusive = false } }
                        },
                    )
                    GameId.WORDGUESS -> WordGuessChallengerPlayScreen(
                        onFinished = { result ->
                            ChallengerExtras.set(ChallengerRunDetails(result.puzzleHistory, result.solvesInTier))
                            navController.navigate(
                                Routes.challengerResult(challengerGameId, result.puzzlesSolved, result.tierReached, result.score, result.isNewHighScore, result.reason, result.previousBest, result.fastestSolveSeconds),
                            ) { popUpTo(Routes.TABS) { inclusive = false } }
                        },
                    )
                    GameId.CHIMPTEST -> ChimpTestChallengerPlayScreen(
                        onFinished = { result ->
                            ChallengerExtras.set(ChallengerRunDetails(result.puzzleHistory, result.solvesInTier))
                            navController.navigate(
                                Routes.challengerResult(challengerGameId, result.puzzlesSolved, result.tierReached, result.score, result.isNewHighScore, result.reason, result.previousBest, result.fastestSolveSeconds),
                            ) { popUpTo(Routes.TABS) { inclusive = false } }
                        },
                    )
                    GameId.STARBATTLE -> StarBattleChallengerPlayScreen(
                        onFinished = { result ->
                            ChallengerExtras.set(ChallengerRunDetails(result.puzzleHistory, result.solvesInTier))
                            navController.navigate(
                                Routes.challengerResult(challengerGameId, result.puzzlesSolved, result.tierReached, result.score, result.isNewHighScore, result.reason, result.previousBest, result.fastestSolveSeconds),
                            ) { popUpTo(Routes.TABS) { inclusive = false } }
                        },
                    )
                    GameId.SUDOKU -> SudokuChallengerPlayScreen(
                        onFinished = { result ->
                            ChallengerExtras.set(ChallengerRunDetails(result.puzzleHistory, result.solvesInTier))
                            navController.navigate(
                                Routes.challengerResult(challengerGameId, result.puzzlesSolved, result.tierReached, result.score, result.isNewHighScore, result.reason, result.previousBest, result.fastestSolveSeconds),
                            ) { popUpTo(Routes.TABS) { inclusive = false } }
                        },
                    )
                    GameId.TAKUZU -> TakuzuChallengerPlayScreen(
                        onFinished = { result ->
                            ChallengerExtras.set(ChallengerRunDetails(result.puzzleHistory, result.solvesInTier))
                            navController.navigate(
                                Routes.challengerResult(challengerGameId, result.puzzlesSolved, result.tierReached, result.score, result.isNewHighScore, result.reason, result.previousBest, result.fastestSolveSeconds),
                            ) { popUpTo(Routes.TABS) { inclusive = false } }
                        },
                    )
                    GameId.NONOGRAM -> NonogramChallengerPlayScreen(
                        onFinished = { result ->
                            ChallengerExtras.set(ChallengerRunDetails(result.puzzleHistory, result.solvesInTier))
                            navController.navigate(
                                Routes.challengerResult(challengerGameId, result.puzzlesSolved, result.tierReached, result.score, result.isNewHighScore, result.reason, result.previousBest, result.fastestSolveSeconds),
                            ) { popUpTo(Routes.TABS) { inclusive = false } }
                        },
                    )
                    else -> Unit
                }
            }

            composable(
                Routes.CHALLENGER_RESULT,
                arguments = listOf(
                    navArgument("gameId") { type = NavType.StringType },
                    navArgument("puzzlesSolved") { type = NavType.IntType },
                    navArgument("tier") { type = NavType.StringType },
                    navArgument("score") { type = NavType.IntType },
                    navArgument("isNewHighScore") { type = NavType.BoolType },
                    navArgument("reason") { type = NavType.StringType },
                    navArgument("previousBest") { type = NavType.IntType },
                    navArgument("fastestSolveSeconds") { type = NavType.FloatType },
                ),
                enterTransition = { fadeIn(animationSpec = tween(250)) },
                exitTransition = { fadeOut(animationSpec = tween(200)) },
            ) { entry ->
                CompositionLocalProvider(
                    LocalSharedTransitionScope provides this@SharedTransitionLayout,
                    LocalAnimatedVisibilityScope provides this,
                ) {
                    val resultGameId = GameId.entries.first { it.key == entry.arguments?.getString("gameId") }
                    val previousBest = entry.arguments?.getInt("previousBest") ?: 0
                    val fastestSolveSeconds = entry.arguments?.getFloat("fastestSolveSeconds")?.takeIf { it >= 0f }?.toDouble()
                    when (resultGameId) {
                        GameId.ANIMALDOKU -> AnimalDokuChallengerResultScreen(
                            puzzlesSolved = entry.arguments?.getInt("puzzlesSolved") ?: 0,
                            tierReached = Difficulty.fromKey(entry.arguments?.getString("tier") ?: "easy"),
                            score = entry.arguments?.getInt("score") ?: 0,
                            isNewHighScore = entry.arguments?.getBoolean("isNewHighScore") ?: false,
                            reason = entry.arguments?.getString("reason") ?: "time_up",
                            previousBest = previousBest,
                            fastestSolveSeconds = fastestSolveSeconds,
                            isMixActive = activeMix != null,
                            onPlayAgain = {
                                playAgainOrDrawMix {
                                    navController.navigate(Routes.challenger(resultGameId)) {
                                        popUpTo(Routes.TABS) { inclusive = false }
                                    }
                                }
                            },
                            onBackToPuzzles = {
                                navController.navigate(Routes.picker(resultGameId)) {
                                    popUpTo(Routes.TABS) { inclusive = false }
                                }
                            },
                            onTryAnotherGame = { endMixAndGoToGames() },
                        )
                        GameId.WORDGUESS -> WordGuessChallengerResultScreen(
                            puzzlesSolved = entry.arguments?.getInt("puzzlesSolved") ?: 0,
                            tierReached = Difficulty.fromKey(entry.arguments?.getString("tier") ?: "easy"),
                            score = entry.arguments?.getInt("score") ?: 0,
                            isNewHighScore = entry.arguments?.getBoolean("isNewHighScore") ?: false,
                            reason = entry.arguments?.getString("reason") ?: "time_up",
                            previousBest = previousBest,
                            fastestSolveSeconds = fastestSolveSeconds,
                            isMixActive = activeMix != null,
                            onPlayAgain = {
                                playAgainOrDrawMix {
                                    navController.navigate(Routes.challenger(resultGameId)) {
                                        popUpTo(Routes.TABS) { inclusive = false }
                                    }
                                }
                            },
                            onBackToPuzzles = {
                                navController.navigate(Routes.picker(resultGameId)) {
                                    popUpTo(Routes.TABS) { inclusive = false }
                                }
                            },
                            onTryAnotherGame = { endMixAndGoToGames() },
                        )
                        GameId.CHIMPTEST -> ChimpTestChallengerResultScreen(
                            puzzlesSolved = entry.arguments?.getInt("puzzlesSolved") ?: 0,
                            tierReached = Difficulty.fromKey(entry.arguments?.getString("tier") ?: "easy"),
                            score = entry.arguments?.getInt("score") ?: 0,
                            isNewHighScore = entry.arguments?.getBoolean("isNewHighScore") ?: false,
                            reason = entry.arguments?.getString("reason") ?: "time_up",
                            previousBest = previousBest,
                            fastestSolveSeconds = fastestSolveSeconds,
                            isMixActive = activeMix != null,
                            onPlayAgain = {
                                playAgainOrDrawMix {
                                    navController.navigate(Routes.challenger(resultGameId)) {
                                        popUpTo(Routes.TABS) { inclusive = false }
                                    }
                                }
                            },
                            onBackToPuzzles = {
                                navController.navigate(Routes.picker(resultGameId)) {
                                    popUpTo(Routes.TABS) { inclusive = false }
                                }
                            },
                            onTryAnotherGame = { endMixAndGoToGames() },
                        )
                        GameId.SUDOKU -> SudokuChallengerResultScreen(
                            puzzlesSolved = entry.arguments?.getInt("puzzlesSolved") ?: 0,
                            tierReached = Difficulty.fromKey(entry.arguments?.getString("tier") ?: "easy"),
                            score = entry.arguments?.getInt("score") ?: 0,
                            isNewHighScore = entry.arguments?.getBoolean("isNewHighScore") ?: false,
                            reason = entry.arguments?.getString("reason") ?: "time_up",
                            previousBest = previousBest,
                            fastestSolveSeconds = fastestSolveSeconds,
                            isMixActive = activeMix != null,
                            onPlayAgain = {
                                playAgainOrDrawMix {
                                    navController.navigate(Routes.challenger(resultGameId)) {
                                        popUpTo(Routes.TABS) { inclusive = false }
                                    }
                                }
                            },
                            onBackToPuzzles = {
                                navController.navigate(Routes.picker(resultGameId)) {
                                    popUpTo(Routes.TABS) { inclusive = false }
                                }
                            },
                            onTryAnotherGame = { endMixAndGoToGames() },
                        )
                        GameId.TAKUZU -> TakuzuChallengerResultScreen(
                            puzzlesSolved = entry.arguments?.getInt("puzzlesSolved") ?: 0,
                            tierReached = Difficulty.fromKey(entry.arguments?.getString("tier") ?: "easy"),
                            score = entry.arguments?.getInt("score") ?: 0,
                            isNewHighScore = entry.arguments?.getBoolean("isNewHighScore") ?: false,
                            reason = entry.arguments?.getString("reason") ?: "time_up",
                            previousBest = previousBest,
                            fastestSolveSeconds = fastestSolveSeconds,
                            isMixActive = activeMix != null,
                            onPlayAgain = {
                                playAgainOrDrawMix {
                                    navController.navigate(Routes.challenger(resultGameId)) {
                                        popUpTo(Routes.TABS) { inclusive = false }
                                    }
                                }
                            },
                            onBackToPuzzles = {
                                navController.navigate(Routes.picker(resultGameId)) {
                                    popUpTo(Routes.TABS) { inclusive = false }
                                }
                            },
                            onTryAnotherGame = { endMixAndGoToGames() },
                        )
                        GameId.NONOGRAM -> NonogramChallengerResultScreen(
                            puzzlesSolved = entry.arguments?.getInt("puzzlesSolved") ?: 0,
                            tierReached = Difficulty.fromKey(entry.arguments?.getString("tier") ?: "easy"),
                            score = entry.arguments?.getInt("score") ?: 0,
                            isNewHighScore = entry.arguments?.getBoolean("isNewHighScore") ?: false,
                            reason = entry.arguments?.getString("reason") ?: "time_up",
                            previousBest = previousBest,
                            fastestSolveSeconds = fastestSolveSeconds,
                            isMixActive = activeMix != null,
                            onPlayAgain = {
                                playAgainOrDrawMix {
                                    navController.navigate(Routes.challenger(resultGameId)) {
                                        popUpTo(Routes.TABS) { inclusive = false }
                                    }
                                }
                            },
                            onBackToPuzzles = {
                                navController.navigate(Routes.picker(resultGameId)) {
                                    popUpTo(Routes.TABS) { inclusive = false }
                                }
                            },
                            onTryAnotherGame = { endMixAndGoToGames() },
                        )
                        GameId.STARBATTLE -> StarBattleChallengerResultScreen(
                            puzzlesSolved = entry.arguments?.getInt("puzzlesSolved") ?: 0,
                            tierReached = Difficulty.fromKey(entry.arguments?.getString("tier") ?: "easy"),
                            score = entry.arguments?.getInt("score") ?: 0,
                            isNewHighScore = entry.arguments?.getBoolean("isNewHighScore") ?: false,
                            reason = entry.arguments?.getString("reason") ?: "time_up",
                            previousBest = previousBest,
                            fastestSolveSeconds = fastestSolveSeconds,
                            isMixActive = activeMix != null,
                            onPlayAgain = {
                                playAgainOrDrawMix {
                                    navController.navigate(Routes.challenger(resultGameId)) {
                                        popUpTo(Routes.TABS) { inclusive = false }
                                    }
                                }
                            },
                            onBackToPuzzles = {
                                navController.navigate(Routes.picker(resultGameId)) {
                                    popUpTo(Routes.TABS) { inclusive = false }
                                }
                            },
                            onTryAnotherGame = { endMixAndGoToGames() },
                        )
                        else -> Unit
                    }
                }
            }

            composable(
                Routes.ANALYZER,
                arguments = listOf(navArgument("gameId") { type = NavType.StringType }),
                enterTransition = { fadeIn(animationSpec = tween(250)) },
                exitTransition = { fadeOut(animationSpec = tween(200)) },
            ) { entry ->
                val analyzerGameId = GameId.entries.first { it.key == entry.arguments?.getString("gameId") }
                val analyzerSnapshot = rememberSaveable { mutableStateOf(AnalyzerHandoff.consume()) }.value
                when (analyzerGameId) {
                    GameId.TAKUZU -> TakuzuAnalyzerScreen(
                        snapshot = analyzerSnapshot,
                        onBack = { navController.popBackStack() },
                    )
                    else -> Unit
                }
            }

            composable(
                Routes.SUPPORT_INFO,
                arguments = listOf(navArgument("key") { type = NavType.StringType }),
                enterTransition = { fadeIn(animationSpec = tween(250)) },
                exitTransition = { fadeOut(animationSpec = tween(200)) },
            ) { entry ->
                SupportInfoScreen(entry.arguments?.getString("key") ?: "about")
            }

            composable(
                Routes.SETTINGS,
                enterTransition = { fadeIn(animationSpec = tween(250)) },
                exitTransition = { fadeOut(animationSpec = tween(200)) },
            ) {
                SettingsPageScreen()
            }

            composable(
                Routes.SUPPORT,
                enterTransition = { fadeIn(animationSpec = tween(250)) },
                exitTransition = { fadeOut(animationSpec = tween(200)) },
            ) {
                SupportPageScreen()
            }

            composable(
                Routes.TRUST,
                enterTransition = { fadeIn(animationSpec = tween(250)) },
                exitTransition = { fadeOut(animationSpec = tween(200)) },
            ) {
                TrustPageScreen(onOpenInfo = { key -> navController.navigate(Routes.supportInfo(key)) })
            }

            composable(
                Routes.ABOUT,
                enterTransition = { fadeIn(animationSpec = tween(250)) },
                exitTransition = { fadeOut(animationSpec = tween(200)) },
            ) {
                AboutPageScreen(onOpenInfo = { key -> navController.navigate(Routes.supportInfo(key)) })
            }
            }
        }
    }
    }

    val dailyRouteToStart = pendingDailyRoute
    if (dailyRouteToStart != null) {
        AlertDialog(
            onDismissRequest = { pendingDailyRoute = null },
            title = { Text(stringResource(R.string.replace_dialog_title)) },
            text = {
                Text(
                    stringResource(
                        if (activeSession?.dailyDate != null) R.string.daily_replace_dialog_message else R.string.replace_dialog_message,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingDailyRoute = null
                    navController.navigate(dailyRouteToStart)
                }) { Text(stringResource(R.string.common_start_new_puzzle)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingDailyRoute = null
                    val activeGameId = activeGameKey?.let { key -> GameId.entries.firstOrNull { it.key == key } }
                    if (activeGameId != null) navController.navigate(resumeActiveRoute(activeGameId))
                }) { Text(stringResource(R.string.common_continue_puzzle)) }
            },
        )
    }

    val mixToStart = pendingMixToStart
    if (mixToStart != null) {
        AlertDialog(
            onDismissRequest = { pendingMixToStart = null },
            title = { Text(stringResource(R.string.replace_dialog_title)) },
            text = {
                Text(
                    stringResource(
                        if (activeSession?.dailyDate != null) R.string.daily_replace_dialog_message else R.string.replace_dialog_message,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingMixToStart = null
                    startMix(mixToStart)
                }) { Text(stringResource(R.string.common_start_new_puzzle)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingMixToStart = null
                    val activeGameId = activeGameKey?.let { key -> GameId.entries.firstOrNull { it.key == key } }
                    if (activeGameId != null) {
                        navController.navigate(resumeActiveRoute(activeGameId))
                    }
                }) { Text(stringResource(R.string.common_continue_puzzle)) }
            },
        )
    }
}

@Composable
private fun rememberNextDaily(dailyKey: String?, finishedGameId: GameId, finishedDifficulty: Difficulty): Pair<GameId, Difficulty>? {
    if (dailyKey == null) return null
    val dailyViewModel: DailyViewModel = hiltViewModel()
    val dailyState by dailyViewModel.state.collectAsState()
    if (dailyState.loading || dailyState.today.toString() != dailyKey) return null
    return remember(dailyState.games, finishedGameId, finishedDifficulty) {
        nextUnplayedDaily(dailyState.games, finishedGameId, finishedDifficulty)
    }
}
