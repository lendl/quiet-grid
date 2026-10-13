package com.quietgrid.app.widget

import android.content.Context
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.testing.unit.GlanceAppWidgetUnitTest
import androidx.glance.appwidget.testing.unit.hasStartActivityClickAction
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.hasContentDescriptionEqualTo
import androidx.test.core.app.ApplicationProvider
import com.quietgrid.app.LaunchRequest
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameCatalog
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.gameDifficultyLabelRes
import com.quietgrid.app.data.ThemeMode
import com.quietgrid.app.toIntent
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import kotlin.time.Duration.Companion.seconds

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class QuickPlayContentTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val wide = DpSize(250.dp, 110.dp)
    private val tall = DpSize(110.dp, 300.dp)

    private val gamesState = QuickPlayState.Games(
        listOf(
            QuickPlayGame(GameId.SUDOKU, Difficulty.HARD),
            QuickPlayGame(GameId.WORDSEARCH, Difficulty.EASY),
            QuickPlayGame(GameId.MINESWEEPER, Difficulty.MEDIUM),
        ),
    )

    private fun title(gameId: GameId) = context.getString(GameCatalog.get(gameId).titleRes)

    private fun difficulty(gameId: GameId, difficulty: Difficulty) = context.getString(gameDifficultyLabelRes(gameId, difficulty))

    private fun runContent(state: QuickPlayState, size: DpSize = wide, block: GlanceAppWidgetUnitTest.() -> Unit) =
        runGlanceAppWidgetUnitTest(timeout = 30.seconds) {
            setContext(context)
            setAppWidgetSize(size)
            provideComposable { QuickPlayContent(state, ThemeMode.LIGHT) }
            block()
        }

    private fun GlanceAppWidgetUnitTest.assertAction(request: LaunchRequest, description: String) {
        onNode(hasStartActivityClickAction(request.toIntent(context)).and(hasContentDescriptionEqualTo(description))).assertExists()
    }

    @Test
    fun `continue state resumes and names the game and difficulty`() =
        runContent(QuickPlayState.Continue(GameId.SUDOKU, Difficulty.HARD)) {
            assertAction(LaunchRequest.Resume, "Continue, ${title(GameId.SUDOKU)}, ${difficulty(GameId.SUDOKU, Difficulty.HARD)}")
        }

    @Test
    fun `continue state without a difficulty names only the game`() =
        runContent(QuickPlayState.Continue(GameId.SUDOKU, null)) {
            assertAction(LaunchRequest.Resume, "Continue, ${title(GameId.SUDOKU)}")
        }

    @Test
    fun `continue state offers a new puzzle that opens the games tab`() =
        runContent(QuickPlayState.Continue(GameId.SUDOKU, Difficulty.HARD)) {
            assertAction(LaunchRequest.OpenGames, "New puzzle")
        }

    @Test
    fun `continue state works stacked`() =
        runContent(QuickPlayState.Continue(GameId.WORDSEARCH, Difficulty.EASY), tall) {
            assertAction(LaunchRequest.Resume, "Continue, ${title(GameId.WORDSEARCH)}, ${difficulty(GameId.WORDSEARCH, Difficulty.EASY)}")
            assertAction(LaunchRequest.OpenGames, "New puzzle")
        }

    @Test
    fun `play mix state starts that mix and names it`() =
        runContent(QuickPlayState.PlayMix("mix-1", "Morning", listOf(GameId.SUDOKU, GameId.TAKUZU))) {
            assertAction(LaunchRequest.StartMix("mix-1"), "Play, Morning")
        }

    @Test
    fun `games state starts each game and names its difficulty`() = runContent(gamesState) {
        gamesState.games.forEach { (game, level) ->
            assertAction(LaunchRequest.StartGame(game, level), "${title(game)}, ${difficulty(game, level)}")
        }
    }

    @Test
    fun `games state works stacked`() = runContent(gamesState, tall) {
        gamesState.games.forEach { (game, level) ->
            assertAction(LaunchRequest.StartGame(game, level), "${title(game)}, ${difficulty(game, level)}")
        }
    }
}
