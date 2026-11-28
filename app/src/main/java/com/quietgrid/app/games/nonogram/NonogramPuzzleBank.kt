package com.quietgrid.app.games.nonogram

import com.quietgrid.app.platform.BundledFiles
import com.quietgrid.app.platform.readText
import com.quietgrid.app.core.Difficulty
import com.quietgrid.engine.nonogram.NonogramPuzzleEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

object NonogramPuzzleBank {
    private var cache: Map<String, List<NonogramPuzzleEntry>>? = null

    private suspend fun load(files: BundledFiles): Map<String, List<NonogramPuzzleEntry>> {
        cache?.let { return it }
        return withContext(Dispatchers.IO) {
            val text = files.readText("nonogram_puzzles.json")
            val entries = json.decodeFromString<List<NonogramPuzzleEntry>>(text)
            val grouped = entries.groupBy { it.difficulty }
            cache = grouped
            grouped
        }
    }

    suspend fun puzzleById(files: BundledFiles, difficulty: Difficulty, id: String): NonogramPuzzleEntry? =
        load(files)[difficulty.key]?.firstOrNull { it.id == id }

    suspend fun ratedPool(files: BundledFiles, difficulty: Difficulty): List<Pair<String, Int>> =
        load(files)[difficulty.key].orEmpty().mapNotNull { entry -> entry.rating?.let { entry.id to it } }

    suspend fun randomPuzzle(files: BundledFiles, difficulty: Difficulty, recentlyPlayedIds: Set<String> = emptySet()): NonogramPuzzleEntry? {
        val pool = load(files)[difficulty.key] ?: return null
        if (pool.isEmpty()) return null
        val candidates = pool.filter { it.id !in recentlyPlayedIds }
        return candidates.ifEmpty { pool }.random()
    }
}
