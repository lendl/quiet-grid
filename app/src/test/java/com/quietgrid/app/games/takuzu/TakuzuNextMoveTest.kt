package com.quietgrid.app.games.takuzu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TakuzuNextMoveTest {

    @Test
    fun `three consecutive equal cells in a row is a triple-mismatch repair hint`() {
        val board = listOf(
            listOf(0, 0, 0, null),
            listOf(1, 0, 1, 0),
            listOf(0, 1, 0, 1),
            listOf(1, 0, 1, 0),
        )

        val hint = getTakuzuNextMoveHint(board)

        assertTrue(hint is TakuzuNextMoveHint.AvoidTriosRepair)
        val repair = hint as TakuzuNextMoveHint.AvoidTriosRepair
        assertEquals(TakuzuLineKind.ROW, repair.lineKind)
        assertEquals(0, repair.lineIndex)
        assertEquals(0, repair.repeatedValue)
    }

    @Test
    fun `more than half a row filled with the same value is a balance-mismatch repair hint`() {
        val board = listOf(
            listOf(0, 1, 0, 0),
            listOf(1, 0, 1, 0),
            listOf(0, 1, 0, 1),
            listOf(1, 0, 1, 0),
        )

        val hint = getTakuzuNextMoveHint(board)

        assertTrue(hint is TakuzuNextMoveHint.CompleteLinesRepair)
        val repair = hint as TakuzuNextMoveHint.CompleteLinesRepair
        assertEquals(TakuzuLineKind.ROW, repair.lineKind)
        assertEquals(0, repair.lineIndex)
        assertEquals(0, repair.filledValue)
        assertEquals(3, repair.filledCount)
    }

    @Test
    fun `two identical completed rows is a duplicate-mismatch repair hint`() {
        val board = listOf(
            listOf(0, 1, 0, 1),
            listOf(null, null, null, null),
            listOf(0, 1, 0, 1),
            listOf(null, null, null, null),
        )

        val hint = getTakuzuNextMoveHint(board)

        assertTrue(hint is TakuzuNextMoveHint.EliminateFilledLinesRepair)
        val repair = hint as TakuzuNextMoveHint.EliminateFilledLinesRepair
        assertEquals(TakuzuLineKind.ROW, repair.lineKind)
        assertEquals(0, repair.firstLineIndex)
        assertEquals(2, repair.secondLineIndex)
    }

    @Test
    fun `a pair of equal cells forcing the next one is a find-pairs progress hint`() {
        val board = listOf(
            listOf(0, 0, null, null),
            listOf(1, 0, 1, 0),
            listOf(1, 1, 0, 0),
            listOf(0, 1, 0, 1),
        )

        val hint = getTakuzuNextMoveHint(board)

        assertTrue(hint is TakuzuNextMoveHint.FindPairs)
        assertEquals(TakuzuLineKind.ROW, (hint as TakuzuNextMoveHint.FindPairs).lineKind)
        assertEquals(0, hint.lineIndex)
    }

    @Test
    fun `a fully solved valid board is paused with no hint`() {
        val board = listOf(
            listOf(0, 1, 0, 1),
            listOf(1, 0, 1, 0),
            listOf(0, 1, 1, 0),
            listOf(1, 0, 0, 1),
        )

        assertEquals(TakuzuNextMoveHint.Paused, getTakuzuNextMoveHint(board))
    }
}
