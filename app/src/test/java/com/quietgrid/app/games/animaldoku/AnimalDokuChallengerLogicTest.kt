// app/src/test/java/com/quietgrid/app/games/animaldoku/AnimalDokuChallengerLogicTest.kt
package com.quietgrid.app.games.animaldoku

import com.quietgrid.app.core.Difficulty
import com.quietgrid.engine.animaldoku.AnimalDokuPuzzleEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private fun testPuzzle(id: String, difficulty: String) = AnimalDokuPuzzleEntry(
    id = id,
    size = 5,
    difficulty = difficulty,
    regions = List(5) { row -> List(5) { row } },
    solution = listOf(0, 1, 2, 3, 4),
)

class AnimalDokuChallengerLogicTest {

    @Test
    fun `createInitialChallengerSession starts on Easy with full lives and starting clock`() {
        val puzzle = testPuzzle("p1", "easy")

        val session = createInitialChallengerSession(puzzle)

        assertEquals(Difficulty.EASY, session.tier)
        assertEquals(0, session.solvesInTier)
        assertEquals(0, session.puzzlesSolved)
        assertEquals(0, session.score)
        assertEquals(ANIMALDOKU_CHALLENGER_STARTING_SECONDS, session.secondsRemaining, 0.0)
        assertEquals(ANIMALDOKU_STARTING_LIVES, session.puzzleSession.lives)
        assertEquals(setOf("p1"), session.servedPuzzleIds)
    }

    @Test
    fun `tierAfterSolve stays on the same tier before the solve threshold`() {
        val (tier, solves) = tierAfterSolve(Difficulty.EASY, solvesInTier = 0)

        assertEquals(Difficulty.EASY, tier)
        assertEquals(1, solves)
    }

    @Test
    fun `tierAfterSolve advances tier once the threshold is reached`() {
        val (tier, solves) = tierAfterSolve(Difficulty.EASY, solvesInTier = ANIMALDOKU_CHALLENGER_SOLVES_PER_TIER - 1)

        assertEquals(Difficulty.MEDIUM, tier)
        assertEquals(0, solves)
    }

    @Test
    fun `tierAfterSolve stays on Expert once reached`() {
        val (tier, solves) = tierAfterSolve(Difficulty.EXPERT, solvesInTier = ANIMALDOKU_CHALLENGER_SOLVES_PER_TIER - 1)

        assertEquals(Difficulty.EXPERT, tier)
        assertEquals(ANIMALDOKU_CHALLENGER_SOLVES_PER_TIER, solves)
    }

    @Test
    fun `advanceChallengerAfterSolve carries lives over, adds bonus time, and loads the next puzzle`() {
        val initial = createInitialChallengerSession(testPuzzle("p1", "easy"))
        val wounded = initial.copy(puzzleSession = initial.puzzleSession.copy(lives = 2))
        val nextPuzzle = testPuzzle("p2", "easy")

        val advanced = advanceChallengerAfterSolve(wounded, nextTier = Difficulty.EASY, nextSolvesInTier = 1, nextPuzzle = nextPuzzle)

        assertEquals(1, advanced.puzzlesSolved)
        assertEquals(2, advanced.puzzleSession.lives)
        assertEquals("p2", advanced.puzzleSession.puzzle.id)
        assertEquals(AnimalDokuStatus.PLAYING, advanced.puzzleSession.status)
        assertTrue(advanced.puzzleSession.cells.all { row -> row.all { it == AnimalDokuCellState.EMPTY } })
        assertEquals(ANIMALDOKU_CHALLENGER_STARTING_SECONDS + ANIMALDOKU_CHALLENGER_BONUS_SECONDS, advanced.secondsRemaining, 0.0)
        assertEquals(0.0, advanced.secondsOnCurrentPuzzle, 0.0)
        assertEquals(setOf("p1", "p2"), advanced.servedPuzzleIds)
        assertEquals(animalDokuScore(2, 0), advanced.score)
    }

    @Test
    fun `tickChallenger counts the clock down and the current puzzle up`() {
        val initial = createInitialChallengerSession(testPuzzle("p1", "easy"))

        val ticked = tickChallenger(initial)

        assertEquals(ANIMALDOKU_CHALLENGER_STARTING_SECONDS - 1.0, ticked.secondsRemaining, 0.0)
        assertEquals(1.0, ticked.secondsOnCurrentPuzzle, 0.0)
    }
}
