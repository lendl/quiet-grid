package com.quietgrid.engine.circuit

import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.Test
import kotlin.random.Random

class CircuitSolverTest {
    private val u = CIRCUIT_UP
    private val r = CIRCUIT_RIGHT
    private val d = CIRCUIT_DOWN
    private val l = CIRCUIT_LEFT

    private val comb = intArrayOf(
        r or d, l or r or d, l or d,
        u or d, u or d, u or d,
        u, u, u,
    )

    @Test
    fun `edge matching alone solves a small flat board`() {
        val board = CircuitBoard(3, 3, 4)
        val result = solveCircuit(board, comb)
        assertTrue(result.solved)
        assertContentEquals(comb, result.masks)
        assertEquals(1, countCircuitSolutions(board, comb))
        assertTrue(result.steps.all { it.technique == CircuitTechnique.EDGE_MATCH })
    }

    @Test
    fun `the logic solver finishes only puzzles with one answer`() {
        val board = CircuitBoard(2, 2, 0)
        val shapes = intArrayOf(r or d, l, u, u)
        val unique = countCircuitSolutions(board, shapes)
        val result = solveCircuit(board, shapes)
        assertEquals(unique == 1, result.solved)
    }

    @Test
    fun `a closed loop of forced links is a contradiction`() {
        val board = CircuitBoard(2, 2, 0)
        val state = CircuitSolverState.withFixed(board, intArrayOf(r or d, r or d, r or d, r or d), mapOf(0 to (r or d), 1 to (l or d), 2 to (u or r), 3 to (u or l)))
        assertTrue(hasCircuitContradiction(state))
    }

    @Test
    fun `no loop removes the orientation that would close a ring`() {
        val board = CircuitBoard(2, 3, 0)
        val shapes = intArrayOf(r or d, l or r or d, l, u or r, l or u, u)
        val fixed = mapOf(0 to (r or d))
        val state = CircuitSolverState.withFixed(board, shapes, fixed)
        val deduction = findCircuitNoLoop(state)!!
        assertEquals(4, deduction.cell)
        assertFalse(deduction.keep and (1 shl (l or u)) != 0)
    }

    @Test
    fun `wrapped boards solve through what-if steps`() {
        val board = CircuitBoard(3, 3, 4, wrap = true)
        val solution = intArrayOf(r, l or r or d, l, u or d, u or d, d, u or r, u or l, u)
        val result = solveCircuit(board, solution)
        if (countCircuitSolutions(board, solution) == 1) assertTrue(result.solved) else assertFalse(result.solved)
    }

    @Test
    fun `hints point at a logically forced tile then at locked mistakes then stop`() {
        val puzzle = CircuitPuzzleEntry("comb", 3, 3, "easy", 4, solution = comb.toList(), start = comb.map { rotateCircuitMask(it) })
        val start = puzzle.start.toIntArray()
        val hint = findCircuitHint(puzzle, start, BooleanArray(9))!!
        assertFalse(hint.isMistake)
        assertEquals(comb[hint.cell], hint.mask)
        assertEquals(CircuitTechnique.EDGE_MATCH, hint.technique)

        val locked = BooleanArray(9).also { it[0] = true }
        val mistake = findCircuitHint(puzzle, start, locked)!!
        assertTrue(mistake.isMistake)
        assertEquals(0, mistake.cell)

        assertNull(findCircuitHint(puzzle, comb.copyOf(), BooleanArray(9)))
    }

    @Test
    fun `every hint on random logic-complete boards matches the solution until solved`() {
        val random = Random(11)
        val board = CircuitBoard(5, 5, 12)
        repeat(20) { round ->
            val solution = generateLogicCompleteTree(board, random)
            val puzzle = CircuitPuzzleEntry("r$round", 5, 5, "easy", 12, solution = solution.toList(), start = solution.map { rotateCircuitMask(it) })
            val current = puzzle.start.toIntArray()
            val locked = BooleanArray(board.size)
            var guard = 0
            while (guard++ < 100) {
                val hint = findCircuitHint(puzzle, current, locked) ?: break
                assertEquals(solution[hint.cell], hint.mask)
                current[hint.cell] = hint.mask
                locked[hint.cell] = true
            }
            assertContentEquals(solution, current)
        }
    }

    @Test
    fun `hints on wrapped boards name a technique and match the solution until solved`() {
        val random = Random(21)
        val board = CircuitBoard(5, 5, 12, wrap = true)
        repeat(10) { round ->
            val solution = generateLogicCompleteTree(board, random)
            val puzzle = CircuitPuzzleEntry("w$round", 5, 5, "hard", 12, wrap = true, solution = solution.toList(), start = solution.map { rotateCircuitMask(it) })
            val current = puzzle.start.toIntArray()
            val locked = BooleanArray(board.size)
            var hint: CircuitHint? = findCircuitHint(puzzle, current, locked)
            var guard = 0
            while (hint != null && guard++ < 100) {
                assertEquals(solution[hint.cell], hint.mask)
                assertNotNull(hint.technique)
                current[hint.cell] = hint.mask
                locked[hint.cell] = true
                hint = findCircuitHint(puzzle, current, locked)
            }
            assertContentEquals(solution, current)
        }
    }

    @Test
    fun `a locked wrong tile is reported before any deduction`() {
        val puzzle = CircuitPuzzleEntry("comb", 3, 3, "easy", 4, solution = comb.toList(), start = comb.map { rotateCircuitMask(it) })
        val current = comb.copyOf().also { it[6] = rotateCircuitMask(comb[6]) }
        val locked = BooleanArray(9).also {
            it[0] = true
            it[6] = true
        }
        val hint = findCircuitHint(puzzle, current, locked)!!
        assertEquals(6, hint.cell)
        assertTrue(hint.isMistake)
        assertNull(hint.technique)
        assertEquals(comb[6], hint.mask)
    }
}
