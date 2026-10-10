package com.quietgrid.app.core.mix

import com.quietgrid.app.core.GameId
import org.junit.Assert.assertEquals
import org.junit.Test

class MixModelsTest {

    private val defaultName: (Int) -> String = { "My Mix #$it" }

    @Test
    fun `nextMixName numbers from one when there are no existing mixes`() {
        assertEquals("My Mix #1", nextMixName(emptyList(), defaultName))
    }

    @Test
    fun `nextMixName numbers past the current mix count`() {
        assertEquals("My Mix #4", nextMixName(listOf("A", "B", "C"), defaultName))
    }

    @Test
    fun `nextMixName skips a number already taken after an earlier mix was deleted`() {
        assertEquals("My Mix #4", nextMixName(listOf("My Mix #2", "My Mix #3"), defaultName))
    }

    @Test
    fun `nextMixName uses the localized name format`() {
        assertEquals("Mijn mix #1", nextMixName(emptyList()) { "Mijn mix #$it" })
    }

    @Test
    fun `groupEntriesByKnownGame groups entries by their game preserving first-seen order`() {
        val entries = listOf(
            MixEntry(gameId = "sudoku", mode = MixEntryMode.PUZZLE, difficulty = "easy", weight = 1),
            MixEntry(gameId = "takuzu", mode = MixEntryMode.PUZZLE, difficulty = "hard", weight = 2),
            MixEntry(gameId = "sudoku", mode = MixEntryMode.PUZZLE, difficulty = "hard", weight = 3),
        )

        val grouped = groupEntriesByKnownGame(entries)

        assertEquals(listOf(GameId.SUDOKU, GameId.TAKUZU), grouped.map { it.first })
        assertEquals(2, grouped.first { it.first == GameId.SUDOKU }.second.size)
    }

    @Test
    fun `groupEntriesByKnownGame skips entries whose gameId is not in the catalog instead of crashing`() {
        val entries = listOf(
            MixEntry(gameId = "sudoku", mode = MixEntryMode.PUZZLE, difficulty = "easy", weight = 1),
            MixEntry(gameId = "retired-game", mode = MixEntryMode.PUZZLE, difficulty = "easy", weight = 1),
        )

        val grouped = groupEntriesByKnownGame(entries)

        assertEquals(listOf(GameId.SUDOKU), grouped.map { it.first })
    }
}
