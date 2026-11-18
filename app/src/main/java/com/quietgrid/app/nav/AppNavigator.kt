package com.quietgrid.app.nav

import android.content.Context
import androidx.compose.runtime.State
import androidx.navigation.NavHostController
import androidx.navigation.toRoute
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameCatalog
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.journey.JourneyRequest
import com.quietgrid.app.core.journey.loadJourneyFile
import com.quietgrid.app.core.journey.locate
import com.quietgrid.app.core.mix.Mix
import com.quietgrid.app.core.mix.MixMode
import com.quietgrid.app.core.mix.drawWeighted
import com.quietgrid.app.core.mix.resolvedCandidates
import com.quietgrid.app.data.ActiveSessionSummary
import com.quietgrid.app.data.GamePlayPreferences
import com.quietgrid.app.data.RepositoriesViewModel
import com.quietgrid.app.data.journeyTrack
import com.quietgrid.app.ui.components.AppTab
import java.time.LocalDate
import kotlin.reflect.KClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AppNavigator(
    val navController: NavHostController,
    val scope: CoroutineScope,
    val repositories: RepositoriesViewModel,
    private val appContext: Context,
    private val activeSession: State<ActiveSessionSummary?>,
    private val activeMixState: State<Mix?>,
    private val resolvePreferences: (GameId) -> GamePlayPreferences,
    private val selectTab: (AppTab) -> Unit,
) {
    val activeMix: Mix?
        get() = activeMixState.value

    fun activeGameKey(): String? = activeSession.value?.gameId

    fun playPreferencesFor(gameId: GameId): GamePlayPreferences = resolvePreferences(gameId)

    fun back() {
        navController.popBackStack()
    }

    fun navigate(route: Any) = navController.navigate(route)

    fun navigateFromTabs(route: Any) = navController.navigate(route) { popUpTo<TabsRoute> { inclusive = false } }

    fun popToTabs() {
        navController.popBackStack<TabsRoute>(inclusive = false)
    }

    fun clearMix() {
        scope.launch { repositories.mixRepository.clearActiveMix() }
    }

    fun openTab(tab: AppTab) {
        selectTab(tab)
        popToTabs()
    }

    fun endMixAndGoToGames() {
        clearMix()
        openTab(AppTab.GAMES)
    }

    fun journeyAnchor(gameId: GameId): KClass<*> = journeyAnchorRoute(gameId.key, journeyBackStack())

    fun returnToJourney(gameId: GameId, stopId: String) {
        scope.launch {
            val track = repositories.settingsRepository.journeyTrack(gameId).first()
            val chapter = loadJourneyFile(appContext, track)?.locate(stopId)?.first?.index
            follow(journeyReturnPlan(gameId, chapter, journeyBackStack()))
        }
    }

    fun returnToJourneyChapter(gameId: GameId, chapter: Int) = follow(journeyReturnPlan(gameId, chapter, journeyBackStack()))

    fun returnToJourneyList(gameId: GameId) = follow(journeyListPlan(gameId, journeyBackStack()))

    private fun follow(plan: JourneyReturnPlan) {
        navController.popBackStack(plan.popTo, inclusive = plan.inclusive)
        plan.push.forEach { navController.navigate(it) }
    }

    fun playJourneyStop(gameId: GameId, difficulty: Difficulty, request: JourneyRequest) {
        val anchor = journeyAnchor(gameId)
        navController.navigate(PlayRoute.of(gameId, difficulty, resume = false, journey = request)) {
            popUpTo(anchor) { inclusive = false }
        }
    }

    fun resumeActiveRoute(gameId: GameId): Any {
        val active = activeSession.value
        val summary = active?.copy(gameId = gameId.key, journey = active.journey.takeIf { active.gameId == gameId.key })
            ?: ActiveSessionSummary(gameId.key, null)
        return resumeRoute(summary) ?: PlayRoute.of(gameId, Difficulty.EASY, resume = true)
    }

    fun resumeActiveGame(gameId: GameId) = navController.navigate(resumeActiveRoute(gameId))

    fun playNextDaily(next: Pair<GameId, Difficulty>?, dailyKey: String?) {
        val date = dailyKey?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return
        val (gameId, difficulty) = next ?: return
        navigateFromTabs(PlayRoute.of(gameId, difficulty, resume = false, daily = date))
    }

    fun startMix(mix: Mix) {
        scope.launch { repositories.mixRepository.setActiveMix(mix.id) }
        drawFromMix(mix)
    }

    fun playAgainOrDrawMix(fallback: () -> Unit) {
        val mix = activeMix
        if (mix != null) drawFromMix(mix) else fallback()
    }

    private fun drawFromMix(mix: Mix) {
        scope.launch {
            val enabledGameIds = GameCatalog.enabledGameIds(repositories.settingsRepository.settings.first().betaGamesEnabled)
            val candidate = drawWeighted(mix.resolvedCandidates().filter { it.gameId in enabledGameIds })
            if (candidate == null) {
                repositories.mixRepository.clearActiveMix()
                openTab(AppTab.GAMES)
                return@launch
            }
            val route = when (val mode = candidate.mode) {
                is MixMode.Puzzle -> PlayRoute.of(candidate.gameId, mode.difficulty, resume = false)
                MixMode.Challenger -> ChallengerRoute(candidate.gameId)
            }
            navigateFromTabs(route)
        }
    }

    private fun journeyBackStack(): List<JourneyBackEntry> = listOfNotNull(
        entryFor(PickerRoute::class)?.let { JourneyBackEntry(PickerRoute::class, it.toRoute<PickerRoute>().gameId.key, null) },
        entryFor(JourneyRoute::class)?.let { JourneyBackEntry(JourneyRoute::class, it.toRoute<JourneyRoute>().gameId.key, null) },
        entryFor(JourneyMapRoute::class)?.toRoute<JourneyMapRoute>()?.let { JourneyBackEntry(JourneyMapRoute::class, it.gameId.key, it.chapter) },
    )

    private fun entryFor(route: KClass<*>) = runCatching { navController.getBackStackEntry(route) }.getOrNull()
}
