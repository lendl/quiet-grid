// engine/src/test/kotlin/com/quietgrid/engine/takuzu/TakuzuImpossibleCombinationTest.kt
package com.quietgrid.engine.takuzu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class TakuzuImpossibleCombinationTest {
    @Test
    fun `countValidLineCompletions counts all completions consistent with fixed cells`() {
        // size-4 line with first two cells fixed to 0: valid completions must have no triple and
        // exactly two 0s/two 1s total -- only "0,0,1,1" starting with 0,0 works besides itself needing
        // a further check; use a fully-blank line where every cell is unconstrained to sanity check > 0.
        val count = countValidLineCompletions(listOf(null, null, null, null))
        assertEquals(6, count) // C(4,2) = 6 balanced, no-triple 4-bit lines for size 4
    }

    @Test
    fun `findImpossibleCombinationMove is null on an empty board`() {
        val board: TakuzuGrid = List(4) { List(4) { null } }
        assertNull(findImpossibleCombinationMove(board))
    }

    @Test
    fun `findImpossibleCombinationMove finds a forced cell when one branch contradicts after genuine proof steps`() {
        // A verified 6x6 board (derived from a valid, fully-solved grid with 4 cells blanked) where
        // the first blank cell [0,2] genuinely requires chained human-proof deduction, not an
        // immediate rule violation, to distinguish the two candidate values:
        //   - placing 0 at [0,2]: runHumanBranchProof takes 2 steps ([5,3]=1, then [0,5]=0) before
        //     hitting a BALANCE contradiction on row 0 (too many 0s).
        //   - placing 1 at [0,2]: runHumanBranchProof takes 3 steps ([5,3]=1, [2,2]=0, [0,5]=0) and
        //     stalls cleanly, recovering the original valid solution -- no contradiction.
        // Since the dead branch (0) contradicts only after 2 non-empty proof steps, this exercises
        // the `blockedProof.steps.isNotEmpty()` guard for real, unlike an immediate-violation fixture.
        val board: TakuzuGrid = listOf(
            listOf(0, 1, null, 0, 1, null),
            listOf(1, 0, 1, 0, 0, 1),
            listOf(0, 1, null, 1, 0, 1),
            listOf(1, 0, 0, 1, 1, 0),
            listOf(0, 0, 1, 0, 1, 1),
            listOf(1, 1, 0, null, 0, 0),
        )
        val move = findImpossibleCombinationMove(board)
        assertNotNull(move)
        assertEquals(TakuzuTechnique.ELIMINATE_IMPOSSIBLE_COMBINATIONS, move!!.technique)
        assertEquals(0, move!!.row)
        assertEquals(2, move!!.col)
        assertEquals(1, move!!.value) // placing 0 contradicts after 2 steps, so 1 is forced
    }

    @Test
    fun `findImpossibleCombinationMove returns null when the only contradiction is an immediate rule violation`() {
        // Regression fixture for the steps.isNotEmpty() guard: blanking [0,0] here makes placing 1
        // an IMMEDIATE row-balance violation (0 human-proof steps) -- a case already forceable by
        // COMPLETE_LINES, not the hardest branch-and-prove technique this function represents. With
        // the guard restored, this must return null rather than misclassifying the move's difficulty.
        val board: TakuzuGrid = listOf(
            listOf(null, 0, 1, 1),
            listOf(0, 1, 0, 1),
            listOf(1, 0, 1, 0),
            listOf(1, 1, 0, 0),
        )
        assertNull(findImpossibleCombinationMove(board))
    }
}
