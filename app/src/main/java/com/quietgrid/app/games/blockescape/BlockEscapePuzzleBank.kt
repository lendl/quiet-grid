package com.quietgrid.app.games.blockescape

import com.quietgrid.app.platform.BundledFiles
import com.quietgrid.app.platform.readText
import com.quietgrid.app.core.Difficulty
import com.quietgrid.engine.blockescape.BlockEscapePuzzleEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

object BlockEscapePuzzleBank {
    private var cache: Map<String, List<BlockEscapePuzzleEntry>>? = null

    private suspend fun load(files: BundledFiles): Map<String, List<BlockEscapePuzzleEntry>> {
        cache?.let { return it }
        return withContext(Dispatchers.IO) {
            val text = files.readText("blockescape_puzzles.json")
            val grouped = json.decodeFromString<List<BlockEscapePuzzleEntry>>(text).groupBy { it.difficulty }
            cache = grouped
            grouped
        }
    }

    suspend fun puzzleById(files: BundledFiles, difficulty: Difficulty, id: String): BlockEscapePuzzleEntry? =
        load(files)[difficulty.key].orEmpty().firstOrNull { it.id == id }

    suspend fun ratedPool(files: BundledFiles, difficulty: Difficulty): List<Pair<String, Int>> =
        load(files)[difficulty.key].orEmpty().mapNotNull { entry -> entry.rating?.let { entry.id to it } }

    suspend fun randomPuzzle(files: BundledFiles, difficulty: Difficulty, recentlyPlayedIds: Set<String> = emptySet()): BlockEscapePuzzleEntry? {
        val pool = load(files)[difficulty.key] ?: return null
        if (pool.isEmpty()) return null
        val candidates = pool.filter { it.id !in recentlyPlayedIds }
        return candidates.ifEmpty { pool }.random()
    }
}
