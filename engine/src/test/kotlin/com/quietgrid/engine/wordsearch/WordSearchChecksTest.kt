package com.quietgrid.engine.wordsearch

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSearchChecksTest {
    @Test
    fun `hasCoverageViolation is true when one placement's cells are all covered by others`() {
        val placements = listOf(
            WordPlacement("1", "AT", WSCellRef(0, 0), WordSearchDirection.RIGHT, listOf(WSCellRef(0, 0), WSCellRef(0, 1))),
            WordPlacement("2", "CAT", WSCellRef(0, 0), WordSearchDirection.RIGHT, listOf(WSCellRef(0, 0), WSCellRef(0, 1), WSCellRef(0, 2))),
        )
        assertTrue(hasCoverageViolation(placements))
    }

    @Test
    fun `hasCoverageViolation is false when every placement has at least one unshared cell`() {
        val placements = listOf(
            WordPlacement("1", "CAT", WSCellRef(0, 0), WordSearchDirection.RIGHT, listOf(WSCellRef(0, 0), WSCellRef(0, 1), WSCellRef(0, 2))),
            WordPlacement("2", "DOG", WSCellRef(1, 0), WordSearchDirection.RIGHT, listOf(WSCellRef(1, 0), WSCellRef(1, 1), WSCellRef(1, 2))),
        )
        assertFalse(hasCoverageViolation(placements))
    }

    @Test
    fun `hasDuplicateOccurrence is true when a word appears again outside its intended placement`() {
        val grid = listOf(
            listOf("C", "A", "T", "C", "A", "T"),
        )
        val words = listOf("CAT" to listOf(WSCellRef(0, 0), WSCellRef(0, 1), WSCellRef(0, 2)))
        assertTrue(hasDuplicateOccurrence(grid, words))
    }
}
