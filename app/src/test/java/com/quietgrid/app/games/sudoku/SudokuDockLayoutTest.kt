package com.quietgrid.app.games.sudoku

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.quietgrid.app.R
import com.quietgrid.app.core.GameId
import com.quietgrid.app.ui.play.GamePlayScaffold
import com.quietgrid.app.ui.play.PLAY_BOARD_TAG
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class SudokuDockLayoutTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    @Config(qualifiers = "w360dp-h640dp-port")
    fun `small phone upright shows the whole pad`() = assertWholePadVisible()

    @Test
    @Config(qualifiers = "w390dp-h844dp-port")
    fun `normal phone upright shows the whole pad`() = assertWholePadVisible()

    @Test
    @Config(qualifiers = "w640dp-h360dp-land")
    fun `small phone sideways shows the whole pad`() = assertWholePadVisible()

    @Test
    @Config(qualifiers = "w844dp-h390dp-land")
    fun `normal phone sideways shows the whole pad`() = assertWholePadVisible()

    @Test
    @Config(qualifiers = "w1280dp-h800dp-land")
    fun `tablet landscape shows the whole pad`() = assertWholePadVisible()

    @Test
    @Config(qualifiers = "w800dp-h1280dp-port")
    fun `tablet upright shows the whole pad`() = assertWholePadVisible()

    private fun assertWholePadVisible() {
        composeRule.setContent {
            MaterialTheme {
                GamePlayScaffold(
                    gameId = GameId.SUDOKU,
                    timer = null,
                    onBack = {},
                    onEndPuzzle = {},
                    dockHeight = ::sudokuDockHeight,
                    dock = { sizeClass ->
                        SudokuDock(
                            sizeClass,
                            SudokuDockModel(
                                digitCount = 9,
                                digitsEnabled = true,
                                clearEnabled = true,
                                isDigitActive = { false },
                                onDigit = {},
                                onClear = {},
                                edit = SudokuEditControls(
                                    noteMode = false,
                                    autoCandidateMode = false,
                                    canUndo = true,
                                    onToggleNoteMode = {},
                                    onToggleAutoCandidateMode = {},
                                    onUndo = {},
                                ),
                            ),
                        )
                    },
                    board = { Box(Modifier.fillMaxSize()) },
                )
            }
        }
        val activity = composeRule.activity
        val board = composeRule.onNodeWithTag(PLAY_BOARD_TAG).getBoundsInRoot()
        listOf(
            composeRule.onNodeWithText("1"),
            composeRule.onNodeWithText("9"),
            labelled(activity.getString(R.string.sudoku_clear_cell)),
            labelled(activity.getString(R.string.sudoku_auto_candidates)),
        ).forEach { node ->
            assertNotClipped(node)
            val bounds = node.getBoundsInRoot()
            assertTrue("pad item $bounds overlaps board $board", bounds.top >= board.bottom || bounds.left >= board.right)
        }
    }

    private fun labelled(label: String): SemanticsNodeInteraction =
        composeRule.onAllNodes(hasText(label) or hasContentDescription(label), useUnmergedTree = true).onFirst()

    private fun assertNotClipped(node: SemanticsNodeInteraction) {
        val clipped = node.getBoundsInRoot()
        val full = node.getUnclippedBoundsInRoot()
        val lostWidth = (full.right - full.left) - (clipped.right - clipped.left)
        val lostHeight = (full.bottom - full.top) - (clipped.bottom - clipped.top)
        assertTrue("$full is clipped to $clipped", lostWidth.value <= 1f && lostHeight.value <= 1f)
    }
}
