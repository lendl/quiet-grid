package com.quietgrid.engine.wordguess

import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class WordGuessTypesTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `WordGuessPuzzleEntry round-trips through JSON`() {
        val entry = WordGuessPuzzleEntry(id = "en-easy-apple", locale = "en", difficulty = "easy", word = "apple")
        val encoded = json.encodeToString(entry)
        assertEquals(entry, json.decodeFromString<WordGuessPuzzleEntry>(encoded))
    }

    @Test
    fun `WordGuessDictionaryEntry round-trips through JSON`() {
        val entry = WordGuessDictionaryEntry(locale = "de", word = "weiss")
        val encoded = json.encodeToString(entry)
        assertEquals(entry, json.decodeFromString<WordGuessDictionaryEntry>(encoded))
    }
}
