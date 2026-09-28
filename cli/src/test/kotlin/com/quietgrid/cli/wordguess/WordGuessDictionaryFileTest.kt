package com.quietgrid.cli.wordguess

import org.junit.Assert.assertEquals
import org.junit.Test

class WordGuessDictionaryFileTest {
    private val dictionary = mapOf("de" to listOf("haben", "weiss"), "nl" to listOf("fiets"))

    @Test
    fun `mergeWordGuessDictionary appends only unseen words to the locale`() {
        val merged = mergeWordGuessDictionary(dictionary, "de", listOf("weiss", "etwas", "etwas"))
        assertEquals(listOf("haben", "weiss", "etwas"), merged["de"])
        assertEquals(listOf("fiets"), merged["nl"])
    }

    @Test
    fun `mergeWordGuessDictionary adds a new locale after the existing ones`() {
        val merged = mergeWordGuessDictionary(dictionary, "pl", listOf("kotek"))
        assertEquals(listOf("de", "nl", "pl"), merged.keys.toList())
        assertEquals(listOf("kotek"), merged["pl"])
    }
}
