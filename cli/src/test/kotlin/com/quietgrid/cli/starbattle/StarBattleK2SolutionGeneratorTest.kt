package com.quietgrid.cli.starbattle

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StarBattleK2SolutionGeneratorTest {
    @Test
    fun `produces a valid k=2 solution for an 8x8 board`() {
        val solution = generateStarBattleSolution(8, 2)
        assertNotNull(solution)
        assertTrue(solution!!.all { it.size == 2 })
        val colCounts = IntArray(8)
        for (row in solution.indices) {
            val cols = solution[row].sorted()
            assertTrue(cols[1] - cols[0] >= 2)
            cols.forEach { colCounts[it]++ }
            if (row > 0) {
                for (prevCol in solution[row - 1]) for (col in cols) {
                    assertTrue(kotlin.math.abs(prevCol - col) >= 2)
                }
            }
        }
        assertTrue(colCounts.all { it == 2 })
    }
}
