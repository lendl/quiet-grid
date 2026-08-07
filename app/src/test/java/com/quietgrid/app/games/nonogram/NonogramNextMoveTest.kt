package com.quietgrid.app.games.nonogram

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NonogramNextMoveTest {

    @Test
    fun `a single blank cell whose clue can only be filled yields an overlap-fill hint`() {
        val puzzle = NonogramPuzzle(id = "p", difficulty = "easy", rows = 1, cols = 1, rowClues = listOf(listOf(1)), colClues = listOf(listOf(1)))
        val board = listOf(listOf(null))

        val hint = getNonogramNextMoveHint(puzzle, board)

        assertTrue(hint is NonogramProgressHint)
        val progress = hint as NonogramProgressHint
        assertEquals(NonogramHintKind.OVERLAP_FILL, progress.kind)
        assertEquals(listOf(NonogramNextMoveTarget(0, 0, 1)), progress.targetCells)
    }

    @Test
    fun `a row with more filled cells than its clue allows is flagged invalid`() {
        val puzzle = NonogramPuzzle(id = "p", difficulty = "easy", rows = 1, cols = 2, rowClues = listOf(listOf(1)), colClues = listOf(listOf(1), listOf(1)))
        val board = listOf(listOf(1, 1))

        val hint = getNonogramNextMoveHint(puzzle, board)

        assertTrue(hint is NonogramInvalidBoardHint)
        assertEquals("row", (hint as NonogramInvalidBoardHint).lineOrientation)
        assertEquals(0, hint.lineIndex)
    }

    @Test
    fun `a fully solved puzzle has no remaining hint`() {
        val puzzle = NonogramPuzzle(id = "p", difficulty = "easy", rows = 1, cols = 1, rowClues = listOf(listOf(1)), colClues = listOf(listOf(1)))
        val board = listOf(listOf(1))

        assertNull(getNonogramNextMoveHint(puzzle, board))
    }
}
