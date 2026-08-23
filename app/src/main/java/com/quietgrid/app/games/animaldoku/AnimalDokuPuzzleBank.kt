package com.quietgrid.app.games.animaldoku

import android.content.Context
import com.quietgrid.app.core.Difficulty
import com.quietgrid.engine.animaldoku.AnimalDokuPuzzleEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

object AnimalDokuPuzzleBank {
    private var cache: Map<String, List<AnimalDokuPuzzleEntry>>? = null

    private suspend fun load(context: Context): Map<String, List<AnimalDokuPuzzleEntry>> {
        cache?.let { return it }
        return withContext(Dispatchers.IO) {
            val text = context.assets.open("animaldoku_puzzles.json").bufferedReader().use { it.readText() }
            val entries = json.decodeFromString<List<AnimalDokuPuzzleEntry>>(text)
            val grouped = entries.groupBy { it.difficulty }
            cache = grouped
            grouped
        }
    }

    suspend fun randomPuzzle(context: Context, difficulty: Difficulty, recentlyPlayedIds: Set<String> = emptySet()): AnimalDokuPuzzleEntry? {
        val pool = load(context)[difficulty.key] ?: return null
        if (pool.isEmpty()) return null
        val candidates = pool.filter { it.id !in recentlyPlayedIds }
        return candidates.ifEmpty { pool }.random()
    }
}
