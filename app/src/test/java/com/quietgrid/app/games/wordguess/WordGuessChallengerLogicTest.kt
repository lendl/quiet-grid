package com.quietgrid.app.games.wordguess

import com.quietgrid.app.core.ChallengerPuzzleSolve
import com.quietgrid.app.core.Difficulty
import com.quietgrid.engine.wordguess.WordGuessPuzzleEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private fun testPuzzle(id: String) = WordGuessPuzzleEntry(
    id = id,
    locale = "en",
    difficulty = Difficulty.EASY.key,
    word = "cat",
)

class WordGuessChallengerLogicTest {

    @Test
    fun `createInitialWordGuessChallengerSession starts with no fastest solve recorded`() {
        val session = createInitialWordGuessChallengerSession(testPuzzle("p1"))

        assertNull(session.fastestSolveSeconds)
    }

    @Test
    fun `advanceWordGuessChallengerAfterSolve records the first solve time as fastest`() {
        val initial = createInitialWordGuessChallengerSession(testPuzzle("p1")).copy(secondsOnCurrentPuzzle = 12.0)

        val advanced = advanceWordGuessChallengerAfterSolve(initial, nextTier = Difficulty.EASY, nextSolvesInTier = 1, nextPuzzle = testPuzzle("p2"))

        assertEquals(12.0, advanced.fastestSolveSeconds!!, 0.0)
    }

    @Test
    fun `advanceWordGuessChallengerAfterSolve keeps the faster of two recorded solves`() {
        val firstSolve = createInitialWordGuessChallengerSession(testPuzzle("p1")).copy(secondsOnCurrentPuzzle = 12.0)
        val afterFirst = advanceWordGuessChallengerAfterSolve(firstSolve, nextTier = Difficulty.EASY, nextSolvesInTier = 1, nextPuzzle = testPuzzle("p2"))

        val secondSolve = afterFirst.copy(secondsOnCurrentPuzzle = 20.0)
        val afterSecond = advanceWordGuessChallengerAfterSolve(secondSolve, nextTier = Difficulty.EASY, nextSolvesInTier = 2, nextPuzzle = testPuzzle("p3"))

        assertEquals(12.0, afterSecond.fastestSolveSeconds!!, 0.0)

        val thirdSolve = afterSecond.copy(secondsOnCurrentPuzzle = 5.0)
        val afterThird = advanceWordGuessChallengerAfterSolve(thirdSolve, nextTier = Difficulty.MEDIUM, nextSolvesInTier = 0, nextPuzzle = testPuzzle("p4"))

        assertEquals(5.0, afterThird.fastestSolveSeconds!!, 0.0)
    }

    @Test
    fun `advanceWordGuessChallengerAfterSolve appends the solved puzzle to the history in order`() {
        val firstSolve = createInitialWordGuessChallengerSession(testPuzzle("p1")).copy(secondsOnCurrentPuzzle = 12.0)
        val afterFirst = advanceWordGuessChallengerAfterSolve(firstSolve, nextTier = Difficulty.EASY, nextSolvesInTier = 1, nextPuzzle = testPuzzle("p2"))

        assertEquals(listOf(ChallengerPuzzleSolve(Difficulty.EASY, 12.0)), afterFirst.puzzleHistory)

        val secondSolve = afterFirst.copy(secondsOnCurrentPuzzle = 20.0)
        val afterSecond = advanceWordGuessChallengerAfterSolve(secondSolve, nextTier = Difficulty.EASY, nextSolvesInTier = 2, nextPuzzle = testPuzzle("p3"))

        assertEquals(
            listOf(ChallengerPuzzleSolve(Difficulty.EASY, 12.0), ChallengerPuzzleSolve(Difficulty.EASY, 20.0)),
            afterSecond.puzzleHistory,
        )

        val thirdSolve = afterSecond.copy(secondsOnCurrentPuzzle = 5.0)
        val afterThird = advanceWordGuessChallengerAfterSolve(thirdSolve, nextTier = Difficulty.MEDIUM, nextSolvesInTier = 0, nextPuzzle = testPuzzle("p4"))

        assertEquals(ChallengerPuzzleSolve(Difficulty.EASY, 5.0), afterThird.puzzleHistory.last())
    }
}
