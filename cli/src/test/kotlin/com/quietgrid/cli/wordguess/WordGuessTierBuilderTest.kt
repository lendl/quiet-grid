// cli/src/test/kotlin/com/quietgrid/cli/wordguess/WordGuessTierBuilderTest.kt
package com.quietgrid.cli.wordguess

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WordGuessTierBuilderTest {
    @Test
    fun `buildWordGuessTiers keeps only words of the requested length`() {
        val raw = listOf("apple", "grape", "kiwi", "mango", "fig")
        val tiers = buildWordGuessTiers(raw, wordLength = 5, commonSize = 10, fullSize = 10)
        assertEquals(listOf("apple", "grape", "mango"), tiers.full)
    }

    @Test
    fun `buildWordGuessTiers drops non-alphabetic tokens and de-duplicates while preserving frequency order`() {
        val raw = listOf("apple", "a1b2c", "apple", "grape")
        val tiers = buildWordGuessTiers(raw, wordLength = 5, commonSize = 10, fullSize = 10)
        assertEquals(listOf("apple", "grape"), tiers.full)
    }

    @Test
    fun `buildWordGuessTiers ascii-folds accented letters`() {
        val raw = listOf("café", "mango")
        val tiers = buildWordGuessTiers(raw, wordLength = 4, commonSize = 10, fullSize = 10)
        assertEquals(listOf("cafe"), tiers.full)
    }

    @Test
    fun `buildWordGuessTiers folds eszett to double-s, which can shift word length`() {
        val raw = listOf("weiß")
        val foldedLength = buildWordGuessTiers(raw, wordLength = 5, commonSize = 10, fullSize = 10)
        assertTrue("weiss" in foldedLength.full)
        val originalLength = buildWordGuessTiers(raw, wordLength = 4, commonSize = 10, fullSize = 10)
        assertTrue(originalLength.full.isEmpty())
    }

    @Test
    fun `buildWordGuessTiers' common tier is a prefix of the full tier, sized independently`() {
        val raw = (1..20).map { "w${it}xyz" }
        val fiveLetterWords = listOf("aaaaa", "bbbbb", "ccccc", "ddddd", "eeeee")
        val tiers = buildWordGuessTiers(fiveLetterWords, wordLength = 5, commonSize = 2, fullSize = 4)
        assertEquals(listOf("aaaaa", "bbbbb"), tiers.common)
        assertEquals(listOf("aaaaa", "bbbbb", "ccccc", "ddddd"), tiers.full)
    }

    @Test
    fun `buildWordGuessTiers' dictionary contains every filtered word regardless of tier size`() {
        val fiveLetterWords = listOf("aaaaa", "bbbbb", "ccccc")
        val tiers = buildWordGuessTiers(fiveLetterWords, wordLength = 5, commonSize = 1, fullSize = 1)
        assertEquals(setOf("aaaaa", "bbbbb", "ccccc"), tiers.dictionary)
    }

    @Test
    fun `sortWordGuessByRarity puts words with fewer rare letters first`() {
        val words = listOf("puzzle", "cotton", "kayak")
        val sorted = sortWordGuessByRarity(words, locale = "en")
        assertEquals(listOf("cotton", "kayak", "puzzle"), sorted)
    }

    @Test
    fun `sortWordGuessByRarity breaks ties alphabetically`() {
        val words = listOf("zebra", "apple", "grape")
        val sorted = sortWordGuessByRarity(words, locale = "en")
        assertEquals(listOf("apple", "grape", "zebra"), sorted)
    }

    @Test
    fun `sortWordGuessByRarity counts occurrences of accented rare letters after folding`() {
        val words = listOf("kayak", "café")
        val sorted = sortWordGuessByRarity(words, locale = "fr")
        assertEquals(listOf("café", "kayak"), sorted)
    }

    @Test
    fun `sortWordGuessByRarity is a no-op for a locale with no rare-letter set`() {
        val words = listOf("zebra", "apple")
        val sorted = sortWordGuessByRarity(words, locale = "de")
        assertEquals(listOf("apple", "zebra"), sorted)
    }
}
