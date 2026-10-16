package com.quietgrid.cli.arrowescape

import com.quietgrid.engine.arrowescape.computeCorridor
import com.quietgrid.engine.arrowescape.isActiveCell
import com.quietgrid.engine.arrowescape.solveArrowEscape
import com.quietgrid.engine.arrowescape.toPiece
import com.quietgrid.engine.core.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.ceil
import kotlin.random.Random

class ArrowEscapeSilhouetteGeneratorTest {
    private val silhouettes = loadArrowEscapeSilhouettes()

    @Test
    fun `bundled silhouettes have unique ids`() {
        assertTrue(silhouettes.size >= 5)
        assertEquals(silhouettes.size, silhouettes.map { it.id }.toSet().size)
    }

    @Test
    fun `each silhouette keeps its declared parts dominant at its minimum size`() {
        for (silhouette in silhouettes) {
            val mask = rasterizeSilhouette(silhouette, silhouette.minRows)
            val components = connectedComponents(mask.active).map { it.size }.sortedDescending()
            assertTrue("${silhouette.id} merges its parts at ${silhouette.minRows} rows", components.size >= silhouette.parts)
            val dominant = components.take(silhouette.parts).sum()
            assertTrue("${silhouette.id} too fragmented at ${silhouette.minRows} rows", dominant >= mask.active.size * 0.85)
        }
    }

    @Test
    fun `silhouette puzzles stay inside the mask and are solvable`() {
        for (silhouette in silhouettes) {
            val generated = generateSequence(1) { it + 1 }.take(50)
                .firstNotNullOf { seed -> generateArrowEscapeSilhouettePuzzle(silhouette, Difficulty.MEDIUM, Random(seed)) }
            val entry = toEntry("t", generated)
            val pieces = entry.pieces.map { it.toPiece() }
            assertEquals(silhouette.id, entry.shape)
            assertTrue(pieces.all { piece -> piece.cells.all { entry.isActiveCell(it.row, it.col) } })
            assertTrue(solveArrowEscape(pieces, entry.rows, entry.cols).solvable)
            val active = generated.mask!!.active.size
            val covered = pieces.sumOf { it.cells.size }
            assertTrue(active - covered <= ceil(active * ARROW_ESCAPE_EMPTY_CELL_TOLERANCE))
        }
    }

    @Test
    fun `no silhouette arrow points back into its own body`() {
        val params = ARROW_ESCAPE_SILHOUETTE_FILL_PARAMS_BY_DIFFICULTY.getValue(Difficulty.MEDIUM)
        for (silhouette in silhouettes) {
            val mask = rasterizeSilhouette(silhouette, silhouette.minRows + 4)
            val active = mask.active.map { it.row to it.col }.toSet()
            for (seed in 1..10) {
                val fill = fillCoverage(mask.rows, mask.cols, params, 1.0, Random(seed), active)!!
                for (piece in fill.pieces) {
                    val head = piece.cells.last()
                    val corridor = computeCorridor(head.row, head.col, piece.headDirection, mask.rows, mask.cols)
                    assertTrue("${silhouette.id} seed $seed", corridor.none { it in piece.cells })
                }
            }
        }
    }
}
