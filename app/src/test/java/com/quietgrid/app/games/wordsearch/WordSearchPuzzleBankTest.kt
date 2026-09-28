package com.quietgrid.app.games.wordsearch

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream

class WordSearchPuzzleBankTest {
    private fun entryJson(id: String, locale: String?): String {
        val localeField = locale?.let { ""","locale":"$it"""" } ?: ""
        return """{"id":"$id","rows":1,"cols":3,"themeId":"animals","words":[],"hiddenWord":"CAT"$localeField}"""
    }

    private fun bank(vararg entries: Pair<String, String?>): () -> InputStream {
        val text = entries.joinToString(",", prefix = "[", postfix = "]") { (id, locale) -> entryJson(id, locale) }
        return { ByteArrayInputStream(text.toByteArray()) }
    }

    @Test
    fun `decodeWordSearchPool keeps only the requested locale in file order`() {
        val pool = decodeWordSearchPool(bank("1" to "en", "2" to "nl", "3" to "de", "4" to "nl"), "nl", "hard")
        assertEquals(listOf("2", "4"), pool.map { it.id })
    }

    @Test
    fun `decodeWordSearchPool treats entries without a locale as english`() {
        val pool = decodeWordSearchPool(bank("1" to null, "2" to "nl", "3" to "en"), "en", "hard")
        assertEquals(listOf("1", "3"), pool.map { it.id })
    }

    @Test
    fun `decodeWordSearchPool falls back to english when the locale has no puzzles`() {
        val pool = decodeWordSearchPool(bank("1" to "en", "2" to "nl", "3" to "en"), "pt", "hard")
        assertEquals(listOf("1", "3"), pool.map { it.id })
    }

    @Test
    fun `decodeWordSearchPool falls back to every puzzle when neither the locale nor english exist`() {
        val pool = decodeWordSearchPool(bank("1" to "nl", "2" to "de"), "pt", "hard")
        assertEquals(listOf("1", "2"), pool.map { it.id })
    }
}
