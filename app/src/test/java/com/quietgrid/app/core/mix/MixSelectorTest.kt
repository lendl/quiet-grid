package com.quietgrid.app.core.mix

import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MixSelectorTest {

    private val sudokuHard = MixCandidate(GameId.SUDOKU, MixMode.Puzzle(Difficulty.HARD), weight = 1)
    private val wordGuessChallenger = MixCandidate(GameId.WORDGUESS, MixMode.Challenger, weight = 1)

    @Test
    fun `drawWeighted returns null for an empty candidate list`() {
        assertNull(drawWeighted(emptyList(), Random(0)))
    }

    @Test
    fun `drawWeighted returns the only candidate when there is exactly one`() {
        assertEquals(sudokuHard, drawWeighted(listOf(sudokuHard), Random(0)))
    }

    @Test
    fun `drawWeighted never returns a zero-weight candidate when a positive-weight one exists`() {
        val zeroWeight = sudokuHard.copy(weight = 0)
        repeat(50) { seed ->
            assertEquals(wordGuessChallenger, drawWeighted(listOf(zeroWeight, wordGuessChallenger), Random(seed)))
        }
    }

    @Test
    fun `drawWeighted distribution roughly matches relative weights over many draws`() {
        val heavy = sudokuHard.copy(weight = 9)
        val light = wordGuessChallenger.copy(weight = 1)
        val draws = 2000
        var heavyCount = 0
        val random = Random(42)
        repeat(draws) {
            if (drawWeighted(listOf(heavy, light), random) == heavy) heavyCount++
        }
        val heavyFraction = heavyCount.toDouble() / draws
        assertTrue("expected heavyFraction near 0.9 but was $heavyFraction", heavyFraction in 0.85..0.95)
    }

    @Test
    fun `resolvedCandidates maps puzzle entries to MixMode Puzzle`() {
        val mix = Mix(
            id = "m1",
            name = "Mixed",
            entries = listOf(MixEntry(gameId = "sudoku", mode = MixEntryMode.PUZZLE, difficulty = "hard", weight = 3)),
        )
        assertEquals(listOf(MixCandidate(GameId.SUDOKU, MixMode.Puzzle(Difficulty.HARD), 3)), mix.resolvedCandidates())
    }

    @Test
    fun `resolvedCandidates maps challenger entries to MixMode Challenger with no difficulty`() {
        val mix = Mix(
            id = "m1",
            name = "Mixed",
            entries = listOf(MixEntry(gameId = "wordguess", mode = MixEntryMode.CHALLENGER, difficulty = null, weight = 2)),
        )
        assertEquals(listOf(MixCandidate(GameId.WORDGUESS, MixMode.Challenger, 2)), mix.resolvedCandidates())
    }

    @Test
    fun `resolvedCandidates drops entries with an unknown gameId`() {
        val mix = Mix(
            id = "m1",
            name = "Mixed",
            entries = listOf(MixEntry(gameId = "not_a_real_game", mode = MixEntryMode.PUZZLE, difficulty = "easy", weight = 1)),
        )
        assertEquals(emptyList<MixCandidate>(), mix.resolvedCandidates())
    }

    @Test
    fun `resolvedCandidates drops puzzle entries with a missing or unknown difficulty`() {
        val mix = Mix(
            id = "m1",
            name = "Mixed",
            entries = listOf(
                MixEntry(gameId = "sudoku", mode = MixEntryMode.PUZZLE, difficulty = null, weight = 1),
                MixEntry(gameId = "sudoku", mode = MixEntryMode.PUZZLE, difficulty = "impossible", weight = 1),
            ),
        )
        assertEquals(emptyList<MixCandidate>(), mix.resolvedCandidates())
    }

    @Test
    fun `resolvedCandidates drops entries with weight below 1`() {
        val mix = Mix(
            id = "m1",
            name = "Mixed",
            entries = listOf(MixEntry(gameId = "sudoku", mode = MixEntryMode.PUZZLE, difficulty = "easy", weight = 0)),
        )
        assertEquals(emptyList<MixCandidate>(), mix.resolvedCandidates())
    }
}
