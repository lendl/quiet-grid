package com.quietgrid.app.games.arrowescape

import com.quietgrid.app.core.Difficulty
import com.quietgrid.engine.arrowescape.ArrowEscapePieceData
import com.quietgrid.engine.arrowescape.ArrowEscapePuzzleEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private fun twoPiecePuzzle() = ArrowEscapePuzzleEntry(
    id = "test",
    difficulty = "easy",
    rows = 2,
    cols = 1,
    pieces = listOf(
        ArrowEscapePieceData(cells = listOf(listOf(0, 0)), headDirection = "down"),
        ArrowEscapePieceData(cells = listOf(listOf(1, 0)), headDirection = "down"),
    ),
)

private fun bottleneckOverFillerPuzzle() = ArrowEscapePuzzleEntry(
    id = "test-bottleneck",
    difficulty = "medium",
    rows = 1,
    cols = 6,
    pieces = listOf(
        ArrowEscapePieceData(cells = listOf(listOf(0, 5)), headDirection = "right"),
        ArrowEscapePieceData(cells = listOf(listOf(0, 2)), headDirection = "left"),
        ArrowEscapePieceData(cells = listOf(listOf(0, 4)), headDirection = "left"),
    ),
)

private fun tapPuzzle() = ArrowEscapePuzzleEntry(
    id = "test-tap",
    difficulty = "easy",
    rows = 3,
    cols = 3,
    pieces = listOf(
        ArrowEscapePieceData(cells = listOf(listOf(0, 0)), headDirection = "right"),
        ArrowEscapePieceData(cells = listOf(listOf(0, 1)), headDirection = "up"),
        ArrowEscapePieceData(cells = listOf(listOf(2, 2)), headDirection = "down"),
    ),
)

private fun chainPuzzle() = ArrowEscapePuzzleEntry(
    id = "test-chain",
    difficulty = "easy",
    rows = 3,
    cols = 2,
    pieces = listOf(
        ArrowEscapePieceData(cells = listOf(listOf(0, 0)), headDirection = "down"),
        ArrowEscapePieceData(cells = listOf(listOf(1, 0)), headDirection = "down"),
        ArrowEscapePieceData(cells = listOf(listOf(2, 0)), headDirection = "down"),
        ArrowEscapePieceData(cells = listOf(listOf(0, 1)), headDirection = "up"),
    ),
)

private fun deadlockRowPuzzle() = ArrowEscapePuzzleEntry(
    id = "test-deadlock",
    difficulty = "easy",
    rows = 1,
    cols = 3,
    pieces = listOf(
        ArrowEscapePieceData(cells = listOf(listOf(0, 0)), headDirection = "right"),
        ArrowEscapePieceData(cells = listOf(listOf(0, 1)), headDirection = "right"),
        ArrowEscapePieceData(cells = listOf(listOf(0, 2)), headDirection = "left"),
    ),
)

class ArrowEscapeLogicTest {
    @Test
    fun `a tap never selects a wrong arrow`() {
        assertNull(resolveArrowEscapeTap(tapPuzzle(), emptySet(), tapRow = 0.5f, tapCol = 0.5f, toleranceCells = 0f, wrongIndices = setOf(0)))
    }

    @Test
    fun `a tap near a wrong arrow selects the next blocked arrow in range instead`() {
        assertEquals(0, resolveArrowEscapeTap(deadlockRowPuzzle(), emptySet(), tapRow = 0.5f, tapCol = 0.5f, toleranceCells = 0.6f))
        assertEquals(1, resolveArrowEscapeTap(deadlockRowPuzzle(), emptySet(), tapRow = 0.5f, tapCol = 0.5f, toleranceCells = 0.6f, wrongIndices = setOf(0)))
    }

    @Test
    fun `a blocked attempt marks the arrow as wrong`() {
        val result = applyArrowEscapeAttempt(createArrowEscapeSession(twoPiecePuzzle()), pieceIndex = 0)!!
        assertEquals(setOf(0), result.session.wrongIndices)
    }

    @Test
    fun `tapping a wrong arrow again does nothing and costs no life`() {
        val marked = applyArrowEscapeAttempt(createArrowEscapeSession(twoPiecePuzzle()), pieceIndex = 0)!!.session
        assertNull(applyArrowEscapeAttempt(marked, pieceIndex = 0))
    }

