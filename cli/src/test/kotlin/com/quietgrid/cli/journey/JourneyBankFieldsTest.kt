package com.quietgrid.cli.journey

import com.quietgrid.engine.animaldoku.AnimalDokuPuzzleEntry
import com.quietgrid.engine.arrowescape.ArrowEscapePuzzleEntry
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class JourneyBankFieldsTest {
    private val assets = "app/src/main/assets"

    private fun assertKnownFields(file: String, serializer: KSerializer<*>) {
        val known = (0 until serializer.descriptor.elementsCount).map { serializer.descriptor.getElementName(it) }.toSet()
        assertTrue(serializer.descriptor.serialName, "rating" in known)
        Json.parseToJsonElement(File("$assets/$file").readText()).jsonArray.forEach { element ->
            val unknown = element.jsonObject.keys - known
            assertTrue("$file has unknown fields $unknown", unknown.isEmpty())
        }
    }

    @Test
    fun `every committed journey bank object uses only fields its model knows`() {
        assertKnownFields("animaldoku_puzzles.json", AnimalDokuPuzzleEntry.serializer())
        listOf("easy", "medium", "hard", "expert", "halloween").forEach { suffix ->
            val file = if (suffix == "halloween") "arrowescape_halloween_puzzles.json" else "arrowescape_puzzles_$suffix.json"
            assertKnownFields(file, ArrowEscapePuzzleEntry.serializer())
        }
    }
}
