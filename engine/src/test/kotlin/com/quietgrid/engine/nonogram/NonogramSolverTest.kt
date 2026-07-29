package com.quietgrid.engine.nonogram

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NonogramSolverTest {
    @Test
    fun `enumerateLinePlacements finds both placements for a single block in a 4-cell blank line`() {
        val placements = enumerateLinePlacements(listOf(null, null, null, null), listOf(3))
        assertEquals(2, placements.size)
    }

    @Test
    fun `analyzeLine reports the overlap-fill cell shared by all placements`() {
        val analysis = analyzeLine(listOf(null, null, null, null), listOf(3))
        assertEquals(listOf(1, 2), analysis!!.overlapFillCells)
    }

    @Test
    fun `analyzeLine returns null when no placement is possible`() {
        val analysis = analyzeLine(listOf(1, 0, 1, null), listOf(1))
        assertNull(analysis)
    }

    @Test
    fun `isNonogramLineComplete matches run-lengths exactly`() {
        assertEquals(true, isNonogramLineComplete(listOf(1, 1, 0, 1), listOf(2, 1)))
        assertEquals(false, isNonogramLineComplete(listOf(1, 1, 0, 1), listOf(1, 1)))
    }
}
