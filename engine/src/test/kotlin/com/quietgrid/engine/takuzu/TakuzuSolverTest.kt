package com.quietgrid.engine.takuzu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TakuzuSolverTest {
    @Test
    fun `countSolutions returns 1 for a fully solved valid grid`() {
        val grid: TakuzuGrid = listOf(
            listOf(0, 0, 1, 1),
            listOf(1, 1, 0, 0),
            listOf(0, 1, 0, 1),
            listOf(1, 0, 1, 0),
        )
        assertEquals(1, countSolutions(grid))
    }

    @Test
    fun `countSolutions returns 0 for an unsolvable puzzle`() {
        val grid: TakuzuGrid = listOf(
            listOf(0, 0, 0, null),
            listOf(null, null, null, null),
            listOf(null, null, null, null),
            listOf(null, null, null, null),
        )
        assertEquals(0, countSolutions(grid))
    }

    @Test
    fun `countSolutions returns greater than 1 for a nearly-blank grid capped at maxCount`() {
        val grid: TakuzuGrid = List(4) { List(4) { null } }
        assertTrue(countSolutions(grid, maxCount = 2) >= 2)
    }

    @Test
    fun `generateSolvedGrid produces a valid, fully-filled, uniquely-lined 6x6 grid`() {
        val grid = generateSolvedGrid(6)
        assertNotNull(grid)
        assertTrue(grid!!.all { row -> row.all { it != null } })
        assertTrue(hasUniqueLines(grid.map { row -> row.map { it!! } }))
    }
}
