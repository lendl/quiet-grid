package com.quietgrid.app.core.themes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeWordSuggestionsTest {

    private val existing = listOf("Dog", "Cat", "Éclair", "Łoś")

    @Test
    fun splitsOnCommasSemicolonsAndNewLines() {
        val result = parseWordSuggestions("Otter, Badger;Mole\nHeron\r\nStork", existing)

        assertEquals(listOf("Otter", "Badger", "Mole", "Heron", "Stork"), result.added)
        assertTrue(result.duplicates.isEmpty())
        assertTrue(result.invalid.isEmpty())
    }

    @Test
    fun trimsAndSkipsBlankEntries() {
        val result = parseWordSuggestions("  Otter  ,, \n\n , Badger ", existing)

        assertEquals(listOf("Otter", "Badger"), result.added)
    }

    @Test
    fun existingWordsMatchIgnoringCaseAndAccents() {
        val result = parseWordSuggestions("dog, ECLAIR, łos, Hamster", existing)

        assertEquals(listOf("dog", "ECLAIR", "łos"), result.duplicates)
        assertEquals(listOf("Hamster"), result.added)
    }

    @Test
    fun repeatedSuggestionsAreKeptOnce() {
        val result = parseWordSuggestions("Otter, otter, ÖTTER", existing)

        assertEquals(listOf("Otter"), result.added)
    }

    @Test
    fun rejectsPhrasesHyphensDigitsAndShortWords() {
        val result = parseWordSuggestions("Red fox, Sea-lion, R2D2, Ox, Bee", existing)

        assertEquals(listOf("Red fox", "Sea-lion", "R2D2", "Ox"), result.invalid)
        assertEquals(listOf("Bee"), result.added)
    }

    @Test
    fun acceptsNonAsciiLetters() {
        val result = parseWordSuggestions("Żubr, Größe, Ñandú", existing)

        assertEquals(listOf("Żubr", "Größe", "Ñandú"), result.added)
    }

    @Test
    fun searchMatchesIgnoringCaseAndAccents() {
        assertTrue(matchesThemeWordSearch("Éclair", "ecl"))
        assertTrue(matchesThemeWordSearch("Hamster", "MST"))
        assertTrue(matchesThemeWordSearch("Hamster", "  "))
        assertFalse(matchesThemeWordSearch("Hamster", "dog"))
    }
}
