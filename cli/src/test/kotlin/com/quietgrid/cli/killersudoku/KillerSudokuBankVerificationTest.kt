package com.quietgrid.cli.killersudoku

import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.killersudoku.KillerCageLayout
import com.quietgrid.engine.killersudoku.KillerSudokuPuzzleEntry
import com.quietgrid.engine.killersudoku.classifyKillerDifficulty
import com.quietgrid.engine.killersudoku.collectKillerMetrics
import com.quietgrid.engine.killersudoku.traceKillerSolve
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class KillerSudokuBankVerificationTest {
    private val bank: List<KillerSudokuPuzzleEntry> = Json { ignoreUnknownKeys = true }
        .decodeFromString(ListSerializer(KillerSudokuPuzzleEntry.serializer()), File("app/src/main/assets/killersudoku_puzzles.json").readText())

    @Test
    fun `every tier has at least 100 puzzles`() {
        Difficulty.entries.forEach { difficulty ->
            assertTrue(difficulty.key, bank.count { it.difficulty == difficulty.key } >= 100)
        }
    }

    @Test
    fun `ids are unique`() {
        assertEquals(bank.size, bank.map { it.id }.toSet().size)
    }

    @Test
    fun `cages partition the grid and match the solution`() {
        bank.forEach { entry ->
            val solution = entry.solution.flatten()
            assertEquals(entry.id, 81, solution.size)
            KillerCageLayout(entry.cages)
            entry.cages.forEach { cage ->
                assertEquals(entry.id, cage.sum, cage.cells.sumOf { solution[it] })
                assertEquals(entry.id, cage.cells.size, cage.cells.map { solution[it] }.toSet().size)
            }
        }
    }

    @Test
    fun `sampled puzzles solve by logic to the stored solution at their tier`() {
        val failing = bank.filterIndexed { index, _ -> index % 10 == 0 }.filterNot { entry ->
            val layout = KillerCageLayout(entry.cages)
            val trace = traceKillerSolve(layout)
            val solution = entry.solution.flatten()
            trace.solved &&
                trace.moves.all { move -> !move.isPlacement || solution[move.placementCell!!] == move.placementDigit } &&
                trace.moves.all { move -> move.eliminations.none { (cell, digit) -> solution[cell] == digit } } &&
                classifyKillerDifficulty(collectKillerMetrics(trace)).key == entry.difficulty
        }.map { it.id }
        assertTrue("$failing", failing.isEmpty())
    }
}
