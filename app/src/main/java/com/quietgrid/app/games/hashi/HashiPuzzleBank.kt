package com.quietgrid.app.games.hashi

import com.quietgrid.app.platform.BundledFiles
import com.quietgrid.app.platform.readText
import com.quietgrid.app.core.Difficulty
import com.quietgrid.engine.hashi.HashiPuzzleEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

object HashiPuzzleBank {
    private var cache: Map<String, List<HashiPuzzleEntry>>? = null

    private suspend fun load(files: BundledFiles): Map<String, List<HashiPuzzleEntry>> {
        cache?.let { return it }
        return withContext(Dispatchers.IO) {
            val text = files.readText("hashi_puzzles.json")
            val grouped = json.decodeFromString<List<HashiPuzzleEntry>>(text).groupBy { it.difficulty }
            cache = grouped
            grouped
        }
    }

    suspend fun randomPuzzle(files: BundledFiles, difficulty: Difficulty, recentlyPlayedIds: Set<String> = emptySet()): HashiPuzzleEntry? {
        val pool = load(files)[difficulty.key] ?: return null
        if (pool.isEmpty()) return null
        val candidates = pool.filter { it.id !in recentlyPlayedIds }
        return candidates.ifEmpty { pool }.random()
    }
}
