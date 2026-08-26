package com.quietgrid.engine.arrowescape

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArrowEscapeDependencyGraphTest {
    @Test
    fun `a piece with no other pieces in its corridor has zero dependencies`() {
        val pieces = listOf(
            ArrowEscapePiece(cells = listOf(CellCoord(0, 0)), headDirection = ArrowDirection.UP),
            ArrowEscapePiece(cells = listOf(CellCoord(1, 0)), headDirection = ArrowDirection.DOWN),
            ArrowEscapePiece(cells = listOf(CellCoord(2, 0)), headDirection = ArrowDirection.DOWN),
        )
        val graph = buildDependencyGraph(pieces, rows = 3, cols = 1)
        assertTrue(graph.dependsOn[2].isEmpty())
        assertEquals(listOf(2), graph.dependsOn[1])
    }

    @Test
    fun `no piece ever depends on itself`() {
        val pieces = listOf(
            ArrowEscapePiece(cells = listOf(CellCoord(0, 0), CellCoord(0, 1)), headDirection = ArrowDirection.RIGHT),
            ArrowEscapePiece(cells = listOf(CellCoord(1, 0)), headDirection = ArrowDirection.RIGHT),
        )
        val graph = buildDependencyGraph(pieces, rows = 2, cols = 2)
        graph.dependsOn.forEachIndexed { index, deps -> assertTrue(index !in deps) }
    }

    @Test
    fun `dependsOn and blocks are consistent inverses`() {
        val pieces = listOf(
            ArrowEscapePiece(cells = listOf(CellCoord(0, 0)), headDirection = ArrowDirection.RIGHT),
            ArrowEscapePiece(cells = listOf(CellCoord(0, 1)), headDirection = ArrowDirection.RIGHT),
            ArrowEscapePiece(cells = listOf(CellCoord(0, 2)), headDirection = ArrowDirection.RIGHT),
        )
        val graph = buildDependencyGraph(pieces, rows = 1, cols = 3)
        graph.dependsOn.forEachIndexed { index, deps ->
            deps.forEach { depIndex -> assertTrue(graph.blocks[depIndex].contains(index)) }
        }
        graph.blocks.forEachIndexed { index, blocked ->
            blocked.forEach { blockedIndex -> assertTrue(graph.dependsOn[blockedIndex].contains(index)) }
        }
    }
}
