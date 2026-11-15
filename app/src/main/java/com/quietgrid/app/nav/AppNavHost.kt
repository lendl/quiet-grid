package com.quietgrid.app.nav

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.quietgrid.app.LaunchRequest
import com.quietgrid.app.R
import com.quietgrid.app.core.GameCatalog
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.mix.Mix
import com.quietgrid.app.core.mix.nextMixName
import com.quietgrid.app.data.AppSettings
import com.quietgrid.app.data.GamePreferenceOverrides
import com.quietgrid.app.data.RepositoriesViewModel
import com.quietgrid.app.data.resolveGamePlayPreferences
import com.quietgrid.app.ui.components.AppTab
import com.quietgrid.app.ui.components.AppTopBar
import com.quietgrid.app.ui.components.BottomNavBar
import com.quietgrid.app.ui.components.ContinueSessionMiniBar
import com.quietgrid.app.ui.components.GamePageTopBar
import com.quietgrid.app.ui.components.ReplacePuzzleSheet
import com.quietgrid.app.ui.components.pressScale
import com.quietgrid.app.ui.play.LocalOpenGameRules
import com.quietgrid.app.ui.play.LocalQuietTipSignals
import com.quietgrid.app.ui.play.QuietTipSignalsViewModel
import com.quietgrid.app.ui.screens.AccountDrawerContent
import com.quietgrid.app.ui.screens.DailyScreen
import com.quietgrid.app.ui.screens.GamesScreen
import com.quietgrid.app.ui.screens.MixesScreen
import com.quietgrid.app.ui.screens.StatsScreen
import com.quietgrid.app.ui.screens.supportInfoTitleRes
import java.util.UUID
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }
val LocalAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost(launchRequest: LaunchRequest? = null, onLaunchRequestHandled: () -> Unit = {}) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    var selectedTab by remember { mutableStateOf(AppTab.GAMES) }
    val scope = rememberCoroutineScope()
    val repositories: RepositoriesViewModel = hiltViewModel()
    val settingsState = repositories.settingsRepository.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    val overridesState = repositories.gamePreferencesRepository.allOverrides.collectAsStateWithLifecycle(initialValue = emptyMap())
    val activeSessionState = repositories.sessionRepository.activeSessionSummary.collectAsStateWithLifecycle(initialValue = null)
    val mixesState = repositories.mixRepository.mixes.collectAsStateWithLifecycle(initialValue = emptyList())
    val activeMixIdState = repositories.mixRepository.activeMixId.collectAsStateWithLifecycle(initialValue = null)
    val activeMixState = remember { derivedStateOf { mixesState.value.firstOrNull { it.id == activeMixIdState.value } } }
    val mixes = mixesState.value
    val settings = settingsState.value
    val context = LocalContext.current
    val appContext = context.applicationContext
    val quietTipSignals: QuietTipSignalsViewModel = hiltViewModel()
    LaunchedEffect(selectedTab) {
        if (selectedTab == AppTab.DAILY) quietTipSignals.dailyTabOpened()
    }

    val navigator = remember(navController) {
        AppNavigator(
            navController = navController,
            scope = scope,
            repositories = repositories,
            appContext = appContext,
            activeSession = activeSessionState,
            activeMixState = activeMixState,
            resolvePreferences = { gameId -> resolveGamePlayPreferences(settingsState.value, overridesState.value[gameId] ?: GamePreferenceOverrides()) },
            selectTab = { tab -> selectedTab = tab },
        )
    }

    var pendingMixToStart by remember { mutableStateOf<Mix?>(null) }
    var pendingDailyRoute by remember { mutableStateOf<Any?>(null) }
    val accountDrawerState = rememberDrawerState(DrawerValue.Closed)
    val mixEditorTriggers = remember { MixEditorTriggers() }

    val destination = backStackEntry?.destination
    fun isOn(route: KClass<*>): Boolean = destination?.hasRoute(route) == true
    val isTabsRoute = destination == null || isOn(TabsRoute::class)

    LaunchRequestEffect(launchRequest, onLaunchRequestHandled) { request ->
        val session = repositories.sessionRepository.activeSessionSummary.first()
        val currentMixes = repositories.mixRepository.mixes.first()
        val onPlayScreen = navController.currentDestination?.let { current ->
            current.hasRoute<PlayRoute>() || current.hasRoute<ChallengerRoute>() || current.hasRoute<EndlessRoute>()
        } == true
        when (val action = launchAction(request, session, currentMixes.map { it.id }.toSet(), onPlayScreen)) {
            LaunchAction.OpenDailyTab -> navigator.openTab(AppTab.DAILY)
            LaunchAction.OpenMixesTab -> navigator.openTab(AppTab.MIXES)
            LaunchAction.OpenTabs -> navigator.popToTabs()
            LaunchAction.Stay -> Unit
            is LaunchAction.Navigate -> {
                if (action.endsMix) repositories.mixRepository.clearActiveMix()
                navigator.navigateFromTabs(action.route)
            }
            is LaunchAction.StartMix -> currentMixes.firstOrNull { it.id == action.mixId }?.let { navigator.startMix(it) }
        }
    }

    ModalNavigationDrawer(
        drawerState = accountDrawerState,
        gesturesEnabled = isTabsRoute || accountDrawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet {
                AccountDrawerContent(
                    onOpenSettings = { openFromDrawer(navigator, accountDrawerState, SettingsRoute) },
                    onOpenSupport = { openFromDrawer(navigator, accountDrawerState, SupportRoute) },
                    onOpenTrust = { openFromDrawer(navigator, accountDrawerState, TrustRoute) },
                    onOpenAbout = { openFromDrawer(navigator, accountDrawerState, AboutRoute) },
                )
            }
        },
    ) {
        Scaffold(
            topBar = {
                AppNavTopBar(
                    entry = backStackEntry,
                    isTabsRoute = isTabsRoute,
                    isOn = ::isOn,
                    navigator = navigator,
                    favoriteGameIds = settings.favoriteGameIds,
                    mixes = mixes,
                    mixEditorTriggers = mixEditorTriggers,
                )
            },
            bottomBar = {
                if (isTabsRoute || isOn(PickerRoute::class) || isOn(SupportInfoRoute::class)) {
                    Column {
                        val activeGameId = navigator.activeGameKey()?.let { key -> GameId.entries.firstOrNull { it.key == key } }
                        if (isTabsRoute && activeGameId != null) {
                            ContinueSessionMiniBar(
                                gameTitle = stringResource(GameCatalog.get(activeGameId).titleRes),
                                onClick = { navigator.resumeActiveGame(activeGameId) },
                            )
                        }
                        BottomNavBar(
                            selectedTab = selectedTab,
                            onSelectTab = { tab ->
                                selectedTab = tab
                                if (!isOn(TabsRoute::class)) navigator.popToTabs()
                            },
                        )
                    }
                }
            },
        ) { padding ->
            CompositionLocalProvider(
                LocalOpenGameRules provides { gameId -> navigator.navigate(RulesRoute(gameId)) },
                LocalQuietTipSignals provides quietTipSignals,
            ) {
                SharedTransitionLayout {
                    NavHost(
                        navController = navController,
                        startDestination = TabsRoute,
                        modifier = Modifier.padding(padding),
                        enterTransition = { fadeIn(animationSpec = tween(250)) },
                        exitTransition = { fadeOut(animationSpec = tween(200)) },
                        predictivePopEnterTransition = { fadeIn(animationSpec = tween(250)) },
                        predictivePopExitTransition = { fadeOut(animationSpec = tween(200)) },
                    ) {
                        composable<TabsRoute> {
                            CompositionLocalProvider(
                                LocalSharedTransitionScope provides this@SharedTransitionLayout,
                                LocalAnimatedVisibilityScope provides this,
                            ) {
                                AnimatedContent(
                                    targetState = selectedTab,
                                    modifier = Modifier.fillMaxSize(),
                                    label = "bottomNavTabCrossfade",
                                    transitionSpec = { fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(150)) },
                                ) { tab ->
                                    val openAccount: () -> Unit = { scope.launch { accountDrawerState.open() } }
                                    when (tab) {
                                        AppTab.DAILY -> DailyScreen(
                                            onOpenAccount = openAccount,
                                            onStartDaily = { gameId, difficulty, date ->
                                                val route = PlayRoute.of(gameId, difficulty, resume = false, daily = date)
                                                if (navigator.activeGameKey() != null) pendingDailyRoute = route else navigator.navigate(route)
                                            },
                                            onResumeDaily = { gameId -> navigator.resumeActiveGame(gameId) },
                                        )
                                        AppTab.GAMES -> GamesScreen(
                                            onOpenGame = { gameId -> navigator.navigate(PickerRoute(gameId)) },
                                            onOpenAccount = openAccount,
                                        )
                                        AppTab.MIXES -> MixesScreen(
                                            mixes = mixes,
                                            onPlay = { mix -> if (navigator.activeGameKey() != null) pendingMixToStart = mix else navigator.startMix(mix) },
                                            onEdit = { mixId -> navigator.navigate(MixEditorRoute(mixId)) },
                                            onNewMix = {
                                                scope.launch {
                                                    val newMix = Mix(
                                                        id = UUID.randomUUID().toString(),
                                                        name = nextMixName(mixes.map { it.name }) { context.resources.getString(R.string.mix_default_name, it) },
                                                        entries = emptyList(),
                                                    )
                                                    repositories.mixRepository.saveMix(newMix)
                                                    navigator.navigate(MixEditorRoute(newMix.id))
                                                }
                                            },
                                            onOpenAccount = openAccount,
                                        )
                                        AppTab.STATS -> StatsScreen(
                                            onOpenAccount = openAccount,
                                            onOpenMilestones = { navigator.navigate(MilestonesRoute) },
                                        )
                                    }
                                }
                            }
                        }
                        gamePageGraph(navigator, this@SharedTransitionLayout)
                        journeyGraph(navigator)
                        mixGraph(navigator, mixEditorTriggers)
                        playGraph(navigator, this@SharedTransitionLayout)
                        challengerGraph(navigator, this@SharedTransitionLayout)
                        endlessGraph(navigator)
                        accountGraph(navigator)
                    }
                }
            }
        }
    }

    fun resumeActiveGame() {
        val activeGameId = navigator.activeGameKey()?.let { key -> GameId.entries.firstOrNull { it.key == key } }
        if (activeGameId != null) navigator.resumeActiveGame(activeGameId)
    }

    val dailyRouteToStart = pendingDailyRoute
    if (dailyRouteToStart != null) {
        ReplacePuzzleSheet(
            onContinue = {
                pendingDailyRoute = null
                resumeActiveGame()
            },
            onStartNew = {
                pendingDailyRoute = null
                navigator.navigate(dailyRouteToStart)
            },
            onDismiss = { pendingDailyRoute = null },
        )
    }

    val mixToStart = pendingMixToStart
    if (mixToStart != null) {
        ReplacePuzzleSheet(
            onContinue = {
                pendingMixToStart = null
                resumeActiveGame()
            },
            onStartNew = {
                pendingMixToStart = null
                navigator.startMix(mixToStart)
            },
            onDismiss = { pendingMixToStart = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun openFromDrawer(navigator: AppNavigator, drawerState: DrawerState, route: Any) {
    navigator.scope.launch { drawerState.close() }
    navigator.navigate(route)
}

@Composable
private fun AppNavTopBar(
    entry: NavBackStackEntry?,
    isTabsRoute: Boolean,
    isOn: (KClass<*>) -> Boolean,
    navigator: AppNavigator,
    favoriteGameIds: Set<String>,
    mixes: List<Mix>,
    mixEditorTriggers: MixEditorTriggers,
) {
    val back = { navigator.back() }
    when {
        isTabsRoute -> Unit
        isOn(PickerRoute::class) -> {
            val gameId = entry?.toRoute<PickerRoute>()?.gameId
            if (gameId != null) {
                GamePageTopBar(
                    gameId,
                    isFavorite = gameId.key in favoriteGameIds,
                    onFavoriteChange = { favorite -> navigator.scope.launch { navigator.repositories.settingsRepository.setFavorite(gameId, favorite) } },
                    onBack = back,
                )
            } else {
                AppTopBar(onBack = back)
            }
        }
        isOn(RulesRoute::class) -> AppTopBar(title = stringResource(R.string.common_rules), onBack = back)
        isOn(JourneyRoute::class) || isOn(JourneyMapRoute::class) -> AppTopBar(title = stringResource(R.string.journey_title), onBack = back)
        isOn(AnalyzerRoute::class) -> AppTopBar(title = stringResource(R.string.analyzer_title), onBack = back)
        FULL_SCREEN_ROUTES.any(isOn) -> Unit
        isOn(SupportInfoRoute::class) -> {
            val titleRes = entry?.toRoute<SupportInfoRoute>()?.key?.let { supportInfoTitleRes(it) }
            AppTopBar(title = titleRes?.let { stringResource(it) }, onBack = back)
        }
        isOn(MixEditorRoute::class) -> {
            val mixId = entry?.toRoute<MixEditorRoute>()?.mixId
            AppTopBar(
                title = mixes.firstOrNull { it.id == mixId }?.name ?: stringResource(R.string.mix_editor_title_edit),
                onBack = back,
                actions = {
                    val renameInteractionSource = remember { MutableInteractionSource() }
                    IconButton(
                        onClick = { mixEditorTriggers.rename++ },
                        interactionSource = renameInteractionSource,
                        modifier = Modifier.pressScale(renameInteractionSource),
                    ) {
                        Icon(Icons.Filled.Create, contentDescription = stringResource(R.string.mix_rename_content_description))
                    }
                    val deleteInteractionSource = remember { MutableInteractionSource() }
                    IconButton(
                        onClick = { mixEditorTriggers.delete++ },
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
        isOn(AddToMixRoute::class) -> AppTopBar(title = stringResource(R.string.mix_add_to_mix), onBack = back)
        isOn(AddToNewMixRoute::class) -> AppTopBar(title = stringResource(R.string.add_to_mix_new_mix), onBack = back)
        isOn(SettingsRoute::class) -> AppTopBar(title = stringResource(R.string.account_preferences_section), onBack = back)
        isOn(MilestonesRoute::class) -> AppTopBar(title = stringResource(R.string.milestones_title), onBack = back)
        isOn(SupportRoute::class) -> AppTopBar(title = stringResource(R.string.support_support_section), onBack = back)
        isOn(TrustRoute::class) -> AppTopBar(title = stringResource(R.string.support_trust_section), onBack = back)
        isOn(AboutRoute::class) -> AppTopBar(title = stringResource(R.string.support_about_section), onBack = back)
        else -> AppTopBar(onBack = back)
    }
}

private val FULL_SCREEN_ROUTES: List<KClass<*>> = listOf(
    PlayRoute::class,
    CompletionRoute::class,
    LossRoute::class,
    JourneyResultRoute::class,
    JourneyChapterRoute::class,
    JourneyMasteredRoute::class,
    ChallengerRoute::class,
    ChallengerResultRoute::class,
    EndlessRoute::class,
    BlockFillEndlessResultRoute::class,
    MinesweeperEndlessResultRoute::class,
)
