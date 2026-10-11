package com.quietgrid.cli.killersudoku

import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.killersudoku.KillerCageLayout
import com.quietgrid.engine.killersudoku.classifyKillerDifficulty
import com.quietgrid.engine.killersudoku.collectKillerMetrics
import com.quietgrid.engine.killersudoku.searchKillerSolutions
import com.quietgrid.engine.killersudoku.traceKillerSolve
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class KillerSudokuGeneratorTest {
    @Test
    fun `generateKillerSudokuPuzzle produces an easy puzzle with sound cages and a unique solution`() {
        val result = generateKillerSudokuPuzzle(Difficulty.EASY, Random(7))
        assertNotNull(result)
        val entry = result!!.entry
        assertEquals("easy", entry.difficulty)
        val solution = entry.solution.flatten()
        entry.cages.forEach { cage ->
            assertEquals(cage.sum, cage.cells.sumOf { solution[it] })
            assertEquals(cage.cells.size, cage.cells.map { solution[it] }.toSet().size)
        }
        val layout = KillerCageLayout(entry.cages)
        val search = searchKillerSolutions(layout)
        assertEquals(1, search.count)
        assertTrue(search.solutions.single().contentEquals(solution.toIntArray()))
        val trace = traceKillerSolve(layout)
        assertTrue(trace.solved)
        assertEquals(Difficulty.EASY, classifyKillerDifficulty(collectKillerMetrics(trace)))
    }

    @Test
    fun `randomKillerSolution is a valid sudoku grid`() {
        val solution = randomKillerSolution(Random(3))
        for (i in 0 until 9) {
            assertEquals((1..9).toSet(), (0 until 9).map { solution[i * 9 + it] }.toSet())
            assertEquals((1..9).toSet(), (0 until 9).map { solution[it * 9 + i] }.toSet())
            val r = i / 3 * 3
            val c = i % 3 * 3
            assertEquals((1..9).toSet(), (0 until 9).map { solution[(r + it / 3) * 9 + c + it % 3] }.toSet())
        }
    }
}
