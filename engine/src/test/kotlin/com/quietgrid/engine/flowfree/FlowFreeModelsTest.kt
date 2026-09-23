package com.quietgrid.engine.flowfree

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FlowFreeModelsTest {
    @Test
    fun `rejects when the number of paths does not match pairCount`() {
        assertFalse(isValidFlowFreePaths(2, 2, listOf(listOf(0, 1))))
    }

    @Test
    fun `rejects a path shorter than 2 cells`() {
        assertFalse(isValidFlowFreePaths(2, 2, listOf(listOf(0), listOf(1, 2, 3))))
    }

    @Test
    fun `rejects a path whose consecutive cells are not orthogonally adjacent`() {
        assertFalse(isValidFlowFreePaths(2, 2, listOf(listOf(0, 3), listOf(1, 2))))
    }

    @Test
    fun `rejects a path that revisits a cell`() {
        assertFalse(isValidFlowFreePaths(2, 1, listOf(listOf(0, 1, 0, 2))))
    }

    @Test
    fun `rejects two paths that share a cell`() {
        assertFalse(isValidFlowFreePaths(2, 2, listOf(listOf(0, 1), listOf(1, 3))))
    }

    @Test
    fun `rejects paths that do not cover the whole grid`() {
        assertFalse(isValidFlowFreePaths(2, 1, listOf(listOf(0, 1))))
    }

    @Test
    fun `accepts two full-coverage non-crossing paths`() {
        assertTrue(isValidFlowFreePaths(2, 2, listOf(listOf(0, 1), listOf(3, 2))))
    }

    @Test
    fun `accepts a self-adjacent spiral path since order, not raw geometric adjacency, defines the path`() {
        val spiral = listOf(0, 1, 2, 5, 8, 7, 6, 3, 4)
        assertTrue(isValidFlowFreePaths(3, 1, listOf(spiral)))
    }

    @Test
    fun `endpointsOf marks the first and last cell of each path and -1 everywhere else`() {
        val endpoints = endpointsOf(2, 2, listOf(listOf(0, 1), listOf(3, 2)))
        assertEquals(0, endpoints[0][0])
        assertEquals(0, endpoints[0][1])
        assertEquals(1, endpoints[1][0])
        assertEquals(1, endpoints[1][1])
    }

    @Test
    fun `encode and decode cell round-trip`() {
        assertEquals(5, flowFreeEncodeCell(1, 2, size = 3))
        assertEquals(1 to 2, flowFreeDecodeCell(5, size = 3))
    }
}
