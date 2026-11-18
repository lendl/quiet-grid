package com.quietgrid.cli.wordguess

import org.junit.Assert.assertEquals
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

    @Test
    fun `loadWordGuessHunspellWords keeps capitalized entries carrying the common noun flag, lowercased`() {
        val words = loadWordGuessHunspellWords("yy", commonNounFlag = "m")
        assertTrue("laufen" in words)
        assertTrue("tisch" in words)
        assertFalse(words.any { it.equals("peter", ignoreCase = true) })
        assertFalse(words.any { it.equals("kbyte", ignoreCase = true) })
    }

    @Test
    fun `loadWordGuessHunspellWords resolves numbered flag aliases`() {
        val words = loadWordGuessHunspellWords("ww")
        assertTrue("jours" in words)
        assertTrue("petites" in words)
        assertTrue("petit" in words)
    }

    @Test
    fun `hunspellParadigmsContaining returns every form of a matched entry`() {
        assertEquals(setOf("petite", "petites", "petit"), hunspellParadigmsContaining("ww", setOf("petit")))
        assertEquals(setOf("jour", "jours"), hunspellParadigmsContaining("ww", setOf("jour")))
        assertTrue(hunspellParadigmsContaining("ww", setOf("absent")).isEmpty())
    }

    @Test
    fun `loadWordGuessHunspellWords spells the ij ligature as two letters`() {
        val words = loadWordGuessHunspellWords("vv")
        assertTrue("tijd" in words)
        assertTrue("prijs" in words)
        assertFalse(words.any { 'ĳ' in it })
    }
}
