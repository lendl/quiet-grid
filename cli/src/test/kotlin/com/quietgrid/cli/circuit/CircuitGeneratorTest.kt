package com.quietgrid.cli.circuit

import com.quietgrid.engine.circuit.CIRCUIT_TIERS
import com.quietgrid.engine.circuit.CircuitBoard
import com.quietgrid.engine.circuit.circuitArmCount
import com.quietgrid.engine.circuit.circuitTeeCount
import com.quietgrid.engine.circuit.classifyCircuitDifficulty
import com.quietgrid.engine.circuit.countCircuitSolutions
import com.quietgrid.engine.circuit.isCircuitSolved
import com.quietgrid.engine.circuit.solveCircuit
import com.quietgrid.engine.circuit.toBoard
import com.quietgrid.engine.core.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class CircuitGeneratorTest {
    @Test
    fun `random trees and their mutations span the board without crosses`() {
        val random = Random(7)
        listOf(false, true).forEach { wrap ->
            val board = CircuitBoard(6, 6, circuitSourceCell(6, 6), wrap)
            repeat(30) {
                val tree = randomCircuitTree(board, random) ?: return@repeat
                assertTrue(isCircuitSolved(board, tree))
                assertTrue(tree.all { circuitArmCount(it) in 1..CIRCUIT_MAX_ARMS })
                val mutated = mutateCircuitTree(board, tree, random) ?: return@repeat
                assertTrue(isCircuitSolved(board, mutated))
            }
        }
    }

    @Test
    fun `capped trees and their mutations never exceed the T-tile cap`() {
        val random = Random(9)
        val board = CircuitBoard(5, 5, circuitSourceCell(5, 5))
        var trees = 0
        var mutations = 0
        repeat(80) {
            val tree = randomCircuitTree(board, random, maxTees = 2) ?: return@repeat
            trees++
            assertTrue(isCircuitSolved(board, tree))
            assertTrue(circuitTeeCount(tree) <= 2)
            val mutated = mutateCircuitTree(board, tree, random, maxTees = 2) ?: return@repeat
            mutations++
            assertTrue(isCircuitSolved(board, mutated))
            assertTrue(circuitTeeCount(mutated) <= 2)
        }
        assertTrue(trees > 0 && mutations > 0)
    }

    @Test
    fun `the logic solver finishes exactly the trees that have one answer`() {
        val random = Random(5)
        listOf(false, true).forEach { wrap ->
            val board = CircuitBoard(5, 5, circuitSourceCell(5, 5), wrap)
            repeat(60) {
                val tree = randomCircuitTree(board, random) ?: return@repeat
                val solved = solveCircuit(board, tree)
                assertEquals(countCircuitSolutions(board, tree) == 1, solved.solved)
            }
        }
    }

    @Test
    fun `generated puzzles land on the requested tier with a single answer`() {
        Difficulty.entries.forEach { difficulty ->
            val entry = generateCircuitPuzzle(difficulty, Random(difficulty.ordinal + 1))!!
            val board = entry.toBoard()
            val shapes = entry.solution.toIntArray()
            assertEquals(CIRCUIT_TIERS.getValue(difficulty).wrap, entry.wrap)
            assertEquals(difficulty, classifyCircuitDifficulty(board, solveCircuit(board, shapes)))
            assertEquals(1, countCircuitSolutions(board, shapes))
        }
    }

}
