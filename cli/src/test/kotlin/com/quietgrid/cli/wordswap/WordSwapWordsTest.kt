package com.quietgrid.cli.wordswap

import com.quietgrid.cli.ASSETS_DIR
import com.quietgrid.cli.readPuzzleEntries
import com.quietgrid.cli.wordguess.loadWordGuessBlocklist
import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.wordswap.WordSwapPuzzleEntry
import com.quietgrid.engine.wordswap.words
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WordSwapWordsTest {

    private val plainWord = Regex("[a-z]{3,7}")

    @Test
    fun everyLocaleHasEnoughPlainWords() {
        WORDSWAP_LOCALES.forEach { locale ->
            val words = loadWordSwapRankedWords(locale)
            assertTrue("$locale has ${words.size}", words.size >= 10_000)
            assertTrue(locale, words.all { plainWord.matches(it) })
            assertEquals(locale, words.size, words.toSet().size)
        }
    }

    @Test
    fun everyWordHasAVowel() {
        val vowel = Regex("[aeiouy]")
        WORDSWAP_LOCALES.forEach { locale ->
            val bare = loadWordSwapRankedWords(locale).filterNot { vowel.containsMatchIn(it) }
            assertTrue("$locale: ${bare.take(20)}", bare.isEmpty())
        }
    }

    @Test
    fun noLocaleListsABlockedWord() {
        WORDSWAP_LOCALES.forEach { locale ->
            val blocked = loadWordGuessBlocklist(locale) + loadWordSwapBlocklist(locale)
            assertTrue(locale, loadWordSwapRankedWords(locale).none { it in blocked })
        }
    }

    @Test
    fun englishBankWordsStayWithinTheirTierVocabulary() {
        val ranked = loadWordSwapRankedWords("en")
        readPuzzleEntries("$ASSETS_DIR/wordswap_puzzles.json", WordSwapPuzzleEntry.serializer()).forEach { puzzle ->
            val vocabulary = WORDSWAP_TIERS.getValue(Difficulty.entries.first { it.key == puzzle.difficulty }).vocabulary
            val allowed = ranked.take(vocabulary).toSet()
            puzzle.words().forEach { word -> assertTrue("${puzzle.id}: $word", word in allowed) }
        }
    }
}
