package com.quietgrid.engine.themeclear

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ThemeClearBankEntryTest {
    private val puzzle = ThemeClearPuzzleEntry(
        id = "tc-easy-animals-tacgod",
        difficulty = "easy",
        themeId = "animals",
        rows = 2,
        cols = 3,
        grid = listOf("TAC", "GOD"),
        words = listOf("CAT", "DOG"),
        locale = "nl",
    )

    @Test
    fun `toPuzzleEntry derives the id and grid shape`() {
        val restored = ThemeClearBankEntry("easy", "animals", listOf("TAC", "GOD"), listOf("CAT", "DOG"), "nl").toPuzzleEntry()
        assertEquals(puzzle, restored)
    }

    @Test
    fun `toBankEntry round-trips through toPuzzleEntry`() {
        assertEquals(puzzle, puzzle.toBankEntry().toPuzzleEntry())
    }

    @Test
    fun `toBankEntry rejects an id that does not follow the grid`() {
        assertThrows(IllegalArgumentException::class.java) { puzzle.copy(id = "tc-custom").toBankEntry() }
    }

    @Test
    fun `toBankEntry rejects a shape that does not match the grid`() {
        assertThrows(IllegalArgumentException::class.java) { puzzle.copy(rows = 3, cols = 2).toBankEntry() }
    }
}
