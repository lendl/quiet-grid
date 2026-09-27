package com.quietgrid.engine.wordsearch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSearchHiddenWordTest {
    @Test
    fun `buildHiddenWordPool dedupes and drops words shorter than 3 letters`() {
        val pool = buildHiddenWordPool(listOf("cat", "CAT", "ox", "dog"))
        assertEquals(listOf("CAT", "DOG"), pool)
    }

    @Test
    fun `reserveHiddenWordCells returns cells sorted into reading order`() {
        val reserved = reserveHiddenWordCells("CAT", rows = 4, cols = 4)
        assertEquals(3, reserved.positions.size)
        for (i in 0 until reserved.positions.size - 1) {
            val a = reserved.positions[i]
            val b = reserved.positions[i + 1]
            assertTrue(a.row < b.row || (a.row == b.row && a.col < b.col))
        }
    }

    private val catPlacement = WordPlacement("1", "CAT", WSCellRef(0, 0), WordSearchDirection.RIGHT, listOf(WSCellRef(0, 0), WSCellRef(0, 1), WSCellRef(0, 2)))
    private val hiddenPositions = listOf(WSCellRef(0, 3), WSCellRef(0, 4), WSCellRef(0, 5))

    @Test
    fun `fillGhostFreeHiddenWord skips a candidate that would repeat a placed word`() {
        val grid = listOf(mutableListOf("C", "A", "T", "#", "#", "#"))
        val chosen = fillGhostFreeHiddenWord(grid, listOf(catPlacement), hiddenPositions, listOf("TAC", "DOG"))
        assertEquals("DOG", chosen)
        assertEquals(listOf("C", "A", "T", "D", "O", "G"), grid[0])
    }

    @Test
    fun `fillGhostFreeHiddenWord returns null when every candidate repeats a placed word`() {
        val grid = listOf(mutableListOf("C", "A", "T", "#", "#", "#"))
        assertNull(fillGhostFreeHiddenWord(grid, listOf(catPlacement), hiddenPositions, listOf("TAC")))
    }
}
