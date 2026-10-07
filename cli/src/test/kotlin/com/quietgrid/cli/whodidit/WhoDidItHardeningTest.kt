package com.quietgrid.cli.whodidit

import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.whodidit.WHODIDIT_EXPERT_MAX_DIRECT_SAME
import com.quietgrid.engine.whodidit.WHODIDIT_SHAPES
import com.quietgrid.engine.whodidit.WHODIDIT_THEMES
import com.quietgrid.engine.whodidit.WhoDidItClue
import com.quietgrid.engine.whodidit.WhoDidItRef
import com.quietgrid.engine.whodidit.WhoDidItTechnique
import com.quietgrid.engine.whodidit.classifyWhoDidIt
import com.quietgrid.engine.whodidit.countWhoDidItExtraClues
import com.quietgrid.engine.whodidit.countWhoDidItSolutions
import com.quietgrid.engine.whodidit.isTrue
import com.quietgrid.engine.whodidit.solveWhoDidIt
import com.quietgrid.engine.whodidit.toSpec
import com.quietgrid.engine.whodidit.whoDidItAllowedClueTypes
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WhoDidItHardeningTest {
    private fun setup(difficulty: Difficulty, seed: Int) =
        pickWhoDidItSetup(WHODIDIT_THEMES[seed % WHODIDIT_THEMES.size], WHODIDIT_SHAPES.getValue(difficulty), Random(seed))!!

    @Test
    fun `statements cover every suspect once and only the culprit lies`() {
        (1..20).forEach { seed ->
            val setup = setup(Difficulty.EXPERT, seed)
            val statements = assignWhoDidItStatements(setup, Random(seed))
            val spec = setup.toSpec(emptyList(), statements)
            assertEquals((0 until setup.shape.size).toList(), statements.map { it.s })
            statements.forEach { statement ->
                val clue = statement.clue
                assertTrue(clue is WhoDidItClue.Same || clue is WhoDidItClue.NotSame || clue is WhoDidItClue.HasTrait)
                if (clue is WhoDidItClue.HasTrait) assertTrue(clue.a.category != clue.trait.category.ordinal)
                assertEquals(statement.s != setup.solution.culprit, clue.isTrue(spec, setup.solution))
            }
            assertEquals(listOf(setup.solution.culprit), statements.filterNot { it.clue.isTrue(spec, setup.solution) }.map { it.s })
        }
    }

    @Test
    fun `weak clue additions are drawn type-first`() {
        val ref = ::WhoDidItRef
        val either = (1..4).flatMap { a -> (0..4).map { b -> WhoDidItClue.Either(ref(0, a), ref(1, b), ref(1, b + 1)) } }
        val notSame = WhoDidItClue.NotSame(ref(0, 0), ref(1, 0))
        val pool = either + notSame
        val firsts = (1..200).map { whoDidItTypeFirstDraws(pool, Random(it)).next() }
        val notSameFirst = firsts.count { it == notSame }
        assertTrue("not-same drawn first $notSameFirst/200", notSameFirst in 70..130)
        val drawn = whoDidItTypeFirstDraws(pool, Random(7)).asSequence().toList()
        assertEquals(pool.toSet(), drawn.toSet())
        assertEquals(pool.size, drawn.size)
        assertEquals(drawn, whoDidItTypeFirstDraws(pool, Random(7)).asSequence().toList())
    }

    @Test(timeout = 300_000)
    fun `hardening never returns clues the technique solver cannot solve`() {
        val returned = (1..10).mapNotNull { seed ->
            val random = Random(seed)
            val setup = setup(Difficulty.HARD, seed)
            val all = enumerateTrueWhoDidItClues(setup, whoDidItAllowedClueTypes(Difficulty.HARD))
            val reduced = reduceWhoDidItClues(setup, all, emptyList(), Difficulty.HARD, random)
            hardenWhoDidIt(setup, reduced, emptyList(), Difficulty.HARD, random)?.let { setup to it }
        }
        assertTrue(returned.isNotEmpty())
        returned.forEach { (setup, clues) ->
            val spec = setup.toSpec(clues, emptyList())
            assertTrue(solveWhoDidIt(spec).solved)
            assertEquals(1, countWhoDidItSolutions(spec))
        }
    }

    @Test(timeout = 300_000)
    fun `shuffled entry is a permutation of the hardened clues and still classifies`() {
        val (hardened, entry) = (1..10).firstNotNullOf { seed ->
            val random = Random(seed)
            val setup = setup(Difficulty.HARD, seed)
            val all = enumerateTrueWhoDidItClues(setup, whoDidItAllowedClueTypes(Difficulty.HARD))
            val reduced = reduceWhoDidItClues(setup, all, emptyList(), Difficulty.HARD, random)
            hardenWhoDidIt(setup, reduced, emptyList(), Difficulty.HARD, random)?.let { hardened ->
                setup.shuffledEntryOrNull(Difficulty.HARD, hardened, emptyList(), random)?.let { hardened to it }
            }
        }
        val spec = entry.toSpec()
        assertEquals(hardened.groupingBy { it }.eachCount(), entry.clues.groupingBy { it }.eachCount())
        assertEquals(1, countWhoDidItSolutions(spec))
        assertEquals(Difficulty.HARD, classifyWhoDidIt(spec, solveWhoDidIt(spec), countWhoDidItExtraClues(spec)))
        assertEquals(entry.id, whoDidItEntryId(Difficulty.HARD, entry))
    }

    @Test(timeout = 600_000)
    fun `generated hard puzzle classifies HARD`() {
        val entry = (1..10).firstNotNullOfOrNull { generateWhoDidItPuzzle(Difficulty.HARD, null, Random(it)) }
        assertNotNull(entry)
        val spec = entry!!.toSpec()
        assertEquals("hard", entry.difficulty)
        assertTrue(entry.statements.isEmpty())
        assertEquals(1, countWhoDidItSolutions(spec))
        assertEquals(Difficulty.HARD, classifyWhoDidIt(spec, solveWhoDidIt(spec), countWhoDidItExtraClues(spec)))
        assertEquals(entry.id, whoDidItEntryId(Difficulty.HARD, entry))
    }

    @Test(timeout = 600_000)
    fun `generated expert puzzle classifies EXPERT and needs liar reasoning`() {
        val entry = (1..10).firstNotNullOfOrNull { generateWhoDidItHardPuzzle(Difficulty.EXPERT, null, Random(it)) }
        assertNotNull(entry)
        val spec = entry!!.toSpec()
        val result = solveWhoDidIt(spec)
        assertEquals("expert", entry.difficulty)
        assertEquals((0 until entry.size).toList(), entry.statements.map { it.s })
        assertEquals(1, countWhoDidItSolutions(spec))
        assertEquals(Difficulty.EXPERT, classifyWhoDidIt(spec, result, countWhoDidItExtraClues(spec)))
        val liars = result.steps.filter { it.technique == WhoDidItTechnique.LIAR_HYPOTHESIS }
        assertTrue(liars.isNotEmpty())
        assertTrue(liars.all { it.depth >= 1 })
        assertTrue(entry.clues.count { it is WhoDidItClue.Same } <= WHODIDIT_EXPERT_MAX_DIRECT_SAME)
    }
}
