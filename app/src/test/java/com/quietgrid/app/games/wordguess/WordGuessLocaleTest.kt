package com.quietgrid.app.games.wordguess

import org.junit.Assert.assertTrue
import org.junit.Test

class WordGuessLocaleTest {
    @Test
    fun `WORDGUESS_SUPPORTED_LOCALES matches the app's five shipped locales`() {
        assertTrue(WORDGUESS_SUPPORTED_LOCALES == setOf("en", "de", "es", "fr", "nl"))
    }
}
