package com.quietgrid.app.nav

import android.content.Context
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.quietgrid.app.R
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.daily.nextUnplayedDaily
import com.quietgrid.app.core.daily.shareDailyResult
import com.quietgrid.app.core.tips.QuietTip
import com.quietgrid.app.games.GameFinish
import com.quietgrid.app.games.GameRegistry
import com.quietgrid.app.games.PlayRequest
import com.quietgrid.app.session.FinishMotion
import com.quietgrid.app.session.LossReason
import com.quietgrid.app.ui.components.AppTab
import com.quietgrid.app.ui.journey.JourneyViewModel
import com.quietgrid.app.ui.play.LocalPlayContext
import com.quietgrid.app.ui.play.PlayContext
import com.quietgrid.app.ui.play.ProvideGamePlayPreferences
import com.quietgrid.app.ui.screens.AnalyzerHandoff
import com.quietgrid.app.ui.screens.CompletionScreen
import com.quietgrid.app.ui.screens.DailyViewModel
import com.quietgrid.app.ui.screens.LossScreen
import com.quietgrid.app.ui.screens.QuietTipActions
import com.quietgrid.app.ui.screens.QuietTipViewModel
import kotlinx.coroutines.launch

private const val FOLD_TAIL_MS = 300

@OptIn(ExperimentalSharedTransitionApi::class)
fun NavGraphBuilder.playGraph(navigator: AppNavigator, sharedScope: SharedTransitionScope) {
    composable<PlayRoute>(
        exitTransition = {
            if (targetState.destination.hasRoute<CompletionRoute>() || targetState.destination.hasRoute<LossRoute>()) {
                fadeOut(animationSpec = snap(delayMillis = if (FinishMotion.reduced) 0 else FOLD_TAIL_MS))
            } else {
                fadeOut(animationSpec = tween(200))
            }
        },
    ) { entry ->
        val route = entry.toRoute<PlayRoute>()
        CompositionLocalProvider(
            LocalSharedTransitionScope provides sharedScope,
            LocalAnimatedVisibilityScope provides this,
            LocalPlayContext provides PlayContext(dailyKey = route.daily, fromMix = navigator.activeMix != null),
        ) {
            PlayDestination(navigator, route)
        }
    }

    composable<CompletionRoute>(enterTransition = { EnterTransition.None }) { entry ->
        val route = entry.toRoute<CompletionRoute>()
        CompositionLocalProvider(
            LocalSharedTransitionScope provides sharedScope,
            LocalAnimatedVisibilityScope provides this,
        ) {
            CompletionDestination(navigator, route)
        }
    }

    composable<LossRoute>(enterTransition = { EnterTransition.None }) { entry ->
        val route = entry.toRoute<LossRoute>()
        CompositionLocalProvider(
            LocalSharedTransitionScope provides sharedScope,
            LocalAnimatedVisibilityScope provides this,
        ) {
            LossDestination(navigator, route)
        }
    }

    composable<AnalyzerRoute> { entry ->
        val route = entry.toRoute<AnalyzerRoute>()
        val snapshot = rememberSaveable { mutableStateOf(AnalyzerHandoff.consume()) }.value
        GameRegistry.of(route.gameId).analyzerScreen?.invoke(snapshot)
    }
}

