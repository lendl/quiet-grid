package com.quietgrid.engine.sudoku

import com.quietgrid.engine.journey.GridTransform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SudokuTransformTest {
    private val classicSolution = listOf(
        "534678912", "672195348", "198342567", "859761423", "426853791", "713924856", "961537284", "287419635", "345286179",
    ).map { row -> row.map { it.digitToInt() } }

    private val classic = SudokuPuzzleEntry(
        id = "s-x",
        difficulty = "easy",
        givens = classicSolution.mapIndexed { r, row -> row.mapIndexed { c, v -> v.takeIf { (r + c) % 3 != 0 } } },
        solution = classicSolution,
    )

    private val miniSolution = listOf(
        listOf(1, 2, 3, 4, 5, 6),
        listOf(4, 5, 6, 1, 2, 3),
        listOf(2, 3, 1, 5, 6, 4),
        listOf(5, 6, 4, 2, 3, 1),
        listOf(3, 1, 2, 6, 4, 5),
        listOf(6, 4, 5, 3, 1, 2),
    )

    private val mini = SudokuPuzzleEntry(
        id = "m-x",
        difficulty = "medium",
        givens = miniSolution.mapIndexed { r, row -> row.mapIndexed { c, v -> v.takeIf { (r + c) % 2 == 0 } } },
        solution = miniSolution,
    )

    private fun isValid(grid: List<List<Int>>): Boolean {
        val layout = SudokuLayout.forSize(grid.size)
        val digits = (1..grid.size).toSet()
        val rows = grid.all { it.toSet() == digits }
        val cols = grid.indices.all { c -> grid.map { it[c] }.toSet() == digits }
        val boxes = (0 until grid.size step layout.boxRows).all { top ->
            (0 until grid.size step layout.boxCols).all { left ->
                (top until top + layout.boxRows).flatMap { r -> (left until left + layout.boxCols).map { c -> grid[r][c] } }.toSet() == digits
            }
        }
        return rows && cols && boxes
    }

    @Test
    fun `fixtures are valid`() {
        assertTrue(isValid(classic.solution))
        assertTrue(isValid(mini.solution))
    }

    @Test
    fun `classic boards allow every transform and stay valid and solvable`() {
        assertEquals(GridTransform.entries - GridTransform.IDENTITY, sudokuReplayCandidates(classic))
        GridTransform.entries.forEach { t ->
            val moved = classic.transformed(t)
            assertTrue(t.name, isValid(moved.solution))
            moved.givens.forEachIndexed { r, row -> row.forEachIndexed { c, v -> if (v != null) assertEquals(t.name, moved.solution[r][c], v) } }
            assertNotNull(t.name, gradeSudoku(moved.givens))
            assertEquals(classic.id, moved.id)
        }
    }

    @Test
    fun `six by six boards only allow transforms that keep their two by three boxes`() {
        assertEquals(listOf(GridTransform.MIRROR_H, GridTransform.ROTATE_180, GridTransform.MIRROR_V), sudokuReplayCandidates(mini))
        sudokuReplayCandidates(mini).forEach { t -> assertTrue(t.name, isValid(mini.transformed(t).solution)) }
        assertFalse(isValid(mini.transformed(GridTransform.ROTATE_90).solution))
    }
}
