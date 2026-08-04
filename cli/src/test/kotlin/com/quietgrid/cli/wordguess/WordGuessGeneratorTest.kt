package com.quietgrid.cli.wordguess

import com.quietgrid.cli.GenerationState
import com.quietgrid.engine.core.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Files

class WordGuessGeneratorTest {
    private fun tiers(common: List<String>, full: List<String>) =
        WordGuessTieredWords(common = common, full = full, dictionary = full.toSet())

    @Test
    fun `generateWordGuessAnswerEntries picks from the common tier for easy`() {
        val tiers5 = tiers(common = listOf("apple", "grape"), full = listOf("apple", "grape", "mango"))
        val tiers6 = tiers(common = listOf("banana"), full = listOf("banana", "orange"))
        val stateFile = Files.createTempDirectory("wordguess-test").resolve("state.json").toString()
        val state = GenerationState(stateFile)

        val entries = generateWordGuessAnswerEntries("en", Difficulty.EASY, tiers5, tiers6, count = 10, state)

        assertEquals(listOf("apple", "grape"), entries.map { it.word })
        assertEquals(listOf("en", "en"), entries.map { it.locale })
        assertEquals(listOf("easy", "easy"), entries.map { it.difficulty })
    }

    @Test
    fun `generateWordGuessAnswerEntries picks from the full tier for medium, respects count`() {
        val tiers5 = tiers(common = listOf("apple"), full = listOf("apple", "grape", "mango"))
        val tiers6 = tiers(common = emptyList(), full = emptyList())
        val stateFile = Files.createTempDirectory("wordguess-test").resolve("state.json").toString()
        val state = GenerationState(stateFile)

        val entries = generateWordGuessAnswerEntries("en", Difficulty.MEDIUM, tiers5, tiers6, count = 2, state)

        assertEquals(listOf("apple", "grape"), entries.map { it.word })
    }

    @Test
    fun `generateWordGuessAnswerEntries skips words already recorded in state`() {
        val tiers5 = tiers(common = listOf("apple", "grape"), full = listOf("apple", "grape"))
        val tiers6 = tiers(common = emptyList(), full = emptyList())
        val stateFile = Files.createTempDirectory("wordguess-test").resolve("state.json").toString()
        val state = GenerationState(stateFile)
        state.recordTried("en:easy:apple", "valid")

        val entries = generateWordGuessAnswerEntries("en", Difficulty.EASY, tiers5, tiers6, count = 10, state)

        assertEquals(listOf("grape"), entries.map { it.word })
    }
}
