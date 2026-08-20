// engine/src/test/kotlin/com/quietgrid/engine/takuzu/TakuzuImpossibleCombinationTest.kt
package com.quietgrid.engine.takuzu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class TakuzuImpossibleCombinationTest {
    @Test
    fun `countValidLineCompletions counts all completions consistent with fixed cells`() {
        val count = countValidLineCompletions(listOf(null, null, null, null))
        assertEquals(6, count)
    }

    @Test
    fun `findImpossibleCombinationMove is null on an empty board`() {
        val board: TakuzuGrid = List(4) { List(4) { null } }
        assertNull(findImpossibleCombinationMove(board))
    }

    @Test
    fun `findImpossibleCombinationMove finds a forced cell when one branch contradicts after genuine proof steps`() {
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
        assertEquals(1, move!!.value)
    }

    @Test
    fun `findImpossibleCombinationMove returns null when the only contradiction is an immediate rule violation`() {
        val board: TakuzuGrid = listOf(
            listOf(null, 0, 1, 1),
            listOf(0, 1, 0, 1),
            listOf(1, 0, 1, 0),
            listOf(1, 1, 0, 0),
        )
        assertNull(findImpossibleCombinationMove(board))
    }
}