    @Test
    fun `a wrong arrow leaves by itself once its way is clear, which can win the puzzle`() {
        val marked = applyArrowEscapeAttempt(createArrowEscapeSession(twoPiecePuzzle()), pieceIndex = 0)!!.session
        val result = applyArrowEscapeAttempt(marked, pieceIndex = 1)!!
        assertTrue(result.removed)
        assertEquals(setOf(0, 1), result.session.removedIndices)
        assertTrue(result.session.wrongIndices.isEmpty())
        assertEquals(ArrowEscapeStatus.WON, result.session.status)
    }

    @Test
    fun `wrong arrows leave in a chain when each exit clears the next one's way`() {
        var session = createArrowEscapeSession(chainPuzzle())
        session = applyArrowEscapeAttempt(session, pieceIndex = 0)!!.session
        session = applyArrowEscapeAttempt(session, pieceIndex = 1)!!.session
        assertEquals(setOf(0, 1), session.wrongIndices)

        val result = applyArrowEscapeAttempt(session, pieceIndex = 2)!!
        assertEquals(setOf(0, 1, 2), result.session.removedIndices)
        assertTrue(result.session.wrongIndices.isEmpty())
        assertEquals(ArrowEscapeStatus.PLAYING, result.session.status)
    }

    @Test
    fun `an arrow that was never wrong stays put when its way clears`() {
        var session = createArrowEscapeSession(chainPuzzle())
        session = applyArrowEscapeAttempt(session, pieceIndex = 0)!!.session

        val result = applyArrowEscapeAttempt(session, pieceIndex = 2)!!
        assertEquals(setOf(2), result.session.removedIndices)
        assertEquals(setOf(0), result.session.wrongIndices)
    }

    @Test
    fun `a tap inside a piece cell selects that piece`() {
        assertEquals(0, resolveArrowEscapeTap(tapPuzzle(), emptySet(), tapRow = 0.5f, tapCol = 0.5f, toleranceCells = 0f))
    }

    @Test
    fun `a tap on a blocked piece next to a removable piece in range selects the removable piece`() {
        assertEquals(1, resolveArrowEscapeTap(tapPuzzle(), emptySet(), tapRow = 0.5f, tapCol = 0.9f, toleranceCells = 0.3f))
    }

    @Test
    fun `a tap on a blocked piece with no removable piece in range keeps the blocked piece`() {
        assertEquals(0, resolveArrowEscapeTap(tapPuzzle(), emptySet(), tapRow = 0.5f, tapCol = 0.5f, toleranceCells = 0.3f))
    }

    @Test
    fun `a tap on an empty cell selects the nearest piece within tolerance`() {
        assertEquals(1, resolveArrowEscapeTap(tapPuzzle(), emptySet(), tapRow = 1.5f, tapCol = 1.5f, toleranceCells = 0.6f))
        assertNull(resolveArrowEscapeTap(tapPuzzle(), emptySet(), tapRow = 1.5f, tapCol = 1.5f, toleranceCells = 0.4f))
    }

    @Test
    fun `a tap ignores pieces that were already removed`() {
        assertNull(resolveArrowEscapeTap(tapPuzzle(), setOf(1), tapRow = 0.5f, tapCol = 1.5f, toleranceCells = 0f))
        assertEquals(0, resolveArrowEscapeTap(tapPuzzle(), setOf(1), tapRow = 0.5f, tapCol = 1.5f, toleranceCells = 0.6f))
    }

    @Test
    fun `a blocked attempt decrements one life, does not remove the piece, and records the selection`() {
        val session = createArrowEscapeSession(twoPiecePuzzle())
        val result = applyArrowEscapeAttempt(session, pieceIndex = 0)!!
        assertEquals(2, result.session.lives)
        assertTrue(result.session.removedIndices.isEmpty())
        assertEquals(0, result.session.selectedIndex)
        assertEquals(ArrowEscapeStatus.PLAYING, result.session.status)
        assertTrue(!result.removed)
    }

    @Test
    fun `a removable attempt clears the piece and can win`() {
        val session = createArrowEscapeSession(twoPiecePuzzle())
        val result = applyArrowEscapeAttempt(session, pieceIndex = 1)!!
        assertTrue(result.removed)
        assertEquals(setOf(1), result.session.removedIndices)
        assertEquals(ArrowEscapeStatus.PLAYING, result.session.status)

        val second = applyArrowEscapeAttempt(result.session, pieceIndex = 0)!!
        assertTrue(second.removed)
        assertEquals(ArrowEscapeStatus.WON, second.session.status)
    }

