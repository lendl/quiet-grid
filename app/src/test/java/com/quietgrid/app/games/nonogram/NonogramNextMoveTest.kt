package com.quietgrid.app.games.nonogram

import com.quietgrid.engine.nonogram.NonogramGrid
import com.quietgrid.engine.nonogram.buildNonogramClues
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NonogramNextMoveTest {

    @Test
    fun `a single blank cell whose clue can only be filled yields an overlap-fill hint`() {
        val puzzle = NonogramPuzzle(id = "p", difficulty = "easy", rows = 1, cols = 1, rowClues = listOf(listOf(1)), colClues = listOf(listOf(1)))
        val board = listOf(listOf(null))

        val hint = getNonogramNextMoveHint(puzzle, board, listOf(listOf(true)))

        assertTrue(hint is NonogramProgressHint)
        val progress = hint as NonogramProgressHint
        assertEquals(NonogramHintKind.OVERLAP_FILL, progress.kind)
        assertEquals(listOf(NonogramNextMoveTarget(0, 0, 1)), progress.targetCells)
    }

    @Test
    fun `a row with more filled cells than its clue allows is flagged invalid`() {
        val puzzle = NonogramPuzzle(id = "p", difficulty = "easy", rows = 1, cols = 2, rowClues = listOf(listOf(1)), colClues = listOf(listOf(1), listOf(1)))
        val board = listOf(listOf(1, 1))

        val hint = getNonogramNextMoveHint(puzzle, board, listOf(listOf(true, false)))

        assertTrue(hint is NonogramInvalidBoardHint)
        assertEquals("row", (hint as NonogramInvalidBoardHint).lineOrientation)
        assertEquals(0, hint.lineIndex)
    }

    @Test
    fun `a fully solved puzzle has no remaining hint`() {
        val puzzle = NonogramPuzzle(id = "p", difficulty = "easy", rows = 1, cols = 1, rowClues = listOf(listOf(1)), colClues = listOf(listOf(1)))
        val board = listOf(listOf(1))

        assertNull(getNonogramNextMoveHint(puzzle, board, listOf(listOf(true))))
    }

    @Test
    fun `a puzzle that stalls on line logic alone still yields a hint via the solution fallback`() {
        val solution = listOf(
            listOf(false, false, true, true, false),
            listOf(false, false, true, true, false),
            listOf(true, true, false, false, false),
            listOf(true, true, false, false, true),
            listOf(true, false, true, false, false),
            listOf(true, true, false, false, false),
            listOf(false, true, false, true, true),
            listOf(false, true, false, false, true),
            listOf(false, false, false, true, true),
            listOf(true, false, true, false, false),
        )
        val rowClues = solution.map { buildNonogramClues(it) }
        val colClues = (0 until 5).map { c -> buildNonogramClues(solution.map { it[c] }) }
        val puzzle = NonogramPuzzle(id = "p", difficulty = "expert", rows = 10, cols = 5, rowClues = rowClues, colClues = colClues)

        var board: NonogramGrid = List(10) { List<Int?>(5) { null } }
        var sawSolutionFallback = false

        repeat(200) {
            if (board.all { row -> row.none { it == null } }) return@repeat
            val hint = getNonogramNextMoveHint(puzzle, board, solution)
            assertNotNull("hint should never be null while the board is unsolved", hint)

            val targets = when (hint) {
                is NonogramRevealFromSolution -> {
                    sawSolutionFallback = true
                    hint.targetCells
                }
                is NonogramProgressHint -> hint.targetCells
                else -> throw AssertionError("unexpected hint type: $hint")
            }

            val next = board.map { it.toMutableList() }
            for (target in targets) next[target.row][target.col] = target.value
            board = next
        }

        assertTrue("expected the solution fallback to fire at least once", sawSolutionFallback)
        assertTrue(board.all { row -> row.none { it == null } })
    }
}
