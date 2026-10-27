package com.quietgrid.app.games.arrowescape

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.compose.ui.unit.dp
import com.quietgrid.app.ui.components.PuzzleBoardContainer
import com.quietgrid.engine.arrowescape.ArrowEscapePieceData
import com.quietgrid.engine.arrowescape.ArrowEscapePuzzleEntry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w390dp-h844dp-port")
class ArrowEscapeBoardZoomTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val bounds = mutableListOf<Rect>()

    private val puzzle = ArrowEscapePuzzleEntry(
        id = "zoom",
        difficulty = "expert",
        rows = 40,
        cols = 40,
        pieces = (0 until 40).flatMap { col ->
            (0 until 40 step 2).map { row ->
                ArrowEscapePieceData(cells = listOf(listOf(row + 1, col), listOf(row, col)), headDirection = "up")
            }
        },
    )

    private fun showBoard() {
        composeRule.setContent {
            MaterialTheme {
                Box(Modifier.size(360.dp).testTag("board")) {
                    PuzzleBoardContainer(
                        visible = true,
                        playFresh = false,
                        zoomable = true,
                        showFrame = false,
                        onVisibleBoundsChange = { bounds += it },
                    ) {
                        ArrowEscapeGrid(
                            puzzle = puzzle,
                            removedIndices = emptySet(),
                            wrongIndices = emptySet(),
                            selectedIndex = null,
                            lastTap = null,
                            visibleBounds = bounds.lastOrNull(),
                            onRequestPan = {},
                            onPieceTap = {},
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `pinching out zooms the board in`() {
        showBoard()
        composeRule.waitForIdle()
        val boardWidth = with(composeRule.density) { 360.dp.toPx() }

        composeRule.onNodeWithTag("board").performTouchInput {
            pinch(
                start0 = center - Offset(20f, 0f),
                end0 = center - Offset(300f, 0f),
                start1 = center + Offset(20f, 0f),
                end1 = center + Offset(300f, 0f),
            )
        }
        composeRule.waitForIdle()

        val after = bounds.lastOrNull()
        assertTrue("boardWidth=$boardWidth after=$after", after != null && after.width < boardWidth / 1.5f)
    }
}
