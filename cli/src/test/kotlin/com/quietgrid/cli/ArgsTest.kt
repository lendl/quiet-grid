package com.quietgrid.cli

import org.junit.Assert.assertEquals
import org.junit.Test

class ArgsTest {
    @Test
    fun `parses generate command flags`() {
        val command = parseArgs(arrayOf(
            "generate",
            "--game", "sudoku",
            "--difficulty", "hard",
            "--count", "5",
            "--out", "app/src/main/assets",
        ))
        assertEquals("sudoku", command.game)
        assertEquals("hard", command.difficulty)
        assertEquals(5, command.count)
        assertEquals("app/src/main/assets", command.outDir)
    }

    @Test
    fun `defaults count to 1 and out to app assets dir when omitted`() {
        val command = parseArgs(arrayOf("generate", "--game", "takuzu", "--difficulty", "easy"))
        assertEquals(1, command.count)
        assertEquals("app/src/main/assets", command.outDir)
    }

    @Test
    fun `parseArgs reads an optional locale flag, defaulting to en`() {
        val withLocale = parseArgs(arrayOf("generate", "--game", "wordguess", "--difficulty", "easy", "--locale", "de"))
        assertEquals("de", withLocale.locale)

        val withoutLocale = parseArgs(arrayOf("generate", "--game", "wordguess", "--difficulty", "easy"))
        assertEquals("en", withoutLocale.locale)
    }

    @Test
    fun `parseArgs reads optional fill-to and threads flags`() {
        val command = parseArgs(arrayOf("generate", "--game", "wordsearch", "--difficulty", "hard", "--fill-to", "30", "--threads", "6"))
        assertEquals(30, command.fillTo)
        assertEquals(6, command.threads)
    }

    @Test
    fun `parseArgs leaves fill-to unset and uses every processor when flags are omitted`() {
        val command = parseArgs(arrayOf("generate", "--game", "wordsearch", "--difficulty", "hard"))
        assertEquals(null, command.fillTo)
        assertEquals(Runtime.getRuntime().availableProcessors(), command.threads)
    }
}
