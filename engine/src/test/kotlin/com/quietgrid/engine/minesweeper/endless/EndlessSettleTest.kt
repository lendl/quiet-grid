package com.quietgrid.engine.minesweeper.endless

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class EndlessSettleTest {

    private val start = ENDLESS_START_SECTION

    private fun everyOpenSectionHasBuiltNeighbours(world: EndlessWorld): Boolean =
        world.sections.values.filter { it.state == SectionState.OPEN }
            .all { section -> section.pos.sideNeighbors().all { world.stateOf(it) != SectionState.UNBUILT } }

    private val southEast = SectionPos(1, 1)

    private fun cornerWorld(flagShared: Boolean = false): EndlessWorld {
        val hidden = "-".repeat(ENDLESS_SECTION_CELLS)
        val eastRows = List(ENDLESS_SECTION_SIZE) { row -> if (row >= 7) ".********" else "*********" }
        val southRows = List(ENDLESS_SECTION_SIZE) { row -> if (row == 0) "*******.." else "*********" }
        val southEastRows = List(ENDLESS_SECTION_SIZE) { row -> if (row == 0) "........." else ".????????" }
        fun cellsWithFlags(rows: List<String>) =
            if (flagShared) rows.joinToString("").map { if (it == '.') 'f' else '-' }.joinToString("") else hidden
        return islandWorld(3, safe = setOf(start), states = mapOf(start to SectionState.OPEN))
            .withSection(section(EAST, SectionState.BUILT, eastRows, cellsWithFlags(eastRows)))
            .withSection(section(SOUTH, SectionState.BUILT, southRows, cellsWithFlags(southRows)))
            .withSection(section(southEast, SectionState.UNBUILT, southEastRows))
    }

    @Test
    fun `settle builds a section an opening runs into and continues it`() {
        val revealed = revealEndlessCell(cornerWorld(), WorldCell(4, 4)).world
        assertTrue(endlessNeedsSettle(revealed))

        val result = settleEndlessWorld(revealed, Random(1)).world

        assertTrue(result.stateOf(southEast) != SectionState.UNBUILT)
        assertEquals(EndlessCellState.REVEALED, result.cellStateAt(WorldCell(9, 9)))
        assertTrue(endlessStalledSections(result).isEmpty())
    }

    @Test
    fun `a stalled opening behind flags is still built`() {
        val revealed = revealEndlessCell(cornerWorld(flagShared = true), WorldCell(4, 4)).world
        assertEquals(SectionState.BUILT, revealed.stateOf(EAST))
        assertEquals(SectionState.BUILT, revealed.stateOf(SOUTH))
        assertEquals(setOf(southEast), endlessStalledSections(revealed))

        val result = settleEndlessWorld(revealed, Random(1)).world

        assertTrue(result.stateOf(southEast) != SectionState.UNBUILT)
        assertEquals(EndlessCellState.REVEALED, result.cellStateAt(WorldCell(9, 9)))
    }

    @Test
    fun `after settle no revealed zero has a hidden neighbour`() {
        for (seed in 1..10) {
            val random = Random(seed)
            val started = startEndlessRun(WorldCell(4, 4), random).world
            val result = settleEndlessWorld(revealEverySafeCell(started, start), random).world

            assertTrue("seed $seed", endlessStalledSections(result).isEmpty())
            assertTrue("seed $seed", everyOpenSectionHasBuiltNeighbours(result))
        }
    }

    private fun startedWithOpenStart(firstSeed: Int): Pair<EndlessWorld, Random> {
        var seed = firstSeed
        while (true) {
            val random = Random(seed)
            val world = startEndlessRun(WorldCell(4, 4), random).world
            if (world.stateOf(start) == SectionState.OPEN) return world to random
            seed++
        }
    }

    @Test
    fun `first tap opens the start section with its neighbours built`() {
        for (seed in 1..10) {
            val tap = WorldCell(4, 4)
            val world = startEndlessRun(tap, Random(seed)).world

            assertTrue(world.stateOf(start) == SectionState.OPEN || world.stateOf(start) == SectionState.CLEARED)
            for (cell in tap.neighbors() + tap) assertFalse(world.mineAt(cell)!!)
            assertEquals(EndlessCellState.REVEALED, world.cellStateAt(tap))
            assertTrue(everyOpenSectionHasBuiltNeighbours(world))
        }
    }

    @Test
    fun `clearing a section flags its mines locks it and opens its neighbours`() {
        val (started, random) = startedWithOpenStart(1)
        val revealed = revealEverySafeCell(started, start)

        val result = settleEndlessWorld(revealed, random)
        val world = result.world

        assertEquals(SectionState.CLEARED, world.stateOf(start))
        assertTrue(result.newlyCleared >= 1)
        for (cell in start.cells()) {
            if (world.mineAt(cell) == true) assertEquals(EndlessCellState.FLAGGED, world.cellStateAt(cell))
        }
        for (neighbor in start.sideNeighbors()) {
            val state = world.stateOf(neighbor)
            assertTrue(state == SectionState.OPEN || state == SectionState.CLEARED)
        }
        assertTrue(everyOpenSectionHasBuiltNeighbours(world))
    }

    @Test
    fun `a section built after k clears gets level k plus one`() {
        var checked = 0
        for (seed in 1..20) {
            val (started, random) = startedWithOpenStart(seed)
            val before = started.sections.values.filter { it.state != SectionState.UNBUILT }.map { it.pos }.toSet()
            val result = settleEndlessWorld(revealEverySafeCell(started, start), random)
            if (result.newlyCleared != 1) continue
            val builtInSettle = result.world.sections.values.filter { it.state != SectionState.UNBUILT && it.pos !in before }
            assertTrue(builtInSettle.isNotEmpty())
            assertTrue(builtInSettle.all { it.level == 2 })
            assertTrue(start.sideNeighbors().all { result.world.sections.getValue(it).level == 1 })
            checked++
        }
        assertTrue(checked > 0)
    }

    @Test
    fun `a neighbour already fully revealed clears right after it opens`() {
        val (started, random) = startedWithOpenStart(1)
        val east = started.sections.getValue(EAST)
        val eastRevealed = east.copy(cells = east.layout.map { if (it == '*') '-' else 'o' }.joinToString(""))
        val world = revealEverySafeCell(started.withSection(eastRevealed), start)

        val result = settleEndlessWorld(world, random)

        assertEquals(SectionState.CLEARED, result.world.stateOf(start))
        assertEquals(SectionState.CLEARED, result.world.stateOf(EAST))
        assertTrue(result.world.clearedCount >= 2)
        assertTrue(EAST.sideNeighbors().all { result.world.stateOf(it) != SectionState.UNBUILT })
        assertTrue(everyOpenSectionHasBuiltNeighbours(result.world))
    }

    @Test
    fun `clearing never opens a neighbour that cannot be solved from it`() {
        val base = islandWorld(3, safe = setOf(start, NORTH, SOUTH, WEST), states = mapOf(start to SectionState.OPEN))
        val revealedStart = base.sections.getValue(start).copy(cells = "o".repeat(ENDLESS_SECTION_CELLS))
        val world = base.withSection(revealedStart).withSection(section(EAST, SectionState.BUILT, List(ENDLESS_SECTION_SIZE) { row -> if (row == 8) "********." else "*********" }))

        val result = settleEndlessWorld(world, Random(1)).world

        assertEquals(SectionState.CLEARED, result.stateOf(start))
        assertEquals(SectionState.BUILT, result.stateOf(EAST))
        for (neighbor in listOf(NORTH, SOUTH, WEST)) {
            val state = result.stateOf(neighbor)
            assertTrue(state == SectionState.OPEN || state == SectionState.CLEARED)
        }
    }

    @Test
    fun `seeded runs repeat exactly`() {
        val first = startEndlessRun(WorldCell(2, 6), Random(5))
        val second = startEndlessRun(WorldCell(2, 6), Random(5))

        assertEquals(first.world, second.world)
        assertEquals(first.stats, second.stats)
    }
}
