// cli/src/main/kotlin/com/quietgrid/cli/JsonPuzzleWriter.kt
package com.quietgrid.cli

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.builtins.ListSerializer
import java.io.File

private val json = Json { ignoreUnknownKeys = true }

fun <T> readPuzzleEntries(path: String, serializer: KSerializer<T>): List<T> {
    val file = File(path)
    return if (file.exists()) json.decodeFromString(ListSerializer(serializer), file.readText()) else emptyList()
}

fun <T> appendPuzzleEntries(path: String, newEntries: List<T>, serializer: KSerializer<T>, dedupeKey: (T) -> String) {
    val file = File(path)
    val existing = readPuzzleEntries(path, serializer)

    val existingKeys = existing.map(dedupeKey).toSet()
    val merged = existing + newEntries.filter { dedupeKey(it) !in existingKeys }

    file.parentFile?.mkdirs()
    file.writeText(json.encodeToString(ListSerializer(serializer), merged))
}
