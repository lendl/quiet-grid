package com.quietgrid.app.games.circuit

import com.quietgrid.app.platform.BundledFiles
import com.quietgrid.app.platform.readText
import com.quietgrid.app.core.Difficulty
import com.quietgrid.engine.circuit.CircuitPuzzleEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

object CircuitPuzzleBank {
    private var cache: Map<String, List<CircuitPuzzleEntry>>? = null

    private suspend fun load(files: BundledFiles): Map<String, List<CircuitPuzzleEntry>> {
        cache?.let { return it }
        return withContext(Dispatchers.IO) {
            val text = files.readText("circuit_puzzles.json")
            val grouped = json.decodeFromString<List<CircuitPuzzleEntry>>(text).groupBy { it.difficulty }
            cache = grouped
            grouped
        }
    }

    suspend fun puzzleById(files: BundledFiles, difficulty: Difficulty, id: String): CircuitPuzzleEntry? =
        load(files)[difficulty.key].orEmpty().firstOrNull { it.id == id }

    suspend fun ratedPool(files: BundledFiles, difficulty: Difficulty): List<Pair<String, Int>> =
        load(files)[difficulty.key].orEmpty().mapNotNull { entry -> entry.rating?.let { entry.id to it } }

    suspend fun randomPuzzle(files: BundledFiles, difficulty: Difficulty, recentlyPlayedIds: Set<String> = emptySet()): CircuitPuzzleEntry? {
        val pool = load(files)[difficulty.key] ?: return null
        if (pool.isEmpty()) return null
        val candidates = pool.filter { it.id !in recentlyPlayedIds }
        return candidates.ifEmpty { pool }.random()
    }
}
