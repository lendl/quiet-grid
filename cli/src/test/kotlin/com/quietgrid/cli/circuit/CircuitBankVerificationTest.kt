package com.quietgrid.cli.circuit

import com.quietgrid.cli.ASSETS_DIR
import com.quietgrid.engine.circuit.CIRCUIT_TIERS
import com.quietgrid.engine.circuit.CircuitPuzzleEntry
import com.quietgrid.engine.circuit.circuitOrientations
import com.quietgrid.engine.circuit.classifyCircuitDifficulty
import com.quietgrid.engine.circuit.countCircuitSolutions
import com.quietgrid.engine.circuit.isCircuitSolved
import com.quietgrid.engine.circuit.solveCircuit
import com.quietgrid.engine.circuit.toBoard
import com.quietgrid.engine.core.Difficulty
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CircuitBankVerificationTest {
    private val bank: List<CircuitPuzzleEntry> = Json { ignoreUnknownKeys = true }
        .decodeFromString(ListSerializer(CircuitPuzzleEntry.serializer()), File("$ASSETS_DIR/circuit_puzzles.json").readText())

    @Test
    fun `every tier has at least 120 puzzles of its size`() {
        Difficulty.entries.forEach { difficulty ->
            val tier = CIRCUIT_TIERS.getValue(difficulty)
            val puzzles = bank.filter { it.difficulty == difficulty.key }
            assertTrue(difficulty.key, puzzles.size >= 120)
            assertTrue(difficulty.key, puzzles.all { it.rows == tier.rows && it.cols == tier.cols && it.wrap == tier.wrap })
        }
    }

    @Test
    fun `ids and layouts are unique`() {
        assertEquals(bank.size, bank.map { it.id }.toSet().size)
        assertEquals(bank.size, bank.map { it.solution }.toSet().size)
    }

    @Test
    fun `every start is the solution with each tile turned out of place`() {
        bank.forEach { entry ->
            assertEquals(entry.id, entry.rows * entry.cols, entry.start.size)
            entry.start.indices.forEach { cell ->
                assertTrue(entry.id, entry.start[cell] in circuitOrientations(entry.solution[cell]))
                assertTrue(entry.id, entry.start[cell] != entry.solution[cell])
            }
        }
    }

    @Test
    fun `every puzzle has one answer, matches its stored solution and its tier`() {
        bank.forEach { entry ->
            val board = entry.toBoard()
            val shapes = entry.solution.toIntArray()
            assertTrue(entry.id, isCircuitSolved(board, shapes))
            assertEquals(entry.id, 1, countCircuitSolutions(board, shapes))
            assertEquals(entry.id, entry.difficulty, classifyCircuitDifficulty(board, solveCircuit(board, shapes))?.key)
        }
    }
}
