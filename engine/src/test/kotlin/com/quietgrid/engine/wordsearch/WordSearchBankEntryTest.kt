package com.quietgrid.engine.wordsearch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class WordSearchBankEntryTest {
    private fun cells(vararg pairs: Pair<Int, Int>) = pairs.map { (row, col) -> WSCellRef(row, col) }

    private val puzzle = WordSearchPuzzleEntry(
        id = "ws-1",
        difficulty = "hard",
        rows = 3,
        cols = 3,
        themeId = "animals",
        grid = listOf(listOf("C", "A", "T"), listOf("O", "R", "O"), listOf("W", "X", "A")),
        words = listOf(
            WSWordEntry("1", "CAT", cells(0 to 0, 0 to 1, 0 to 2)),
            WSWordEntry("2", "COW", cells(0 to 0, 1 to 0, 2 to 0)),
            WSWordEntry("3", "ARC", cells(2 to 2, 1 to 1, 0 to 0)),
        ),
        hiddenWord = WSHiddenWord("OX", "animals", cells(1 to 2, 2 to 1)),
        locale = "nl",
    )

    @Test
    fun `toBankEntry stores each word as its start cell and direction`() {
        val bankEntry = puzzle.toBankEntry()
        assertEquals(listOf("CAT 0 0 RIGHT", "COW 0 0 DOWN", "ARC 2 2 UP_LEFT"), bankEntry.words)
        assertEquals("OX", bankEntry.hiddenWord)
    }

    @Test
    fun `toPuzzleEntry rebuilds the grid, word positions and hidden word cells`() {
        assertEquals(puzzle, puzzle.toBankEntry().toPuzzleEntry("hard"))
    }

    @Test
    fun `toPuzzleEntry numbers words by their order in the bank entry`() {
        val renumbered = puzzle.copy(words = puzzle.words.mapIndexed { index, word -> word.copy(id = "w$index") })
        assertEquals(listOf("1", "2", "3"), renumbered.toBankEntry().toPuzzleEntry("hard").words.map { it.id })
    }

    @Test
    fun `toBankEntry rejects a hidden word whose cells are not the free cells in reading order`() {
        val scrambled = puzzle.copy(
            grid = listOf(listOf("C", "A", "T"), listOf("O", "R", "X"), listOf("W", "O", "A")),
            hiddenWord = WSHiddenWord("OX", "animals", cells(2 to 1, 1 to 2)),
        )
        assertThrows(IllegalArgumentException::class.java) { scrambled.toBankEntry() }
    }

    @Test
    fun `toPuzzleEntry rejects a hidden word that does not fill the free cells`() {
        val bankEntry = puzzle.toBankEntry().copy(hiddenWord = "OXEN")
        assertThrows(IllegalArgumentException::class.java) { bankEntry.toPuzzleEntry("hard") }
    }
}
