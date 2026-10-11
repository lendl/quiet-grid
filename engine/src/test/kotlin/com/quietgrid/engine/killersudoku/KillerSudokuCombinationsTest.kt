package com.quietgrid.engine.killersudoku

import com.quietgrid.engine.sudoku.digitToBit
import com.quietgrid.engine.sudoku.iterateMaskDigits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KillerSudokuCombinationsTest {
    @Test
    fun `two-cell cages with a single combination only allow those digits`() {
        assertEquals(listOf(1, 2), iterateMaskDigits(cageComboUnion(0, 2, 3)))
        assertEquals(listOf(8, 9), iterateMaskDigits(cageComboUnion(0, 2, 17)))
        assertEquals(listOf(1, 2, 3), iterateMaskDigits(cageComboUnion(0, 3, 6)))
    }

    @Test
    fun `placed digits are excluded and shrink the remaining combinations`() {
        assertEquals(listOf(1, 4), iterateMaskDigits(cageComboUnion(digitToBit[2], 2, 5)))
        assertEquals(0, cageComboUnion(digitToBit[1] or digitToBit[2], 1, 1))
    }

    @Test
    fun `remainder feasibility treats an empty remainder as exact`() {
        assertTrue(isCageRemainderFeasible(0, 0, 0))
        assertFalse(isCageRemainderFeasible(0, 0, 3))
        assertFalse(isCageRemainderFeasible(0, 2, 18))
    }

    @Test
    fun `cage combinations lists every digit set for a sum`() {
        assertEquals(2, cageCombinations(2, 5).size)
        assertEquals(4, cageCombinations(2, 10).size)
    }

    @Test
    fun `cage support follows the candidates actually left in each cell`() {
        val result = computeCageSupport(intArrayOf(digitToBit[1] or digitToBit[3], 0x1FF), 0, 4)!!
        assertEquals(listOf(1, 3), iterateMaskDigits(result.support[0]))
        assertEquals(listOf(1, 3), iterateMaskDigits(result.support[1]))
        assertEquals(digitToBit[1] or digitToBit[3], result.requiredMask)
        assertNull(computeCageSupport(intArrayOf(digitToBit[5], digitToBit[5]), 0, 10))
    }
}
