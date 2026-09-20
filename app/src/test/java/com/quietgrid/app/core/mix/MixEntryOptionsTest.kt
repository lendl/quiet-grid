package com.quietgrid.app.core.mix

import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MixEntryOptionsTest {

    @Test
    fun `allModeOptionsFor a challenger-capable game returns 4 difficulties plus Challenger`() {
        val options = allModeOptionsFor(GameId.WORDGUESS)

        assertEquals(5, options.size)
        assertTrue(MixEntryOption(MixEntryMode.CHALLENGER, null) in options)
        Difficulty.entries.forEach { difficulty ->
            assertTrue(MixEntryOption(MixEntryMode.PUZZLE, difficulty.key) in options)
        }
    }

    @Test
    fun `allModeOptionsFor a non-challenger game returns only the 4 difficulties`() {
        val options = allModeOptionsFor(GameId.WORDSEARCH)

        assertEquals(4, options.size)
        assertTrue(options.none { it.mode == MixEntryMode.CHALLENGER })
    }

    @Test
    fun `missingModeOptionsFor with no existing entries returns every option for that game`() {
        val missing = missingModeOptionsFor(GameId.SUDOKU, emptyList())

        assertEquals(allModeOptionsFor(GameId.SUDOKU), missing)
    }

    @Test
    fun `missingModeOptionsFor excludes a puzzle mode that already has an entry`() {
        val existing = listOf(MixEntry(gameId = "wordsearch", mode = MixEntryMode.PUZZLE, difficulty = "easy", weight = 1))

        val missing = missingModeOptionsFor(GameId.WORDSEARCH, existing)

        assertEquals(3, missing.size)
        assertTrue(MixEntryOption(MixEntryMode.PUZZLE, "easy") !in missing)
        assertTrue(MixEntryOption(MixEntryMode.PUZZLE, "hard") in missing)
    }

    @Test
    fun `missingModeOptionsFor excludes Challenger once it has an entry`() {
        val existing = listOf(MixEntry(gameId = "wordguess", mode = MixEntryMode.CHALLENGER, difficulty = null, weight = 1))

        val missing = missingModeOptionsFor(GameId.WORDGUESS, existing)

        assertEquals(4, missing.size)
        assertTrue(missing.none { it.mode == MixEntryMode.CHALLENGER })
    }

    @Test
    fun `missingModeOptionsFor returns empty once every mode for that game is used`() {
        val existing = allModeOptionsFor(GameId.WORDGUESS)
            .map { MixEntry(gameId = "wordguess", mode = it.mode, difficulty = it.difficulty, weight = 1) }

        val missing = missingModeOptionsFor(GameId.WORDGUESS, existing)

        assertTrue(missing.isEmpty())
    }

    @Test
    fun `mixesEligibleForQuickAdd returns every mix when none has the entry`() {
        val mixes = listOf(
            Mix(id = "mix-1", name = "Evenings", entries = emptyList()),
            Mix(id = "mix-2", name = "Weekends", entries = listOf(MixEntry(gameId = "sudoku", mode = MixEntryMode.PUZZLE, difficulty = "hard", weight = 1))),
        )

        val eligible = mixesEligibleForQuickAdd(mixes, GameId.WORDSEARCH, Difficulty.EASY)

        assertEquals(mixes, eligible)
    }

    @Test
    fun `mixesEligibleForQuickAdd excludes a mix that already has the entry`() {
        val hasIt = Mix(id = "mix-1", name = "Evenings", entries = listOf(MixEntry(gameId = "wordsearch", mode = MixEntryMode.PUZZLE, difficulty = "easy", weight = 1)))
        val missingIt = Mix(id = "mix-2", name = "Weekends", entries = emptyList())

        val eligible = mixesEligibleForQuickAdd(listOf(hasIt, missingIt), GameId.WORDSEARCH, Difficulty.EASY)

        assertEquals(listOf(missingIt), eligible)
    }

    @Test
    fun `mixesEligibleForQuickAdd returns empty once every mix has the entry`() {
        val mixes = listOf(
            Mix(id = "mix-1", name = "Evenings", entries = listOf(MixEntry(gameId = "wordsearch", mode = MixEntryMode.PUZZLE, difficulty = "easy", weight = 1))),
            Mix(id = "mix-2", name = "Weekends", entries = listOf(MixEntry(gameId = "wordsearch", mode = MixEntryMode.PUZZLE, difficulty = "easy", weight = 3))),
        )

        val eligible = mixesEligibleForQuickAdd(mixes, GameId.WORDSEARCH, Difficulty.EASY)

        assertTrue(eligible.isEmpty())
    }

    @Test
    fun `mixesEligibleForQuickAdd ignores a Challenger entry for the same game`() {
        val mix = Mix(id = "mix-1", name = "Evenings", entries = listOf(MixEntry(gameId = "wordguess", mode = MixEntryMode.CHALLENGER, difficulty = null, weight = 1)))

        val eligible = mixesEligibleForQuickAdd(listOf(mix), GameId.WORDGUESS, Difficulty.EASY)

        assertEquals(listOf(mix), eligible)
    }

    @Test
    fun `mixesEligibleForQuickAdd returns empty for an empty mixes list`() {
        assertTrue(mixesEligibleForQuickAdd(emptyList(), GameId.SUDOKU, Difficulty.HARD).isEmpty())
    }
}