    @Test
    fun `zero lives after a blocked attempt ends the run as LOST`() {
        var session = createArrowEscapeSession(twoPiecePuzzle()).copy(lives = 1)
        val result = applyArrowEscapeAttempt(session, pieceIndex = 0)!!
        assertEquals(ArrowEscapeStatus.LOST, result.session.status)
        assertEquals(0, result.session.lives)
    }

    @Test
    fun `applyArrowEscapeHint selects the first currently-removable piece`() {
        val session = createArrowEscapeSession(twoPiecePuzzle())
        val hinted = applyArrowEscapeHint(session)!!
        assertEquals(1, hinted.selectedIndex)
    }

    @Test
    fun `applyArrowEscapeHint returns null when nothing is removable`() {
        val session = createArrowEscapeSession(twoPiecePuzzle()).copy(removedIndices = emptySet(), lives = 0, status = ArrowEscapeStatus.LOST)
        assertNull(applyArrowEscapeHint(session))
    }

    @Test
    fun `applyArrowEscapeHint prefers a piece that unblocks another over an inert filler`() {
        val session = createArrowEscapeSession(bottleneckOverFillerPuzzle())
        val hinted = applyArrowEscapeHint(session)!!
        assertEquals(1, hinted.selectedIndex)
    }

    @Test
    fun `a hint counts once per newly hinted arrow`() {
        val first = applyArrowEscapeHint(createArrowEscapeSession(twoPiecePuzzle()))!!
        assertEquals(1, first.hintsUsed)
        assertEquals(1, applyArrowEscapeHint(first)!!.hintsUsed)
        val afterRemoval = applyArrowEscapeAttempt(first, pieceIndex = 1)!!.session
        assertEquals(2, applyArrowEscapeHint(afterRemoval)!!.hintsUsed)
    }

    @Test
    fun `a clean solve at par scores 60000 on any board size`() {
        assertEquals(60_000, arrowEscapeScore(Difficulty.MEDIUM, arrowCount = 15, elapsedSeconds = 30, mistakes = 0, hintsUsed = 0))
        assertEquals(60_000, arrowEscapeScore(Difficulty.MEDIUM, arrowCount = 120, elapsedSeconds = 240, mistakes = 0, hintsUsed = 0))
    }

    @Test
    fun `every second counts so a fast clean solve never reaches the maximum`() {
        val faster = arrowEscapeScore(Difficulty.EXPERT, arrowCount = 117, elapsedSeconds = 135, mistakes = 0, hintsUsed = 0)
        val slower = arrowEscapeScore(Difficulty.EXPERT, arrowCount = 117, elapsedSeconds = 136, mistakes = 0, hintsUsed = 0)
        assertTrue(faster > slower)
        assertEquals(81_279, faster)
        assertTrue(arrowEscapeScore(Difficulty.EXPERT, arrowCount = 117, elapsedSeconds = 30, mistakes = 0, hintsUsed = 0) < 100_000)
    }

    @Test
    fun `the same pace per arrow scores the same on small and large boards`() {
        val small = arrowEscapeScore(Difficulty.MEDIUM, arrowCount = 15, elapsedSeconds = 60, mistakes = 0, hintsUsed = 0)
        val large = arrowEscapeScore(Difficulty.MEDIUM, arrowCount = 120, elapsedSeconds = 480, mistakes = 0, hintsUsed = 0)
        assertEquals(large, small)
        assertEquals(40_000, small)
    }

    @Test
    fun `a mistake costs the same on any board size`() {
        val small = arrowEscapeScore(Difficulty.MEDIUM, arrowCount = 15, elapsedSeconds = 30, mistakes = 1, hintsUsed = 0)
        val large = arrowEscapeScore(Difficulty.MEDIUM, arrowCount = 120, elapsedSeconds = 240, mistakes = 1, hintsUsed = 0)
        assertEquals(54_000, small)
        assertEquals(54_000, large)
    }

    @Test
    fun `a hint costs the same on any board size`() {
        val small = arrowEscapeScore(Difficulty.MEDIUM, arrowCount = 15, elapsedSeconds = 30, mistakes = 0, hintsUsed = 1)
        val large = arrowEscapeScore(Difficulty.MEDIUM, arrowCount = 120, elapsedSeconds = 240, mistakes = 0, hintsUsed = 1)
        assertEquals(58_000, small)
        assertEquals(58_000, large)
    }

    @Test
    fun `heavy mistakes and hints bottom out their share without going negative`() {
        val score = arrowEscapeScore(Difficulty.EXPERT, arrowCount = 2, elapsedSeconds = 6, mistakes = 2, hintsUsed = 5)
        assertEquals(40_000, score)
    }
}
