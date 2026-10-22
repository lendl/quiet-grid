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
            val result = settleEndlessWorld(revealEverySafeCell(started, start), random)
            if (result.newlyCleared != 1) continue
            val ringTwo = result.world.sections.values.filter { it.state != SectionState.UNBUILT && it.pos.ring == 2 }
            assertTrue(ringTwo.isNotEmpty())
            assertTrue(ringTwo.all { it.level == 2 })
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
    fun `clearing opens only the neighbours that can be entered from it`() {
        val base = allSafeWorld(3, mapOf(start to SectionState.OPEN))
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
