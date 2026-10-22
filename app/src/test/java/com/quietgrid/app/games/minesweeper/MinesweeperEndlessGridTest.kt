package com.quietgrid.app.games.minesweeper

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.quietgrid.engine.minesweeper.endless.WorldCell
import com.quietgrid.engine.minesweeper.endless.startEndlessRun
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w390dp-h844dp-port")
class MinesweeperEndlessGridTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val reveals = mutableListOf<WorldCell>()
    private val flags = mutableListOf<WorldCell>()
    private val cameras = mutableListOf<MinesweeperEndlessCamera>()

    private fun showGrid() {
        val world = startEndlessRun(WorldCell(4, 4), Random(7)).world
        composeRule.setContent {
            MaterialTheme {
                MinesweeperEndlessGrid(
                    world = world,
                    camera = MinesweeperEndlessCamera(),
                    revealMines = false,
                    onCameraChanged = { cameras += it },
                    onReveal = { reveals += it },
                    onToggleFlag = { flags += it },
                    modifier = Modifier.size(330.dp).testTag("grid"),
                )
            }
        }
    }

    @Test
    fun `a tap reveals the cell under the finger`() {
        showGrid()

        composeRule.onNodeWithTag("grid").performTouchInput { down(center); up() }

        assertEquals(listOf(WorldCell(4, 4)), reveals)
        assertTrue(flags.isEmpty())
    }

    @Test
    fun `a cancelled touch reveals nothing`() {
        showGrid()

        composeRule.onNodeWithTag("grid").performTouchInput { down(center); cancel() }

        assertTrue(reveals.isEmpty())
        assertTrue(flags.isEmpty())
    }

    @Test
    fun `a long press flags instead of revealing`() {
        showGrid()

        composeRule.onNodeWithTag("grid").performTouchInput { down(center) }
        composeRule.mainClock.advanceTimeBy(1_000)
        composeRule.onNodeWithTag("grid").performTouchInput { up() }

        assertEquals(listOf(WorldCell(4, 4)), flags)
        assertTrue(reveals.isEmpty())
    }

    @Test
    fun `a drag pans instead of revealing`() {
        showGrid()

        composeRule.onNodeWithTag("grid").performTouchInput {
            down(center)
            moveBy(Offset(-200f, 0f))
            up()
        }

        assertTrue(reveals.isEmpty())
        assertTrue(cameras.single().centerX > 4.5f)
    }
}
