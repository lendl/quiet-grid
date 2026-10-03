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

    private val wordsearchEasy = MixEntry(gameId = "wordsearch", mode = MixEntryMode.PUZZLE, difficulty = "easy", weight = 1)
    private val sudokuHard = MixEntry(gameId = "sudoku", mode = MixEntryMode.PUZZLE, difficulty = "hard", weight = 3)

    @Test
    fun `mixIdsContainingPuzzle returns only mixes with that exact puzzle entry`() {
        val mixes = listOf(
            Mix(id = "has-it", name = "Evenings", entries = listOf(wordsearchEasy)),
            Mix(id = "other-difficulty", name = "Weekends", entries = listOf(wordsearchEasy.copy(difficulty = "hard"))),
            Mix(id = "challenger", name = "Fast", entries = listOf(MixEntry(gameId = "wordsearch", mode = MixEntryMode.CHALLENGER, difficulty = null, weight = 1))),
            Mix(id = "empty", name = "Empty", entries = emptyList()),
        )

        assertEquals(setOf("has-it"), mixIdsContainingPuzzle(mixes, GameId.WORDSEARCH, Difficulty.EASY))
    }

    @Test
    fun `applyPuzzleMembership adds a weight 1 entry to a newly selected mix`() {
        val mix = Mix(id = "mix-1", name = "Evenings", entries = listOf(sudokuHard))

        val updated = applyPuzzleMembership(listOf(mix), GameId.WORDSEARCH, Difficulty.EASY, setOf("mix-1"))

        assertEquals(listOf(sudokuHard, wordsearchEasy), updated.single().entries)
    }

    @Test
    fun `applyPuzzleMembership removes the entry from a deselected mix and keeps the rest`() {
        val mix = Mix(id = "mix-1", name = "Evenings", entries = listOf(sudokuHard, wordsearchEasy))

        val updated = applyPuzzleMembership(listOf(mix), GameId.WORDSEARCH, Difficulty.EASY, emptySet())

        assertEquals(listOf(sudokuHard), updated.single().entries)
    }

    @Test
    fun `applyPuzzleMembership keeps the existing weight when a mix stays selected`() {
        val heavy = wordsearchEasy.copy(weight = 7)
        val mix = Mix(id = "mix-1", name = "Evenings", entries = listOf(heavy))

        val updated = applyPuzzleMembership(listOf(mix), GameId.WORDSEARCH, Difficulty.EASY, setOf("mix-1"))

        assertEquals(listOf(heavy), updated.single().entries)
    }

    @Test
    fun `applyPuzzleMembership leaves an unselected mix without the entry untouched`() {
        val mix = Mix(id = "mix-1", name = "Evenings", entries = listOf(sudokuHard))

        val updated = applyPuzzleMembership(listOf(mix), GameId.WORDSEARCH, Difficulty.EASY, emptySet())

        assertEquals(listOf(mix), updated)
    }

    @Test
    fun `applyPuzzleMembership keeps a Challenger entry for the same game when deselected`() {
        val challenger = MixEntry(gameId = "wordsearch", mode = MixEntryMode.CHALLENGER, difficulty = null, weight = 1)
        val mix = Mix(id = "mix-1", name = "Evenings", entries = listOf(challenger, wordsearchEasy))

        val updated = applyPuzzleMembership(listOf(mix), GameId.WORDSEARCH, Difficulty.EASY, emptySet())

        assertEquals(listOf(challenger), updated.single().entries)
    }
}
