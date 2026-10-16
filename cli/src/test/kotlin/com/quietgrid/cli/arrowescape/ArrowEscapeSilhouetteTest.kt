package com.quietgrid.cli.arrowescape

import com.quietgrid.engine.arrowescape.ArrowEscapePuzzleEntry
import com.quietgrid.engine.arrowescape.CellCoord
import com.quietgrid.engine.arrowescape.isActiveCell
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArrowEscapeSilhouetteTest {
    private val ring = parseArrowEscapeSilhouettes(
        """
        [{"id":"ring","name":"Ring","width":100,"height":50,"minRows":6,"shapes":[
          {"type":"ellipse","x":50,"y":25,"rx":48,"ry":24},
          {"type":"circle","x":50,"y":25,"r":12,"cut":true},
          {"type":"circle","x":97,"y":2,"r":1.5}
        ]}]
        """.trimIndent(),
    ).single()

    @Test
    fun `columns follow the silhouette aspect ratio`() {
        assertEquals(20, ring.colsForRows(10))
        assertEquals(rasterizeSilhouette(ring, 10).cols, 20)
    }

    @Test
    fun `cut shapes leave holes and tiny specks are dropped`() {
        val mask = rasterizeSilhouette(ring, 20)
        assertFalse(CellCoord(10, 20) in mask.active)
        assertTrue(CellCoord(10, 3) in mask.active)
        assertFalse(CellCoord(0, 39) in mask.active)
    }

    @Test
    fun `every active cell has an active neighbour so it can hold an arrow`() {
        for (rows in ring.minRows..30) {
            val mask = rasterizeSilhouette(ring, rows)
            for (cell in mask.active) {
                assertTrue(neighbors(cell).any { it in mask.active })
            }
        }
    }

    @Test
    fun `board for a cell target grows until it holds enough cells`() {
        val mask = silhouetteBoardForCellTarget(ring, 300)
        assertTrue(mask.active.size >= 300)
        assertTrue(rasterizeSilhouette(ring, mask.rows - 1).active.size < 300)
    }

    @Test
    fun `board for a cell target stops at the size cap`() {
        val mask = silhouetteBoardForCellTarget(ring, 100_000, maxRows = 60, maxCols = 45)
        assertTrue(mask.cols <= 45)
        assertTrue(ring.colsForRows(mask.rows + 1) > 45)
    }

    @Test
    fun `mask rows round trip through the puzzle entry`() {
        val mask = rasterizeSilhouette(ring, 8)
        val entry = ArrowEscapePuzzleEntry("x", "easy", mask.rows, mask.cols, emptyList(), "ring", mask.toMaskRows())
        for (row in 0 until mask.rows) for (col in 0 until mask.cols) {
            assertEquals(CellCoord(row, col) in mask.active, entry.isActiveCell(row, col))
        }
        assertFalse(entry.isActiveCell(-1, 0))
    }

    @Test
    fun `silhouettes declare one part unless they say otherwise`() {
        assertEquals(1, ring.parts)
    }
}
