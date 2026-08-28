package com.quietgrid.cli.wordguess

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WordGuessHunspellCorpusTest {
    @Test
    fun `loadWordGuessHunspellWords expands the dictionary and drops capitalized entries`() {
        val words = loadWordGuessHunspellWords("xx")
        assertTrue("cat" in words)
        assertTrue("dog" in words)
        assertFalse(words.any { it.equals("susan", ignoreCase = true) })
    }
}
