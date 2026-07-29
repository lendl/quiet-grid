// cli/src/test/kotlin/com/quietgrid/cli/sudoku/SudokuGeneratorTest.kt
package com.quietgrid.cli.sudoku

import com.quietgrid.engine.core.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SudokuGeneratorTest {
    @Test
    fun `generateSudokuPuzzle produces a solvable easy puzzle with a unique solution`() {
        val entry = generateSudokuPuzzle(Difficulty.EASY, idPrefix = "s9")
        assertNotNull(entry)
        assertEquals("easy", entry!!.difficulty)
        assertEquals(1, countSudokuSolutions(entry.givens, limit = 2))
    }
}
