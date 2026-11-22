package com.quietgrid.engine.circuit

import com.quietgrid.engine.core.Difficulty
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.Test
import kotlin.random.Random

class CircuitDifficultyTest {

    private fun board(size: Int, wrap: Boolean) = CircuitBoard(size, size, (size / 2) * size + size / 2, wrap)

    private fun result(size: Int, longWhatIfs: Int, shortWhatIfs: Int = 0, loops: Int = 0, tees: Int = 0): CircuitSolveResult {
        val steps = buildList {
            repeat(longWhatIfs) { add(CircuitSolveStep(CircuitTechnique.CONTRADICTION, it, 2, chainLength = 5)) }
            repeat(shortWhatIfs) { add(CircuitSolveStep(CircuitTechnique.CONTRADICTION, it, 2, chainLength = 1)) }
            repeat(loops) { add(CircuitSolveStep(CircuitTechnique.NO_LOOP, it, 0)) }
            add(CircuitSolveStep(CircuitTechnique.EDGE_MATCH, 0, 0))
        }
        val tee = CIRCUIT_UP or CIRCUIT_RIGHT or CIRCUIT_DOWN
        return CircuitSolveResult(true, steps, IntArray(size * size) { if (it < tees) tee else CIRCUIT_UP })
    }

    private fun classify(size: Int, wrap: Boolean, longWhatIfs: Int, shortWhatIfs: Int = 0, loops: Int = 0, tees: Int = 0): Difficulty? =
        classifyCircuitDifficulty(board(size, wrap), result(size, longWhatIfs, shortWhatIfs, loops, tees))

    @Test
    fun `hard is seven by seven wrapped`() {
        val tier = CIRCUIT_TIERS.getValue(Difficulty.HARD)
        assertEquals(Triple(7, 7, true), Triple(tier.rows, tier.cols, tier.wrap))
    }

    @Test
    fun `a six by six wrapped board has no tier`() {
        assertNull(classify(6, true, 5))
    }

    @Test
    fun `easy accepts no long what-if and rejects one`() {
        assertEquals(Difficulty.EASY, classify(5, false, 0))
        assertNull(classify(5, false, 1))
    }

    @Test
    fun `easy accepts two T tiles and rejects three`() {
        assertEquals(Difficulty.EASY, classify(5, false, 0, tees = 2))
        assertNull(classify(5, false, 0, tees = 3))
    }

    @Test
    fun `only easy caps the T tiles`() {
        assertEquals(Difficulty.MEDIUM, classify(7, false, 1, tees = 20))
        assertEquals(Difficulty.HARD, classify(7, true, 3, tees = 20))
        assertEquals(Difficulty.EXPERT, classify(8, true, 12, tees = 20))
    }

    @Test
    fun `the T-tile count looks only at three-armed tiles`() {
        val tee = CIRCUIT_UP or CIRCUIT_RIGHT or CIRCUIT_DOWN
        assertEquals(2, circuitTeeCount(intArrayOf(tee, CIRCUIT_UP, CIRCUIT_UP or CIRCUIT_DOWN, CIRCUIT_UP or CIRCUIT_RIGHT, rotateCircuitMask(tee))))
    }

    @Test
    fun `medium accepts one and two long what-ifs and rejects zero and three`() {
        assertEquals(Difficulty.MEDIUM, classify(7, false, 1))
        assertEquals(Difficulty.MEDIUM, classify(7, false, 2))
        assertNull(classify(7, false, 0))
        assertNull(classify(7, false, 3))
    }

    @Test
    fun `medium no longer needs a no-loop step`() {
        assertEquals(Difficulty.MEDIUM, classify(7, false, 1, loops = 0))
        assertNull(classify(7, false, 0, loops = 3))
    }

    @Test
    fun `hard accepts three and nine long what-ifs and rejects two and ten`() {
        assertEquals(Difficulty.HARD, classify(7, true, 3))
        assertEquals(Difficulty.HARD, classify(7, true, 9))
        assertNull(classify(7, true, 2))
        assertNull(classify(7, true, 10))
    }

    @Test
    fun `expert accepts twelve and forty long what-ifs and rejects eleven and forty-one`() {
        assertEquals(Difficulty.EXPERT, classify(8, true, 12))
        assertEquals(Difficulty.EXPERT, classify(8, true, 40))
        assertNull(classify(8, true, 11))
        assertNull(classify(8, true, 41))
    }

    @Test
    fun `one-step what-ifs do not count toward a band`() {
        assertEquals(Difficulty.HARD, classify(7, true, 3, shortWhatIfs = 5))
        assertNull(classify(7, true, 2, shortWhatIfs = 5))
        assertEquals(Difficulty.EASY, classify(5, false, 0, shortWhatIfs = 2))
    }

    @Test
    fun `the tier distance counts only long what-ifs`() {
        val hard = CIRCUIT_TIERS.getValue(Difficulty.HARD)
        assertEquals(1, circuitTierDistance(hard, analyzeCircuitSolveResult(result(7, 2, shortWhatIfs = 4))))
        assertEquals(0, circuitTierDistance(hard, analyzeCircuitSolveResult(result(7, 3, shortWhatIfs = 4))))
    }

    @Test
    fun `a lamp facing a lamp is a short what-if whatever cells come first in scan order`() {
        val u = CIRCUIT_UP
        val r = CIRCUIT_RIGHT
        val d = CIRCUIT_DOWN
        val l = CIRCUIT_LEFT
        val board = CircuitBoard(3, 3, 0)
        val shapes = intArrayOf(r or d, l or r or d, l, u or d, u or r, l, u or r, l or r, l)
        val state = CircuitSolverState.fromShapes(board, shapes)
        val lampAbove = 5
        val keep = state.domains[lampAbove] and (1 shl d).inv()
        val deduction = CircuitDeduction(CircuitTechnique.CONTRADICTION, lampAbove, keep)
        assertTrue(circuitWhatIfChainLength(state, deduction) < CIRCUIT_LONG_WHAT_IF_CHAIN)
    }

    @Test
    fun `the solver records a chain length on every what-if and only there`() {
        val random = Random(3)
        val wrapped = CircuitBoard(5, 5, 12, wrap = true)
        var longWhatIfs = 0
        repeat(15) {
            val tree = generateLogicCompleteTree(wrapped, random)
            solveCircuit(wrapped, tree).steps.forEach { step ->
                if (step.technique == CircuitTechnique.CONTRADICTION) {
                    assertTrue(step.chainLength >= 0)
                    if (step.chainLength >= CIRCUIT_LONG_WHAT_IF_CHAIN) longWhatIfs++
                } else {
                    assertEquals(0, step.chainLength)
                }
            }
        }
        assertTrue(longWhatIfs > 0)
    }
}
