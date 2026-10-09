package com.quietgrid.app.widget

import android.content.Context
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.testing.unit.hasStartActivityClickAction
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.hasAnyDescendant
import androidx.glance.testing.unit.hasText
import androidx.test.core.app.ApplicationProvider
import com.quietgrid.app.LaunchRequest
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameCatalog
import com.quietgrid.app.core.GameId
import com.quietgrid.app.toIntent
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class QuickPlayContentTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val gamesState = QuickPlayState.Games(
        listOf(
            QuickPlayGame(GameId.SUDOKU, Difficulty.HARD),
            QuickPlayGame(GameId.WORDSEARCH, Difficulty.EASY),
            QuickPlayGame(GameId.MINESWEEPER, Difficulty.EASY),
        ),
    )

    private fun runContent(state: QuickPlayState, block: androidx.glance.appwidget.testing.unit.GlanceAppWidgetUnitTest.() -> Unit) =
        runGlanceAppWidgetUnitTest {
            setContext(context)
            setAppWidgetSize(DpSize(250.dp, 110.dp))
            provideComposable { QuickPlayContent(state, isPencilTheme = false) }
            block()
        }

    @Test
    fun `continue state shows only the continue button`() = runContent(QuickPlayState.Continue) {
        onNode(hasStartActivityClickAction(LaunchRequest.Resume.toIntent(context)).and(hasAnyDescendant(hasText("Continue"))))
            .assertExists()
        onNode(hasText("Play")).assertDoesNotExist()
    }

    @Test
    fun `play mix state starts that mix`() = runContent(QuickPlayState.PlayMix("mix-1")) {
        onNode(hasStartActivityClickAction(LaunchRequest.StartMix("mix-1").toIntent(context)).and(hasAnyDescendant(hasText("Play"))))
            .assertExists()
    }

    @Test
    fun `games state shows three game names each starting its own game`() = runContent(gamesState) {
        gamesState.games.forEach { (game, difficulty) ->
            onNode(
                hasStartActivityClickAction(LaunchRequest.StartGame(game, difficulty).toIntent(context))
                    .and(hasAnyDescendant(hasText(context.getString(GameCatalog.get(game).titleRes)))),
            ).assertExists()
        }
    }

    @Test
    fun `games state shows no difficulty`() = runContent(gamesState) {
        onNode(hasText("Hard", ignoreCase = true)).assertDoesNotExist()
    }
}
