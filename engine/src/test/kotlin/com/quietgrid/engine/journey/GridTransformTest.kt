package com.quietgrid.engine.journey

import org.junit.Assert.assertEquals
import org.junit.Test

class GridTransformTest {
    private fun image(t: GridTransform, rows: Int, cols: Int): List<Pair<Int, Int>> =
        (0 until rows).flatMap { r -> (0 until cols).map { c -> t.mapCell(r, c, rows, cols) } }

    @Test
    fun `eight distinct bijections on a square grid`() {
        val transforms = gridTransformsFor(4, 4)
        assertEquals(GridTransform.entries.toList(), transforms)
        val all = (0 until 4).flatMap { r -> (0 until 4).map { c -> r to c } }.toSet()
        transforms.forEach { t -> assertEquals(t.name, all, image(t, 4, 4).toSet()) }
        assertEquals(8, transforms.map { image(it, 4, 4) }.toSet().size)
    }

    @Test
    fun `rotating four times is identity`() {
        var cell = 1 to 2
        repeat(4) { cell = GridTransform.ROTATE_90.mapCell(cell.first, cell.second, 4, 4) }
        assertEquals(1 to 2, cell)
        assertEquals(0 to 3, GridTransform.ROTATE_90.mapCell(0, 0, 4, 4))
    }

    @Test
    fun `non-square grids get four transforms that keep their size`() {
        val transforms = gridTransformsFor(3, 5)
        assertEquals(listOf(GridTransform.IDENTITY, GridTransform.MIRROR_H, GridTransform.ROTATE_180, GridTransform.MIRROR_V), transforms)
        transforms.forEach { t ->
            assertEquals(3 to 5, t.outputSize(3, 5))
            val cells = image(t, 3, 5)
            assertEquals(15, cells.toSet().size)
            cells.forEach { (r, c) -> assert(r in 0 until 3 && c in 0 until 5) }
        }
    }

    @Test
    fun `rotation swaps rows and cols`() {
        assertEquals(5 to 3, GridTransform.ROTATE_90.outputSize(3, 5))
        assertEquals(5 to 3, GridTransform.TRANSPOSE.outputSize(3, 5))
        assertEquals(0 to 2, GridTransform.ROTATE_90.mapCell(0, 0, 3, 5))
        assertEquals(15, image(GridTransform.ROTATE_270, 3, 5).toSet().size)
    }

    @Test
    fun `ids round-trip and unknown id is identity`() {
        GridTransform.entries.forEach { assertEquals(it, gridTransformById(it.id)) }
        assertEquals(GridTransform.IDENTITY, gridTransformById(42))
        assertEquals(1, GridTransform.MIRROR_H.id)
    }
}
