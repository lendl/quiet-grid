package com.quietgrid.app.games.wordsearch

import com.quietgrid.app.core.Difficulty
import com.quietgrid.engine.wordsearch.WSCellRef
import com.quietgrid.engine.wordsearch.WSHiddenWord
import com.quietgrid.engine.wordsearch.WSWordEntry
import com.quietgrid.engine.wordsearch.WordSearchPuzzleEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** A 3x3 grid: row0 spells the single findable word "cat"; row1 is the hidden word "dog". */
private val puzzle = WordSearchPuzzleEntry(
    id = "p",
    difficulty = Difficulty.EASY.key,
    rows = 3,
    cols = 3,
    themeId = "animals",
    grid = listOf(listOf("c", "a", "t"), listOf("d", "o", "g"), listOf("x", "y", "z")),
    words = listOf(WSWordEntry(id = "cat", word = "cat", positions = listOf(WSCellRef(0, 0), WSCellRef(0, 1), WSCellRef(0, 2)))),
    hiddenWord = WSHiddenWord(word = "dog", clue = "a pet", positions = listOf(WSCellRef(1, 0), WSCellRef(1, 1), WSCellRef(1, 2))),
)

private fun freshSession() = WordSearchSession(
    puzzle = puzzle,
    foundWordIds = emptyList(),
    tempSelection = null,
    hiddenWordMode = false,
    hiddenWordProgress = emptyList(),
    hiddenWordSolved = false,
    accuracyDrops = 0,
)

class WordSearchLogicTest {

    @Test
    fun `wordSearchScore rewards skipping straight to the hidden word and penalizes time and accuracy drops`() {
        val allFound = freshSession().copy(foundWordIds = listOf("cat"))
        val noneFound = freshSession()

        // Regular words are optional decoys around the real goal (the hidden word), so leaving one
        // unfound ("skipped") actually adds a bonus rather than costing points.
        assertTrue(wordSearchScore(0, noneFound) > wordSearchScore(0, allFound))
        assertTrue(wordSearchScore(0, allFound) > wordSearchScore(100, allFound))
        assertTrue(wordSearchScore(0, allFound) > wordSearchScore(0, allFound.copy(accuracyDrops = 3)))
    }

    @Test
    fun `wordSearchAccuracyPct drops by 10 per accuracy drop down to zero`() {
        assertEquals(100, wordSearchAccuracyPct(0))
        assertEquals(70, wordSearchAccuracyPct(3))
        assertEquals(0, wordSearchAccuracyPct(20))
    }

    @Test
    fun `beginSelection outside the grid is rejected`() {
        assertNull(wsBeginSelection(freshSession(), WSCellRef(9, 9)))
    }

    @Test
    fun `beginSelection starts a single-cell path`() {
        val result = wsBeginSelection(freshSession(), WSCellRef(0, 0))!!

        assertEquals(listOf(WSCellRef(0, 0)), result.tempSelection?.path)
    }

    @Test
    fun `updateSelection extends a straight line from the start`() {
        val started = wsBeginSelection(freshSession(), WSCellRef(0, 0))!!

        val extended = wsUpdateSelection(started, WSCellRef(0, 2))!!

        assertEquals(listOf(WSCellRef(0, 0), WSCellRef(0, 1), WSCellRef(0, 2)), extended.tempSelection?.path)
    }

    @Test
    fun `updateSelection rejects a cell that would not form a straight line`() {
        val started = wsBeginSelection(freshSession(), WSCellRef(0, 0))!!

        assertNull(wsUpdateSelection(started, WSCellRef(1, 2)))
    }

    @Test
    fun `updateSelection with no active selection is rejected`() {
        assertNull(wsUpdateSelection(freshSession(), WSCellRef(0, 0)))
    }

    @Test
    fun `clearSelection removes an active selection and is a no-op without one`() {
        val started = wsBeginSelection(freshSession(), WSCellRef(0, 0))!!

        assertNull(wsClearSelection(started)?.tempSelection)
        assertNull(wsClearSelection(freshSession()))
    }

    @Test
    fun `committing a selection matching a word forward marks it found`() {
        val selected = wsBeginSelection(freshSession(), WSCellRef(0, 0)).let { wsUpdateSelection(it!!, WSCellRef(0, 2)) }!!

        val result = wsCommitSelection(selected)!!

        assertEquals(listOf("cat"), result.foundWordIds)
        assertNull(result.tempSelection)
    }

    @Test
    fun `committing a selection matching a word backward also marks it found`() {
        val selected = wsBeginSelection(freshSession(), WSCellRef(0, 2)).let { wsUpdateSelection(it!!, WSCellRef(0, 0)) }!!

        val result = wsCommitSelection(selected)!!

        assertEquals(listOf("cat"), result.foundWordIds)
    }

