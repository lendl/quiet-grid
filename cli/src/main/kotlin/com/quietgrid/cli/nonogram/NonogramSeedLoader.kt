package com.quietgrid.cli.nonogram

import com.quietgrid.engine.nonogram.NonogramPuzzleEntry
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer
import java.io.File

fun loadNonogramSeeds(assetsDir: String): List<NonogramPuzzleEntry> {
    val file = File("$assetsDir/nonogram_puzzles.json")
    if (!file.exists()) return emptyList()
    val json = Json { ignoreUnknownKeys = true }
    return json.decodeFromString(ListSerializer(NonogramPuzzleEntry.serializer()), file.readText())
}
