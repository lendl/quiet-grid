package com.quietgrid.app.games.blockescape

import com.quietgrid.app.core.Difficulty
import com.quietgrid.engine.blockescape.BlockEscapeBlock
import com.quietgrid.engine.blockescape.BlockEscapePuzzleEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class BlockEscapeChallengerLogicTest {
    private fun puzzle(id: String) = BlockEscapePuzzleEntry(
        id = id,
        difficulty = "easy",
        blocks = listOf(BlockEscapeBlock(2, 0, 2, true), BlockEscapeBlock(0, 2, 3, false)),
        minMoves = 2,
    )

    @Test
    fun `three solves move the run up a tier and expert is the last tier`() {
        assertEquals(Difficulty.EASY to 1, blockEscapeChallengerTierAfterSolve(Difficulty.EASY, 0))
        assertEquals(Difficulty.MEDIUM to 0, blockEscapeChallengerTierAfterSolve(Difficulty.EASY, 2))
        assertEquals(Difficulty.EXPERT to 3, blockEscapeChallengerTierAfterSolve(Difficulty.EXPERT, 2))
    }

    @Test
    fun `a solve adds score, bonus time and the next puzzle`() {
        val start = createInitialBlockEscapeChallengerSession(puzzle("a"))
        val ticked = (1..10).fold(start) { session, _ -> tickBlockEscapeChallenger(session) }
        val solved = ticked.copy(puzzleSession = ticked.puzzleSession.copy(moves = 2))
        val next = advanceBlockEscapeChallengerAfterSolve(solved, Difficulty.EASY, 1, puzzle("b"))
        assertEquals(1, next.puzzlesSolved)
        assertEquals(1000 - 10, next.score)
        assertEquals(BLOCKESCAPE_CHALLENGER_STARTING_SECONDS - 10 + BLOCKESCAPE_CHALLENGER_BONUS_SECONDS, next.secondsRemaining, 0.0)
        assertEquals(0.0, next.secondsOnCurrentPuzzle, 0.0)
        assertEquals(10.0, next.fastestSolveSeconds!!, 0.0)
        assertEquals(setOf("a", "b"), next.servedPuzzleIds)
        assertEquals("b", next.puzzleSession.puzzle.id)
        assertEquals(1, next.puzzleHistory.size)
    }
}
