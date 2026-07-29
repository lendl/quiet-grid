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
}
