package com.quietgrid.app.games.blockescape

import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import com.quietgrid.app.R
import com.quietgrid.app.core.GameId
import com.quietgrid.app.ui.play.GamePlayScaffold
import com.quietgrid.app.ui.play.PLAY_BOARD_TAG
import com.quietgrid.app.ui.play.PlayHintAction
import com.quietgrid.engine.blockescape.BlockEscapeBlock
import com.quietgrid.engine.blockescape.BlockEscapeMove
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private val layoutBoard = listOf(
    BlockEscapeBlock(2, 0, 2, true),
    BlockEscapeBlock(1, 2, 2, false),
    BlockEscapeBlock(2, 4, 3, false),
    BlockEscapeBlock(5, 3, 2, true),
    BlockEscapeBlock(0, 1, 3, true),
    BlockEscapeBlock(3, 2, 2, false),
    BlockEscapeBlock(4, 0, 2, true),
)

@RunWith(RobolectricTestRunner::class)
class BlockEscapeLayoutTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    @Config(qualifiers = "w360dp-h640dp-port")
    fun `small phone upright keeps the board still and shows undo`() = assertBoardStillAndUndoVisible()

    @Test
    @Config(qualifiers = "w390dp-h844dp-port")
    fun `normal phone upright keeps the board still and shows undo`() = assertBoardStillAndUndoVisible()

    @Test
    @Config(qualifiers = "w640dp-h360dp-land")
    fun `small phone sideways keeps the board still and shows undo`() = assertBoardStillAndUndoVisible()

    @Test
    @Config(qualifiers = "w844dp-h390dp-land")
    fun `normal phone sideways keeps the board still and shows undo`() = assertBoardStillAndUndoVisible()

    @Test
    @Config(qualifiers = "w1280dp-h800dp-land")
    fun `tablet landscape keeps the board still and shows undo`() = assertBoardStillAndUndoVisible()

    @Test
    @Config(qualifiers = "w800dp-h1280dp-port")
    fun `tablet upright keeps the board still and shows undo`() = assertBoardStillAndUndoVisible()

    private fun assertBoardStillAndUndoVisible() {
        var hintOn by mutableStateOf(false)
        composeRule.setContent {
            MaterialTheme {
                GamePlayScaffold(
                    gameId = GameId.BLOCKESCAPE,
                    timer = null,
                    onBack = {},
                    onEndPuzzle = {},
                    hint = PlayHintAction(active = hintOn, busy = false, contentDescription = "hint", onClick = {}),
                    dockHeight = ::blockEscapeDockHeight,
                    dock = { sizeClass -> BlockEscapeDock(sizeClass, canUndo = true, onUndo = {}) },
                    board = { BlockEscapeGrid(layoutBoard, hint = if (hintOn) BlockEscapeMove(3, 2) else null, onMove = {}) },
                )
            }
        }
        val boardOff = composeRule.onNodeWithTag(PLAY_BOARD_TAG).getBoundsInRoot()
        hintOn = true
        composeRule.waitForIdle()
        val boardOn = composeRule.onNodeWithTag(PLAY_BOARD_TAG).getBoundsInRoot()
        assertEquals(boardOff, boardOn)

        val root = composeRule.onRoot().getBoundsInRoot()
        val undo = composeRule.onNodeWithText(composeRule.activity.getString(R.string.blockescape_undo), useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue("undo $undo outside root $root", undo.left >= root.left && undo.top >= root.top && undo.right <= root.right && undo.bottom <= root.bottom)
    }
}
