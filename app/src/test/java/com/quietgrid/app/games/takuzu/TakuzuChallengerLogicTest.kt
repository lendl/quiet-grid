package com.quietgrid.app.games.takuzu

import com.quietgrid.app.core.ChallengerPuzzleSolve
import com.quietgrid.app.core.Difficulty
import com.quietgrid.engine.takuzu.TakuzuPuzzleEntry
import com.quietgrid.engine.takuzu.gridToHex
import com.quietgrid.engine.takuzu.maskToHex
import org.junit.Assert.assertEquals
import org.junit.Test

private val SOLUTION_GRID = listOf(
    listOf(0, 1, 0, 1),
    listOf(1, 0, 1, 0),
    listOf(0, 1, 1, 0),
    listOf(1, 0, 0, 1),
)
private val SINGLE_BLANK_MASK = listOf(
    listOf(false, true, true, true),
    listOf(true, true, true, true),
    listOf(true, true, true, true),
    listOf(true, true, true, true),
)

private fun testPuzzle(id: String, difficulty: String) = TakuzuPuzzleEntry(
    id = id,
    size = 4,
    difficulty = difficulty,
    solution = gridToHex(SOLUTION_GRID),
    mask = maskToHex(SINGLE_BLANK_MASK),
)

class TakuzuChallengerLogicTest {

    @Test
    fun `createInitialTakuzuChallengerSession starts on Easy with full lives and starting clock`() {
        val puzzle = testPuzzle("p1", "easy")

        val session = createInitialTakuzuChallengerSession(puzzle)

        assertEquals(Difficulty.EASY, session.tier)
        assertEquals(0, session.solvesInTier)
        assertEquals(0, session.puzzlesSolved)
        assertEquals(0, session.score)
        assertEquals(TAKUZU_CHALLENGER_STARTING_SECONDS, session.secondsRemaining, 0.0)
        assertEquals(TAKUZU_CHALLENGER_STARTING_LIVES, session.livesRemaining)
        assertEquals(setOf("p1"), session.servedPuzzleIds)
    }

    @Test
    fun `takuzuChallengerTierAfterSolve stays on the same tier before the solve threshold`() {
        val (tier, solves) = takuzuChallengerTierAfterSolve(Difficulty.EASY, solvesInTier = 0)

        assertEquals(Difficulty.EASY, tier)
        assertEquals(1, solves)
    }

    @Test
    fun `takuzuChallengerTierAfterSolve advances tier once the threshold is reached`() {
        val (tier, solves) = takuzuChallengerTierAfterSolve(Difficulty.EASY, solvesInTier = TAKUZU_CHALLENGER_SOLVES_PER_TIER - 1)

        assertEquals(Difficulty.MEDIUM, tier)
        assertEquals(0, solves)
    }

    @Test
    fun `advanceTakuzuChallengerAfterSolve keeps lives, adds bonus time, and loads the next puzzle`() {
        val initial = createInitialTakuzuChallengerSession(testPuzzle("p1", "easy")).copy(livesRemaining = 2)
        val nextPuzzle = testPuzzle("p2", "easy")

        val advanced = advanceTakuzuChallengerAfterSolve(initial, nextTier = Difficulty.EASY, nextSolvesInTier = 1, nextPuzzle = nextPuzzle)

        assertEquals(1, advanced.puzzlesSolved)
        assertEquals(2, advanced.livesRemaining)
        assertEquals("p2", advanced.puzzleSession.puzzle.id)
        assertEquals(TAKUZU_CHALLENGER_STARTING_SECONDS + TAKUZU_CHALLENGER_BONUS_SECONDS, advanced.secondsRemaining, 0.0)
        assertEquals(0.0, advanced.secondsOnCurrentPuzzle, 0.0)
        assertEquals(setOf("p1", "p2"), advanced.servedPuzzleIds)
        assertEquals(takuzuScore(Difficulty.EASY, 0, 0), advanced.score)
    }

    @Test
    fun `tickTakuzuChallenger counts the clock down and the current puzzle up`() {
        val initial = createInitialTakuzuChallengerSession(testPuzzle("p1", "easy"))

        val ticked = tickTakuzuChallenger(initial)

        assertEquals(TAKUZU_CHALLENGER_STARTING_SECONDS - 1.0, ticked.secondsRemaining, 0.0)
        assertEquals(1.0, ticked.secondsOnCurrentPuzzle, 0.0)
    }

    @Test
    fun `advanceTakuzuChallengerAfterSolve appends the solved puzzle to the history and tracks fastest solve`() {
        val firstSolve = createInitialTakuzuChallengerSession(testPuzzle("p1", "easy")).copy(secondsOnCurrentPuzzle = 12.0)

        val afterFirst = advanceTakuzuChallengerAfterSolve(firstSolve, nextTier = Difficulty.EASY, nextSolvesInTier = 1, nextPuzzle = testPuzzle("p2", "easy"))

        assertEquals(listOf(ChallengerPuzzleSolve(Difficulty.EASY, 12.0)), afterFirst.puzzleHistory)
        assertEquals(12.0, afterFirst.fastestSolveSeconds!!, 0.0)

        val secondSolve = afterFirst.copy(secondsOnCurrentPuzzle = 5.0)
        val afterSecond = advanceTakuzuChallengerAfterSolve(secondSolve, nextTier = Difficulty.EASY, nextSolvesInTier = 2, nextPuzzle = testPuzzle("p3", "easy"))

        assertEquals(5.0, afterSecond.fastestSolveSeconds!!, 0.0)
    }
}
