package com.quietgrid.engine.whodidit

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

class WhoDidItSolverStateTest {
    private fun s(slot: Int) = WhoDidItRef(0, slot)
    private fun p(slot: Int) = WhoDidItRef(1, slot)
    private fun i(slot: Int) = WhoDidItRef(2, slot)

    private fun solvedState(): WhoDidItGridState {
        val solution = fixtureSolution()
        val state = WhoDidItGridState(3, 3)
        for (lo in 0 until 3) for (hi in lo + 1 until 3) for (suspect in 0 until 3) {
            state.set(WhoDidItRef(lo, solution.slotOf[lo][suspect]), WhoDidItRef(hi, solution.slotOf[hi][suspect]), WhoDidItMark.YES)
        }
        state.autoFill()
        state.culpritCandidates.retainAll(setOf(solution.culprit))
        return state
    }

    @Test
    fun setIsSymmetricAndReportsChange() {
        val state = WhoDidItGridState(3, 3)
        assertEquals(WhoDidItMark.UNKNOWN, state.mark(s(0), p(1)))
        assertTrue(state.set(s(0), p(1), WhoDidItMark.YES))
        assertEquals(WhoDidItMark.YES, state.mark(p(1), s(0)))
        assertFalse(state.set(p(1), s(0), WhoDidItMark.YES))
        assertTrue(state.set(p(1), s(0), WhoDidItMark.NO))
        assertEquals(WhoDidItMark.NO, state.mark(s(0), p(1)))
    }

    @Test
    fun autoFillMarksRowAndColumnNo() {
        val state = WhoDidItGridState(3, 3)
        state.set(s(0), p(1), WhoDidItMark.YES)
        state.autoFill()
        assertEquals(WhoDidItMark.NO, state.mark(s(0), p(0)))
        assertEquals(WhoDidItMark.NO, state.mark(s(0), p(2)))
        assertEquals(WhoDidItMark.NO, state.mark(s(1), p(1)))
        assertEquals(WhoDidItMark.NO, state.mark(s(2), p(1)))
        assertEquals(WhoDidItMark.UNKNOWN, state.mark(s(1), p(0)))
        assertEquals(WhoDidItMark.UNKNOWN, state.mark(s(0), i(0)))
    }

    @Test
    fun copyIsIndependent() {
        val state = WhoDidItGridState(3, 3)
        state.set(s(0), p(1), WhoDidItMark.YES)
        val clone = state.copy()
        clone.set(s(1), p(1), WhoDidItMark.NO)
        clone.culpritCandidates.remove(0)
        assertEquals(WhoDidItMark.UNKNOWN, state.mark(s(1), p(1)))
        assertEquals(setOf(0, 1, 2), state.culpritCandidates)
        assertEquals(WhoDidItMark.YES, clone.mark(s(0), p(1)))
    }

    @Test
    fun contradictionOnFullyEliminatedRow() {
        val state = WhoDidItGridState(3, 3)
        assertFalse(state.hasContradiction())
        for (slot in 0 until 3) state.set(s(0), p(slot), WhoDidItMark.NO)
        assertTrue(state.hasContradiction())
    }

    @Test
    fun contradictionOnTwoYesInOneRow() {
        val state = WhoDidItGridState(3, 3)
        state.set(s(0), p(0), WhoDidItMark.YES)
        state.set(s(0), p(1), WhoDidItMark.YES)
        assertTrue(state.hasContradiction())
    }

    @Test
    fun contradictionThroughThirdCategory() {
        val state = WhoDidItGridState(3, 3)
        state.set(s(0), p(0), WhoDidItMark.YES)
        state.set(p(0), i(0), WhoDidItMark.YES)
        assertFalse(state.hasContradiction())
        state.set(s(0), i(0), WhoDidItMark.NO)
        assertTrue(state.hasContradiction())
    }

    @Test
    fun contradictionOnYesNoMismatchThroughThirdCategory() {
        val state = WhoDidItGridState(3, 3)
        state.set(s(0), p(0), WhoDidItMark.YES)
        state.set(p(0), i(0), WhoDidItMark.NO)
        state.set(s(0), i(0), WhoDidItMark.YES)
        assertTrue(state.hasContradiction())
    }

    @Test
    fun contradictionOnEmptyCandidates() {
        val state = WhoDidItGridState(3, 3)
        state.culpritCandidates.clear()
        assertTrue(state.hasContradiction())
    }

    @Test
    fun solvedStateIsSolvedWithoutContradiction() {
        val state = solvedState()
        assertTrue(state.isSolved())
        assertFalse(state.hasContradiction())
    }

    @Test
    fun isSolvedNeedsSingleCulpritAndAllRows() {
        assertFalse(WhoDidItGridState(3, 3).isSolved())
        val state = solvedState()
        state.culpritCandidates.add(0)
        assertFalse(state.isSolved())
    }

    @Test
    fun applyAppliesChangesRemovalsAndAutoFills() {
        val state = WhoDidItGridState(3, 3)
        state.apply(
            WhoDidItStep(
                WhoDidItTechnique.CLUE_DIRECT,
                changes = listOf(WhoDidItCellChange(s(1), p(2), WhoDidItMark.YES)),
                culpritRemovals = setOf(0, 1),
            ),
        )
        assertEquals(WhoDidItMark.YES, state.mark(s(1), p(2)))
        assertEquals(WhoDidItMark.NO, state.mark(s(1), p(0)))
        assertEquals(WhoDidItMark.NO, state.mark(s(0), p(2)))
        assertEquals(setOf(2), state.culpritCandidates)
    }

    @Test
    fun effectiveCluesAddsRemovedCandidateStatementAsTrue() {
        val spec = liarFixture()
        val state = WhoDidItGridState(3, 3)
        assertEquals(spec.clues.size, effectiveClues(spec, state).size)
        state.culpritCandidates.remove(0)
        val effective = effectiveClues(spec, state)
        assertEquals(spec.clues.size + 1, effective.size)
        assertEquals(spec.statements[0].clue to WhoDidItSource.Statement(0), effective.last())
    }

    @Test
    fun effectiveCluesNegatesSoleCandidateStatement() {
        val spec = liarFixture()
        val state = WhoDidItGridState(3, 3)
        state.culpritCandidates.removeAll(setOf(0, 1))
        val effective = effectiveClues(spec, state).drop(spec.clues.size)
        assertEquals(
            listOf(
                spec.statements[0].clue to WhoDidItSource.Statement(0),
                spec.statements[1].clue to WhoDidItSource.Statement(1),
                WhoDidItClue.NotSame(s(2), i(0)) to WhoDidItSource.Statement(2),
            ),
            effective,
        )
    }
}
