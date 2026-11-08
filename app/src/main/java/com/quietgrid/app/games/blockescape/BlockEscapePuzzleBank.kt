package com.quietgrid.app.games.blockescape

import android.content.Context
import com.quietgrid.app.core.Difficulty
import com.quietgrid.engine.blockescape.BlockEscapePuzzleEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

object BlockEscapePuzzleBank {
    private var cache: Map<String, List<BlockEscapePuzzleEntry>>? = null

    private suspend fun load(context: Context): Map<String, List<BlockEscapePuzzleEntry>> {
        cache?.let { return it }
        return withContext(Dispatchers.IO) {
            val text = context.assets.open("blockescape_puzzles.json").bufferedReader().use { it.readText() }
            val grouped = json.decodeFromString<List<BlockEscapePuzzleEntry>>(text).groupBy { it.difficulty }
            cache = grouped
            grouped
        }
    }

    suspend fun randomPuzzle(context: Context, difficulty: Difficulty, recentlyPlayedIds: Set<String> = emptySet()): BlockEscapePuzzleEntry? {
        val pool = load(context)[difficulty.key] ?: return null
        if (pool.isEmpty()) return null
        val candidates = pool.filter { it.id !in recentlyPlayedIds }
        return candidates.ifEmpty { pool }.random()
    }
}
