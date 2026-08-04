package com.quietgrid.cli.wordguess

import org.junit.Assert.assertEquals
import org.junit.Test

class WordGuessFrequencyCorpusTest {
    @Test
    fun `loadWordGuessFrequencyWords reads words in file order, lowercased, dropping the count column`() {
        val words = loadWordGuessFrequencyWords("xx")
        assertEquals(listOf("the", "apple", "grape", "a1b2c", "hi"), words)
    }

    @Test(expected = IllegalStateException::class)
    fun `loadWordGuessFrequencyWords errors for an unknown locale`() {
        loadWordGuessFrequencyWords("zz")
    }
}
