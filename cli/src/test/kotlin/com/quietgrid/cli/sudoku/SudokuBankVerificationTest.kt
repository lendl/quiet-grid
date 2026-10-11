package com.quietgrid.cli.sudoku

import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.sudoku.SudokuCandidateEliminationMove
import com.quietgrid.engine.sudoku.SudokuPlacementMove
import com.quietgrid.engine.sudoku.SudokuPuzzleEntry
import com.quietgrid.engine.sudoku.SudokuSolveTrace
import com.quietgrid.engine.sudoku.gradeSudoku
import com.quietgrid.engine.sudoku.traceHumanSolve
import com.quietgrid.engine.sudoku.traceStrictSolve
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SudokuBankVerificationTest {
    private val bank: List<SudokuPuzzleEntry> = Json { ignoreUnknownKeys = true }
        .decodeFromString(ListSerializer(SudokuPuzzleEntry.serializer()), File("app/src/main/assets/sudoku_puzzles.json").readText())

    @Test
    fun `ids are unique`() {
        assertEquals(bank.size, bank.map { it.id }.toSet().size)
    }

    private fun SudokuSolveTrace.agreesWith(solution: List<List<Int>>): Boolean = solved && moves.all { move ->
        when (move) {
            is SudokuPlacementMove -> solution[move.targetRow][move.targetCol] == move.digit
            is SudokuCandidateEliminationMove -> move.eliminations.none { (row, col, digit) -> solution[row][col] == digit }
        }
    }

    @Test
    fun `every puzzle solves by logic to the stored solution`() {
        val failing = bank.filterNot { traceHumanSolve(it.givens).agreesWith(it.solution) }.map { it.id }
        assertTrue("$failing", failing.isEmpty())
    }

    @Test
    fun `every strict solve step agrees with the stored solution`() {
        val failing = bank.filterNot { traceStrictSolve(it.givens).agreesWith(it.solution) }.map { it.id }
        assertTrue("$failing", failing.isEmpty())
    }

    @Test
    fun `every stored label matches the grader`() {
        val mismatched = bank.filter { gradeSudoku(it.givens)?.difficulty?.key != it.difficulty }.map { it.id }
        assertTrue("$mismatched", mismatched.isEmpty())
    }

    @Test
    fun `every tier has at least 200 puzzles`() {
        for (tier in Difficulty.entries) {
            val count = bank.count { it.difficulty == tier.key }
            assertTrue("${tier.key} has $count", count >= 200)
        }
    }
}
