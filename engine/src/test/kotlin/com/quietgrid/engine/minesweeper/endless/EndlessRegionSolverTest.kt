package com.quietgrid.engine.minesweeper.endless

import com.quietgrid.engine.minesweeper.MinesweeperTechnique
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class EndlessRegionSolverTest {

    private val start = ENDLESS_START_SECTION

    @Test
    fun `an empty section opens from a known neighbour`() {
        val world = allSafeWorld(2, mapOf(start to SectionState.CLEARED))

        val trace = solveEndlessSection(world, EAST, EndlessEntry.FromSection(start))

        assertTrue(trace.solved)
        assertEquals(0, trace.cluePairSteps)
    }

    @Test
    fun `the solver never opens a cell outside the target`() {
        val world = allSafeWorld(2, mapOf(start to SectionState.CLEARED))

        val trace = solveEndlessSection(world, EAST, EndlessEntry.FromSection(start))

        assertEquals(81, trace.openedCount)
    }

    @Test
    fun `a section walled off by mines cannot be solved`() {
        val walled = List(ENDLESS_SECTION_SIZE) { row -> if (row == 8) "********." else "*********" }
        val world = allSafeWorld(2, mapOf(start to SectionState.CLEARED))
            .withSection(section(EAST, SectionState.BUILT, walled))

        assertFalse(solveEndlessSection(world, EAST, EndlessEntry.FromSection(start)).solved)
    }

    @Test
    fun `cells already revealed count as knowledge`() {
        val walled = List(ENDLESS_SECTION_SIZE) { row -> if (row == 8) "********." else "*********" }
        val revealedCorner = "-".repeat(ENDLESS_SECTION_CELLS - 1) + "o"
        val world = allSafeWorld(2, mapOf(start to SectionState.CLEARED))
            .withSection(section(EAST, SectionState.BUILT, walled, cells = revealedCorner))

        val trace = solveEndlessSection(world, EAST, EndlessEntry.FromSection(start))

        assertTrue(trace.solved)
        assertEquals(1, trace.openedCount)
        assertEquals(0, trace.steps)
    }

    @Test
    fun `a revealed cell in a closed-off pocket makes the section solvable`() {
        val rows = rowsWithMines(0 to 0, 1 to 0, 2 to 0, 0 to 1, 0 to 2, 2 to 1, 1 to 2, 2 to 2)
        val base = allSafeWorld(2, mapOf(start to SectionState.CLEARED))
            .withSection(section(EAST, SectionState.BUILT, rows))
        val unaided = solveEndlessSection(base, EAST, EndlessEntry.FromSection(start))
        val walledCell = WorldCell(10, 1)
        val withReveal = base.withSection(
            base.sections.getValue(EAST).let { it.copy(cells = StringBuilder(it.cells).apply { set(walledCell.localIndex, 'o') }.toString()) },
        )

        val aided = solveEndlessSection(withReveal, EAST, EndlessEntry.FromSection(start))

        assertFalse(unaided.solved)
        assertTrue(aided.solved)
    }

    @Test
    fun `first tap on an empty section is one big cascade`() {
        val world = allSafeWorld(2, emptyMap())

        val trace = solveEndlessSection(world, start, EndlessEntry.FirstTap(WorldCell(4, 4)))

        assertTrue(trace.solved)
        assertEquals(81, trace.largestCascade)
        assertEquals(81, trace.openedCount)
        assertEquals(0, trace.steps)
    }

    @Test
    fun `clue pair steps appear only when single clues are not enough`() {
        var pairsSeen = 0
        for (seed in 1..200) {
            val world = fixedPair(EAST, start, 18, 8, Random(seed))
            val all = solveEndlessSection(world, EAST, EndlessEntry.FromSection(start))
            val single = solveEndlessSection(world, EAST, EndlessEntry.FromSection(start), setOf(MinesweeperTechnique.CLUE_COUNT))
            if (all.solved && all.cluePairSteps == 0) assertTrue("seed $seed", single.solved)
            if (single.solved) assertTrue("seed $seed", all.solved && all.cluePairSteps == 0)
            if (all.cluePairSteps > 0) pairsSeen++
        }
        assertTrue("no clue pair steps in 200 layouts", pairsSeen > 0)
    }

    @Test
    fun `bottlenecks never exceed steps`() {
        for (seed in 1..100) {
            val trace = solveEndlessSection(fixedPair(EAST, start, 14, 8, Random(seed)), EAST, EndlessEntry.FromSection(start))
            assertTrue(trace.bottleneckSteps <= trace.steps)
            assertTrue(trace.cluePairSteps <= trace.steps)
        }
    }
}
