package com.quietgrid.engine.killersudoku

import com.quietgrid.engine.journey.GridTransform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KillerSudokuTransformTest {
    private val entry = KillerSudokuPuzzleEntry(
        id = "ks-easy-6vk0m1",
        difficulty = "easy",
        cages = listOf(
            KillerSudokuCage(13, listOf(0, 9, 18)),
            KillerSudokuCage(11, listOf(1, 10)),
            KillerSudokuCage(15, listOf(2, 3, 11, 12)),
            KillerSudokuCage(15, listOf(4, 13, 14)),
            KillerSudokuCage(17, listOf(5, 6)),
            KillerSudokuCage(12, listOf(7, 8, 17)),
            KillerSudokuCage(9, listOf(15, 24)),
            KillerSudokuCage(15, listOf(16, 25)),
            KillerSudokuCage(17, listOf(19, 20, 21)),
            KillerSudokuCage(10, listOf(22, 23)),
            KillerSudokuCage(7, listOf(26, 35)),
            KillerSudokuCage(16, listOf(27, 28, 29)),
            KillerSudokuCage(10, listOf(30, 31)),
            KillerSudokuCage(13, listOf(32, 33, 34)),
            KillerSudokuCage(15, listOf(36, 45, 54)),
            KillerSudokuCage(14, listOf(37, 38, 39)),
            KillerSudokuCage(9, listOf(40, 41)),
            KillerSudokuCage(5, listOf(42, 43)),
            KillerSudokuCage(29, listOf(44, 53, 62, 71)),
            KillerSudokuCage(10, listOf(46, 47)),
            KillerSudokuCage(12, listOf(48, 49)),
            KillerSudokuCage(13, listOf(50, 51)),
            KillerSudokuCage(6, listOf(52, 61, 70)),
            KillerSudokuCage(17, listOf(55, 64)),
            KillerSudokuCage(10, listOf(56, 57)),
            KillerSudokuCage(10, listOf(58, 59)),
            KillerSudokuCage(14, listOf(60, 68, 69)),
            KillerSudokuCage(16, listOf(63, 72, 73)),
            KillerSudokuCage(3, listOf(65, 74)),
            KillerSudokuCage(7, listOf(66, 67)),
            KillerSudokuCage(9, listOf(75, 76)),
            KillerSudokuCage(17, listOf(77, 78)),
            KillerSudokuCage(9, listOf(79, 80)),
        ),
        solution = listOf("146359872", "975182463", "328764591", "457913286", "863527149", "219846735", "584691327", "792435618", "631278954").map { row -> row.map { it.digitToInt() } },
    )

    private fun isValidSolution(puzzle: KillerSudokuPuzzleEntry): Boolean {
        val grid = puzzle.solution
        val digits = (1..9).toSet()
        val rows = grid.all { it.toSet() == digits }
        val cols = (0 until 9).all { c -> grid.map { it[c] }.toSet() == digits }
        val boxes = (0 until 9).all { b -> (0 until 9).map { i -> grid[b / 3 * 3 + i / 3][b % 3 * 3 + i % 3] }.toSet() == digits }
        val cages = puzzle.cages.all { cage ->
            val values = cage.cells.map { grid[it / 9][it % 9] }
            values.sum() == cage.sum && values.toSet().size == values.size
        }
        return rows && cols && boxes && cages
    }

    @Test
    fun `every transform keeps a valid cage layout and solution`() {
        assertTrue(isValidSolution(entry))
        GridTransform.entries.forEach { t ->
            val moved = entry.transformed(t)
            assertTrue(t.name, isValidSolution(moved))
            assertEquals(t.name, (0 until 81).toList(), moved.cages.flatMap { it.cells }.sorted())
            assertEquals(entry.id, moved.id)
        }
        assertTrue(entry.transformed(GridTransform.ROTATE_90).cages != entry.cages)
    }

    @Test
    fun `every transform still solves by logic`() {
        GridTransform.entries.forEach { t ->
            assertTrue(t.name, traceKillerSolve(KillerCageLayout(entry.transformed(t).cages)).solved)
        }
    }

    @Test
    fun `replay candidates are every transform but the original`() {
        assertEquals(GridTransform.entries - GridTransform.IDENTITY, killerSudokuReplayCandidates(entry))
    }
}
