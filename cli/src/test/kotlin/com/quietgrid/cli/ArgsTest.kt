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
            "--out", "shared/src/commonMain/composeResources/files",
        ))
        assertEquals("sudoku", command.game)
        assertEquals("hard", command.difficulty)
        assertEquals(5, command.count)
        assertEquals("shared/src/commonMain/composeResources/files", command.outDir)
    }

    @Test
    fun `defaults count to 1 and out to the bank folder when omitted`() {
        val command = parseArgs(arrayOf("generate", "--game", "takuzu", "--difficulty", "easy"))
        assertEquals(1, command.count)
        assertEquals("shared/src/commonMain/composeResources/files", command.outDir)
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
    fun `parseArgs reads optional theme flag`() {
        assertEquals("castle", parseArgs(arrayOf("generate", "--game", "whodidit", "--difficulty", "easy", "--theme", "castle")).theme)
        assertEquals(null, parseArgs(arrayOf("generate", "--game", "whodidit", "--difficulty", "easy")).theme)
    }

    @Test
    fun `parseArgs leaves fill-to unset and uses every processor when flags are omitted`() {
        val command = parseArgs(arrayOf("generate", "--game", "wordsearch", "--difficulty", "hard"))
        assertEquals(null, command.fillTo)
        assertEquals(Runtime.getRuntime().availableProcessors(), command.threads)
    }

    @Test
    fun `parses journey command with default out and scales paths`() {
        assertEquals(
            com.quietgrid.cli.journey.JourneyArgs("animaldoku", "shared/src/commonMain/composeResources/files", "cli/src/main/resources/journey_rating_scales.json"),
            com.quietgrid.cli.journey.parseJourneyArgs(arrayOf("journey", "--game", "animaldoku")),
        )
    }
}
