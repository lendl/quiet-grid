package com.quietgrid.app.games.starbattle

import com.quietgrid.app.core.ChallengerPuzzleSolve
import com.quietgrid.app.core.Difficulty
import com.quietgrid.engine.starbattle.StarBattlePuzzleEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private fun testPuzzle(id: String, difficulty: String) = StarBattlePuzzleEntry(
    id = id,
    size = 5,
    difficulty = difficulty,
    k = 1,
    regions = List(5) { row -> List(5) { row } },
    solution = List(5) { row -> listOf(row) },
)

class StarBattleChallengerLogicTest {

    @Test
    fun `createInitialStarBattleChallengerSession starts on Easy with full lives and starting clock`() {
        val puzzle = testPuzzle("p1", "easy")

        val session = createInitialStarBattleChallengerSession(puzzle)

        assertEquals(Difficulty.EASY, session.tier)
        assertEquals(0, session.solvesInTier)
        assertEquals(0, session.puzzlesSolved)
        assertEquals(0, session.score)
        assertEquals(STARBATTLE_CHALLENGER_STARTING_SECONDS, session.secondsRemaining, 0.0)
        assertEquals(STARBATTLE_STARTING_LIVES, session.puzzleSession.lives)
        assertEquals(setOf("p1"), session.servedPuzzleIds)
    }

    @Test
    fun `starBattleChallengerTierAfterSolve stays on the same tier before the solve threshold`() {
        val (tier, solves) = starBattleChallengerTierAfterSolve(Difficulty.EASY, solvesInTier = 0)

        assertEquals(Difficulty.EASY, tier)
        assertEquals(1, solves)
    }

    @Test
    fun `starBattleChallengerTierAfterSolve advances tier once the threshold is reached`() {
        val (tier, solves) = starBattleChallengerTierAfterSolve(Difficulty.EASY, solvesInTier = STARBATTLE_CHALLENGER_SOLVES_PER_TIER - 1)

        assertEquals(Difficulty.MEDIUM, tier)
        assertEquals(0, solves)
    }

    @Test
    fun `starBattleChallengerTierAfterSolve stays on Expert once reached`() {
        val (tier, solves) = starBattleChallengerTierAfterSolve(Difficulty.EXPERT, solvesInTier = STARBATTLE_CHALLENGER_SOLVES_PER_TIER - 1)

        assertEquals(Difficulty.EXPERT, tier)
        assertEquals(STARBATTLE_CHALLENGER_SOLVES_PER_TIER, solves)
    }

    @Test
    fun `advanceStarBattleChallengerAfterSolve carries lives over, adds bonus time, and loads the next puzzle`() {
        val initial = createInitialStarBattleChallengerSession(testPuzzle("p1", "easy"))
        val wounded = initial.copy(puzzleSession = initial.puzzleSession.copy(lives = 2))
        val nextPuzzle = testPuzzle("p2", "easy")

        val advanced = advanceStarBattleChallengerAfterSolve(wounded, nextTier = Difficulty.EASY, nextSolvesInTier = 1, nextPuzzle = nextPuzzle)

        assertEquals(1, advanced.puzzlesSolved)
        assertEquals(2, advanced.puzzleSession.lives)
        assertEquals("p2", advanced.puzzleSession.puzzle.id)
        assertEquals(StarBattleStatus.PLAYING, advanced.puzzleSession.status)
        assertTrue(advanced.puzzleSession.cells.all { row -> row.all { it == StarBattleCellState.EMPTY } })
        assertEquals(STARBATTLE_CHALLENGER_STARTING_SECONDS + STARBATTLE_CHALLENGER_BONUS_SECONDS, advanced.secondsRemaining, 0.0)
        assertEquals(0.0, advanced.secondsOnCurrentPuzzle, 0.0)
        assertEquals(setOf("p1", "p2"), advanced.servedPuzzleIds)
        assertEquals(starBattleScore(2, 0), advanced.score)
    }

    @Test
    fun `tickStarBattleChallenger counts the clock down and the current puzzle up`() {
        val initial = createInitialStarBattleChallengerSession(testPuzzle("p1", "easy"))

        val ticked = tickStarBattleChallenger(initial)

        assertEquals(STARBATTLE_CHALLENGER_STARTING_SECONDS - 1.0, ticked.secondsRemaining, 0.0)
        assertEquals(1.0, ticked.secondsOnCurrentPuzzle, 0.0)
    }

    @Test
    fun `advanceStarBattleChallengerAfterSolve appends the solved puzzle to the history and tracks fastest solve`() {
        val firstSolve = createInitialStarBattleChallengerSession(testPuzzle("p1", "easy")).copy(secondsOnCurrentPuzzle = 12.0)

        val afterFirst = advanceStarBattleChallengerAfterSolve(firstSolve, nextTier = Difficulty.EASY, nextSolvesInTier = 1, nextPuzzle = testPuzzle("p2", "easy"))

        assertEquals(listOf(ChallengerPuzzleSolve(Difficulty.EASY, 12.0)), afterFirst.puzzleHistory)
        assertEquals(12.0, afterFirst.fastestSolveSeconds!!, 0.0)

        val secondSolve = afterFirst.copy(secondsOnCurrentPuzzle = 5.0)
        val afterSecond = advanceStarBattleChallengerAfterSolve(secondSolve, nextTier = Difficulty.EASY, nextSolvesInTier = 2, nextPuzzle = testPuzzle("p3", "easy"))

        assertEquals(5.0, afterSecond.fastestSolveSeconds!!, 0.0)
    }
}
