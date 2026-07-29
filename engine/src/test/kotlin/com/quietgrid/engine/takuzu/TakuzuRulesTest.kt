package com.quietgrid.engine.takuzu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TakuzuRulesTest {
    @Test
    fun `otherValue flips 0 and 1`() {
        assertEquals(1, otherValue(0))
        assertEquals(0, otherValue(1))
    }

    @Test
    fun `noThreeConsec rejects three identical values in a row`() {
        assertFalse(noThreeConsec(listOf(0, 0, 0, 1)))
        assertTrue(noThreeConsec(listOf(0, 0, 1, 0)))
    }

    @Test
    fun `equalHalvesFeasible rejects a line with more than half of one value`() {
        assertFalse(equalHalvesFeasible(listOf(0, 0, 0, null), 4))
        assertTrue(equalHalvesFeasible(listOf(0, 0, null, null), 4))
    }

    @Test
    fun `isLegalCell rejects a placement that creates a triple`() {
        val grid: TakuzuGrid = listOf(
            listOf(0, 0, null, null),
            listOf(null, null, null, null),
            listOf(null, null, null, null),
            listOf(null, null, null, null),
        )
        val mutable = grid.map { it.toMutableList() }.toMutableList()
        mutable[0][2] = 0
        assertFalse(isLegalCell(mutable, 0, 2, 4))
    }

    @Test
    fun `hasUniqueLines detects a duplicate row`() {
        val grid = listOf(
            listOf(0, 1, 0, 1),
            listOf(0, 1, 0, 1),
            listOf(1, 0, 1, 0),
            listOf(1, 0, 0, 1),
        )
        assertFalse(hasUniqueLines(grid))
    }
}
