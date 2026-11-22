package com.quietgrid.engine.wordguess

import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.Test

class WordGuessValidationTest {
    private val dictionary = setOf("apple", "grape", "mango")

    @Test
    fun `isValidGuess returns true for a word in the dictionary`() {
        assertTrue(isValidGuess("apple", dictionary))
    }

    @Test
    fun `isValidGuess returns false for a word not in the dictionary`() {
        assertFalse(isValidGuess("zzzzz", dictionary))
    }

    @Test
    fun `isValidGuess normalizes case before checking`() {
        assertTrue(isValidGuess("APPLE", dictionary))
    }
}
