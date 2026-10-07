package com.quietgrid.cli.whodidit

import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.whodidit.WHODIDIT_SHAPES
import com.quietgrid.engine.whodidit.WHODIDIT_THEMES
import com.quietgrid.engine.whodidit.WHODIDIT_EASY_MIN_CLUES
import com.quietgrid.engine.whodidit.WhoDidItCategory
import com.quietgrid.engine.whodidit.WhoDidItClue
import com.quietgrid.engine.whodidit.WhoDidItClueType
import com.quietgrid.engine.whodidit.WhoDidItPuzzleEntry
import com.quietgrid.engine.whodidit.WhoDidItRef
import com.quietgrid.engine.whodidit.WhoDidItSolution
import com.quietgrid.engine.whodidit.WhoDidItTechnique
import com.quietgrid.engine.whodidit.WhoDidItTrait
import com.quietgrid.engine.whodidit.classifyWhoDidIt
import com.quietgrid.engine.whodidit.countWhoDidItExtraClues
import com.quietgrid.engine.whodidit.countWhoDidItSolutions
import com.quietgrid.engine.whodidit.isTrue
import com.quietgrid.engine.whodidit.ownerOf
import com.quietgrid.engine.whodidit.solveWhoDidIt
import com.quietgrid.engine.whodidit.toSpec
import com.quietgrid.engine.whodidit.whoDidItAllowedClueTypes
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WhoDidItGeneratorTest {
    @Test
    fun `pickWhoDidItSetup never returns a constant trait dimension`() {
        val random = Random(1)
        repeat(50) { index ->
            val theme = WHODIDIT_THEMES[index % WHODIDIT_THEMES.size]
            val shape = WHODIDIT_SHAPES.values.toList()[index % 4]
            val setup = pickWhoDidItSetup(theme, shape, random)!!
            WhoDidItCategory.entries.take(shape.categoryCount).forEach { category ->
                val pool = theme.pools.getValue(category)
                val dimensions = pool.flatten().map { it.dimension }.toSet()
                dimensions.forEach { dimension ->
                    val values = setup.entries[category.ordinal].flatMap { pool[it] }.filter { it.dimension == dimension }.toSet()
                    assertTrue(values.size >= 2)
                }
            }
        }
    }

    @Test
    fun `enumerated clues are all true for the setup solution`() {
        val random = Random(3)
        Difficulty.entries.forEach { difficulty ->
            val setup = pickWhoDidItSetup(WHODIDIT_THEMES.first(), WHODIDIT_SHAPES.getValue(difficulty), random)!!
            val allowed = whoDidItAllowedClueTypes(difficulty)
            val clues = enumerateTrueWhoDidItClues(setup, allowed)
            val spec = setup.toSpec(clues, emptyList())
            assertTrue(clues.isNotEmpty())
            assertTrue(clues.all { it.isTrue(spec, setup.solution) && it.type in allowed })
        }
    }

    @Test
    fun `generated easy puzzle is unique, solved, easy-shaped and classifies EASY`() {
        val entry = (7..30).firstNotNullOfOrNull { generateWhoDidItPuzzle(Difficulty.EASY, themeId = "bakery", random = Random(it)) }!!
        val spec = entry.toSpec()
        assertEquals(1, countWhoDidItSolutions(spec))
        assertEquals(Difficulty.EASY, classifyWhoDidIt(spec, solveWhoDidIt(spec), countWhoDidItExtraClues(spec)))
        assertTrue(entry.clues.all { it.type in whoDidItAllowedClueTypes(Difficulty.EASY) })
    }

    @Test
    fun `unthemed easy generation spreads across all themes`() {
        val themes = (1..70).mapNotNull { generateWhoDidItPuzzle(Difficulty.EASY, themeId = null, random = Random(it))?.theme }
        val counts = themes.groupingBy { it }.eachCount()
        assertEquals(WHODIDIT_THEMES.map { it.id }.toSet(), counts.keys)
        assertTrue(counts.toString(), counts.values.all { it <= themes.size * 3 / 10 })
    }

    @Test
    fun `generated medium puzzle classifies MEDIUM`() {
        val entry = (1..20).firstNotNullOfOrNull { generateWhoDidItPuzzle(Difficulty.MEDIUM, themeId = null, random = Random(it)) }
        assertNotNull(entry)
        val spec = entry!!.toSpec()
        assertEquals(1, countWhoDidItSolutions(spec))
        assertEquals(Difficulty.MEDIUM, classifyWhoDidIt(spec, solveWhoDidIt(spec), countWhoDidItExtraClues(spec)))
    }

    @Test(timeout = 300_000)
    fun `generated medium puzzles keep either-or clues well below half`() {
        val shares = (1..12).mapNotNull { generateWhoDidItPuzzle(Difficulty.MEDIUM, null, Random(it)) }
            .map { entry -> entry.clues.count { it.type == WhoDidItClueType.EITHER }.toDouble() / entry.clues.size }
        assertTrue(shares.size >= 10)
        val mean = shares.average()
        assertTrue("mean either share $mean over ${shares.size} puzzles", mean < 0.45)
    }

    private fun WhoDidItPuzzleEntry.inEnumerationOrder(difficulty: Difficulty): Boolean {
        val setup = WhoDidItSetup(theme, case, WHODIDIT_SHAPES.getValue(difficulty), entries, WhoDidItSolution(solution, culprit))
        val index = enumerateTrueWhoDidItClues(setup, whoDidItAllowedClueTypes(difficulty)).withIndex().associate { it.value to it.index }
        return clues == clues.sortedBy { index.getValue(it) }
    }

    @Test
    fun `generated clue order is shuffled away from enumeration order and still classifies`() {
        val generated = (1..10).flatMap { seed ->
            listOf(Difficulty.EASY, Difficulty.MEDIUM).mapNotNull { difficulty ->
                generateWhoDidItPuzzle(difficulty, null, Random(seed))?.let { difficulty to it }
            }
        }
        assertTrue(generated.isNotEmpty())
        assertTrue(generated.any { (difficulty, entry) -> !entry.inEnumerationOrder(difficulty) })
        generated.forEach { (difficulty, entry) ->
            val spec = entry.toSpec()
            assertEquals(1, countWhoDidItSolutions(spec))
            assertEquals(difficulty, classifyWhoDidIt(spec, solveWhoDidIt(spec), countWhoDidItExtraClues(spec)))
        }
    }

    private fun hardSetup(seed: Int) =
        pickWhoDidItSetup(WHODIDIT_THEMES.first(), WHODIDIT_SHAPES.getValue(Difficulty.HARD), Random(seed))!!

    @Test
    fun `enumerated clues obey structural rules`() {
        (1..5).forEach { seed ->
            val setup = hardSetup(seed)
            val spec = setup.toSpec(emptyList(), emptyList())
            val clues = enumerateTrueWhoDidItClues(setup, whoDidItAllowedClueTypes(Difficulty.HARD))
            val types = clues.map { it.type }.toSet()
            assertEquals(WhoDidItClueType.entries.toSet(), types)
            fun holders(trait: WhoDidItTrait) = spec.traits[trait.category.ordinal].count { trait in it }
            clues.forEach { clue ->
                when (clue) {
                    is WhoDidItClue.HasTrait -> {
                        assertTrue(clue.a.category != clue.trait.category.ordinal)
                        assertTrue(holders(clue.trait) in 1 until setup.shape.size)
                    }
                    is WhoDidItClue.LacksTrait -> {
                        assertTrue(clue.a.category != clue.trait.category.ordinal)
                        assertTrue(holders(clue.trait) in 1 until setup.shape.size)
                    }
                    is WhoDidItClue.CrimeTrait -> assertTrue(holders(clue.trait) in 1 until setup.shape.size)
                    is WhoDidItClue.Crime -> assertTrue(clue.e.category != 0)
                    is WhoDidItClue.Either -> {
                        assertEquals(clue.b.category, clue.c.category)
                        assertTrue(clue.a.category != clue.b.category)
                        assertTrue(clue.b != clue.c)
                    }
                    else -> Unit
                }
            }
        }
    }

    @Test
    fun `enumeration is complete for Same and NotSame`() {
        (1..5).forEach { seed ->
            val setup = hardSetup(seed)
            val shape = setup.shape
            val clues = enumerateTrueWhoDidItClues(setup, whoDidItAllowedClueTypes(Difficulty.HARD))
            val pairs = shape.categoryCount * (shape.categoryCount - 1) / 2
            assertEquals(shape.size * pairs, clues.count { it is WhoDidItClue.Same })
            val notSame = clues.filterIsInstance<WhoDidItClue.NotSame>().map { it.a to it.b }.toSet()
            val refs = (0 until shape.categoryCount).flatMap { c -> (0 until shape.size).map { WhoDidItRef(c, it) } }
            for (a in refs) for (b in refs) {
                if (a.category < b.category && setup.solution.ownerOf(a) != setup.solution.ownerOf(b)) {
                    assertTrue(notSame.contains(a to b))
                }
            }
            assertEquals(shape.size * pairs * (shape.size - 1), notSame.size)
        }
    }

    @Test
    fun `easy reduction keeps the minimum clue count and stays unique`() {
        val setup = pickWhoDidItSetup(WHODIDIT_THEMES.first(), WHODIDIT_SHAPES.getValue(Difficulty.EASY), Random(2))!!
        val all = enumerateTrueWhoDidItClues(setup, whoDidItAllowedClueTypes(Difficulty.EASY))
        val reduced = reduceWhoDidItClues(setup, all, emptyList(), Difficulty.EASY, Random(2))
        assertTrue(reduced.size >= WHODIDIT_EASY_MIN_CLUES)
        assertTrue(reduced.size < all.size)
        assertEquals(1, countWhoDidItSolutions(setup.toSpec(reduced, emptyList())))
    }

    @Test
    fun `hard reduction result is minimal`() {
        val setup = hardSetup(4)
        val all = enumerateTrueWhoDidItClues(setup, whoDidItAllowedClueTypes(Difficulty.HARD))
        val reduced = reduceWhoDidItClues(setup, all, emptyList(), Difficulty.HARD, Random(4))
        assertEquals(1, countWhoDidItSolutions(setup.toSpec(reduced, emptyList())))
        reduced.forEach { clue ->
            assertTrue(countWhoDidItSolutions(setup.toSpec(reduced - clue, emptyList())) != 1)
        }
    }

    @Test
    fun `medium reduction trace has no contradiction or liar steps`() {
        val setup = pickWhoDidItSetup(WHODIDIT_THEMES.first(), WHODIDIT_SHAPES.getValue(Difficulty.MEDIUM), Random(5))!!
        val all = enumerateTrueWhoDidItClues(setup, whoDidItAllowedClueTypes(Difficulty.MEDIUM))
        val reduced = reduceWhoDidItClues(setup, all, emptyList(), Difficulty.MEDIUM, Random(5))
        val result = solveWhoDidIt(setup.toSpec(reduced, emptyList()))
        assertTrue(result.solved)
        val techniques = result.steps.map { it.technique }.toSet()
        assertFalse(WhoDidItTechnique.CONTRADICTION in techniques)
        assertFalse(WhoDidItTechnique.LIAR_HYPOTHESIS in techniques)
    }

    @Test
    fun `entry id has the expected shape and is deterministic`() {
        val entry = (1..30).firstNotNullOf { generateWhoDidItPuzzle(Difficulty.EASY, "bakery", Random(it)) }
        assertTrue(Regex("whodidit-easy-[0-9a-f]+").matches(entry.id))
        assertEquals(entry.id, whoDidItEntryId(Difficulty.EASY, entry))
        assertEquals(whoDidItEntryId(Difficulty.EASY, entry), whoDidItEntryId(Difficulty.EASY, entry.copy(id = "other")))
        val changed = entry.copy(clues = entry.clues.drop(1))
        assertNotEquals(entry.id, whoDidItEntryId(Difficulty.EASY, changed))
    }
}
