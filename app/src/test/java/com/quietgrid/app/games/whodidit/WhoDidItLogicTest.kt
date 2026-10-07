package com.quietgrid.app.games.whodidit

import com.quietgrid.engine.whodidit.WhoDidItMark
import com.quietgrid.engine.whodidit.WhoDidItRef
import com.quietgrid.engine.whodidit.paired
import com.quietgrid.engine.whodidit.whoDidItCategoryPairs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private val PUZZLE = WHODIDIT_TEST_PUZZLE

private fun fresh() = createWhoDidItSession(PUZZLE)

private fun WhoDidItSession.tap(b: Int, r: Int, c: Int): WhoDidItSession =
    applyWhoDidItTap(this, b, r, c, false)?.session ?: this

private fun WhoDidItSession.mark(b: Int, r: Int, c: Int) = blocks[b][r][c]

private fun WhoDidItSession.placeO(b: Int, r: Int, c: Int) = tap(b, r, c).tap(b, r, c)

private fun correctCells(): List<Triple<Int, Int, Int>> =
    whoDidItCategoryPairs(3).flatMapIndexed { b, (rowCategory, colCategory) ->
        (0 until 3).flatMap { r ->
            (0 until 3)
                .filter { c -> PUZZLE.paired(WhoDidItRef(rowCategory, r), WhoDidItRef(colCategory, c)) }
                .map { c -> Triple(b, r, c) }
        }
    }

private fun wrongColumn(b: Int, r: Int, correct: Int) = (0 until 3).first { it != correct }

class WhoDidItLogicTest {
    @Test
    fun `tap cycles empty, X, O, empty`() {
        var s = fresh()
        s = s.tap(0, 0, 0)
        assertEquals(WhoDidItCellMark.X, s.mark(0, 0, 0))
        s = s.tap(0, 0, 0)
        assertEquals(WhoDidItCellMark.O, s.mark(0, 0, 0))
        s = s.tap(0, 0, 0)
        assertEquals(WhoDidItCellMark.EMPTY, s.mark(0, 0, 0))
    }

    @Test
    fun `placing O auto-fills its block row and column and removing it clears them`() {
        val placed = fresh().placeO(0, 1, 1)
        assertEquals(WhoDidItCellMark.AUTO_X, placed.mark(0, 1, 0))
        assertEquals(WhoDidItCellMark.AUTO_X, placed.mark(0, 1, 2))
        assertEquals(WhoDidItCellMark.AUTO_X, placed.mark(0, 0, 1))
        assertEquals(WhoDidItCellMark.AUTO_X, placed.mark(0, 2, 1))
        assertEquals(WhoDidItCellMark.EMPTY, placed.mark(0, 0, 0))
        assertEquals(WhoDidItCellMark.EMPTY, placed.mark(1, 1, 0))
        val removed = placed.tap(0, 1, 1)
        assertTrue(removed.blocks.flatten().flatten().all { it == WhoDidItCellMark.EMPTY })
    }

    @Test
    fun `player X stays when an O is placed in its row and when that O is removed`() {
        var s = fresh().tap(0, 1, 0)
        s = s.placeO(0, 1, 1)
        assertEquals(WhoDidItCellMark.X, s.mark(0, 1, 0))
        s = s.tap(0, 1, 1)
        assertEquals(WhoDidItCellMark.X, s.mark(0, 1, 0))
        assertEquals(WhoDidItCellMark.EMPTY, s.mark(0, 1, 2))
    }

    @Test
    fun `tapping an AUTO_X cell is a no-op`() {
        val s = fresh().placeO(0, 1, 1)
        assertNull(applyWhoDidItTap(s, 0, 1, 0, false))
    }

    @Test
    fun `X in a row that already has an O cycles to EMPTY, never a second O`() {
        val rowCase = fresh().tap(0, 1, 0).placeO(0, 1, 1).tap(0, 1, 0)
        assertEquals(WhoDidItCellMark.AUTO_X, rowCase.mark(0, 1, 0))
        val colCase = fresh().tap(0, 2, 1).placeO(0, 0, 1).tap(0, 2, 1)
        assertEquals(WhoDidItCellMark.AUTO_X, colCase.mark(0, 2, 1))
        val spam = fresh().placeO(0, 1, 1)
        var s = spam
        repeat(4) { for (r in 0 until 3) for (c in 0 until 3) s = s.tap(0, r, c) }
        for (row in s.blocks[0]) assertTrue(row.count { it == WhoDidItCellMark.O } <= 1)
        for (c in 0 until 3) assertTrue((0 until 3).count { s.blocks[0][it][c] == WhoDidItCellMark.O } <= 1)
    }

    @Test
    fun `wrong O with checkPicks reverts and reports wrongPick`() {
        val (b, r, c) = correctCells().first()
        val wrong = wrongColumn(b, r, c)
        val s = fresh().tap(b, r, wrong)
        val result = applyWhoDidItTap(s, b, r, wrong, true)!!
        assertTrue(result.wrongPick)
        assertEquals(s, result.session)
        assertEquals(WhoDidItCellMark.X, result.session.mark(b, r, wrong))
    }

