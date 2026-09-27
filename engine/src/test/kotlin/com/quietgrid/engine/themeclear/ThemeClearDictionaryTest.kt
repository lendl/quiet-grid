package com.quietgrid.engine.themeclear

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeClearDictionaryTest {

    @Test
    fun `words are normalized to uppercase and short words are dropped`() {
        val dictionary = ThemeClearDictionary(listOf("Lion", "ox", " zebra ", "Sea-lion"))
        assertEquals(setOf("LION", "ZEBRA", "SEALION"), dictionary.words)
    }

    @Test
    fun `diacritics are stripped instead of dropping the letter`() {
        val dictionary = ThemeClearDictionary(listOf("Maïs", "Pinguïn", "België", "Café"))
        assertEquals(setOf("MAIS", "PINGUIN", "BELGIE", "CAFE"), dictionary.words)
    }

    @Test
    fun `hasPrefix is true for every prefix of a word and false otherwise`() {
        val dictionary = ThemeClearDictionary(listOf("LION"))
        assertTrue(dictionary.hasPrefix("L"))
        assertTrue(dictionary.hasPrefix("LIO"))
        assertTrue(dictionary.hasPrefix("LION"))
        assertFalse(dictionary.hasPrefix("LX"))
        assertFalse(dictionary.hasPrefix("LIONS"))
    }

    @Test
    fun `canExtend is true when a longer word can still be built from the remaining letters`() {
        val dictionary = ThemeClearDictionary(listOf("APPLE", "APPLEPIE"))
        assertTrue(dictionary.canExtend("APPLE", "EIPX"))
    }

    @Test
    fun `canExtend is false when the remaining letters cannot finish the longer word`() {
        val dictionary = ThemeClearDictionary(listOf("APPLE", "APPLEPIE"))
        assertFalse(dictionary.canExtend("APPLE", "PI"))
    }

    @Test
    fun `letter counts and canBuild respect duplicate letters`() {
        val counts = themeClearLetterCounts("APPLE")
        assertEquals(2, counts['P' - 'A'])
        assertTrue(themeClearCanBuild("PAP", counts))
        assertFalse(themeClearCanBuild("PPP", counts))
    }

    @Test
    fun `parseThemeClearThemes reads locale keyed theme lists`() {
        val parsed = parseThemeClearThemes("""{"en":[{"themeId":"animals","words":["LION","ZEBRA"]}]}""")
        assertEquals(listOf(ThemeClearTheme("animals", listOf("LION", "ZEBRA"))), parsed["en"])
    }

    @Test
    fun `trap rate is zero when nothing is spellable`() {
        assertEquals(0.0, ThemeClearMetrics(finishCount = 0, trapWords = 0, spellableWords = 0).trapRate, 0.0)
        assertEquals(0.25, ThemeClearMetrics(finishCount = 1, trapWords = 1, spellableWords = 4).trapRate, 1e-9)
    }

    @Test
    fun `buildableWords returns sorted words spellable from letters respecting duplicate letters`() {
        val dictionary = ThemeClearDictionary(listOf("CAT", "ACT", "TACT"))
        assertEquals(listOf("ACT", "CAT"), dictionary.buildableWords("CAT"))
    }
}
