package com.quietgrid.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
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
    private val small = DpSize(110.dp, 110.dp)

    private val games = listOf(
        QuickPlayGame(GameId.SUDOKU, Difficulty.HARD),
        QuickPlayGame(GameId.KILLERSUDOKU, Difficulty.EASY),
        QuickPlayGame(GameId.MINESWEEPER, Difficulty.MEDIUM),
    )

    private val continueSudoku = QuickPlayContinue(GameId.SUDOKU, Difficulty.HARD)

    private val morningMix = QuickPlayMix("mix-1", "Morning", listOf(GameId.SUDOKU, GameId.TAKUZU))

    private fun title(gameId: GameId) = context.getString(GameCatalog.get(gameId).titleRes)

    private fun difficulty(gameId: GameId, difficulty: Difficulty) = context.getString(gameDifficultyLabelRes(gameId, difficulty))

    private fun runContent(size: DpSize, content: @Composable () -> Unit, block: GlanceAppWidgetUnitTest.() -> Unit) =
        runGlanceAppWidgetUnitTest(timeout = 30.seconds) {
            setContext(context)
            setAppWidgetSize(size)
            provideComposable(content)
            block()
        }

    private fun runQuickPlay(state: QuickPlayState, size: DpSize = wide, block: GlanceAppWidgetUnitTest.() -> Unit) =
        runContent(size, { QuickPlayContent(state, ThemeMode.LIGHT) }, block)

    private fun runMix(state: MixWidgetState, size: DpSize = wide, block: GlanceAppWidgetUnitTest.() -> Unit) =
        runContent(size, { MixWidgetContent(state, ThemeMode.LIGHT) }, block)

    private fun GlanceAppWidgetUnitTest.assertAction(request: LaunchRequest, description: String) {
        onNode(hasStartActivityClickAction(request.toIntent(context)).and(hasContentDescriptionEqualTo(description))).assertExists()
    }

    private fun GlanceAppWidgetUnitTest.assertNoAction(request: LaunchRequest) {
        onNode(hasStartActivityClickAction(request.toIntent(context))).assertDoesNotExist()
    }

    private fun GlanceAppWidgetUnitTest.assertGames() {
        games.forEach { (game, level) -> assertAction(LaunchRequest.StartGame(game, level), "${title(game)}, ${difficulty(game, level)}") }
    }

    private fun GlanceAppWidgetUnitTest.assertContinueSudoku() {
        assertAction(LaunchRequest.Resume, "Continue, ${title(GameId.SUDOKU)}, ${difficulty(GameId.SUDOKU, Difficulty.HARD)}")
    }

    @Test
    fun `games start each game and name its difficulty, with no Continue`() = runQuickPlay(QuickPlayState(null, games)) {
        assertGames()
        assertNoAction(LaunchRequest.Resume)
    }

    @Test
    fun `games work stacked`() = runQuickPlay(QuickPlayState(null, games), tall) {
        assertGames()
    }

    @Test
    fun `puzzle in progress adds a Continue strip above the games`() = runQuickPlay(QuickPlayState(continueSudoku, games)) {
        assertContinueSudoku()
        assertGames()
    }

    @Test
    fun `Continue without a difficulty names only the game`() =
        runQuickPlay(QuickPlayState(QuickPlayContinue(GameId.SUDOKU, null), games)) {
            assertAction(LaunchRequest.Resume, "Continue, ${title(GameId.SUDOKU)}")
        }

    @Test
    fun `Continue strip and games fit stacked and small widgets`() {
        runQuickPlay(QuickPlayState(continueSudoku, games), tall) {
            assertContinueSudoku()
            assertGames()
        }
        runQuickPlay(QuickPlayState(continueSudoku, games), small) {
            assertContinueSudoku()
            assertGames()
        }
    }

    @Test
    fun `mix widget starts its mix and names it`() = runMix(MixWidgetState(null, morningMix)) {
        assertAction(LaunchRequest.StartMix("mix-1"), "Play, Morning")
        assertNoAction(LaunchRequest.Resume)
    }

    @Test
    fun `mix widget with a puzzle in progress shows Continue and Play`() = runMix(MixWidgetState(continueSudoku, morningMix)) {
        assertContinueSudoku()
        assertAction(LaunchRequest.StartMix("mix-1"), "Play, Morning")
    }

    @Test
    fun `mix widget without a mix offers to create one in the mixes tab`() {
        runMix(MixWidgetState(null, null)) {
            assertAction(LaunchRequest.OpenMixes, "Create a mix")
        }
        runMix(MixWidgetState(continueSudoku, null), tall) {
            assertContinueSudoku()
            assertAction(LaunchRequest.OpenMixes, "Create a mix")
        }
    }
}