    @Test
    fun `committing a selection matching no word just clears it`() {
        val selected = wsBeginSelection(freshSession(), WSCellRef(2, 0)).let { wsUpdateSelection(it!!, WSCellRef(2, 2)) }!!

        val result = wsCommitSelection(selected)!!

        assertEquals(emptyList<String>(), result.foundWordIds)
        assertNull(result.tempSelection)
    }

    @Test
    fun `finding the last word enters hidden-word mode`() {
        val selected = wsBeginSelection(freshSession(), WSCellRef(0, 0)).let { wsUpdateSelection(it!!, WSCellRef(0, 2)) }!!

        val result = wsCommitSelection(selected)!!

        assertTrue(result.hiddenWordMode)
    }

    @Test
    fun `toggleHiddenWordMode flips the flag and is rejected once solved`() {
        val session = freshSession()

        assertTrue(wsToggleHiddenWordMode(session)!!.hiddenWordMode)

        val solved = session.copy(hiddenWordSolved = true)
        assertNull(wsToggleHiddenWordMode(solved))
    }

    @Test
    fun `inputHiddenWordCell requires hidden-word mode to be active`() {
        assertNull(wsInputHiddenWordCell(freshSession(), WSCellRef(1, 0)))
    }

    @Test
    fun `inputHiddenWordCell advances progress on the correct next letter`() {
        val session = freshSession().copy(hiddenWordMode = true)

        val result = wsInputHiddenWordCell(session, WSCellRef(1, 0))!!

        assertEquals(listOf(WSCellRef(1, 0)), result.hiddenWordProgress)
        assertFalse(result.hiddenWordSolved)
    }

    @Test
    fun `inputHiddenWordCell resets progress and drops accuracy on a wrong letter`() {
        val session = freshSession().copy(hiddenWordMode = true)
        val started = wsInputHiddenWordCell(session, WSCellRef(1, 0))!!

        val result = wsInputHiddenWordCell(started, WSCellRef(1, 2))!!

        assertEquals(emptyList<WSCellRef>(), result.hiddenWordProgress)
        assertEquals(1, result.accuracyDrops)
    }

    @Test
    fun `inputHiddenWordCell a wrong first letter with no progress yet is rejected outright`() {
        val session = freshSession().copy(hiddenWordMode = true)

        assertNull(wsInputHiddenWordCell(session, WSCellRef(1, 2)))
    }

    @Test
    fun `inputHiddenWordCell solves the puzzle and exits hidden-word mode on the final letter`() {
        val session = freshSession().copy(hiddenWordMode = true)
        val afterFirstTwo = puzzle.hiddenWord.positions.dropLast(1).fold(session) { acc, cell -> wsInputHiddenWordCell(acc, cell)!! }

        val result = wsInputHiddenWordCell(afterFirstTwo, puzzle.hiddenWord.positions.last())!!

        assertTrue(result.hiddenWordSolved)
        assertFalse(result.hiddenWordMode)
    }

    @Test
    fun `nextMoveHint targets the first unfound word when nothing overlaps yet`() {
        val hint = wsNextMoveHint(freshSession())

        assertTrue(hint is WSNextMoveHint.FindWord)
        assertEquals("cat", (hint as WSNextMoveHint.FindWord).word)
    }

    @Test
    fun `nextMoveHint falls back to the hidden-letter clue once every word is found`() {
        val session = freshSession().copy(foundWordIds = listOf("cat"))

        val hint = wsNextMoveHint(session)

        assertTrue(hint is WSNextMoveHint.FindHiddenLetter)
        assertEquals("a pet", (hint as WSNextMoveHint.FindHiddenLetter).clue)
    }

    @Test
    fun `nextMoveHint in hidden-word mode always targets the hidden letter`() {
        val session = freshSession().copy(hiddenWordMode = true)

        val hint = wsNextMoveHint(session)

        assertTrue(hint is WSNextMoveHint.FindHiddenLetter)
    }

    @Test
    fun `isWordSearchSolved mirrors hiddenWordSolved`() {
        assertFalse(isWordSearchSolved(freshSession()))
        assertTrue(isWordSearchSolved(freshSession().copy(hiddenWordSolved = true)))
    }

    @Test
    fun `hasMeaningfulProgress is false for a fresh session and true once anything changes`() {
        assertFalse(wordSearchHasMeaningfulProgress(freshSession()))
        assertTrue(wordSearchHasMeaningfulProgress(freshSession().copy(foundWordIds = listOf("cat"))))
        assertTrue(wordSearchHasMeaningfulProgress(freshSession().copy(accuracyDrops = 1)))
    }
}
