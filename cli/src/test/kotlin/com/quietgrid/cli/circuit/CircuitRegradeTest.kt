package com.quietgrid.cli.circuit

import com.quietgrid.engine.circuit.CircuitBoard
import com.quietgrid.engine.circuit.CircuitPuzzleEntry
import com.quietgrid.engine.circuit.CircuitTier
import com.quietgrid.engine.circuit.countCircuitSolutions
import com.quietgrid.engine.circuit.rotateCircuitMask
import com.quietgrid.engine.core.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class CircuitRegradeTest {

    private val easy = generateCircuitPuzzle(Difficulty.EASY, Random(1))!!

    private fun entryFor(board: CircuitBoard, solution: IntArray, id: String, difficulty: String) = CircuitPuzzleEntry(
        id = id,
        rows = board.rows,
        cols = board.cols,
        difficulty = difficulty,
        source = board.source,
        wrap = board.wrap,
        solution = solution.toList(),
        start = solution.map { rotateCircuitMask(it) },
    )

    @Test
    fun `a puzzle whose stored tier is wrong is moved`() {
        val report = regradeCircuitBank(listOf(easy.copy(difficulty = "medium")))
        assertEquals(listOf(CircuitTierMove(easy.id, "medium", "easy")), report.moved)
        assertEquals("easy", report.kept.single().difficulty)
        assertTrue(report.untiered.isEmpty())
    }

    @Test
    fun `a six by six wrapped puzzle is untiered and not broken`() {
        val board = CircuitBoard(6, 6, circuitSourceCell(6, 6), wrap = true)
        val (solution, _) = climbCircuitTree(board, CircuitTier(6, 6, true, 0..1000), Random(1))!!
        val report = regradeCircuitBank(listOf(entryFor(board, solution, "six", "hard")))
        assertEquals(listOf(CircuitUntiered("six", broken = false, solverGap = false)), report.untiered)
        assertTrue(report.kept.isEmpty())
    }

    @Test
    fun `a puzzle with two answers is untiered and broken`() {
        val board = CircuitBoard(5, 5, 12, wrap = true)
        val random = Random(9)
        val ambiguous = generateSequence { randomCircuitTree(board, random) }
            .first { countCircuitSolutions(board, it) >= 2 }
        val report = regradeCircuitBank(listOf(entryFor(board, ambiguous, "two", "hard")))
        assertEquals(listOf(CircuitUntiered("two", broken = true, solverGap = false)), report.untiered)
    }

    @Test
    fun `a correctly tiered puzzle is kept unchanged`() {
        val report = regradeCircuitBank(listOf(easy))
        assertEquals(listOf(easy), report.kept)
        assertTrue(report.moved.isEmpty())
        assertTrue(report.untiered.isEmpty())
    }
}