@Composable
private fun PlayDestination(navigator: AppNavigator, route: PlayRoute) {
    val gameId = route.gameId
    val journeyRequest = route.journey
    val journeyViewModel: JourneyViewModel = hiltViewModel()

    fun restartJourney() {
        val request = journeyRequest ?: return
        navigator.scope.launch {
            val target = journeyViewModel.playTarget(gameId, request.stopId)
            navigator.playJourneyStop(gameId, target?.first ?: route.difficulty, target?.second ?: request)
        }
    }

    fun finish(result: GameFinish) {
        val reason = result.lossReason ?: LossReason.ABANDONED
        when {
            journeyRequest != null -> {
                val next: Any = if (result.solved) {
                    JourneyResultRoute(gameId, journeyRequest.stopId, result.elapsedSeconds, result.journeySignals.hintsUsed, result.journeySignals.mistakes)
                } else {
                    LossRoute(gameId, result.difficulty, result.elapsedSeconds, reason, result.score, journeyStop = journeyRequest.stopId)
                }
                val anchor = navigator.journeyAnchor(gameId)
                navigator.navController.navigate(next) { popUpTo(anchor) { inclusive = false } }
            }
            result.solved -> navigator.navigateFromTabs(
                CompletionRoute(gameId, result.difficulty, result.score, result.accuracyPct, result.elapsedSeconds, result.isFirstSolve, result.isNewHighScore, daily = route.daily),
            )
            else -> navigator.navigateFromTabs(LossRoute(gameId, result.difficulty, result.elapsedSeconds, reason, result.score, daily = route.daily))
        }
    }

    val definition = GameRegistry.of(gameId)
    val playPreferences = navigator.playPreferencesFor(gameId)
    val keepAwake = playPreferences.keepScreenOn || definition.capabilities.alwaysKeepScreenOn
    ProvideGamePlayPreferences(playPreferences) {
        Column(Modifier.fillMaxSize().then(if (keepAwake) Modifier.keepScreenOn() else Modifier)) {
            Box(Modifier.weight(1f)) {
                definition.PlayScreen(
                    PlayRequest(
                        difficulty = route.difficulty,
                        resume = route.resume,
                        dailyDate = route.dailyDate,
                        journey = journeyRequest,
                        onRestarted = { restartJourney() },
                        onBack = { navigator.back() },
                        onFinished = { result -> finish(result) },
                    ),
                )
            }
        }
    }
}

@Composable
private fun CompletionDestination(navigator: AppNavigator, route: CompletionRoute) {
    val gameId = route.gameId
    val nextDaily = rememberNextDaily(route.daily, gameId, route.difficulty)
    val context = LocalContext.current
    val resultTip = rememberResultTip(navigator, gameId, solved = true, eligible = route.daily == null && navigator.activeMix == null)
    ProvideGamePlayPreferences(navigator.playPreferencesFor(gameId)) {
        CompletionScreen(
            gameId = gameId,
            difficulty = route.difficulty,
            score = route.score,
            accuracyPct = route.accuracyPct,
            elapsedSeconds = route.elapsedSeconds,
            isFirstSolve = route.isFirstSolve,
            isNewHighScore = route.isNewHighScore,
            isMixActive = navigator.activeMix != null,
            onAddToMix = { navigator.navigate(AddToMixRoute(gameId, route.difficulty)) },
            onPlayAgain = {
                navigator.playAgainOrDrawMix { navigator.navigateFromTabs(PlayRoute.of(gameId, route.difficulty, resume = false)) }
            },
            onOtherDifficulty = { navigator.navigateFromTabs(PickerRoute(gameId)) },
            onTryAnotherGame = { navigator.endMixAndGoToGames() },
            dailyDate = route.daily,
            onShareDaily = { shareDaily(navigator, context, gameId, route.difficulty, route.daily) },
            onBackToDaily = { navigator.openTab(AppTab.DAILY) },
            hasNextDaily = nextDaily != null,
            onPlayNextDaily = { navigator.playNextDaily(nextDaily, route.daily) },
            tip = resultTip.tip,
            tipActions = resultTip.actions,
            onPrimaryTapped = resultTip.onPrimaryTapped,
        )
    }
}

