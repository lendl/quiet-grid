package com.quietgrid.engine.sudoku

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SudokuSolveTraceTest {
    private val solved: SudokuGrid = listOf(
        listOf(5, 3, 4, 6, 7, 8, 9, 1, 2),
        listOf(6, 7, 2, 1, 9, 5, 3, 4, 8),
        listOf(1, 9, 8, 3, 4, 2, 5, 6, 7),
        listOf(8, 5, 9, 7, 6, 1, 4, 2, 3),
        listOf(4, 2, 6, 8, 5, 3, 7, 9, 1),
        listOf(7, 1, 3, 9, 2, 4, 8, 5, 6),
        listOf(9, 6, 1, 5, 3, 7, 2, 8, 4),
        listOf(2, 8, 7, 4, 1, 9, 6, 3, 5),
        listOf(3, 4, 5, 2, 8, 6, 1, 7, 9),
    )

    private fun gridOf(vararg cells: Triple<Int, Int, Int>): SudokuGrid {
        val rows = MutableList(9) { MutableList<Int?>(9) { null } }
        cells.forEach { (r, c, v) -> rows[r][c] = v }
        return rows
    }

    @Test
    fun `traceHumanSolve solves a puzzle with only one blank via a single naked single`() {
        val puzzle = solved.mapIndexed { r, row -> row.mapIndexed { c, v -> if (r == 0 && c == 0) null else v } }
        val trace = traceHumanSolve(puzzle)
        assertTrue(trace.solved)
        assertEquals(1, trace.moves.size)
        assertEquals(SudokuTechnique.NAKED_SINGLE, trace.moves[0].technique)
    }

    @Test
    fun `traceHumanSolve reports blocked, not solved, for a fully blank board (needs a guess)`() {
        val blank: SudokuGrid = List(9) { List(9) { null } }
        val trace = traceHumanSolve(blank)
        assertTrue(!trace.solved)
        assertTrue(trace.blocked)
    }

    @Test
    fun `traceHumanSolve applies two independent naked singles in sequence to fully solve`() {
        val puzzle = solved.mapIndexed { r, row ->
            row.mapIndexed { c, v -> if ((r == 0 && c == 0) || (r == 4 && c == 4)) null else v }
        }
        val trace = traceHumanSolve(puzzle)
        assertTrue(trace.solved)
        assertTrue(!trace.blocked)
        assertEquals(2, trace.moves.size)
        assertTrue(trace.moves.all { it.technique == SudokuTechnique.NAKED_SINGLE })
    }

    @Test
    fun `findNextMove picks a later-ordered technique over an earlier-ordered one when its score is genuinely lower`() {
        val board = gridOf(
            Triple(0, 2, 3), Triple(1, 2, 4), Triple(2, 0, 5), Triple(2, 1, 6), Triple(2, 2, 7),
            Triple(0, 4, 1), Triple(0, 5, 8),
            Triple(7, 2, 9), Triple(6, 4, 7), Triple(7, 6, 7), Triple(3, 1, 7),
        )
        val move = findNextMove(board)
        assertTrue(move is SudokuCandidateEliminationMove)
        val elimination = move as SudokuCandidateEliminationMove
        assertEquals(SudokuTechnique.NAKED_PAIR, elimination.technique)
        assertEquals(4, elimination.complexity)
        assertEquals(listOf(SudokuHouseRef("box", 0)), elimination.houses)
        assertEquals(
            listOf(SudokuCellRef(0, 0), SudokuCellRef(0, 1)),
            elimination.evidenceCells,
        )
        assertEquals(
            listOf(Triple(1, 0, 2), Triple(1, 0, 9), Triple(1, 1, 2), Triple(1, 1, 9)),
            elimination.eliminations,
        )
    }

    @Test
    fun `allowedTechniques filtering makes the dispatcher fall back to the next-best allowed technique`() {
        val board = gridOf(
            Triple(8, 0, 1), Triple(8, 1, 2), Triple(8, 2, 9), Triple(8, 3, 3), Triple(8, 4, 4),
            Triple(8, 5, 6), Triple(8, 6, 7), Triple(8, 7, 8),
        )

        val defaultMove = findNextMove(board)
        assertTrue(defaultMove is SudokuPlacementMove)
        val defaultPlacement = defaultMove as SudokuPlacementMove
        assertEquals(SudokuTechnique.NAKED_SINGLE, defaultPlacement.technique)
        assertEquals(8, defaultPlacement.targetRow)
        assertEquals(8, defaultPlacement.targetCol)
        assertEquals(5, defaultPlacement.digit)
        assertEquals(0, defaultPlacement.complexity)

        val allowedTechniques = SudokuTechnique.entries.filter { it != SudokuTechnique.NAKED_SINGLE }
        val fallbackMove = findNextMove(board, allowedTechniques)
        assertTrue(fallbackMove is SudokuPlacementMove)
        val fallbackPlacement = fallbackMove as SudokuPlacementMove
        assertEquals(SudokuTechnique.HIDDEN_SINGLE, fallbackPlacement.technique)
        assertEquals(8, fallbackPlacement.targetRow)
        assertEquals(8, fallbackPlacement.targetCol)
        assertEquals(5, fallbackPlacement.digit)
    }

    @Test
    fun `allowedTechniques excluding the only viable techniques blocks the trace instead of solving`() {
        val puzzle = solved.mapIndexed { r, row -> row.mapIndexed { c, v -> if (r == 0 && c == 0) null else v } }
        val allowedTechniques = SudokuTechnique.entries.filter {
            it != SudokuTechnique.NAKED_SINGLE && it != SudokuTechnique.HIDDEN_SINGLE
        }

        assertNull(findNextMove(puzzle, allowedTechniques))

        val trace = traceHumanSolve(puzzle, allowedTechniques)
        assertTrue(!trace.solved)
        assertTrue(trace.blocked)
        assertTrue(trace.moves.isEmpty())
    }
}
