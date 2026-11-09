package com.quietgrid.app.games.wordswap

import com.quietgrid.engine.wordswap.WordSwapDirection
import com.quietgrid.engine.wordswap.WordSwapHint
import com.quietgrid.engine.wordswap.WordSwapMove
import com.quietgrid.engine.wordswap.WordSwapPuzzleEntry
import com.quietgrid.engine.wordswap.WordSwapTechnique
import com.quietgrid.engine.wordswap.findWordSwapSwapHint
import com.quietgrid.engine.wordswap.wordSwapLineSlots
import com.quietgrid.engine.wordswap.wordSwapMinSwaps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSwapLogicTest {

    private val solution = "bat" + "a.o" + "dog"
    private val start = "tab" + "o.a" + "dgo"
    private val puzzle = WordSwapPuzzleEntry(
        id = "ws-test",
        difficulty = "easy",
        rows = 3,
        cols = 3,
        solution = solution,
        start = start,
        slots = wordSwapLineSlots(solution, 3, 3),
        minSwaps = wordSwapMinSwaps(start, solution),
    )

    @Test
    fun swappingCountsAndCanSolve() {
        var session = createWordSwapSession(puzzle)
        while (session.status == WordSwapStatus.PLAYING) {
            val move = findWordSwapSwapHint(session.board, session.puzzle.solution)!!
            session = applyWordSwapTiles(session, move)!!
        }
        assertEquals(solution, session.board)
        assertEquals(puzzle.minSwaps, session.swaps)
        assertEquals(100, wordSwapAccuracy(session))
    }

    @Test
    fun swapsIntoHolesAreIgnored() {
        val session = createWordSwapSession(puzzle)
        assertNull(applyWordSwapTiles(session, WordSwapMove(0, 4)))
    }

    @Test
    fun swappingEqualLettersIsNotAMove() {
        val session = createWordSwapSession(puzzle.copy(start = "bat" + "a.o" + "dgo"))
        assertNull(applyWordSwapTiles(session, WordSwapMove(1, 3)))
    }

    @Test
    fun hintCellsCoverTheHintedLine() {
        val hint = WordSwapHint.Placement(0, "bat", WordSwapTechnique.LENGTH_SINGLE)
        assertEquals(setOf(0, 1, 2), wordSwapHintCells(puzzle, hint))
    }

    @Test
    fun swapHintCellsAreTheTwoTiles() {
        assertEquals(setOf(0, 2), wordSwapHintCells(puzzle, WordSwapHint.Swap(WordSwapMove(0, 2))))
        assertEquals(emptySet<Int>(), wordSwapHintCells(puzzle, null))
    }

    @Test
    fun hintNeverAppliesASwap() {
        val session = createWordSwapSession(puzzle)
        assertNotNull(findWordSwapSessionHint(session))
        assertEquals(puzzle.start, session.board)
    }

    @Test
    fun restartReturnsTheStartingBoardButKeepsTheCount() {
        val moved = applyWordSwapTiles(createWordSwapSession(puzzle), WordSwapMove(0, 2))!!
        val restarted = restartWordSwap(moved)!!
        assertEquals(start, restarted.board)
        assertEquals(1, restarted.swaps)
        assertNull(restartWordSwap(restarted))
    }

    @Test
    fun undoKeepsTheSwapCount() {
        val first = createWordSwapSession(puzzle)
        val moved = applyWordSwapTiles(first, WordSwapMove(0, 2))!!
        val undone = undoWordSwap(moved, first)
        assertEquals(puzzle.start, undone.board)
        assertEquals(1, undone.swaps)
    }

    @Test
    fun undoAfterRestartKeepsTheSwapCount() {
        val moved = applyWordSwapTiles(createWordSwapSession(puzzle), WordSwapMove(0, 2))!!
        val restarted = restartWordSwap(moved)!!
        val undone = undoWordSwap(restarted, moved)
        assertEquals(moved.board, undone.board)
        assertEquals(1, undone.swaps)
    }

    @Test
    fun bankListsWordsByDirectionThenAlphabetically() {
        val bank = wordSwapBank(createWordSwapSession(puzzle))
        assertEquals(listOf("bat", "dog", "bad", "tog"), bank.map { it.word })
        assertEquals(listOf(WordSwapDirection.ACROSS, WordSwapDirection.ACROSS, WordSwapDirection.DOWN, WordSwapDirection.DOWN), bank.map { it.direction })
        assertTrue(bank.none { it.done })
    }

    @Test
    fun finishedWordsAreMarkedOnTheBoardAndInTheBank() {
        val session = createWordSwapSession(puzzle).copy(board = "bat" + "o.a" + "dgo")
        assertEquals(setOf(0, 1, 2), wordSwapDoneCells(session))
        assertEquals(listOf("bat"), wordSwapBank(session).filter { it.done }.map { it.word })
    }

    @Test
    fun scoreDropsWithHintsExtraSwapsAndTime() {
        val base = createWordSwapSession(puzzle).copy(swaps = puzzle.minSwaps)
        assertEquals(1000, wordSwapScore(base, 0))
        assertEquals(900, wordSwapScore(base.copy(hintsUsed = 1), 0))
        assertEquals(980, wordSwapScore(base.copy(swaps = puzzle.minSwaps + 2), 0))
        assertEquals(940, wordSwapScore(base, 60))
    }

    @Test
    fun restoreRejectsBoardsWithOtherLetters() {
        val session = createWordSwapSession(puzzle)
        assertTrue(isRestorableWordSwapSession(session))
        assertFalse(isRestorableWordSwapSession(session.copy(board = "zzz" + "a.o" + "dog")))
        assertFalse(isRestorableWordSwapSession(session.copy(board = "bat" + "ao." + "dog")))
    }
}
