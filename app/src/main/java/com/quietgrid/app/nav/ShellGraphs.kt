package com.quietgrid.app.nav

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.quietgrid.app.core.journey.JourneySignals
import com.quietgrid.app.ui.journey.JourneyResultScreen
import com.quietgrid.app.ui.journey.JourneyScreen
import com.quietgrid.app.ui.journey.MasteredCelebrationScreen
import com.quietgrid.app.ui.journey.map.JourneyMapScreen
import com.quietgrid.app.ui.play.ProvideGamePlayPreferences
import com.quietgrid.app.ui.screens.AboutPageScreen
import com.quietgrid.app.ui.screens.AddToMixScreen
import com.quietgrid.app.ui.screens.HowToPlayScreen
import com.quietgrid.app.ui.screens.MilestonesScreen
import com.quietgrid.app.ui.screens.MixEditorScreen
import com.quietgrid.app.ui.screens.NewMixScreen
import com.quietgrid.app.ui.screens.PuzzlePickerScreen
import com.quietgrid.app.ui.screens.SettingsPageScreen
import com.quietgrid.app.ui.screens.SupportInfoScreen
import com.quietgrid.app.ui.screens.SupportPageScreen
import com.quietgrid.app.ui.screens.TrustPageScreen

class MixEditorTriggers {
    var rename by mutableIntStateOf(0)
    var delete by mutableIntStateOf(0)
}

@OptIn(ExperimentalSharedTransitionApi::class)
fun NavGraphBuilder.gamePageGraph(navigator: AppNavigator, sharedScope: SharedTransitionScope) {
    composable<RulesRoute> { entry ->
        HowToPlayScreen(entry.toRoute<RulesRoute>().gameId)
    }

    composable<PickerRoute> { entry ->
        val gameId = entry.toRoute<PickerRoute>().gameId
        CompositionLocalProvider(
            LocalSharedTransitionScope provides sharedScope,
            LocalAnimatedVisibilityScope provides this,
        ) {
            PuzzlePickerScreen(
                gameId = gameId,
                onPickDifficulty = { difficulty ->
                    navigator.clearMix()
                    navigator.navigate(PlayRoute.of(gameId, difficulty, resume = false))
                },
                onResumeActiveGame = { activeGameId -> navigator.resumeActiveGame(activeGameId) },
                onStartChallenger = {
                    navigator.clearMix()
                    navigator.navigate(ChallengerRoute(gameId))
                },
                onStartEndless = {
                    navigator.clearMix()
                    navigator.navigate(EndlessRoute(gameId, resume = false))
                },
                onOpenJourney = { chapter -> navigator.openJourney(gameId, chapter) },
            )
        }
    }
}

fun NavGraphBuilder.journeyGraph(navigator: AppNavigator) {
    composable<JourneyRoute> { entry ->
        val gameId = entry.toRoute<JourneyRoute>().gameId
        JourneyScreen(
            gameId = gameId,
            onOpenChapter = { chapter -> navigator.navigate(JourneyMapRoute(gameId, chapter)) },
            onOpenMastered = { navigator.navigate(JourneyMasteredRoute(gameId)) },
        )
    }

    composable<JourneyMapRoute> { entry ->
        val route = entry.toRoute<JourneyMapRoute>()
        JourneyMapScreen(
            gameId = route.gameId,
            chapter = route.chapter,
            onPlayStop = { difficulty, request ->
                navigator.clearMix()
                navigator.playJourneyStop(route.gameId, difficulty, request)
            },
            onResumeActiveGame = { activeGameId -> navigator.resumeActiveGame(activeGameId) },
        )
    }

    composable<JourneyResultRoute> { entry ->
        val route = entry.toRoute<JourneyResultRoute>()
        ProvideGamePlayPreferences(navigator.playPreferencesFor(route.gameId)) {
            JourneyResultScreen(
                gameId = route.gameId,
                stopId = route.stopId,
                elapsedSeconds = route.elapsedSeconds,
                signals = JourneySignals(hintsUsed = route.hintsUsed, mistakes = route.mistakes),
                onPlay = { difficulty, request -> navigator.playJourneyStop(route.gameId, difficulty, request) },
                onMap = { navigator.returnToJourney(route.gameId, route.stopId) },
                onJourneyList = { navigator.returnToJourneyList(route.gameId) },
                onAddToMix = { difficulty -> navigator.navigate(AddToMixRoute(route.gameId, difficulty)) },
                onOpenGamePage = { navigator.openGamePage(route.gameId) },
                newBadgeId = rememberNewBadge(),
                onOpenMilestones = { navigator.navigate(MilestonesRoute) },
            )
        }
    }

    composable<JourneyMasteredRoute> { entry ->
        val route = entry.toRoute<JourneyMasteredRoute>()
        ProvideGamePlayPreferences(navigator.playPreferencesFor(route.gameId)) {
            MasteredCelebrationScreen(
                gameId = route.gameId,
                onClose = { navigator.back() },
                onDone = { navigator.returnToJourneyList(route.gameId) },
                onViewMap = { chapter -> navigator.returnToJourneyChapter(route.gameId, chapter) },
            )
        }
    }
}

fun NavGraphBuilder.mixGraph(navigator: AppNavigator, triggers: MixEditorTriggers) {
    composable<MixEditorRoute> { entry ->
        val mixId = entry.toRoute<MixEditorRoute>().mixId
        LaunchedEffect(mixId) {
            triggers.rename = 0
            triggers.delete = 0
        }
        MixEditorScreen(
            mixId = mixId,
            onDone = { navigator.back() },
            renameTrigger = triggers.rename,
            deleteTrigger = triggers.delete,
        )
    }

    composable<AddToMixRoute> { entry ->
        val route = entry.toRoute<AddToMixRoute>()
        AddToMixScreen(
            gameId = route.gameId,
            difficulty = route.difficulty,
            onDone = { navigator.back() },
            onNewMix = { navigator.navigate(AddToNewMixRoute(route.gameId, route.difficulty)) },
        )
    }

    composable<AddToNewMixRoute> { entry ->
        val route = entry.toRoute<AddToNewMixRoute>()
        NewMixScreen(
            gameId = route.gameId,
            difficulty = route.difficulty,
            onCreated = { navigator.navController.popBackStack<AddToMixRoute>(inclusive = true) },
        )
    }
}

fun NavGraphBuilder.accountGraph(navigator: AppNavigator) {
    composable<SupportInfoRoute> { entry ->
        SupportInfoScreen(entry.toRoute<SupportInfoRoute>().key)
    }
    composable<SettingsRoute> { SettingsPageScreen() }
    composable<MilestonesRoute> { MilestonesScreen() }
    composable<SupportRoute> { SupportPageScreen() }
    composable<TrustRoute> { TrustPageScreen(onOpenInfo = { key -> navigator.navigate(SupportInfoRoute(key)) }) }
    composable<AboutRoute> { AboutPageScreen(onOpenInfo = { key -> navigator.navigate(SupportInfoRoute(key)) }) }
}
