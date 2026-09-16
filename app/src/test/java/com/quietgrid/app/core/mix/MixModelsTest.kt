package com.quietgrid.app.core.mix

import com.quietgrid.app.core.GameId
import org.junit.Assert.assertEquals
import org.junit.Test

class MixModelsTest {

    @Test
    fun `nextMixName numbers from one when there are no existing mixes`() {
        assertEquals("My Mix #1", nextMixName(existingMixCount = 0))
    }

    @Test
    fun `nextMixName numbers past the current mix count`() {
        assertEquals("My Mix #4", nextMixName(existingMixCount = 3))
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
