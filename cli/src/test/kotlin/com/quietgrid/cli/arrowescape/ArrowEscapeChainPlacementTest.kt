package com.quietgrid.cli.arrowescape

import com.quietgrid.engine.arrowescape.buildDependencyGraph
import com.quietgrid.engine.arrowescape.computeCorridor
import com.quietgrid.engine.arrowescape.measureArrowEscapePuzzle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ArrowEscapeChainPlacementTest {
    @Test
    fun `placeChain of length N produces N four-cell pieces stacked toward an edge`() {
        val pieces = placeChain(10, 10, 5, mutableSetOf(), Random(11))
        assertTrue(pieces != null)
        assertEquals(5, pieces!!.size)
        pieces.forEach { assertEquals(CHAIN_LINK_LEN, it.cells.size) }
    }

    @Test
    fun `the anchor piece closest to the edge has an empty corridor`() {
        val pieces = placeChain(12, 12, 4, mutableSetOf(), Random(22))!!
        val anchor = pieces[0]
        val head = anchor.cells.last()
        val corridor = computeCorridor(head.row, head.col, anchor.headDirection, 12, 12)
        val chainCells = pieces.flatMap { it.cells }.toSet()
        assertTrue(corridor.none { it in chainCells })
    }

    @Test
    fun `the far piece depends on all other chain pieces directly`() {
        val pieces = placeChain(20, 20, 6, mutableSetOf(), Random(33))!!
        val graph = buildDependencyGraph(pieces, 20, 20)
        val farIndex = pieces.size - 1
        assertEquals((0 until farIndex).toList(), graph.dependsOn[farIndex].sorted())
    }

    @Test
    fun `a chain of length 6 has max fan-out 5 and chokepoint count 3`() {
        val pieces = placeChain(20, 20, 6, mutableSetOf(), Random(88))!!
        val graph = buildDependencyGraph(pieces, 20, 20)
        val metrics = measureArrowEscapePuzzle(pieces, graph)
        assertEquals(5, metrics.maxFanOut)
        assertEquals(3, metrics.chokepointCount)
    }

    @Test
    fun `placeChain respects the occupied set and returns null when it cannot fit`() {
        val occupied = mutableSetOf<Pair<Int, Int>>()
        for (r in 0 until 5) for (c in 0 until 5) occupied.add(r to c)
        assertNull(placeChain(5, 5, 3, occupied, Random(44)))
    }
}
