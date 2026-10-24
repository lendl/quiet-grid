package com.quietgrid.engine.minesweeper.endless

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class EndlessOpeningsTest {

    private val start = ENDLESS_START_SECTION

    @Test
    fun `an opening counts its zeros and the cells around them`() {
        val world = islandWorld(2, safe = emptySet())
            .withSection(section(start, SectionState.BUILT, rowsWithMines(*Array(ENDLESS_SECTION_SIZE) { 4 to it })))

        assertEquals(36, largestEndlessOpeningThrough(world, start, Int.MAX_VALUE))
    }

    @Test
    fun `an opening is measured across a section border`() {
        val world = islandWorld(2, safe = setOf(start, EAST))

        assertEquals(162, largestEndlessOpeningThrough(world, start, Int.MAX_VALUE))
        assertEquals(162, largestEndlessOpeningThrough(world, EAST, Int.MAX_VALUE))
    }

    @Test
    fun `a cell whose number is not known yet stops the opening`() {
        val eastLayout = List(ENDLESS_SECTION_SIZE) { ".????????" }
        val world = islandWorld(2, safe = setOf(start))
            .withSection(section(EAST, SectionState.UNBUILT, eastLayout))

        assertEquals(90, largestEndlessOpeningThrough(world, start, Int.MAX_VALUE))
    }

    @Test
    fun `the measure stops early above the limit`() {
        val world = islandWorld(2, safe = setOf(start, EAST))

        val measured = largestEndlessOpeningThrough(world, start, limit = 30)

        assertTrue("measured $measured", measured in 31..39)
    }

    @Test
    fun `repair moves free mines until the opening fits the cap`() {
        val clustered = List(ENDLESS_SECTION_SIZE) { row -> if (row < 3) "***......" else "........." }
        val world = islandWorld(2, safe = emptySet())
            .withSection(section(start, SectionState.BUILT, clustered))
        assertTrue(largestEndlessOpeningThrough(world, start) > 30)

        val repaired = repairEndlessOpenings(world, start, (0 until ENDLESS_SECTION_CELLS).toSet(), Random(1), 30)

        assertEquals(9, start.cells().count { repaired.mineAt(it) == true })
        assertTrue(largestEndlessOpeningThrough(repaired, start) <= 30)
    }

    @Test
    fun `repair never moves a cell outside the free set`() {
        val clustered = List(ENDLESS_SECTION_SIZE) { row -> if (row < 3) "***......" else "........." }
        val world = islandWorld(2, safe = emptySet())
            .withSection(section(start, SectionState.BUILT, clustered))
        val free = (0 until ENDLESS_SECTION_SIZE * 5).toSet()

        val repaired = repairEndlessOpenings(world, start, free, Random(1), 30)

        for (cell in start.cells()) {
            if (cell.localIndex !in free) assertEquals("$cell", world.mineAt(cell), repaired.mineAt(cell))
        }
        assertEquals(9, start.cells().count { repaired.mineAt(it) == true })
    }

    @Test
    fun `a section with no zero has no opening`() {
        assertEquals(0, largestEndlessOpeningThrough(islandWorld(2, safe = emptySet()), start, Int.MAX_VALUE))
    }
}
