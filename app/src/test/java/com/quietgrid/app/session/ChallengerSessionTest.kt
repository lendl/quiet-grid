package com.quietgrid.app.session

import com.quietgrid.app.core.ChallengerPuzzleSolve
import com.quietgrid.app.core.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Test

class ChallengerSessionTest {
    private val config = ChallengerConfig(startingSeconds = 100.0, bonusSeconds = 20.0, solvesPerTier = 3, startingLives = 2)

    @Test
    fun `start uses the config clock and lives on Easy`() {
        val session = startChallengerSession(config, "puzzle", "p1")

        assertEquals(Difficulty.EASY, session.tier)
        assertEquals(100.0, session.secondsRemaining, 0.0)
        assertEquals(2, session.livesRemaining)
        assertEquals(setOf("p1"), session.servedPuzzleIds)
    }

    @Test
    fun `tier advances after the configured solves and stops at Expert`() {
        assertEquals(Difficulty.EASY to 2, challengerTierAfterSolve(config, Difficulty.EASY, 1))
        assertEquals(Difficulty.MEDIUM to 0, challengerTierAfterSolve(config, Difficulty.EASY, 2))
        assertEquals(Difficulty.EXPERT to 3, challengerTierAfterSolve(config, Difficulty.EXPERT, 2))
    }

    @Test
    fun `advance credits the solve, adds bonus time and swaps the puzzle`() {
        val solving = startChallengerSession(config, "first", "p1").tick().tick()

        val advanced = solving.advanceAfterSolve(config, gainedScore = 50, nextTier = Difficulty.EASY, nextSolvesInTier = 1, nextPuzzleSession = "second", nextPuzzleId = "p2")

        assertEquals("second", advanced.puzzleSession)
        assertEquals(1, advanced.puzzlesSolved)
        assertEquals(50, advanced.score)
        assertEquals(118.0, advanced.secondsRemaining, 0.0)
        assertEquals(0.0, advanced.secondsOnCurrentPuzzle, 0.0)
        assertEquals(2.0, advanced.fastestSolveSeconds!!, 0.0)
        assertEquals(listOf(ChallengerPuzzleSolve(Difficulty.EASY, 2.0)), advanced.puzzleHistory)
        assertEquals(setOf("p1", "p2"), advanced.servedPuzzleIds)
    }

    @Test
    fun `credit keeps the current puzzle and the fastest solve`() {
        val session = startChallengerSession(config, "only").copy(fastestSolveSeconds = 1.0, secondsOnCurrentPuzzle = 5.0)

        val credited = session.creditSolve(10)

        assertEquals("only", credited.puzzleSession)
        assertEquals(1.0, credited.fastestSolveSeconds!!, 0.0)
        assertEquals(10, credited.score)
    }
}
