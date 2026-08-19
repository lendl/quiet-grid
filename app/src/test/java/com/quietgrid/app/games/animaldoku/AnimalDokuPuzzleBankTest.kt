package com.quietgrid.app.games.animaldoku

import com.quietgrid.engine.animaldoku.AnimalDokuPuzzleEntry
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class AnimalDokuPuzzleBankTest {
    @Test
    fun `AnimalDokuPuzzleEntry list decodes from the bundled asset JSON shape`() {
        val json = """[{"id":"ad5-01234","size":5,"difficulty":"easy","regions":[[0,0,1,1,1],[0,0,1,1,1],[0,2,2,1,1],[3,2,2,4,4],[3,3,2,4,4]],"solution":[1,3,0,4,2]}]"""
        val decoded = Json.decodeFromString<List<AnimalDokuPuzzleEntry>>(json)
        assertEquals(1, decoded.size)
        assertEquals(5, decoded[0].size)
        assertEquals("easy", decoded[0].difficulty)
    }
}
