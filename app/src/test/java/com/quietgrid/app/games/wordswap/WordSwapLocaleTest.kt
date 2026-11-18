package com.quietgrid.app.games.wordswap

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class WordSwapLocaleTest {

    private val original = Locale.getDefault()

    @After
    fun restoreLocale() {
        Locale.setDefault(original)
    }

    @Test
    fun `setting wins over system language`() {
        Locale.setDefault(Locale.GERMAN)
        assertEquals("nl", currentWordSwapLocale("nl"))
    }

    @Test
    fun `empty setting follows system language`() {
        Locale.setDefault(Locale.ITALIAN)
        assertEquals("it", currentWordSwapLocale(""))
    }

    @Test
    fun `unsupported language falls back to English`() {
        Locale.setDefault(Locale.forLanguageTag("sv"))
        assertEquals("en", currentWordSwapLocale(""))
    }

    @Test
    fun `locale comes from the id prefix`() {
        assertEquals("nl", wordSwapLocaleOfId("ws-nl-easy-1f3a"))
        assertEquals("en", wordSwapLocaleOfId("ws-easy-1f3a"))
        assertEquals("en", wordSwapLocaleOfId("ws-expert-9"))
    }

    @Test
    fun `bank asset per locale`() {
        assertEquals("wordswap_puzzles.json", wordSwapBankAsset("en"))
        assertEquals("wordswap_puzzles_pt.json", wordSwapBankAsset("pt"))
    }

    @Test
    fun `supported locales match the puzzle languages`() {
        assertEquals(setOf("en", "de", "es", "fr", "it", "nl", "pl", "pt"), WORDSWAP_SUPPORTED_LOCALES)
    }
}
