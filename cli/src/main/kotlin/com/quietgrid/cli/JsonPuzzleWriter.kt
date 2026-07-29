// cli/src/main/kotlin/com/quietgrid/cli/JsonPuzzleWriter.kt
package com.quietgrid.cli

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.builtins.ListSerializer
import java.io.File

private val json = Json { ignoreUnknownKeys = true }

/**
 * Appends [newEntries] to the JSON array at [path], creating it if absent. Entries whose
 * [dedupeKey] already exists in the file are skipped (never duplicated, never overwritten).
 */
fun <T> appendPuzzleEntries(path: String, newEntries: List<T>, serializer: KSerializer<T>, dedupeKey: (T) -> String) {
    val listSerializer = ListSerializer(serializer)
    val file = File(path)
    val existing: List<T> = if (file.exists()) {
        json.decodeFromString(listSerializer, file.readText())
    } else {
        emptyList()
    }

    val existingKeys = existing.map(dedupeKey).toSet()
    val merged = existing + newEntries.filter { dedupeKey(it) !in existingKeys }

    file.parentFile?.mkdirs()
    file.writeText(json.encodeToString(listSerializer, merged))
}
