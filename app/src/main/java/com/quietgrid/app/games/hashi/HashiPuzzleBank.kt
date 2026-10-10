package com.quietgrid.app.games.hashi

import android.content.Context
import com.quietgrid.app.core.Difficulty
import com.quietgrid.engine.hashi.HashiPuzzleEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

object HashiPuzzleBank {
    private var cache: Map<String, List<HashiPuzzleEntry>>? = null

    private suspend fun load(context: Context): Map<String, List<HashiPuzzleEntry>> {
        cache?.let { return it }
        return withContext(Dispatchers.IO) {
            val text = context.assets.open("hashi_puzzles.json").bufferedReader().use { it.readText() }
            val grouped = json.decodeFromString<List<HashiPuzzleEntry>>(text).groupBy { it.difficulty }
            cache = grouped
            grouped
        }
    }

    suspend fun randomPuzzle(context: Context, difficulty: Difficulty, recentlyPlayedIds: Set<String> = emptySet()): HashiPuzzleEntry? {
        val pool = load(context)[difficulty.key] ?: return null
        if (pool.isEmpty()) return null
        val candidates = pool.filter { it.id !in recentlyPlayedIds }
        return candidates.ifEmpty { pool }.random()
    }
}
