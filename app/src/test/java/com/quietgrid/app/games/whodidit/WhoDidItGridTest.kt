package com.quietgrid.app.games.whodidit

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.quietgrid.engine.whodidit.WhoDidItPuzzleEntry
import com.quietgrid.engine.whodidit.WhoDidItRef
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val SLOT_TAG = "whodidit-test-board-slot"

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class WhoDidItGridTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun textFor(puzzle: WhoDidItPuzzleEntry) =
        WhoDidItText(ApplicationProvider.getApplicationContext<android.content.Context>().resources, puzzle)

    private fun samplePuzzles(): List<WhoDidItPuzzleEntry> {
        val raw = javaClass.classLoader!!.getResource("whodidit_sample_puzzles.json")!!.readText()
        return Json { ignoreUnknownKeys = true }.decodeFromString(ListSerializer(WhoDidItPuzzleEntry.serializer()), raw)
    }

    private fun render(
        puzzle: WhoDidItPuzzleEntry,
        taps: MutableList<Triple<Int, Int, Int>> = mutableListOf(),
        hintCells: Map<Triple<Int, Int, Int>, WhoDidItCellMark> = emptyMap(),
        blocks: List<List<List<WhoDidItCellMark>>> = createWhoDidItSession(puzzle).blocks,
    ) {
        composeRule.setContent {
            WhoDidItGrid(
                puzzle = puzzle,
                blocks = blocks,
                text = textFor(puzzle),
                hintCells = hintCells,
                wrongPickEvent = null,
                onCellTap = { b, r, c -> taps.add(Triple(b, r, c)) },
            )
        }
    }

    private fun renderInSlot(puzzle: WhoDidItPuzzleEntry, width: Dp, height: Dp) {
        val session = createWhoDidItSession(puzzle)
        composeRule.setContent {
            Box(Modifier.size(width, height).testTag(SLOT_TAG)) {
                WhoDidItGrid(
                    puzzle = puzzle,
                    blocks = session.blocks,
                    text = textFor(puzzle),
                    hintCells = emptyMap(),
                    wrongPickEvent = null,
                    onCellTap = { _, _, _ -> },
                )
            }
        }
    }

    private val cellMatcher = SemanticsMatcher("cell tag") {
        it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("whodidit-cell-") == true
    }

    private fun cellCount(): Int =
        composeRule.onAllNodes(cellMatcher, useUnmergedTree = true).fetchSemanticsNodes().size

    private fun SemanticsNode.unclippedBounds(): Rect =
        Rect(positionInRoot, Size(size.width.toFloat(), size.height.toFloat()))

    private fun assertEveryCellFitsTheSlot(expectedCells: Int) {
        val slot = composeRule.onNodeWithTag(SLOT_TAG).fetchSemanticsNode().unclippedBounds()
        val cells = composeRule.onAllNodes(cellMatcher, useUnmergedTree = true).fetchSemanticsNodes()
        assertEquals(expectedCells, cells.size)
        cells.forEach { node ->
            val tag = node.config[SemanticsProperties.TestTag]
            val bounds = node.unclippedBounds()
            assertTrue("$tag width ${bounds.width}", bounds.width > 0f)
            assertTrue("$tag height ${bounds.height}", bounds.height > 0f)
            assertTrue(
                "$tag $bounds outside $slot",
                bounds.left >= slot.left - 0.5f && bounds.top >= slot.top - 0.5f &&
                    bounds.right <= slot.right + 0.5f && bounds.bottom <= slot.bottom + 0.5f,
            )
        }
    }

    @Test
    fun `easy 3x3 renders block count times size squared cells`() {
        val puzzle = loadWhoDidItBankForTest().first { it.difficulty == "easy" }
        render(puzzle)
        assertEquals(27, cellCount())
    }

    @Test
    fun `expert 5x4 renders six blocks of 25 cells`() {
        val puzzle = loadWhoDidItBankForTest().first { it.difficulty == "expert" }
        render(puzzle)
        assertEquals(150, cellCount())
    }

    @Test
    @Config(qualifiers = "w360dp-h780dp")
    fun `expert 5x4 fits entirely inside a phone sized board slot`() {
        renderInSlot(samplePuzzles().first { it.difficulty == "expert" }, 312.dp, 300.dp)
        assertEveryCellFitsTheSlot(150)
    }

    @Test
    @Config(qualifiers = "w360dp-h780dp")
    fun `easy 3x3 fits entirely inside a short board slot`() {
        renderInSlot(samplePuzzles().first { it.difficulty == "easy" }, 312.dp, 200.dp)
        assertEveryCellFitsTheSlot(27)
    }

    @Test
    fun `clicking a cell reports block row and col`() {
        val taps = mutableListOf<Triple<Int, Int, Int>>()
        render(loadWhoDidItBankForTest().first { it.difficulty == "easy" }, taps)
        composeRule.onNodeWithTag("whodidit-cell-0-0-0").performClick()
        assertEquals(listOf(Triple(0, 0, 0)), taps)
    }

    @Test
    fun `clicking a label shows its trait card`() {
        val puzzle = loadWhoDidItBankForTest().first { it.difficulty == "easy" }
        render(puzzle)
        val line = textFor(puzzle).cardLines(WhoDidItRef(0, 0)).first()
        composeRule.onNodeWithTag("whodidit-label-0-0").performClick()
        composeRule.onNodeWithText(line).assertIsDisplayed()
    }

    @Test
    fun `hinted cells preview the intended mark`() {
        val puzzle = samplePuzzles().first { it.difficulty == "easy" }
        render(
            puzzle,
            hintCells = mapOf(Triple(0, 0, 0) to WhoDidItCellMark.O, Triple(0, 1, 2) to WhoDidItCellMark.X),
        )
        composeRule.onNode(
            hasTestTag("whodidit-hint-preview-o") and hasAnyAncestor(hasTestTag("whodidit-cell-0-0-0")),
            useUnmergedTree = true,
        ).assertExists()
        composeRule.onNode(
            hasTestTag("whodidit-hint-preview-x") and hasAnyAncestor(hasTestTag("whodidit-cell-0-1-2")),
            useUnmergedTree = true,
        ).assertExists()
        composeRule.onAllNodesWithTag("whodidit-hint-preview-o", useUnmergedTree = true).assertCountEquals(1)
        composeRule.onAllNodesWithTag("whodidit-hint-preview-x", useUnmergedTree = true).assertCountEquals(1)
    }

    @Test
    fun `a hinted cell that already holds a mark shows no preview`() {
        val puzzle = samplePuzzles().first { it.difficulty == "easy" }
        val blocks = createWhoDidItSession(puzzle).blocks.mapIndexed { b, block ->
            block.mapIndexed { r, row ->
                row.mapIndexed { c, mark -> if (b == 0 && r == 1 && c == 2) WhoDidItCellMark.X else mark }
            }
        }
        render(puzzle, hintCells = mapOf(Triple(0, 1, 2) to WhoDidItCellMark.X), blocks = blocks)
        composeRule.onAllNodesWithTag("whodidit-hint-preview-x", useUnmergedTree = true).assertCountEquals(0)
    }
}
