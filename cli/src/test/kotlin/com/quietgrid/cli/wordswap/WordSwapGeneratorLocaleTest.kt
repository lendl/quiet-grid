package com.quietgrid.cli.wordswap

import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.wordswap.words
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class WordSwapGeneratorLocaleTest {

    private val random = Random(8)

    @Test
    fun dutchPuzzleUsesDutchWordsAndPrefixedId() {
        val puzzle = generateSequence { generateWordSwapPuzzle(Difficulty.EASY, "nl", random) }.take(200).filterNotNull().first()
        assertTrue(puzzle.id, puzzle.id.startsWith("ws-nl-easy-"))
        val dutch = loadWordSwapRankedWords("nl").take(2_500).toSet()
        assertTrue(puzzle.words().toString(), puzzle.words().all { it in dutch })
    }

    @Test
    fun englishIdHasNoLocale() {
        val puzzle = generateSequence { generateWordSwapPuzzle(Difficulty.EASY, "en", random) }.take(200).filterNotNull().first()
        assertTrue(puzzle.id, Regex("ws-easy-[0-9a-f]+").matches(puzzle.id))
    }

    @Test
    fun bankFileNames() {
        assertEquals("wordswap_puzzles.json", wordSwapBankFile("en"))
        assertEquals("wordswap_puzzles_pl.json", wordSwapBankFile("pl"))
    }
}
