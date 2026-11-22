package com.quietgrid.engine.whodidit

import com.quietgrid.engine.assertFinishesWithin
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class WhoDidItBruteForceTest {
    @Test
    fun easyFixtureIsUnique() {
        assertEquals(1, countWhoDidItSolutions(easyFixture()))
        assertEquals(fixtureSolution(), findWhoDidItSolution(easyFixture()))
    }

    @Test
    fun ambiguousFixtureHasTwoOrMore() {
        assertEquals(2, countWhoDidItSolutions(ambiguousFixture()))
        assertEquals(3, countWhoDidItSolutions(ambiguousFixture(), cap = 10))
    }

    @Test
    fun liarFixtureIsUniqueOnlyThroughLiarRule() {
        assertEquals(1, countWhoDidItSolutions(liarFixture()))
        assertEquals(fixtureSolution().culprit, findWhoDidItSolution(liarFixture())!!.culprit)
        assertEquals(3, countWhoDidItSolutions(liarFixture().copy(statements = emptyList()), cap = 10))
    }

    @Test
    fun twoLiarsMeanNoSolution() {
        val statements = liarStatements().toMutableList()
        statements[1] = WhoDidItStatement(1, WhoDidItClue.Same(WhoDidItRef(0, 1), WhoDidItRef(1, 0)))
        assertEquals(0, countWhoDidItSolutions(liarFixture().copy(statements = statements)))
    }

    @Test
    fun duplicatedClueIsExtra() {
        val spec = easyFixture()
        assertEquals(0, countWhoDidItExtraClues(spec))
        val duplicated = spec.copy(clues = spec.clues + spec.clues.first())
        assertEquals(2, countWhoDidItExtraClues(duplicated))
    }

    @Test
    fun findReturnsNullWhenUnsatisfiable() {
        val spec = easyFixture()
        val broken = spec.copy(clues = spec.clues + WhoDidItClue.Crime(WhoDidItRef(2, 0)))
        assertEquals(null, findWhoDidItSolution(broken))
        assertNotNull(findWhoDidItSolution(spec))
    }

    @Test
    fun expertSizedFixtureIsUniqueAndFast() = assertFinishesWithin(20.seconds) {
        val spec = expertSizedFixture()
        assertEquals(1, countWhoDidItSolutions(spec))
        assertEquals(expertSizedSolution(), findWhoDidItSolution(spec))
        countWhoDidItExtraClues(spec)
    }

    @Test
    fun statementCluesThatDependOnTheCulpritAreRejected() {
        val spec = liarFixture()
        listOf(
            WhoDidItClue.Crime(WhoDidItRef(1, 0)),
            WhoDidItClue.CrimeTrait(WhoDidItTrait.SUSPECT_TALL),
        ).forEach { clue ->
            val bad = spec.copy(statements = spec.statements + WhoDidItStatement(0, clue))
            val error = assertFailsWith<IllegalArgumentException> { countWhoDidItSolutions(bad) }
            assertTrue(error.message!!.contains("culprit"), error.message)
            assertFailsWith<IllegalArgumentException> { findWhoDidItSolution(bad) }
        }
    }

    @Test
    fun toSpecResolvesTraitsThroughTheme() {
        val entry = WhoDidItPuzzleEntry(
            id = "t", difficulty = "easy", theme = "bakery", case = 0, size = 3,
            categories = listOf(WhoDidItCategory.SUSPECT, WhoDidItCategory.PLACE, WhoDidItCategory.ITEM),
            entries = listOf(listOf(0, 1, 2), listOf(1, 0, 3), listOf(5, 2, 4)),
            solution = listOf(listOf(0, 1, 2), listOf(1, 2, 0), listOf(2, 0, 1)),
            culprit = 2,
            clues = listOf(WhoDidItClue.Crime(WhoDidItRef(2, 0))),
        )
        val spec = entry.toSpec()
        val pools = whoDidItTheme("bakery").pools
        assertEquals(3, spec.size)
        assertEquals(3, spec.categoryCount)
        assertEquals(entry.clues, spec.clues)
        assertEquals(pools.getValue(WhoDidItCategory.PLACE)[1], spec.traits[1][0])
        assertEquals(pools.getValue(WhoDidItCategory.PLACE)[3], spec.traits[1][2])
        assertEquals(pools.getValue(WhoDidItCategory.ITEM)[5], spec.traits[2][0])
        assertEquals(pools.getValue(WhoDidItCategory.SUSPECT)[2], spec.traits[0][2])
    }

    @Test
    fun solutionValueCopiesSolutionAndCulprit() {
        val entry = WhoDidItPuzzleEntry(
            id = "t", difficulty = "easy", theme = "bakery", case = 0, size = 3,
            categories = listOf(WhoDidItCategory.SUSPECT, WhoDidItCategory.PLACE, WhoDidItCategory.ITEM),
            entries = listOf(listOf(0, 1, 2), listOf(0, 1, 2), listOf(0, 1, 2)),
            solution = listOf(listOf(0, 1, 2), listOf(1, 2, 0), listOf(2, 0, 1)),
            culprit = 1,
            clues = emptyList(),
        )
        assertEquals(WhoDidItSolution(entry.solution, 1), entry.solutionValue())
    }
}
