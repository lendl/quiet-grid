package com.quietgrid.app.games.whodidit

import com.quietgrid.engine.whodidit.WhoDidItCategory
import com.quietgrid.engine.whodidit.WhoDidItPuzzleEntry
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

internal val WHODIDIT_TEST_PUZZLE = WhoDidItPuzzleEntry(
    id = "test",
    difficulty = "easy",
    theme = "bakery",
    case = 0,
    size = 3,
    categories = listOf(WhoDidItCategory.SUSPECT, WhoDidItCategory.PLACE, WhoDidItCategory.ITEM),
    entries = listOf(listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8)),
    solution = listOf(listOf(0, 1, 2), listOf(1, 2, 0), listOf(2, 0, 1)),
    culprit = 0,
    clues = emptyList(),
)

private fun tapAll(taps: List<Triple<Int, Int, Int>>): WhoDidItSession =
    taps.fold(createWhoDidItSession(WHODIDIT_TEST_PUZZLE)) { s, (b, r, c) ->
        applyWhoDidItTap(s, b, r, c, false)?.session ?: s
    }

class WhoDidItModelsTest {
    @Test
    fun `encode then decode restores O, X and AUTO_X exactly`() {
        val tapped = tapAll(
            listOf(
                Triple(0, 1, 0), Triple(0, 1, 0), Triple(0, 0, 2),
                Triple(1, 2, 2), Triple(1, 2, 2), Triple(1, 0, 0),
            ),
        )
        val decoded = decodeWhoDidItMarks(encodeWhoDidItMarks(tapped.blocks), WHODIDIT_TEST_PUZZLE)
        assertEquals(tapped.blocks, decoded)
        val marks = decoded.flatten().flatten()
        assertTrue(WhoDidItCellMark.O in marks)
        assertTrue(WhoDidItCellMark.X in marks)
        assertTrue(WhoDidItCellMark.AUTO_X in marks)
    }

    @Test
    fun `persisted session JSON round-trips`() {
        val tapped = tapAll(listOf(Triple(0, 0, 0), Triple(0, 0, 0), Triple(1, 1, 1)))
        val persisted = WhoDidItPersistedSession(
            WHODIDIT_TEST_PUZZLE,
            encodeWhoDidItMarks(tapped.blocks),
            tapped.status.name,
        )
        val json = Json { ignoreUnknownKeys = true }
        val text = json.encodeToString(WhoDidItPersistedSession.serializer(), persisted)
        assertEquals(persisted, json.decodeFromString(WhoDidItPersistedSession.serializer(), text))
    }
}
