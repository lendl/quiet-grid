package com.quietgrid.cli.sudoku

import com.quietgrid.engine.sudoku.SudokuCandidateEliminationMove
import com.quietgrid.engine.sudoku.SudokuPlacementMove
import com.quietgrid.engine.sudoku.SudokuPuzzleEntry
import com.quietgrid.engine.sudoku.traceHumanSolve
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

    @Test
    fun `every puzzle solves by logic to the stored solution`() {
        val failing = bank.filterNot { entry ->
            val trace = traceHumanSolve(entry.givens)
            trace.solved && trace.moves.all { move ->
                when (move) {
                    is SudokuPlacementMove -> entry.solution[move.targetRow][move.targetCol] == move.digit
                    is SudokuCandidateEliminationMove -> move.eliminations.none { (row, col, digit) -> entry.solution[row][col] == digit }
                }
            }
        }.map { it.id }
        assertTrue("$failing", failing.isEmpty())
    }
}
