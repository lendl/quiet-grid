package com.quietgrid.app.games.starbattle

import android.content.Context
import com.quietgrid.app.core.Difficulty
import com.quietgrid.engine.starbattle.StarBattlePuzzleEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

object StarBattlePuzzleBank {
    private var cache: Map<String, List<StarBattlePuzzleEntry>>? = null

    private suspend fun load(context: Context): Map<String, List<StarBattlePuzzleEntry>> {
        cache?.let { return it }
        return withContext(Dispatchers.IO) {
            val text = context.assets.open("starbattle_puzzles.json").bufferedReader().use { it.readText() }
            val entries = json.decodeFromString<List<StarBattlePuzzleEntry>>(text)
            val grouped = entries.groupBy { it.difficulty }
            cache = grouped
            grouped
        }
    }

    suspend fun puzzleById(context: Context, difficulty: Difficulty, id: String): StarBattlePuzzleEntry? =
        load(context)[difficulty.key]?.firstOrNull { it.id == id }

    suspend fun ratedPool(context: Context, difficulty: Difficulty): List<Pair<String, Int>> =
        load(context)[difficulty.key].orEmpty().mapNotNull { entry -> entry.rating?.let { entry.id to it } }

    suspend fun randomPuzzle(context: Context, difficulty: Difficulty, recentlyPlayedIds: Set<String> = emptySet()): StarBattlePuzzleEntry? {
        val pool = load(context)[difficulty.key] ?: return null
        if (pool.isEmpty()) return null
        val candidates = pool.filter { it.id !in recentlyPlayedIds }
        return candidates.ifEmpty { pool }.random()
    }
}
