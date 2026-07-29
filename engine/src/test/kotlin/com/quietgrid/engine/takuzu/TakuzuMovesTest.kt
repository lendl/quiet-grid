package com.quietgrid.engine.takuzu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TakuzuMovesTest {
    @Test
    fun `findPairMoveInLine finds a forced cell after a leading pair`() {
        val move = findPairMoveInLine(listOf(0, 0, null, null))
        assertEquals(2 to 1, move)
    }

    @Test
    fun `findAvoidTrioMoveInLine finds a forced empty gap between matching ends`() {
        val move = findAvoidTrioMoveInLine(listOf(1, null, 1, null))
        assertEquals(1 to 0, move)
    }

    @Test
    fun `findPairsMove scans rows then columns`() {
        val board: TakuzuGrid = listOf(
            listOf(null, null, null, null),
            listOf(1, 1, null, null),
            listOf(null, null, null, null),
            listOf(null, null, null, null),
        )
        val move = findPairsMove(board)
        assertEquals(TakuzuMove(1, 2, 0, TakuzuTechnique.FIND_PAIRS), move)
    }

    @Test
    fun `findCompleteLinesMove fills the remaining cell once half the values are used`() {
        val board: TakuzuGrid = listOf(
            listOf(0, 0, 1, null),
            listOf(null, null, null, null),
            listOf(null, null, null, null),
            listOf(null, null, null, null),
        )
        val move = findCompleteLinesMove(board)
        assertEquals(TakuzuMove(0, 3, 1, TakuzuTechnique.COMPLETE_LINES), move)
    }

    @Test
    fun `findEliminateFilledLinesMove is null when no row is fully filled elsewhere`() {
        val board: TakuzuGrid = listOf(
            listOf(0, 1, 0, 1),
            listOf(null, null, null, null),
            listOf(null, null, null, null),
            listOf(null, null, null, null),
        )
        assertNull(findEliminateFilledLinesMove(board))
    }
}
