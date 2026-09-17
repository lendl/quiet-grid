package com.quietgrid.app.games.nonogram

import com.quietgrid.app.core.ChallengerPuzzleSolve
import com.quietgrid.app.core.Difficulty
import com.quietgrid.engine.nonogram.NonogramPuzzleEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private fun testPuzzle(id: String, difficulty: String) = NonogramPuzzleEntry(
    id = id,
    difficulty = difficulty,
    rows = 2,
    cols = 2,
    solution = listOf(listOf(true, false), listOf(false, true)),
)

class NonogramChallengerLogicTest {

    @Test
    fun `createInitialNonogramChallengerSession starts on Easy with full lives and starting clock`() {
        val puzzle = testPuzzle("p1", "easy")

        val session = createInitialNonogramChallengerSession(puzzle)

        assertEquals(Difficulty.EASY, session.tier)
        assertEquals(0, session.solvesInTier)
        assertEquals(0, session.puzzlesSolved)
        assertEquals(0, session.score)
        assertEquals(NONOGRAM_CHALLENGER_STARTING_SECONDS, session.secondsRemaining, 0.0)
        assertEquals(NONOGRAM_CHALLENGER_STARTING_LIVES, session.livesRemaining)
        assertEquals(setOf("p1"), session.servedPuzzleIds)
    }

    @Test
    fun `nonogramChallengerTierAfterSolve stays on the same tier before the solve threshold`() {
        val (tier, solves) = nonogramChallengerTierAfterSolve(Difficulty.EASY, solvesInTier = 0)

        assertEquals(Difficulty.EASY, tier)
        assertEquals(1, solves)
    }

    @Test
    fun `nonogramChallengerTierAfterSolve advances tier once the threshold is reached`() {
        val (tier, solves) = nonogramChallengerTierAfterSolve(Difficulty.EASY, solvesInTier = NONOGRAM_CHALLENGER_SOLVES_PER_TIER - 1)

        assertEquals(Difficulty.MEDIUM, tier)
        assertEquals(0, solves)
    }

    @Test
    fun `advanceNonogramChallengerAfterSolve keeps lives, adds bonus time, and loads the next puzzle`() {
        val initial = createInitialNonogramChallengerSession(testPuzzle("p1", "easy")).copy(livesRemaining = 2)
        val nextPuzzle = testPuzzle("p2", "easy")

        val advanced = advanceNonogramChallengerAfterSolve(initial, nextTier = Difficulty.EASY, nextSolvesInTier = 1, nextPuzzle = nextPuzzle)

        assertEquals(1, advanced.puzzlesSolved)
        assertEquals(2, advanced.livesRemaining)
        assertEquals("p2", advanced.puzzleSession.puzzle.id)
        assertEquals(NONOGRAM_CHALLENGER_STARTING_SECONDS + NONOGRAM_CHALLENGER_BONUS_SECONDS, advanced.secondsRemaining, 0.0)
        assertEquals(0.0, advanced.secondsOnCurrentPuzzle, 0.0)
        assertEquals(setOf("p1", "p2"), advanced.servedPuzzleIds)
        assertEquals(nonogramScore(0), advanced.score)
    }

    @Test
    fun `tickNonogramChallenger counts the clock down and the current puzzle up`() {
        val initial = createInitialNonogramChallengerSession(testPuzzle("p1", "easy"))

        val ticked = tickNonogramChallenger(initial)

        assertEquals(NONOGRAM_CHALLENGER_STARTING_SECONDS - 1.0, ticked.secondsRemaining, 0.0)
        assertEquals(1.0, ticked.secondsOnCurrentPuzzle, 0.0)
    }

    @Test
    fun `applyNonogramChallengerTap fills a correct cell and reports no wrongness`() {
        val session = createInitialNonogramChallengerSession(testPuzzle("p1", "easy"))

        val result = applyNonogramChallengerTap(session, row = 0, col = 0, mode = NonogramInputMode.FILL)

        assertFalse(result.wasWrong)
        assertEquals(1, result.session.puzzleSession.board[0][0])
    }

    @Test
    fun `applyNonogramChallengerTap reports wrongness and reverts a fill where the solution is blank`() {
        val session = createInitialNonogramChallengerSession(testPuzzle("p1", "easy"))

        val result = applyNonogramChallengerTap(session, row = 0, col = 1, mode = NonogramInputMode.FILL)

        assertTrue(result.wasWrong)
        assertNull(result.session.puzzleSession.board[0][1])
    }
}
