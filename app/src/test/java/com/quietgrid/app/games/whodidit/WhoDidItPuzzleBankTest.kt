package com.quietgrid.app.games.whodidit

import com.quietgrid.engine.whodidit.WhoDidItPuzzleEntry
import java.io.File
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private val json = Json { ignoreUnknownKeys = true }

internal fun loadWhoDidItBankForTest(): List<WhoDidItPuzzleEntry> =
    json.decodeFromString(File("src/main/assets/whodidit_puzzles.json").readText())

class WhoDidItPuzzleBankTest {
    @Test
    fun `bank entry list decodes from the asset JSON shape`() {
        val entries = loadWhoDidItBankForTest()
        assertTrue(entries.isNotEmpty())
        assertEquals(setOf("easy", "medium", "hard", "expert"), entries.map { it.difficulty }.toSet())
        assertTrue(entries.all { isPlayable(it) })
    }

    @Test
    fun `isPlayable rejects unknown theme ids and out-of-range case indexes`() {
        val entry = loadWhoDidItBankForTest().first()
        assertTrue(isPlayable(entry))
        assertFalse(isPlayable(entry.copy(theme = "no-such-theme")))
        assertFalse(isPlayable(entry.copy(case = 9999)))
        assertFalse(isPlayable(entry.copy(case = -1)))
    }
}
