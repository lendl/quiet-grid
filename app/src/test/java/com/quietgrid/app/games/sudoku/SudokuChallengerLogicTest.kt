package com.quietgrid.app.games.sudoku

import com.quietgrid.app.core.ChallengerPuzzleSolve
import com.quietgrid.app.core.Difficulty
import com.quietgrid.engine.sudoku.SudokuPuzzleEntry
import org.junit.Assert.assertEquals
import org.junit.Test

private val SOLVED_GRID = listOf(
    listOf(5, 3, 4, 6, 7, 8, 9, 1, 2),
    listOf(6, 7, 2, 1, 9, 5, 3, 4, 8),
    listOf(1, 9, 8, 3, 4, 2, 5, 6, 7),
    listOf(8, 5, 9, 7, 6, 1, 4, 2, 3),
    listOf(4, 2, 6, 8, 5, 3, 7, 9, 1),
    listOf(7, 1, 3, 9, 2, 4, 8, 5, 6),
    listOf(9, 6, 1, 5, 3, 7, 2, 8, 4),
    listOf(2, 8, 7, 4, 1, 9, 6, 3, 5),
    listOf(3, 4, 5, 2, 8, 6, 1, 7, 9),
)

private fun testPuzzle(id: String, difficulty: String): SudokuPuzzleEntry {
    val givens = SOLVED_GRID.mapIndexed { r, row -> row.mapIndexed { c, v -> if (r == 0 && c == 0) null else v } }
    return SudokuPuzzleEntry(id = id, difficulty = difficulty, givens = givens, solution = SOLVED_GRID)
}

class SudokuChallengerLogicTest {

    @Test
    fun `createInitialSudokuChallengerSession starts on Easy with full lives and starting clock`() {
        val puzzle = testPuzzle("p1", "easy")

        val session = createInitialSudokuChallengerSession(puzzle)

        assertEquals(Difficulty.EASY, session.tier)
        assertEquals(0, session.solvesInTier)
        assertEquals(0, session.puzzlesSolved)
        assertEquals(0, session.score)
        assertEquals(SUDOKU_CHALLENGER_STARTING_SECONDS, session.secondsRemaining, 0.0)
        assertEquals(SUDOKU_CHALLENGER_STARTING_LIVES, session.livesRemaining)
        assertEquals(setOf("p1"), session.servedPuzzleIds)
    }

    @Test
    fun `sudokuChallengerTierAfterSolve stays on the same tier before the solve threshold`() {
        val (tier, solves) = sudokuChallengerTierAfterSolve(Difficulty.EASY, solvesInTier = 0)

        assertEquals(Difficulty.EASY, tier)
        assertEquals(1, solves)
    }

    @Test
    fun `sudokuChallengerTierAfterSolve advances tier once the threshold is reached`() {
        val (tier, solves) = sudokuChallengerTierAfterSolve(Difficulty.EASY, solvesInTier = SUDOKU_CHALLENGER_SOLVES_PER_TIER - 1)

        assertEquals(Difficulty.MEDIUM, tier)
        assertEquals(0, solves)
    }

    @Test
    fun `advanceSudokuChallengerAfterSolve keeps lives, adds bonus time, and loads the next puzzle`() {
        val initial = createInitialSudokuChallengerSession(testPuzzle("p1", "easy")).copy(livesRemaining = 2)
        val nextPuzzle = testPuzzle("p2", "easy")

        val advanced = advanceSudokuChallengerAfterSolve(initial, nextTier = Difficulty.EASY, nextSolvesInTier = 1, nextPuzzle = nextPuzzle)

        assertEquals(1, advanced.puzzlesSolved)
        assertEquals(2, advanced.livesRemaining)
        assertEquals("p2", advanced.puzzleSession.puzzle.id)
        assertEquals(SUDOKU_CHALLENGER_STARTING_SECONDS + SUDOKU_CHALLENGER_BONUS_SECONDS, advanced.secondsRemaining, 0.0)
        assertEquals(0.0, advanced.secondsOnCurrentPuzzle, 0.0)
        assertEquals(setOf("p1", "p2"), advanced.servedPuzzleIds)
        assertEquals(sudokuScore(Difficulty.EASY, 0, 0), advanced.score)
    }

    @Test
    fun `tickSudokuChallenger counts the clock down and the current puzzle up`() {
        val initial = createInitialSudokuChallengerSession(testPuzzle("p1", "easy"))

        val ticked = tickSudokuChallenger(initial)

        assertEquals(SUDOKU_CHALLENGER_STARTING_SECONDS - 1.0, ticked.secondsRemaining, 0.0)
        assertEquals(1.0, ticked.secondsOnCurrentPuzzle, 0.0)
    }

    @Test
    fun `advanceSudokuChallengerAfterSolve appends the solved puzzle to the history and tracks fastest solve`() {
        val firstSolve = createInitialSudokuChallengerSession(testPuzzle("p1", "easy")).copy(secondsOnCurrentPuzzle = 12.0)

        val afterFirst = advanceSudokuChallengerAfterSolve(firstSolve, nextTier = Difficulty.EASY, nextSolvesInTier = 1, nextPuzzle = testPuzzle("p2", "easy"))

        assertEquals(listOf(ChallengerPuzzleSolve(Difficulty.EASY, 12.0)), afterFirst.puzzleHistory)
        assertEquals(12.0, afterFirst.fastestSolveSeconds!!, 0.0)

        val secondSolve = afterFirst.copy(secondsOnCurrentPuzzle = 5.0)
        val afterSecond = advanceSudokuChallengerAfterSolve(secondSolve, nextTier = Difficulty.EASY, nextSolvesInTier = 2, nextPuzzle = testPuzzle("p3", "easy"))

        assertEquals(5.0, afterSecond.fastestSolveSeconds!!, 0.0)
    }
}
