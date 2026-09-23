package com.quietgrid.engine.flowfree

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FlowFreeGeneratorTest {
    @Test
    fun `generateHamiltonianPath visits every cell of a 7x7 grid exactly once`() {
        val path = generateHamiltonianPath(7, random = Random(1))
        assertNotNull(path)
        assertEquals(49, path!!.size)
        assertEquals(49, path.toSet().size)
        for (row in 0 until 7) for (col in 0 until 7) assertTrue((row to col) in path)
    }

    @Test
    fun `consecutive cells in the path are always orthogonally adjacent`() {
        val path = generateHamiltonianPath(5, random = Random(2))
        assertNotNull(path)
        for (i in 0 until path!!.size - 1) {
            val (r1, c1) = path[i]
            val (r2, c2) = path[i + 1]
            assertEquals(1, kotlin.math.abs(r1 - r2) + kotlin.math.abs(c1 - c2))
        }
    }

    @Test
    fun `partitionIntoPaths splits a path into exactly pairCount segments each with at least 2 cells`() {
        val path = generateHamiltonianPath(5, random = Random(3))!!
        val segments = partitionIntoPaths(path, pairCount = 5, random = Random(4))
        assertNotNull(segments)
        assertEquals(5, segments!!.size)
        assertTrue(segments.all { it.size >= 2 })
        assertEquals(25, segments.sumOf { it.size })
    }

    @Test
    fun `generateFlowFreeSolution produces paths that pass isValidFlowFreePaths`() {
        val paths = generateFlowFreeSolution(7, pairCount = 6, random = Random(5))
        assertNotNull(paths)
        assertTrue(isValidFlowFreePaths(7, 6, paths!!))
    }
}
