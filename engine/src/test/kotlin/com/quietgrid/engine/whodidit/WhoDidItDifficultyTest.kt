package com.quietgrid.engine.whodidit

import com.quietgrid.engine.core.Difficulty
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull

class WhoDidItDifficultyTest {
    private val ref = WhoDidItRef(0, 0)
    private val ref2 = WhoDidItRef(1, 0)

    private val cd = WhoDidItTechnique.CLUE_DIRECT
    private val single = WhoDidItTechnique.SINGLE
    private val prop = WhoDidItTechnique.CLUE_PROPAGATION
    private val inc = WhoDidItTechnique.INCOMPATIBLE
    private val liar = WhoDidItTechnique.LIAR_HYPOTHESIS

    private fun spec(
        difficulty: Difficulty,
        clues: List<WhoDidItClue> = listOf(WhoDidItClue.NotSame(ref, ref2)),
        statements: List<WhoDidItStatement> = emptyList(),
    ): WhoDidItPuzzleSpec {
        val shape = WHODIDIT_SHAPES.getValue(difficulty)
        return WhoDidItPuzzleSpec(
            shape.size,
            shape.categoryCount,
            List(shape.categoryCount) { List(shape.size) { emptySet() } },
            clues,
            statements,
        )
    }

    private fun expertSpec(clues: List<WhoDidItClue> = listOf(WhoDidItClue.NotSame(ref, ref2))) =
        spec(Difficulty.EXPERT, clues, listOf(WhoDidItStatement(0, WhoDidItClue.Crime(ref))))

    private fun steps(vararg techniques: WhoDidItTechnique) = techniques.map { WhoDidItStep(it) }

    private fun result(vararg techniques: WhoDidItTechnique, solved: Boolean = true) =
        WhoDidItSolveResult(solved, steps(*techniques))

    private fun contradiction(depth: Int) = WhoDidItStep(WhoDidItTechnique.CONTRADICTION, depth = depth)

    private fun resultOf(list: List<WhoDidItStep>) = WhoDidItSolveResult(true, list)

    @Test
    fun allowedClueTypesPerTier() {
        assertEquals(
            setOf(WhoDidItClueType.SAME, WhoDidItClueType.NOT_SAME, WhoDidItClueType.TRAIT, WhoDidItClueType.CRIME),
            whoDidItAllowedClueTypes(Difficulty.EASY),
        )
        assertEquals(
            setOf(
                WhoDidItClueType.SAME, WhoDidItClueType.NOT_SAME, WhoDidItClueType.TRAIT, WhoDidItClueType.CRIME,
                WhoDidItClueType.EITHER, WhoDidItClueType.NOT_TRAIT, WhoDidItClueType.CRIME_TRAIT,
            ),
            whoDidItAllowedClueTypes(Difficulty.MEDIUM),
        )
        assertEquals(WhoDidItClueType.entries.toSet(), whoDidItAllowedClueTypes(Difficulty.HARD))
        assertEquals(WhoDidItClueType.entries.toSet(), whoDidItAllowedClueTypes(Difficulty.EXPERT))
    }

    @Test
    fun profileOfTrace() {
        val profile = profileWhoDidIt(resultOf(listOf(WhoDidItStep(cd), WhoDidItStep(inc), contradiction(2), contradiction(4))))
        assertEquals(WhoDidItTechnique.CONTRADICTION, profile.peak)
        assertEquals(cd, profile.opening)
        assertEquals(4, profile.maxContradictionDepth)
        assertEquals(4, profile.stepCount)
        assertEquals(2, profile.counts[WhoDidItTechnique.CONTRADICTION])
        assertEquals(1, profile.counts[inc])
    }

    @Test
    fun profileOfEmptyTrace() {
        val profile = profileWhoDidIt(WhoDidItSolveResult(true, emptyList()))
        assertNull(profile.peak)
        assertNull(profile.opening)
        assertEquals(0, profile.maxContradictionDepth)
        assertEquals(0, profile.stepCount)
    }

    @Test
    fun traceScoreExactValue() {
        val profile = WhoDidItProfile(
            peak = liar,
            opening = cd,
            counts = mapOf(
                WhoDidItTechnique.CONTRADICTION to 2,
                inc to 3,
                liar to 1,
                prop to 4,
                WhoDidItTechnique.STATEMENT_CHECK to 2,
            ),
            maxContradictionDepth = 3,
            stepCount = 12,
        )
        assertEquals(80, whoDidItTraceScore(profile))
    }

    @Test
    fun unsolvedIsRejected() {
        assertNull(classifyWhoDidIt(spec(Difficulty.EASY), result(cd, single, solved = false), 0))
    }

    @Test
    fun easyPasses() {
        assertEquals(Difficulty.EASY, classifyWhoDidIt(spec(Difficulty.EASY), result(cd, single, WhoDidItTechnique.TRANSITIVE), 0))
    }

    @Test
    fun easyRejectsPeakAboveTransitive() {
        assertNull(classifyWhoDidIt(spec(Difficulty.EASY), result(cd, prop), 0))
    }

    @Test
    fun easyRejectsOpeningSingle() {
        assertNull(classifyWhoDidIt(spec(Difficulty.EASY), result(single, cd), 0))
    }

    @Test
    fun easyRejectsDisallowedClueType() {
        val easy = spec(Difficulty.EASY, clues = listOf(WhoDidItClue.Either(ref, ref2, WhoDidItRef(1, 1))))
        assertNull(classifyWhoDidIt(easy, result(cd, single), 0))
    }

