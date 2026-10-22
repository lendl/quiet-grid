package com.quietgrid.engine.minesweeper.endless

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class EndlessPlayTest {

    private val start = ENDLESS_START_SECTION

    private fun worldWithStartLayout(rows: List<String>): EndlessWorld =
        allSafeWorld(2, mapOf(start to SectionState.OPEN))
            .withSection(section(start, SectionState.OPEN, rows))

    @Test
    fun `a cascade fills the tapped section and its side neighbours but not the corners`() {
        val world = allSafeWorld(2, mapOf(start to SectionState.OPEN))

        val result = revealEndlessCell(world, WorldCell(4, 4)).world

        for (pos in listOf(start) + start.sideNeighbors()) {
            assertTrue(pos.cells().all { result.cellStateAt(it) == EndlessCellState.REVEALED })
        }
        assertTrue(SectionPos(1, -1).cells().none { result.cellStateAt(it) == EndlessCellState.REVEALED })
        assertTrue(SectionPos(2, 0).cells().none { result.cellStateAt(it) == EndlessCellState.REVEALED })
    }

    @Test
    fun `taps and flags in closed or cleared sections do nothing`() {
        val world = allSafeWorld(2, mapOf(start to SectionState.OPEN, WEST to SectionState.CLEARED))

        assertSame(world, revealEndlessCell(world, localCell(EAST, 4, 4)).world)
        assertSame(world, toggleEndlessFlag(world, localCell(EAST, 4, 4)))
        assertSame(world, revealEndlessCell(world, localCell(WEST, 4, 4)).world)
        assertSame(world, toggleEndlessFlag(world, localCell(WEST, 4, 4)))
        assertFalse(canTapEndlessCell(world, localCell(EAST, 4, 4)))
        assertTrue(canTapEndlessCell(world, localCell(start, 4, 4)))
    }

    @Test
    fun `tapping a mine leaves a known mine and reports the hit`() {
        val world = worldWithStartLayout(rowsWithMines(0 to 0))

        val result = revealEndlessCell(world, WorldCell(0, 0))

        assertTrue(result.hitMine)
        assertEquals(EndlessCellState.KNOWN_MINE, result.world.cellStateAt(WorldCell(0, 0)))
        assertEquals(EndlessCellState.HIDDEN, result.world.cellStateAt(WorldCell(1, 1)))
    }

    @Test
    fun `known mines and flagged mines do not block clearing`() {
        var world = worldWithStartLayout(rowsWithMines(0 to 0, 8 to 8))
        world = revealEndlessCell(world, WorldCell(0, 0)).world
        world = toggleEndlessFlag(world, WorldCell(8, 8))
        assertFalse(world.isResolved(start))

        world = revealEverySafeCell(world, start)

        assertTrue(world.isResolved(start))
        assertTrue(endlessNeedsSettle(world))
    }

    @Test
    fun `a tap on a flagged cell reveals nothing`() {
        val flagged = toggleEndlessFlag(allSafeWorld(2, mapOf(start to SectionState.OPEN)), WorldCell(4, 4))

        val result = revealEndlessCell(flagged, WorldCell(4, 4))

        assertFalse(result.hitMine)
        assertSame(flagged, result.world)
        assertEquals(EndlessCellState.FLAGGED, result.world.cellStateAt(WorldCell(4, 4)))
    }

    @Test
    fun `a flag on a safe cell blocks clearing`() {
        var world = toggleEndlessFlag(allSafeWorld(2, mapOf(start to SectionState.OPEN)), WorldCell(0, 0))

        world = revealEndlessCell(world, WorldCell(8, 8)).world

        assertEquals(EndlessCellState.FLAGGED, world.cellStateAt(WorldCell(0, 0)))
        assertFalse(world.isResolved(start))
    }

    @Test
    fun `numbers count mines across section borders`() {
        val world = allSafeWorld(2, mapOf(start to SectionState.OPEN))
            .withSection(section(EAST, SectionState.BUILT, rowsWithMines(0 to 4)))

        assertEquals(1, world.numberAt(WorldCell(8, 4)))
        assertEquals(1, world.numberAt(WorldCell(8, 3)))
        assertEquals(0, world.numberAt(WorldCell(7, 4)))
    }

    @Test
    fun `a cascade stops at numbers`() {
        val mineColumn = rowsWithMines(*Array(ENDLESS_SECTION_SIZE) { 0 to it })
        val world = allSafeWorld(2, mapOf(start to SectionState.OPEN))
            .withSection(section(EAST, SectionState.BUILT, mineColumn))

        val result = revealEndlessCell(world, WorldCell(0, 0)).world

        assertEquals(EndlessCellState.REVEALED, result.cellStateAt(WorldCell(8, 4)))
        assertEquals(3, result.numberAt(WorldCell(8, 4)))
        assertEquals(EndlessCellState.HIDDEN, result.cellStateAt(WorldCell(9, 4)))
        assertTrue(EAST.cells().none { result.cellStateAt(it) == EndlessCellState.REVEALED })
    }

    @Test
    fun `unfixed cells have no mine value`() {
        assertNull(EndlessWorld.EMPTY.mineAt(WorldCell(3, 3)))
        assertEquals(SectionState.UNBUILT, EndlessWorld.EMPTY.stateOf(start))
    }

    @Test
    fun `needs settle when an open section is resolved or has an unbuilt neighbour`() {
        val closedAround = allSafeWorld(2, mapOf(start to SectionState.OPEN))
        assertFalse(endlessNeedsSettle(closedAround))

        val missingNorth = EndlessWorld(closedAround.sections - NORTH)
        assertTrue(endlessNeedsSettle(missingNorth))

        val resolved = revealEndlessCell(closedAround, WorldCell(4, 4)).world
        assertTrue(endlessNeedsSettle(resolved))
    }

    @Test
    fun `a section is seen when a cell in it or its halo is revealed`() {
        val world = allSafeWorld(2, mapOf(start to SectionState.OPEN))
            .withSection(section(EAST, SectionState.BUILT, rowsWithMines(0 to 4)))
        assertFalse(world.isSeen(EAST))

        val result = revealEndlessCell(world, WorldCell(0, 0)).world

        assertTrue(result.isSeen(EAST))
        assertTrue(result.isSeen(SectionPos(2, 0)))
        assertFalse(result.isSeen(SectionPos(3, 0)))
    }

    @Test
    fun `cleared count and furthest ring follow cleared sections`() {
        val world = allSafeWorld(
            2,
            mapOf(start to SectionState.CLEARED, EAST to SectionState.CLEARED, SectionPos(1, 1) to SectionState.CLEARED),
        )

        assertEquals(3, world.clearedCount)
        assertEquals(2, world.furthestClearedRing)
        assertEquals(0, EndlessWorld.EMPTY.furthestClearedRing)
    }
}
