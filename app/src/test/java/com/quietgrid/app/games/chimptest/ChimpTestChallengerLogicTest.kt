package com.quietgrid.app.games.chimptest

import com.quietgrid.app.core.ChallengerPuzzleSolve
import com.quietgrid.app.core.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChimpTestChallengerLogicTest {

    @Test
    fun `createInitialChimpTestChallengerSession starts with no fastest solve recorded`() {
        val session = createInitialChimpTestChallengerSession()

        assertNull(session.fastestSolveSeconds)
    }

    @Test
    fun `advanceChimpTestChallengerAfterSolve records the first solve time as fastest`() {
        val initial = createInitialChimpTestChallengerSession().copy(secondsOnCurrentPuzzle = 12.0)
        val (nextTier, nextSolvesInTier) = chimpTestChallengerTierAfterSolve(initial.tier, initial.solvesInTier)

        val advanced = advanceChimpTestChallengerAfterSolve(initial, nextTier, nextSolvesInTier)

        assertEquals(12.0, advanced.fastestSolveSeconds!!, 0.0)
    }

    @Test
    fun `advanceChimpTestChallengerAfterSolve keeps the faster of two recorded solves`() {
        val firstSolve = createInitialChimpTestChallengerSession().copy(secondsOnCurrentPuzzle = 12.0)
        val (tierAfterFirst, solvesAfterFirst) = chimpTestChallengerTierAfterSolve(firstSolve.tier, firstSolve.solvesInTier)
        val afterFirst = advanceChimpTestChallengerAfterSolve(firstSolve, tierAfterFirst, solvesAfterFirst)

        val secondSolve = afterFirst.copy(secondsOnCurrentPuzzle = 20.0)
        val (tierAfterSecond, solvesAfterSecond) = chimpTestChallengerTierAfterSolve(secondSolve.tier, secondSolve.solvesInTier)
        val afterSecond = advanceChimpTestChallengerAfterSolve(secondSolve, tierAfterSecond, solvesAfterSecond)

        assertEquals(12.0, afterSecond.fastestSolveSeconds!!, 0.0)

        val thirdSolve = afterSecond.copy(secondsOnCurrentPuzzle = 5.0)
        val (tierAfterThird, solvesAfterThird) = chimpTestChallengerTierAfterSolve(thirdSolve.tier, thirdSolve.solvesInTier)
        val afterThird = advanceChimpTestChallengerAfterSolve(thirdSolve, tierAfterThird, solvesAfterThird)

        assertEquals(5.0, afterThird.fastestSolveSeconds!!, 0.0)
    }

    @Test
    fun `advanceChimpTestChallengerAfterSolve appends the solved puzzle to the history in order`() {
        val firstSolve = createInitialChimpTestChallengerSession().copy(secondsOnCurrentPuzzle = 12.0)
        val (tierAfterFirst, solvesAfterFirst) = chimpTestChallengerTierAfterSolve(firstSolve.tier, firstSolve.solvesInTier)
        val afterFirst = advanceChimpTestChallengerAfterSolve(firstSolve, tierAfterFirst, solvesAfterFirst)

        assertEquals(listOf(ChallengerPuzzleSolve(Difficulty.EASY, 12.0)), afterFirst.puzzleHistory)

        val secondSolve = afterFirst.copy(secondsOnCurrentPuzzle = 20.0)
        val (tierAfterSecond, solvesAfterSecond) = chimpTestChallengerTierAfterSolve(secondSolve.tier, secondSolve.solvesInTier)
        val afterSecond = advanceChimpTestChallengerAfterSolve(secondSolve, tierAfterSecond, solvesAfterSecond)

        assertEquals(
            listOf(ChallengerPuzzleSolve(Difficulty.EASY, 12.0), ChallengerPuzzleSolve(Difficulty.EASY, 20.0)),
            afterSecond.puzzleHistory,
        )

        val thirdSolve = afterSecond.copy(secondsOnCurrentPuzzle = 5.0)
        val (tierAfterThird, solvesAfterThird) = chimpTestChallengerTierAfterSolve(thirdSolve.tier, thirdSolve.solvesInTier)
        val afterThird = advanceChimpTestChallengerAfterSolve(thirdSolve, tierAfterThird, solvesAfterThird)

        assertEquals(ChallengerPuzzleSolve(Difficulty.EASY, 5.0), afterThird.puzzleHistory.last())
    }
}