    @Test
    fun `wrong O without checkPicks stays and the full board is complete but not solved`() {
        val correct = correctCells()
        var s = fresh()
        for ((b, r, c) in correct) {
            val shifted = (c + 1) % 3
            s = s.placeO(b, r, shifted)
        }
        assertTrue(isWhoDidItBoardComplete(s))
        assertFalse(isWhoDidItSolved(s))
        assertTrue(hasWhoDidItWrongMarks(s))
        assertEquals(WhoDidItStatus.PLAYING, s.status)
    }

    @Test
    fun `placing every correct O sets status WON`() {
        var s = fresh()
        val cells = correctCells()
        for ((i, cell) in cells.withIndex()) {
            val first = applyWhoDidItTap(s, cell.first, cell.second, cell.third, true)!!
            val second = applyWhoDidItTap(first.session, cell.first, cell.second, cell.third, true)!!
            assertFalse(second.wrongPick)
            s = second.session
            if (i < cells.lastIndex) assertEquals(WhoDidItStatus.PLAYING, s.status)
        }
        assertEquals(WhoDidItStatus.WON, s.status)
        assertTrue(isWhoDidItSolved(s))
    }

    @Test
    fun `hasWhoDidItWrongMarks detects a wrong O and a player X on a paired cell`() {
        val (b, r, c) = correctCells().first()
        val wrong = wrongColumn(b, r, c)
        assertFalse(hasWhoDidItWrongMarks(fresh()))
        assertTrue(hasWhoDidItWrongMarks(fresh().tap(b, r, c)))
        assertTrue(hasWhoDidItWrongMarks(fresh().placeO(b, r, wrong)))
        assertFalse(hasWhoDidItWrongMarks(fresh().tap(b, r, wrong)))
        assertFalse(hasWhoDidItWrongMarks(fresh().placeO(b, r, c)))
    }

    @Test
    fun `toWhoDidItGridState ignores wrong marks`() {
        val (b, r, c) = correctCells().first()
        val (rowCategory, colCategory) = whoDidItCategoryPairs(3)[b]
        val wrong = wrongColumn(b, r, c)
        val rowRef = WhoDidItRef(rowCategory, r)
        val wrongState = toWhoDidItGridState(fresh().placeO(b, r, wrong).tap(b, r, c))
        assertEquals(WhoDidItMark.UNKNOWN, wrongState.mark(rowRef, WhoDidItRef(colCategory, wrong)))
        assertEquals(WhoDidItMark.UNKNOWN, wrongState.mark(rowRef, WhoDidItRef(colCategory, c)))
        val good = toWhoDidItGridState(fresh().placeO(b, r, c))
        assertEquals(WhoDidItMark.YES, good.mark(rowRef, WhoDidItRef(colCategory, c)))
        assertEquals(WhoDidItMark.NO, good.mark(rowRef, WhoDidItRef(colCategory, wrong)))
        val withX = toWhoDidItGridState(fresh().tap(b, r, wrong))
        assertEquals(WhoDidItMark.NO, withX.mark(rowRef, WhoDidItRef(colCategory, wrong)))
    }

    @Test
    fun `whoDidItCellFor inverts the category pair order for both ref orders`() {
        val index = whoDidItCategoryPairs(3).indexOf(2 to 1)
        assertEquals(Triple(index, 0, 1), whoDidItCellFor(3, WhoDidItRef(2, 0), WhoDidItRef(1, 1)))
        assertEquals(Triple(index, 0, 1), whoDidItCellFor(3, WhoDidItRef(1, 1), WhoDidItRef(2, 0)))
        assertEquals(Triple(0, 2, 1), whoDidItCellFor(3, WhoDidItRef(1, 1), WhoDidItRef(0, 2)))
    }

    @Test
    fun `not quite shows only for a complete wrong board on hard and expert`() {
        var wrong = fresh()
        for ((b, r, c) in correctCells()) wrong = wrong.placeO(b, r, (c + 1) % 3)
        var right = fresh()
        for ((b, r, c) in correctCells()) right = right.placeO(b, r, c)
        fun WhoDidItSession.at(difficulty: String) = copy(puzzle = puzzle.copy(difficulty = difficulty))

        assertTrue(whoDidItShowsNotQuite(wrong.at("hard")))
        assertTrue(whoDidItShowsNotQuite(wrong.at("expert")))
        assertFalse(whoDidItShowsNotQuite(wrong.at("easy")))
        assertFalse(whoDidItShowsNotQuite(wrong.at("medium")))
        assertFalse(whoDidItShowsNotQuite(right.at("hard")))
        assertFalse(whoDidItShowsNotQuite(fresh().placeO(0, 0, 0).at("hard")))
    }
}
