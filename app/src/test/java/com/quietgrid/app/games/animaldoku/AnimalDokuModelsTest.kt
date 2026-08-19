// app/src/test/java/com/quietgrid/app/games/animaldoku/AnimalDokuModelsTest.kt
package com.quietgrid.app.games.animaldoku

import com.quietgrid.engine.animaldoku.AnimalDokuPuzzleEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private val TEST_PUZZLE = AnimalDokuPuzzleEntry(
    id = "test",
    size = 5,
    difficulty = "easy",
    regions = List(5) { row -> List(5) { row } },
    solution = listOf(0, 1, 2, 3, 4),
)

class AnimalDokuModelsTest {
    @Test
    fun `createAnimalDokuSession starts with an all-empty board, 3 lives, and PLAYING status`() {
        val session = createAnimalDokuSession(TEST_PUZZLE)
        assertEquals(3, session.lives)
        assertEquals(AnimalDokuStatus.PLAYING, session.status)
        assertTrue(session.cells.all { row -> row.all { it == AnimalDokuCellState.EMPTY } })
        assertEquals(5, session.cells.size)
    }
}
