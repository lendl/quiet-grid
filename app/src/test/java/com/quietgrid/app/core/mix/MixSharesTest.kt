package com.quietgrid.app.core.mix

import org.junit.Assert.assertEquals
import org.junit.Test

class MixSharesTest {

    private val sudokuEasy = MixEntry("sudoku", MixEntryMode.PUZZLE, "easy", weight = 1)
    private val sudokuHard = MixEntry("sudoku", MixEntryMode.PUZZLE, "hard", weight = 2)
    private val takuzuEasy = MixEntry("takuzu", MixEntryMode.PUZZLE, "easy", weight = 3)

    @Test
    fun `entry share is its weight over the total weight rounded to a whole percent`() {
        val entries = listOf(sudokuEasy, sudokuHard, takuzuEasy)
        assertEquals(17, entrySharePercent(sudokuEasy, entries))
        assertEquals(33, entrySharePercent(sudokuHard, entries))
        assertEquals(50, entrySharePercent(takuzuEasy, entries))
    }

    @Test
    fun `game share sums its entries before rounding`() {
        val entries = listOf(sudokuEasy, sudokuHard, takuzuEasy)
        assertEquals(50, gameSharePercent("sudoku", entries))
        assertEquals(50, gameSharePercent("takuzu", entries))
    }

    @Test
    fun `shares are zero when there are no entries`() {
        assertEquals(0, gameSharePercent("sudoku", emptyList()))
    }

    @Test
    fun `plus raises weight up to ten`() {
        val entries = listOf(sudokuHard.copy(weight = 9))
        val once = stepEntryWeight(entries, sudokuHard, +1)
        assertEquals(10, once.single().weight)
        assertEquals(10, stepEntryWeight(once, sudokuHard, +1).single().weight)
    }

    @Test
    fun `minus lowers weight and removes the entry at weight one`() {
        val entries = listOf(sudokuEasy, sudokuHard)
        assertEquals(listOf(sudokuEasy, sudokuHard.copy(weight = 1)), stepEntryWeight(entries, sudokuHard, -1))
        assertEquals(listOf(sudokuHard), stepEntryWeight(entries, sudokuEasy, -1))
    }
}
