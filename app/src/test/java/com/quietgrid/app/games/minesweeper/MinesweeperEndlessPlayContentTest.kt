package com.quietgrid.app.games.minesweeper

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import com.quietgrid.app.ui.play.PLAY_BOARD_TAG
import com.quietgrid.engine.minesweeper.endless.WorldCell
import com.quietgrid.engine.minesweeper.endless.startEndlessRun
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
class MinesweeperEndlessPlayContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun assertBoardFitsAndIsPlayable() {
        val session = MinesweeperEndlessSession(
            world = startEndlessRun(WorldCell(4, 4), Random(7)).world,
            hearts = 2,
            bestAtStart = 9,
            camera = MinesweeperEndlessCamera(),
        )
        composeRule.setContent {
            MaterialTheme {
                MinesweeperEndlessPlayContent(
                    session = session,
                    timer = null,
                    playFresh = false,
                    building = false,
                    revealMines = false,
                    onBack = {},
                    onEndRun = {},
                    onReveal = {},
                    onToggleFlag = {},
                    onCameraChanged = {},
                )
            }
        }
        val root = composeRule.onRoot().getBoundsInRoot()
        val board = composeRule.onNodeWithTag(PLAY_BOARD_TAG).getBoundsInRoot()
        assertTrue(board.left >= root.left && board.top >= root.top && board.right <= root.right && board.bottom <= root.bottom)
        val shortSide = minOf(board.right - board.left, board.bottom - board.top)
        assertTrue("board short side ${shortSide.value}dp", shortSide.value >= 200f)
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-port")
    fun `small phone upright`() = assertBoardFitsAndIsPlayable()

    @Test
    @Config(qualifiers = "w390dp-h844dp-port")
    fun `normal phone upright`() = assertBoardFitsAndIsPlayable()

    @Test
    @Config(qualifiers = "w640dp-h360dp-land")
    fun `small phone sideways`() = assertBoardFitsAndIsPlayable()

    @Test
    @Config(qualifiers = "w844dp-h390dp-land")
    fun `normal phone sideways`() = assertBoardFitsAndIsPlayable()

    @Test
    @Config(qualifiers = "w1280dp-h800dp-land")
    fun `tablet landscape`() = assertBoardFitsAndIsPlayable()

    @Test
    @Config(qualifiers = "w800dp-h1280dp-port")
    fun `tablet upright`() = assertBoardFitsAndIsPlayable()
}