    @Test
    fun easyRejectsStatements() {
        val easy = spec(Difficulty.EASY, statements = listOf(WhoDidItStatement(0, WhoDidItClue.Crime(ref))))
        assertNull(classifyWhoDidIt(easy, result(cd, single), 0))
    }

    @Test
    fun unknownShapeIsRejected() {
        val odd = WhoDidItPuzzleSpec(6, 3, emptyList(), emptyList(), emptyList())
        assertNull(classifyWhoDidIt(odd, result(cd, single), 0))
    }

    @Test
    fun mediumPasses() {
        assertEquals(Difficulty.MEDIUM, classifyWhoDidIt(spec(Difficulty.MEDIUM), result(cd, prop, prop, inc), 2))
    }

    @Test
    fun mediumRejectsContradiction() {
        assertNull(classifyWhoDidIt(spec(Difficulty.MEDIUM), resultOf(steps(cd, prop, prop, prop) + contradiction(1)), 0))
    }

    @Test
    fun mediumRejectsTwoIncompatible() {
        assertNull(classifyWhoDidIt(spec(Difficulty.MEDIUM), result(cd, prop, inc, inc), 0))
    }

    @Test
    fun mediumRejectsLowPropagation() {
        assertNull(classifyWhoDidIt(spec(Difficulty.MEDIUM), result(cd, prop, inc), 0))
    }

    @Test
    fun mediumRejectsTooManyExtraClues() {
        assertNull(classifyWhoDidIt(spec(Difficulty.MEDIUM), result(cd, prop, prop, prop), 3))
    }

    @Test
    fun hardPassesWithContradiction() {
        val trace = resultOf(steps(cd, prop) + contradiction(3))
        assertEquals(Difficulty.HARD, classifyWhoDidIt(spec(Difficulty.HARD), trace, 0))
    }

    @Test
    fun hardRejectsContradictionDepthAboveCeiling() {
        val trace = resultOf(steps(cd, prop) + contradiction(4))
        assertNull(classifyWhoDidIt(spec(Difficulty.HARD), trace, 0))
    }

    @Test
    fun hardPassesWithIncompatibleFloor() {
        assertEquals(Difficulty.HARD, classifyWhoDidIt(spec(Difficulty.HARD), result(cd, prop, inc, inc), 0))
    }

    @Test
    fun hardRejectsBelowIncompatibleFloorWithoutContradiction() {
        assertNull(classifyWhoDidIt(spec(Difficulty.HARD), result(cd, prop, inc), 0))
    }

    @Test
    fun hardRejectsLiar() {
        val trace = resultOf(steps(cd, liar) + contradiction(1))
        assertNull(classifyWhoDidIt(spec(Difficulty.HARD), trace, 0))
    }

    @Test
    fun hardRejectsExtraClues() {
        val trace = resultOf(steps(cd, prop) + contradiction(1))
        assertNull(classifyWhoDidIt(spec(Difficulty.HARD), trace, 1))
    }

    @Test
    fun hardWithContradictionIgnoresIncompatibleCount() {
        val trace = resultOf(steps(cd, inc, inc) + contradiction(5))
        assertNull(classifyWhoDidIt(spec(Difficulty.HARD), trace, 0))
    }

    @Test
    fun expertPassesWithDepth() {
        val trace = resultOf(steps(cd, liar) + contradiction(3))
        assertEquals(Difficulty.EXPERT, classifyWhoDidIt(expertSpec(), trace, 0))
    }

    @Test
    fun expertPassesWithRepeats() {
        val trace = resultOf(steps(cd, liar) + contradiction(1) + contradiction(1))
        assertEquals(Difficulty.EXPERT, classifyWhoDidIt(expertSpec(), trace, 0))
    }

    @Test
    fun expertRejectsShallowSingleContradiction() {
        val trace = resultOf(steps(cd, liar) + contradiction(2))
        assertNull(classifyWhoDidIt(expertSpec(), trace, 0))
    }

    @Test
    fun expertRejectsNoLiar() {
        val trace = resultOf(steps(cd) + contradiction(3))
        assertNull(classifyWhoDidIt(expertSpec(), trace, 0))
    }

    @Test
    fun expertRejectsStatementCheckWithoutLiar() {
        val trace = resultOf(steps(cd, WhoDidItTechnique.STATEMENT_CHECK) + contradiction(3))
        assertNull(classifyWhoDidIt(expertSpec(), trace, 0))
    }

    @Test
    fun expertRejectsExtraClues() {
        val trace = resultOf(steps(cd, liar) + contradiction(3))
        assertNull(classifyWhoDidIt(expertSpec(), trace, 1))
    }

    @Test
    fun expertRejectsThreeSameClues() {
        val same = WhoDidItClue.Same(ref, ref2)
        val trace = resultOf(steps(cd, liar) + contradiction(3))
        assertEquals(Difficulty.EXPERT, classifyWhoDidIt(expertSpec(listOf(same, same)), trace, 0))
        assertNull(classifyWhoDidIt(expertSpec(listOf(same, same, same)), trace, 0))
    }

    @Test
    fun expertRequiresStatements() {
        val trace = resultOf(steps(cd, liar) + contradiction(3))
        assertNull(classifyWhoDidIt(spec(Difficulty.EXPERT), trace, 0))
    }
}
