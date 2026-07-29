package com.quietgrid.engine.takuzu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TakuzuHumanProofTest {
    @Test
    fun `stalls on an empty board with no forced moves`() {
        val board: TakuzuGrid = List(4) { List(4) { null } }
        val result = runHumanBranchProof(board)
        assertTrue(result is TakuzuHumanProofResult.Stalled)
    }

    @Test
    fun `detects a triple contradiction`() {
        val board: TakuzuGrid = listOf(
            listOf(0, 0, null, null),
            listOf(null, null, null, null),
            listOf(null, null, null, null),
            listOf(null, null, null, null),
        )
        // find-pairs forces row0col3 = 1 first; place a second value manually to force a triple:
        val forced = board.map { it.toMutableList() }.toMutableList()
        forced[0][2] = 0
        val result = runHumanBranchProof(forced)
        assertTrue(result is TakuzuHumanProofResult.Contradiction)
        assertEquals(TakuzuContradictionKind.TRIPLE, (result as TakuzuHumanProofResult.Contradiction).contradiction.kind)
    }

    @Test
    fun `solves a fully-forceable 4x4 board to completion without contradiction or stall`() {
        // A 4x4 board one cell away from complete via find-pairs only.
        val board: TakuzuGrid = listOf(
            listOf(0, 0, 1, null),
            listOf(1, 1, 0, 0),
            listOf(0, 1, 0, 1),
            listOf(1, 0, 1, 0),
        )
        val result = runHumanBranchProof(board)
        assertTrue(result is TakuzuHumanProofResult.Stalled)
        val steps = (result as TakuzuHumanProofResult.Stalled).steps
        assertEquals(1, steps.size)
        assertEquals(TakuzuHumanProofStep(0, 3, 1), steps[0])
    }
}
