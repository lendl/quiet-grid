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

    // Builds a 9x9 grid of nulls with the given (row, col, digit) triples filled in -- lets
    // each fixture below spell out only the cells it cares about, mirroring the helper used in
    // BasicTechniquesTest for individual technique fixtures.
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
        // Blank out two cells that share no row, column, or box -- (0,0) is in box 0, (4,4) is
        // in box 4 -- so each is independently forced from the start (neither depends on the
        // other being filled in first). Confirms the loop drives the state to completion across
        // multiple iterations, re-deriving candidates and re-scoring after each applied move.
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
        // Proves findNextMoveInState's selection is driven by real score comparison across
        // techniques, not "first applicable dispatcher wins" -- by constructing a board where
        // NakedPairTechnique (index 2 in orderedTechniqueDispatchers, base score 6) beats
        // HiddenSingleTechnique (index 1, dispatched first, base score 3) on total score, and
        // verifying (via BasicTechniquesTest-style direct dispatcher calls, values confirmed by
        // actually running the code) that both techniques are genuinely evaluated -- HiddenSingle
        // is not skipped by the early "technique base score >= bestScore" prune, and NakedPair's
        // findMove is actually invoked and its move actually wins.
        //
        // Box 0 has only 5 givens -- (0,2)=3, (1,2)=4, (2,0)=5, (2,1)=6, (2,2)=7 -- leaving 4
        // blanks: (0,0), (0,1), (1,0), (1,1). Placing (0,4)=1 and (0,5)=8 strips digits 1 and 8
        // from row 0, so (0,0)/(0,1) end up with exactly {2,9} (a naked pair), while (1,0)/(1,1)
        // (row 1, unaffected by the row-0 strips) keep all four box-0-missing digits {1,2,8,9}
        // each -- so no digit is confined to a single box-0 cell, and there is deliberately no
        // coincidental hidden single sitting in this same house (unlike a minimal 3-blank box,
        // where stripping one digit from two cells out of three always leaves it a hidden single
        // in the third -- confirmed by running the code on that layout during fixture design).
        // NakedPairTechnique.findMove returns this pair with complexity 4 (4 empty box-0 cells),
        // so score = 6 + 4 = 10.
        //
        // Box 6 (rows 6-8, cols 0-2) has only one given -- (7,2)=9 -- leaving 8 blanks. Digit 7 is
        // stripped from every box-6 cell except (8,0): (2,2)=7 already excludes it from column 2's
        // (6,2)/(8,2); (6,4)=7 excludes it from the rest of row 6; (7,6)=7 excludes it from the
        // rest of row 7; (3,1)=7 excludes it from column 1's (6,1)/(7,1)/(8,1). That leaves (8,0)
        // as the only box-6 cell that can hold 7 -- a hidden single, complexity 8 (8 empty box
        // cells * 1.0 box weight), so score = 3 + 8 = 11.
        //
        // HiddenSingleTechnique (index 1) is dispatched first and sets bestScore = 11. Because
        // NakedPairTechnique's base score (6) is less than 11, it is NOT skipped by the early
        // prune -- its findMove is actually called, returns score 10, and 10 < 11 makes it the
        // new best. A naive "first applicable technique wins" dispatcher would wrongly return the
        // hidden single; the real score-comparing dispatcher correctly returns the naked pair.
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
        // Row 8 is filled with every digit except 5, which is missing from column 8: a naked
        // single at (8,8), complexity 0 (score 2) -- cheaper than anything else on this sparse
        // board, so it wins by default. Because row 8 has only that one blank cell, the same cell
        // is *also* a hidden single from row 8's perspective (score 3 + 1 = 4). Excluding
        // NAKED_SINGLE must make the dispatcher fall through to that next-cheapest applicable
        // technique instead of returning null -- proving the allowedTechniques filter is actually
        // consulted per-dispatcher during selection, not just used to gate the overall result.
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
        // A single blank on an otherwise fully solved board is forced by every one of its row,
        // column, and box each having just that one empty cell -- so it registers as both a
        // naked single AND a hidden single (any house with exactly one blank coincides with
        // both). Excluding both is required to genuinely starve the dispatcher; with nothing left
        // to pair, point, or chain on a board this close to solved, no other technique can step in.
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
