package com.quietgrid.engine.minesweeper.endless

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EndlessGeometryTest {

    @Test
    fun `negative cells map to the section on their left and above`() {
        assertEquals(SectionPos(-1, -1), WorldCell(-1, -1).section)
        assertEquals(80, WorldCell(-1, -1).localIndex)
        assertEquals(SectionPos(1, 0), WorldCell(9, 0).section)
        assertEquals(0, WorldCell(9, 0).localIndex)
        assertEquals(SectionPos(-1, 0), WorldCell(-9, 8).section)
        assertEquals(72, WorldCell(-9, 8).localIndex)
    }

    @Test
    fun `halo has forty cells and none inside the section`() {
        val pos = SectionPos(-2, 1)
        val halo = pos.halo()

        assertEquals(40, halo.size)
        assertEquals(40, halo.toSet().size)
        assertTrue(halo.none { it.section == pos })
        assertTrue(halo.all { cell -> cell.neighbors().any { it.section == pos } })
    }

    @Test
    fun `section cells are row-major and match their local index`() {
        val cells = SectionPos(3, -4).cells()

        assertEquals(81, cells.size)
        cells.forEachIndexed { index, cell ->
            assertEquals(SectionPos(3, -4), cell.section)
            assertEquals(index, cell.localIndex)
        }
    }

    @Test
    fun `side neighbours come in north east south west order`() {
        assertEquals(
            listOf(SectionPos(2, 4), SectionPos(3, 5), SectionPos(2, 6), SectionPos(1, 5)),
            SectionPos(2, 5).sideNeighbors(),
        )
    }

    @Test
    fun `ring counts side steps from the start`() {
        assertEquals(5, SectionPos(-2, 3).ring)
        assertEquals(0, ENDLESS_START_SECTION.ring)
    }

    @Test
    fun `a cell has eight neighbours`() {
        val neighbors = WorldCell(0, 0).neighbors()

        assertEquals(8, neighbors.toSet().size)
        assertTrue(WorldCell(-1, -1) in neighbors)
        assertTrue(WorldCell(0, 0) !in neighbors)
    }
}
