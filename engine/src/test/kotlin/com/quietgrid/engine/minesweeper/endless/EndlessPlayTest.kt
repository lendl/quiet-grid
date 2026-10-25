package com.quietgrid.engine.minesweeper.endless

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class EndlessPlayTest {

    private val start = ENDLESS_START_SECTION

    private fun worldWithStartLayout(rows: List<String>): EndlessWorld =
        islandWorld(2, safe = setOf(start), states = mapOf(start to SectionState.OPEN))
            .withSection(section(start, SectionState.OPEN, rows))

    @Test
    fun `an opening crosses into side and diagonal sections`() {
        val northEast = SectionPos(1, -1)
        val world = islandWorld(2, safe = setOf(start, EAST, northEast), states = mapOf(start to SectionState.OPEN))

        val result = revealEndlessCell(world, WorldCell(4, 4)).world

        for (pos in listOf(start, EAST, northEast)) {
            assertTrue("$pos", pos.cells().all { result.cellStateAt(it) == EndlessCellState.REVEALED })
        }
        assertEquals(SectionState.OPEN, result.stateOf(EAST))
        assertEquals(SectionState.OPEN, result.stateOf(northEast))
        assertTrue(SectionPos(2, 0).cells().none { result.cellStateAt(it) == EndlessCellState.REVEALED })
    }

    @Test
    fun `an opening never enters an unbuilt section`() {
        val world = islandWorld(2, safe = setOf(start), states = mapOf(start to SectionState.OPEN))
            .withSection(section(EAST, SectionState.UNBUILT))

        val result = revealEndlessCell(world, WorldCell(4, 4)).world

        assertTrue(EAST.cells().none { result.cellStateAt(it) == EndlessCellState.REVEALED })
        assertEquals(setOf(EAST), endlessStalledSections(result))
        assertTrue(endlessNeedsSettle(result))
    }

    @Test
    fun `a flagged cell stops the opening`() {
        val flaggedCell = localCell(EAST, 0, 4)
        val world = toggleEndlessFlag(
            islandWorld(2, safe = setOf(start, EAST), states = mapOf(start to SectionState.OPEN, EAST to SectionState.OPEN)),
            flaggedCell,
        )

        val result = revealEndlessCell(world, WorldCell(4, 4)).world

        assertEquals(EndlessCellState.FLAGGED, result.cellStateAt(flaggedCell))
        assertTrue(endlessStalledSections(result).isEmpty())
        val settled = settleEndlessWorld(result, Random(1)).world
        assertEquals(EndlessCellState.FLAGGED, settled.cellStateAt(flaggedCell))
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
        var world = toggleEndlessFlag(islandWorld(2, safe = setOf(start), states = mapOf(start to SectionState.OPEN)), WorldCell(0, 0))

        world = revealEndlessCell(world, WorldCell(8, 8)).world

        assertEquals(EndlessCellState.FLAGGED, world.cellStateAt(WorldCell(0, 0)))
        assertFalse(world.isResolved(start))
    }

    private fun revealedNumberWorld(vararg mines: Pair<Int, Int>, number: WorldCell = WorldCell(4, 4)): EndlessWorld =
        revealEndlessCell(worldWithStartLayout(rowsWithMines(*mines)), number).world

    private fun flagged(world: EndlessWorld, vararg cells: WorldCell): EndlessWorld =
        cells.fold(world) { current, cell -> toggleEndlessFlag(current, cell) }

    @Test
    fun `a chord with matching flags opens every covered neighbour`() {
        val world = flagged(revealedNumberWorld(3 to 3), WorldCell(3, 3))

        val result = chordEndlessCell(world, WorldCell(4, 4))

        assertFalse(result.hitMine)
        for (neighbor in WorldCell(4, 4).neighbors()) {
            val expected = if (neighbor == WorldCell(3, 3)) EndlessCellState.FLAGGED else EndlessCellState.REVEALED
            assertEquals("$neighbor", expected, result.world.cellStateAt(neighbor))
        }
    }

    @Test
    fun `a chord with a wrong flag opens the mine`() {
        val world = flagged(revealedNumberWorld(3 to 3), WorldCell(5, 5))

        val result = chordEndlessCell(world, WorldCell(4, 4))

        assertTrue(result.hitMine)
        assertEquals(EndlessCellState.KNOWN_MINE, result.world.cellStateAt(WorldCell(3, 3)))
        assertEquals(EndlessCellState.FLAGGED, result.world.cellStateAt(WorldCell(5, 5)))
        assertEquals(EndlessCellState.REVEALED, result.world.cellStateAt(WorldCell(4, 3)))
    }

    @Test
    fun `a chord that opens two mines reports one hit`() {
        val world = flagged(revealedNumberWorld(3 to 3, 5 to 5), WorldCell(4, 3), WorldCell(5, 4))

        val result = chordEndlessCell(world, WorldCell(4, 4))

        assertTrue(result.hitMine)
        assertEquals(EndlessCellState.KNOWN_MINE, result.world.cellStateAt(WorldCell(3, 3)))
        assertEquals(EndlessCellState.KNOWN_MINE, result.world.cellStateAt(WorldCell(5, 5)))
        assertEquals(EndlessCellState.REVEALED, result.world.cellStateAt(WorldCell(3, 4)))
    }

    @Test
    fun `a chord does nothing with too few or too many flags`() {
        val bare = revealedNumberWorld(3 to 3)
        val tooMany = flagged(bare, WorldCell(3, 3), WorldCell(5, 5))

        assertSame(bare, chordEndlessCell(bare, WorldCell(4, 4)).world)
        assertSame(tooMany, chordEndlessCell(tooMany, WorldCell(4, 4)).world)
        assertFalse(chordEndlessCell(tooMany, WorldCell(4, 4)).hitMine)
    }

    @Test
    fun `a chord does nothing on a covered cell, a zero, or outside an open section`() {
        val world = flagged(revealedNumberWorld(3 to 3), WorldCell(3, 3))
        val withZero = revealEndlessCell(world, WorldCell(7, 7)).world
        val closed = world.withSection(world.sections.getValue(start).copy(state = SectionState.CLEARED))

        assertSame(world, chordEndlessCell(world, WorldCell(4, 3)).world)
        assertEquals(0, withZero.numberAt(WorldCell(7, 7)))
        assertSame(withZero, chordEndlessCell(withZero, WorldCell(7, 7)).world)
        assertSame(closed, chordEndlessCell(closed, WorldCell(4, 4)).world)
    }

    @Test
    fun `a known mine counts toward the flags of a chord`() {
        val world = revealEndlessCell(revealedNumberWorld(3 to 3), WorldCell(3, 3)).world

        val result = chordEndlessCell(world, WorldCell(4, 4))

        assertFalse(result.hitMine)
        assertEquals(EndlessCellState.KNOWN_MINE, result.world.cellStateAt(WorldCell(3, 3)))
        assertEquals(EndlessCellState.REVEALED, result.world.cellStateAt(WorldCell(5, 5)))
    }

    @Test
    fun `a chord never opens a cell in an unbuilt section`() {
        val border = WorldCell(8, 4)
        val base = islandWorld(2, safe = setOf(start), states = mapOf(start to SectionState.OPEN))
            .withSection(section(start, SectionState.OPEN, rowsWithMines(7 to 4)))
            .withSection(section(EAST, SectionState.UNBUILT))
        val world = flagged(revealEndlessCell(base, border).world, WorldCell(7, 4))

        val result = chordEndlessCell(world, border).world

        assertEquals(EndlessCellState.REVEALED, result.cellStateAt(WorldCell(8, 3)))
        assertEquals(EndlessCellState.REVEALED, result.cellStateAt(WorldCell(7, 5)))
        assertTrue(EAST.cells().none { result.cellStateAt(it) == EndlessCellState.REVEALED })
        assertEquals(SectionState.UNBUILT, result.stateOf(EAST))
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
        val world = islandWorld(2, safe = setOf(start), states = mapOf(start to SectionState.OPEN))
            .withSection(section(EAST, SectionState.BUILT, mineColumn))

        val result = revealEndlessCell(world, WorldCell(4, 4)).world

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
        val closedAround = islandWorld(2, safe = setOf(start), states = mapOf(start to SectionState.OPEN))
        assertFalse(endlessNeedsSettle(closedAround))

        val missingNorth = EndlessWorld(closedAround.sections - NORTH)
        assertTrue(endlessNeedsSettle(missingNorth))

        val resolved = revealEndlessCell(closedAround, WorldCell(4, 4)).world
        assertTrue(endlessNeedsSettle(resolved))
    }

    @Test
    fun `a section is seen when a cell in it or its halo is revealed`() {
        val world = islandWorld(2, safe = setOf(start), states = mapOf(start to SectionState.OPEN))
            .withSection(section(EAST, SectionState.BUILT, rowsWithMines(0 to 4)))
        assertFalse(world.isSeen(EAST))

        val result = revealEndlessCell(world, WorldCell(4, 4)).world

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
