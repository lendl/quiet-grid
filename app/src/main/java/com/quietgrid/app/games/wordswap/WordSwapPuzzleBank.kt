package com.quietgrid.app.games.wordswap

import android.content.Context
import com.quietgrid.app.core.Difficulty
import com.quietgrid.engine.wordswap.WordSwapPuzzleEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

object WordSwapPuzzleBank {
    private var cache: Map<String, List<WordSwapPuzzleEntry>>? = null

    private suspend fun load(context: Context): Map<String, List<WordSwapPuzzleEntry>> {
        cache?.let { return it }
        return withContext(Dispatchers.IO) {
            val text = context.assets.open("wordswap_puzzles.json").bufferedReader().use { it.readText() }
            val grouped = json.decodeFromString<List<WordSwapPuzzleEntry>>(text).groupBy { it.difficulty }
            cache = grouped
            grouped
        }
    }

    suspend fun puzzleById(context: Context, difficulty: Difficulty, id: String): WordSwapPuzzleEntry? =
        load(context)[difficulty.key].orEmpty().firstOrNull { it.id == id }

    suspend fun ratedPool(context: Context, difficulty: Difficulty): List<Pair<String, Int>> =
        load(context)[difficulty.key].orEmpty().mapNotNull { entry -> entry.rating?.let { entry.id to it } }

    suspend fun randomPuzzle(context: Context, difficulty: Difficulty, recentlyPlayedIds: Set<String> = emptySet()): WordSwapPuzzleEntry? {
        val pool = load(context)[difficulty.key] ?: return null
        if (pool.isEmpty()) return null
        val candidates = pool.filter { it.id !in recentlyPlayedIds }
        return candidates.ifEmpty { pool }.random()
    }
}
