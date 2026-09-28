package com.quietgrid.cli.themeclear

import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.themeclear.THEMECLEAR_TIERS
import com.quietgrid.engine.themeclear.ThemeClearDictionary
import com.quietgrid.engine.themeclear.ThemeClearSolver
import com.quietgrid.engine.themeclear.ThemeClearTheme
import com.quietgrid.engine.themeclear.ThemeClearTierSpec
import com.quietgrid.engine.themeclear.themeClearMeetsTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ThemeClearGeneratorTest {

    @Test
    fun `theme deficits count up to the target and never past the cap`() {
        val perTheme = mapOf("animals" to 28, "food" to 30, "space" to 3)
        assertEquals(mapOf("animals" to 2, "space" to 27, "home" to 30), themeClearThemeDeficits(perTheme, listOf("animals", "food", "space", "home"), 30))
        assertEquals(THEMECLEAR_MAX_PUZZLES_PER_THEME - 3, themeClearThemeDeficits(perTheme, listOf("space"), 500).getValue("space"))
    }

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
    fun `generated puzzles meet their tier on a full metrics run`() {
        val theme = ThemeClearTheme("animals", listOf("BEAR", "LION", "WOLF", "DEER", "GOAT", "CRAB", "MOLE", "SEAL", "HARE", "LYNX"))
        val generator = ThemeClearGenerator(listOf(theme), Random(9))
        val solver = ThemeClearSolver(ThemeClearDictionary(theme.words))
        repeat(5) {
            val entry = generator.generate(Difficulty.EASY, "en")
            assertNotNull(entry)
            val letters = entry!!.grid.joinToString("")
            assertTrue(entry.id, themeClearMeetsTier(Difficulty.EASY, letters.length, solver.analyze(letters)))
        }
    }

    @Test
    fun `generate targets the requested theme`() {
        val animals = ThemeClearTheme("animals", listOf("BEAR", "LION", "WOLF", "DEER", "GOAT", "CRAB", "MOLE", "SEAL", "HARE", "LYNX"))
        val food = ThemeClearTheme("food", listOf("RICE", "BEAN", "CORN", "LIME", "PEAR", "PLUM", "KALE", "TACO", "SOUP", "CAKE"))
        val generator = ThemeClearGenerator(listOf(animals, food), Random(5))
        repeat(5) { assertEquals("food", generator.generate(Difficulty.EASY, "en", themeId = "food")?.themeId) }
    }
}
