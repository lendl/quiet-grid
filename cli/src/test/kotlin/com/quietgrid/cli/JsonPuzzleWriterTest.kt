// cli/src/test/kotlin/com/quietgrid/cli/JsonPuzzleWriterTest.kt
package com.quietgrid.cli

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Files

@Serializable
private data class Fixture(val id: String, val value: Int)

class JsonPuzzleWriterTest {
    @Test
    fun `appendPuzzleEntries creates a new file when none exists`() {
        val dir = Files.createTempDirectory("jpw-test")
        val path = dir.resolve("fixtures.json").toString()

        appendPuzzleEntries(path, listOf(Fixture("a", 1)), Fixture.serializer(), Fixture::id)

        val text = java.io.File(path).readText()
        assertEquals(true, text.contains("\"a\""))
    }

    @Test
    fun `appendPuzzleEntries merges without duplicating an existing id`() {
        val dir = Files.createTempDirectory("jpw-test")
        val path = dir.resolve("fixtures.json").toString()

        appendPuzzleEntries(path, listOf(Fixture("a", 1)), Fixture.serializer(), Fixture::id)
        appendPuzzleEntries(path, listOf(Fixture("a", 2), Fixture("b", 3)), Fixture.serializer(), Fixture::id)

        val json = kotlinx.serialization.json.Json.parseToJsonElement(java.io.File(path).readText())
        assertEquals(2, json.jsonArray.size)
    }
}
