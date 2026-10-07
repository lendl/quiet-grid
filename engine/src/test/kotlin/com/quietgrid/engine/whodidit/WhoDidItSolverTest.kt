package com.quietgrid.engine.whodidit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WhoDidItSolverTest {
    private fun allPairs(spec: WhoDidItPuzzleSpec): List<Pair<WhoDidItRef, WhoDidItRef>> =
        whoDidItCategoryPairs(spec.categoryCount).flatMap { (rowCategory, colCategory) ->
            (0 until spec.size).flatMap { row ->
                (0 until spec.size).map { col -> WhoDidItRef(rowCategory, row) to WhoDidItRef(colCategory, col) }
            }
        }

    private fun snapshot(spec: WhoDidItPuzzleSpec, state: WhoDidItGridState) =
        allPairs(spec).map { (a, b) -> state.mark(a, b) } to state.culpritCandidates.toSet()

    private fun paired(solution: WhoDidItSolution, a: WhoDidItRef, b: WhoDidItRef) =
        solution.ownerOf(a) == solution.ownerOf(b)

    private fun assertAgrees(solution: WhoDidItSolution, steps: List<WhoDidItStep>) {
        for (step in steps) {
            for (change in step.changes) {
                val expected = if (paired(solution, change.a, change.b)) WhoDidItMark.YES else WhoDidItMark.NO
                assertEquals(step.toString(), expected, change.mark)
            }
            assertFalse(step.toString(), solution.culprit in step.culpritRemovals)
        }
    }

    private fun boardState(spec: WhoDidItPuzzleSpec, solution: WhoDidItSolution, suspects: Set<Int>): WhoDidItGridState {
        val state = WhoDidItGridState(spec.size, spec.categoryCount)
        for ((a, b) in allPairs(spec)) {
            if (paired(solution, a, b) && solution.ownerOf(a) in suspects) state.set(a, b, WhoDidItMark.YES)
        }
        state.autoFill()
        return state
    }

    private fun solvedState(spec: WhoDidItPuzzleSpec): WhoDidItGridState {
        val state = WhoDidItGridState(spec.size, spec.categoryCount)
        solveWhoDidIt(spec).steps.forEach { state.apply(it) }
        return state
    }

    @Test
    fun `easy fixture solves without hypothesis techniques`() {
        val result = solveWhoDidIt(easyFixture())
        assertTrue(result.solved)
        assertTrue(result.steps.all { it.technique.ordinal <= WhoDidItTechnique.TRANSITIVE.ordinal })
    }

    @Test
    fun `liar fixture is solved by statement checks and identifies the culprit`() {
        val result = solveWhoDidIt(liarFixture())
        assertTrue(result.solved)
        assertTrue(result.steps.any { it.technique == WhoDidItTechnique.STATEMENT_CHECK })
        assertTrue(result.steps.none { it.technique == WhoDidItTechnique.LIAR_HYPOTHESIS })
        val remaining = (0 until 3).toSet() - result.steps.flatMap { it.culpritRemovals }.toSet()
        assertEquals(setOf(fixtureSolution().culprit), remaining)
    }

    @Test
    fun `contradiction fixture records depth within the cap`() {
        val spec = contradictionFixture()
        assertEquals(1, countWhoDidItSolutions(spec))
        assertEquals(contradictionSolution(), findWhoDidItSolution(spec))
        val result = solveWhoDidIt(spec)
        assertTrue(result.solved)
        val contradictions = result.steps.filter { it.technique == WhoDidItTechnique.CONTRADICTION }
        assertTrue(contradictions.isNotEmpty())
        assertTrue(contradictions.all { it.depth in 0..WHODIDIT_HYPOTHESIS_DEPTH_CAP })
        assertTrue(contradictions.any { it.depth >= 1 })
        assertTrue(result.steps.none { it.technique == WhoDidItTechnique.LIAR_HYPOTHESIS })
        assertAgrees(contradictionSolution(), result.steps)
    }

    @Test
    fun `ambiguous fixture is not solved`() {
        assertFalse(solveWhoDidIt(ambiguousFixture()).solved)
    }

    @Test
    fun `liar hypothesis needs statements and leaves depth-0 checks to STATEMENT_CHECK`() {
        assertNull(findLiarHypothesis(easyFixture(), WhoDidItGridState(3, 3)))
        val full = boardState(liarFixture(), fixtureSolution(), setOf(0, 1, 2))
        assertEquals(
            WhoDidItStep(
                WhoDidItTechnique.STATEMENT_CHECK,
                sources = listOf(WhoDidItSource.Statement(0)),
                culpritRemovals = setOf(0),
            ),
            findStatementCheck(liarFixture(), full),
        )
        assertNull(findLiarHypothesis(liarFixture(), full))
    }

    @Test
    fun `liar hypothesis returns the shallowest real what-if and skips depth-0 suspects`() {
        val spec = liarFixture().copy(clues = emptyList())
        val state = WhoDidItGridState(3, 3)
        state.set(WhoDidItRef(0, 2), WhoDidItRef(1, 0), WhoDidItMark.YES)
        state.set(WhoDidItRef(1, 0), WhoDidItRef(2, 0), WhoDidItMark.NO)
        state.autoFill()
        assertEquals(
            WhoDidItStep(
                WhoDidItTechnique.STATEMENT_CHECK,
                sources = listOf(WhoDidItSource.Statement(1)),
                culpritRemovals = setOf(1),
            ),
            findStatementCheck(spec, state),
        )
        val step = findLiarHypothesis(spec, state)
        assertEquals(
            WhoDidItStep(
                WhoDidItTechnique.LIAR_HYPOTHESIS,
                sources = listOf(WhoDidItSource.Statement(0)),
                culpritRemovals = setOf(0),
                depth = 2,
            ),
            step,
        )
        assertAgrees(fixtureSolution(), listOf(step!!))
    }

    @Test
    fun `nextWhoDidItStep does not mutate the given state`() {
        val spec = liarFixture()
        val state = boardState(spec, fixtureSolution(), setOf(0, 1, 2))
        val before = snapshot(spec, state)
        val step = nextWhoDidItStep(spec, state)
        assertEquals(WhoDidItTechnique.STATEMENT_CHECK, step?.technique)
        assertEquals(before, snapshot(spec, state))
        nextWhoDidItCellStep(spec, state)
        assertEquals(before, snapshot(spec, state))

        val contradictionSpec = contradictionFixture()
        val stuck = WhoDidItGridState(3, 3)
        solveWhoDidIt(contradictionSpec).steps
            .takeWhile { it.technique != WhoDidItTechnique.CONTRADICTION }
            .forEach { stuck.apply(it) }
        val stuckBefore = snapshot(contradictionSpec, stuck)
        assertEquals(WhoDidItTechnique.CONTRADICTION, nextWhoDidItStep(contradictionSpec, stuck)?.technique)
        assertEquals(WhoDidItTechnique.CONTRADICTION, nextWhoDidItCellStep(contradictionSpec, stuck)?.technique)
        assertEquals(stuckBefore, snapshot(contradictionSpec, stuck))
    }

    @Test
    fun `nextWhoDidItCellStep skips culprit-only steps and returns a step with changes`() {
        val base = easyFixture()
        val spec = base.copy(clues = listOf(WhoDidItClue.Crime(WhoDidItRef(0, 2))) + base.clues.dropLast(1))
        val state = WhoDidItGridState(3, 3)
        val first = nextWhoDidItStep(spec, state)
        assertNotNull(first)
        assertTrue(first!!.changes.isEmpty())
        assertEquals(setOf(0, 1), first.culpritRemovals)
        assertEquals(
            WhoDidItStep(
                WhoDidItTechnique.CLUE_DIRECT,
                sources = listOf(WhoDidItSource.Clue(2)),
                changes = listOf(WhoDidItCellChange(WhoDidItRef(0, 1), WhoDidItRef(1, 2), WhoDidItMark.YES)),
            ),
            nextWhoDidItCellStep(spec, state),
        )
    }

    @Test
    fun `nextWhoDidItCellStep returns null when only culprit reasoning remains`() {
        val spec = liarFixture()
        val state = boardState(spec, fixtureSolution(), setOf(0, 1, 2))
        assertNotNull(nextWhoDidItStep(spec, state))
        assertNull(nextWhoDidItCellStep(spec, state))
    }

    @Test
    fun `nextWhoDidItCellStep returns null on a solved state`() {
        for (spec in listOf(easyFixture(), liarFixture())) {
            val state = solvedState(spec)
            assertTrue(state.isSolved())
            assertNull(nextWhoDidItStep(spec, state))
            assertNull(nextWhoDidItCellStep(spec, state))
        }
        val fullBoard = boardState(easyFixture(), fixtureSolution(), setOf(0, 1, 2))
        assertNull(nextWhoDidItCellStep(easyFixture(), fullBoard))
    }

    @Test
    fun `nextWhoDidItCellStep on a state built from a partial correct board returns a step whose changes agree with the solution`() {
        val spec = expertSizedFixture()
        val solution = expertSizedSolution()
        for (suspects in listOf(emptySet(), setOf(0), setOf(1, 3), setOf(0, 2, 4))) {
            val step = nextWhoDidItCellStep(spec, boardState(spec, solution, suspects))
            assertNotNull(suspects.toString(), step)
            assertTrue(step!!.changes.isNotEmpty())
            assertAgrees(solution, listOf(step))
        }
    }

    @Test
    fun `next step on a contradictory state is null`() {
        val state = WhoDidItGridState(3, 3)
        state.set(WhoDidItRef(0, 0), WhoDidItRef(1, 0), WhoDidItMark.YES)
        state.set(WhoDidItRef(0, 0), WhoDidItRef(1, 1), WhoDidItMark.YES)
        assertNull(nextWhoDidItStep(easyFixture(), state))
        assertNull(nextWhoDidItCellStep(easyFixture(), state))
    }

    @Test
    fun `small fixtures only take steps that agree with the solution`() {
        assertAgrees(fixtureSolution(), solveWhoDidIt(easyFixture()).steps)
        assertAgrees(fixtureSolution(), solveWhoDidIt(liarFixture()).steps)
    }

    @Test(timeout = 60_000)
    fun `every solver step on the expert-sized fixture agrees with the real solution`() {
        val spec = expertSizedFixture()
        val result = solveWhoDidIt(spec)
        assertTrue(result.solved)
        assertAgrees(expertSizedSolution(), result.steps)
        val liars = result.steps.filter { it.technique == WhoDidItTechnique.LIAR_HYPOTHESIS }
        assertTrue(liars.isNotEmpty())
        assertTrue(liars.toString(), liars.all { it.depth >= 1 })
    }

    @Test(timeout = 60_000)
    fun `nextWhoDidItHintPath returns the skipped culprit-only steps before the cell step`() {
        val spec = expertSizedFixture()
        val steps = solveWhoDidIt(spec).steps
        val liarTechniques = setOf(WhoDidItTechnique.STATEMENT_CHECK, WhoDidItTechnique.LIAR_HYPOTHESIS)
        val liarIndex = steps.indexOfFirst { it.technique in liarTechniques }
        assertTrue(liarIndex >= 0)
        val state = WhoDidItGridState(spec.size, spec.categoryCount)
        steps.take(liarIndex).forEach { state.apply(it) }
        val before = snapshot(spec, state)
        val path = nextWhoDidItHintPath(spec, state)
        assertEquals(steps[liarIndex], path.prelude.first())
        assertTrue(path.prelude.toString(), path.prelude.all { it.changes.isEmpty() })
        val step = path.step
        assertNotNull(step)
        assertTrue(step!!.changes.isNotEmpty())
        assertEquals(nextWhoDidItCellStep(spec, state), step)
        assertAgrees(expertSizedSolution(), path.prelude + step)
        assertEquals(before, snapshot(spec, state))
    }

    @Test
    fun `nextWhoDidItHintPath keeps the prelude when no cell step remains and is empty on a contradiction`() {
        val spec = liarFixture()
        val path = nextWhoDidItHintPath(spec, boardState(spec, fixtureSolution(), setOf(0, 1, 2)))
        assertNull(path.step)
        assertEquals(WhoDidItTechnique.STATEMENT_CHECK, path.prelude.first().technique)
        assertAgrees(fixtureSolution(), path.prelude)

        val contradicted = WhoDidItGridState(3, 3)
        contradicted.set(WhoDidItRef(0, 0), WhoDidItRef(1, 0), WhoDidItMark.YES)
        contradicted.set(WhoDidItRef(0, 0), WhoDidItRef(1, 1), WhoDidItMark.YES)
        assertEquals(WhoDidItHintPath(emptyList(), null), nextWhoDidItHintPath(easyFixture(), contradicted))

        val fresh = nextWhoDidItHintPath(easyFixture(), WhoDidItGridState(3, 3))
        assertTrue(fresh.prelude.isEmpty())
        assertEquals(nextWhoDidItCellStep(easyFixture(), WhoDidItGridState(3, 3)), fresh.step)
    }
}
