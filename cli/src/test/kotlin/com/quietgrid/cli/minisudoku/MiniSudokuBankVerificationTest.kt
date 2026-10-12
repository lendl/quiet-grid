package com.quietgrid.cli.minisudoku

import com.quietgrid.cli.sudoku.countSudokuSolutions
import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.sudoku.MINI_SUDOKU_SIZE_BY_DIFFICULTY
import com.quietgrid.engine.sudoku.SudokuCandidateEliminationMove
import com.quietgrid.engine.sudoku.SudokuPlacementMove
import com.quietgrid.engine.sudoku.SudokuPuzzleEntry
import com.quietgrid.engine.sudoku.SudokuSolveTrace
import com.quietgrid.engine.sudoku.classifyMiniSudoku
import com.quietgrid.engine.sudoku.traceHumanSolve
import com.quietgrid.engine.sudoku.traceStrictSolve
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MiniSudokuBankVerificationTest {
    private val bank: List<SudokuPuzzleEntry> =
        Json.decodeFromString(File("app/src/main/assets/minisudoku_puzzles.json").readText())

    private fun SudokuSolveTrace.agreesWith(solution: List<List<Int>>): Boolean = solved && moves.all { move ->
        when (move) {
            is SudokuPlacementMove -> solution[move.targetRow][move.targetCol] == move.digit
            is SudokuCandidateEliminationMove -> move.eliminations.none { (row, col, digit) -> solution[row][col] == digit }
        }
    }

    @Test
    fun `ids are unique`() {
        assertEquals(bank.size, bank.map { it.id }.toSet().size)
    }

    @Test
    fun `every tier has at least 200 puzzles at its grid size`() {
        for (tier in Difficulty.entries) {
            val entries = bank.filter { it.difficulty == tier.key }
            assertTrue("${tier.key} has ${entries.size}", entries.size >= 200)
            assertTrue(tier.key, entries.all { it.givens.size == MINI_SUDOKU_SIZE_BY_DIFFICULTY.getValue(tier) })
        }
    }

    @Test
    fun `every puzzle has a unique solution matching its stored grid`() {
        val failing = bank.filterNot { entry ->
            countSudokuSolutions(entry.givens, limit = 2) == 1 &&
                entry.givens.indices.all { r -> entry.givens[r].indices.all { c -> entry.givens[r][c].let { it == null || it == entry.solution[r][c] } } }
        }.map { it.id }
        assertTrue("$failing", failing.isEmpty())
    }

    @Test
    fun `every puzzle solves by logic to the stored solution`() {
        val failing = bank.filterNot { traceHumanSolve(it.givens).agreesWith(it.solution) && traceStrictSolve(it.givens).agreesWith(it.solution) }
            .map { it.id }
        assertTrue("$failing", failing.isEmpty())
    }

    @Test
    fun `every stored label matches the grader`() {
        val mismatched = bank.filter { classifyMiniSudoku(it.givens)?.key != it.difficulty }.map { it.id }
        assertTrue("$mismatched", mismatched.isEmpty())
    }
}
