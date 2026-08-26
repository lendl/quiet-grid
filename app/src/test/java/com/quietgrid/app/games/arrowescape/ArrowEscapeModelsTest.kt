package com.quietgrid.app.games.arrowescape

import com.quietgrid.engine.arrowescape.ArrowEscapePieceData
import com.quietgrid.engine.arrowescape.ArrowEscapePuzzleEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private val TEST_PUZZLE = ArrowEscapePuzzleEntry(
    id = "test",
    difficulty = "easy",
    rows = 2,
    cols = 1,
    pieces = listOf(
        ArrowEscapePieceData(cells = listOf(listOf(0, 0)), headDirection = "down"),
        ArrowEscapePieceData(cells = listOf(listOf(1, 0)), headDirection = "down"),
    ),
)

class ArrowEscapeModelsTest {
    @Test
    fun `createArrowEscapeSession starts with no removed pieces, 3 lives, and PLAYING status`() {
        val session = createArrowEscapeSession(TEST_PUZZLE)
        assertEquals(3, session.lives)
        assertEquals(ArrowEscapeStatus.PLAYING, session.status)
        assertTrue(session.removedIndices.isEmpty())
        assertNull(session.selectedIndex)
    }
}
