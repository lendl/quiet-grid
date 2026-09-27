package com.quietgrid.cli.themeclear

import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.themeclear.THEMECLEAR_TIERS
import com.quietgrid.engine.themeclear.ThemeClearMetrics
import com.quietgrid.engine.themeclear.ThemeClearTheme
import com.quietgrid.engine.themeclear.ThemeClearTierSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ThemeClearGeneratorTest {

    @Test
    fun `rectangles allow both orientations`() {
        assertEquals(setOf(4 to 3, 3 to 4), themeClearRectangles(12).toSet())
    }

    @Test
    fun `prime letter counts have no rectangle`() {
        assertTrue(themeClearRectangles(13).isEmpty())
    }

    @Test
    fun `rectangles never exceed seven columns or a two to one ratio`() {
        assertEquals(listOf(8 to 6), themeClearRectangles(48))
    }

    @Test
    fun `drawn words respect the tier's minimum word count and letter band`() {
        val pool = listOf("BEAR", "LION", "WOLF", "DEER", "GOAT", "CRAB", "MOLE", "SEAL", "HARE", "LYNX")
        val spec = ThemeClearTierSpec(letters = 12..16, minWords = 3, minBlindSuccess = 0.0, maxBlindSuccess = 1.0)
        val words = drawThemeClearWords(pool, spec, Random(7))
        assertNotNull(words)
        assertTrue(words!!.size >= 3)
        assertTrue(words.sumOf { it.length } in 12..16)
    }

    @Test
    fun `candidate entry grid holds exactly the intended letters`() {
        val theme = ThemeClearTheme("animals", listOf("BEAR", "LION", "WOLF", "DEER", "GOAT", "CRAB", "MOLE", "SEAL", "HARE", "LYNX"))
        val generator = ThemeClearGenerator(listOf(theme), Random(3))
        val spec = THEMECLEAR_TIERS.getValue(Difficulty.EASY)
        val candidate = (1..200).firstNotNullOfOrNull { generator.drawCandidate(spec) }
        assertNotNull(candidate)
        val entry = candidate!!.toEntry(Difficulty.EASY, "en", Random(3))
        assertEquals(entry.rows, entry.grid.size)
        assertTrue(entry.grid.all { it.length == entry.cols })
        assertEquals(entry.words.joinToString("").toList().sorted(), entry.grid.joinToString("").toList().sorted())
        assertTrue(entry.id.startsWith("tc-easy-animals-"))
    }

    @Test
    fun `candidate metrics match a direct solver run`() {
        val theme = ThemeClearTheme("animals", listOf("BEAR", "LION", "WOLF", "DEER", "GOAT", "CRAB", "MOLE", "SEAL", "HARE", "LYNX"))
        val generator = ThemeClearGenerator(listOf(theme), Random(9))
        val candidate = (1..200).firstNotNullOfOrNull { generator.drawCandidate(THEMECLEAR_TIERS.getValue(Difficulty.EASY)) }
        assertNotNull(candidate)
        val solver = com.quietgrid.engine.themeclear.ThemeClearSolver(com.quietgrid.engine.themeclear.ThemeClearDictionary(theme.words))
        assertEquals(solver.analyze(candidate!!.words.joinToString("")), candidate.metrics)
    }
}
