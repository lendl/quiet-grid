package com.quietgrid.app.games.circuit

import android.content.Context
import com.quietgrid.app.core.Difficulty
import com.quietgrid.engine.circuit.CircuitPuzzleEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

object CircuitPuzzleBank {
    private var cache: Map<String, List<CircuitPuzzleEntry>>? = null

    private suspend fun load(context: Context): Map<String, List<CircuitPuzzleEntry>> {
        cache?.let { return it }
        return withContext(Dispatchers.IO) {
            val text = context.assets.open("circuit_puzzles.json").bufferedReader().use { it.readText() }
            val grouped = json.decodeFromString<List<CircuitPuzzleEntry>>(text).groupBy { it.difficulty }
            cache = grouped
            grouped
        }
    }

    suspend fun randomPuzzle(context: Context, difficulty: Difficulty, recentlyPlayedIds: Set<String> = emptySet()): CircuitPuzzleEntry? {
        val pool = load(context)[difficulty.key] ?: return null
        if (pool.isEmpty()) return null
        val candidates = pool.filter { it.id !in recentlyPlayedIds }
        return candidates.ifEmpty { pool }.random()
    }
}
