package com.quietgrid.app.games.arrowescape

import android.content.Context
import com.quietgrid.app.core.Difficulty
import com.quietgrid.engine.arrowescape.ArrowEscapePuzzleEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

object ArrowEscapePuzzleBank {
    private var cache: Map<String, List<ArrowEscapePuzzleEntry>>? = null

    private suspend fun load(context: Context): Map<String, List<ArrowEscapePuzzleEntry>> {
        cache?.let { return it }
        return withContext(Dispatchers.IO) {
            val text = context.assets.open("arrowescape_puzzles.json").bufferedReader().use { it.readText() }
            val entries = json.decodeFromString<List<ArrowEscapePuzzleEntry>>(text)
            val grouped = entries.groupBy { it.difficulty }
            cache = grouped
            grouped
        }
    }

    suspend fun randomPuzzle(context: Context, difficulty: Difficulty, recentlyPlayedIds: Set<String> = emptySet()): ArrowEscapePuzzleEntry? {
        val pool = load(context)[difficulty.key] ?: return null
        if (pool.isEmpty()) return null
        val candidates = pool.filter { it.id !in recentlyPlayedIds }
        return candidates.ifEmpty { pool }.random()
    }
}