@Composable
private fun LossDestination(navigator: AppNavigator, route: LossRoute) {
    val gameId = route.gameId
    val journeyStop = route.journeyStop
    val journeyViewModel: JourneyViewModel = hiltViewModel()
    val nextDaily = rememberNextDaily(route.daily, gameId, route.difficulty)
    val context = LocalContext.current
    val resultTip = rememberResultTip(
        navigator,
        gameId,
        solved = false,
        eligible = route.daily == null && journeyStop == null && navigator.activeMix == null,
    )
    ProvideGamePlayPreferences(navigator.playPreferencesFor(gameId)) {
        LossScreen(
            gameId = gameId,
            difficulty = route.difficulty,
            elapsedSeconds = route.elapsedSeconds,
            reason = route.reason,
            score = route.score,
            isMixActive = navigator.activeMix != null,
            onAddToMix = { navigator.navigate(AddToMixRoute(gameId, route.difficulty)) },
            onRetry = {
                if (journeyStop != null) {
                    navigator.scope.launch {
                        val target = journeyViewModel.playTarget(gameId, journeyStop)
                        if (target != null) navigator.playJourneyStop(gameId, target.first, target.second) else navigator.returnToJourney(gameId, journeyStop)
                    }
                } else {
                    navigator.playAgainOrDrawMix { navigator.navigateFromTabs(PlayRoute.of(gameId, route.difficulty, resume = false)) }
                }
            },
            onOtherDifficulty = { navigator.navigateFromTabs(PickerRoute(gameId)) },
            onTryAnotherGame = { navigator.endMixAndGoToGames() },
            onWalkThroughSolve = { navigator.navigate(AnalyzerRoute(gameId)) },
            dailyDate = route.daily,
            onShareDaily = { shareDaily(navigator, context, gameId, route.difficulty, route.daily) },
            onBackToDaily = { navigator.openTab(AppTab.DAILY) },
            hasNextDaily = nextDaily != null,
            onPlayNextDaily = { navigator.playNextDaily(nextDaily, route.daily) },
            onBackToJourney = journeyStop?.let { stop -> { navigator.returnToJourney(gameId, stop) } },
            tip = resultTip.tip,
            tipActions = resultTip.actions,
            onPrimaryTapped = resultTip.onPrimaryTapped,
        )
    }
}

private class ResultTip(val tip: QuietTip?, val actions: QuietTipActions, val onPrimaryTapped: (Boolean) -> Unit)

@Composable
private fun rememberResultTip(navigator: AppNavigator, gameId: GameId, solved: Boolean, eligible: Boolean): ResultTip {
    val viewModel: QuietTipViewModel = hiltViewModel()
    LaunchedEffect(viewModel) { viewModel.load(gameId, solved, eligible) }
    val tip by viewModel.tip.collectAsStateWithLifecycle()
    val resources = LocalContext.current.resources
    val current = tip
    val actions = remember(current, viewModel) {
        if (current == null) QuietTipActions() else quietTipActions(current, viewModel, navigator) { resources.getString(R.string.mix_default_name, it) }
    }
    return ResultTip(current, actions, viewModel::recordResultExit)
}

private fun quietTipActions(
    tip: QuietTip,
    viewModel: QuietTipViewModel,
    navigator: AppNavigator,
    mixName: (Int) -> String,
): QuietTipActions = QuietTipActions(
    onOpened = { viewModel.opened(tip) },
    onAction = {
        when (tip) {
            is QuietTip.MakeMix -> viewModel.makeMix(tip, mixName) { mixId -> navigator.navigate(MixEditorRoute(mixId)) }
            QuietTip.TryDaily -> viewModel.tryDaily(tip) { navigator.openTab(AppTab.DAILY) }
            is QuietTip.LockRotation, is QuietTip.KeepAwake, QuietTip.ReduceMotion -> viewModel.applySetting(tip)
        }
    },
    onUndo = { viewModel.undoSetting(tip) },
    onDoneClosed = { viewModel.hide() },
    onQuietThis = { viewModel.quietThis(tip) },
    onQuietAll = { viewModel.quietAll() },
    onSnooze = { viewModel.snooze(tip) },
)

private fun shareDaily(navigator: AppNavigator, context: Context, gameId: GameId, difficulty: Difficulty, dailyKey: String?) {
    if (dailyKey == null) return
    navigator.scope.launch {
        shareDailyResult(context, navigator.repositories.playHistoryRepository, gameId, difficulty, dailyKey)
    }
}

@Composable
private fun rememberNextDaily(dailyKey: String?, finishedGameId: GameId, finishedDifficulty: Difficulty): Pair<GameId, Difficulty>? {
    if (dailyKey == null) return null
    val dailyViewModel: DailyViewModel = hiltViewModel()
    val dailyState by dailyViewModel.state.collectAsStateWithLifecycle()
    if (dailyState.loading || dailyState.today.toString() != dailyKey) return null
    return remember(dailyState.games, finishedGameId, finishedDifficulty) {
        nextUnplayedDaily(dailyState.games, finishedGameId, finishedDifficulty)
    }
}
