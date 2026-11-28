package com.quietgrid.app.nav

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.journey.JourneyRequest
import com.quietgrid.app.session.LossReason
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TypedRoutesNavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `routes with enum, nullable and default args survive navigation`() {
        lateinit var navController: NavHostController
        val seen = mutableListOf<Any>()
        composeRule.setContent {
            navController = rememberNavController()
            NavHost(navController, startDestination = TabsRoute) {
                composable<TabsRoute> {}
                composable<PlayRoute> { seen += it.toRoute<PlayRoute>() }
                composable<LossRoute> { seen += it.toRoute<LossRoute>() }
                composable<ChallengerResultRoute> { seen += it.toRoute<ChallengerResultRoute>() }
                composable<JourneyMapRoute> { seen += it.toRoute<JourneyMapRoute>() }
            }
        }
        val routes = listOf(
            PlayRoute.of(GameId.GAME_2048, Difficulty.EXPERT, resume = true, daily = LocalDate(2026, 11, 13), journey = JourneyRequest("s1", "p1", 3)),
            PlayRoute.of(GameId.SUDOKU, Difficulty.EASY, resume = false),
            LossRoute(GameId.ARROWESCAPE, Difficulty.HARD, 30, LossReason.OUT_OF_LIVES, 120, journeyStop = "ae-1"),
            ChallengerResultRoute(GameId.TAKUZU, 4, Difficulty.MEDIUM, 900, true, "time_up", 700, 12.5),
            ChallengerResultRoute(GameId.CHIMPTEST, 0, Difficulty.EASY, 0, false, "abandoned", 0, null),
            JourneyMapRoute(GameId.ANIMALDOKU, 2),
        )

        routes.forEach { route ->
            composeRule.runOnIdle { navController.navigate(route) }
            composeRule.waitForIdle()
            assertEquals(route, seen.last())
        }
    }
}
