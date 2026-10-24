package com.quietgrid.app.games.arrowescape

import android.content.Context
import com.quietgrid.app.core.Difficulty
import com.quietgrid.engine.arrowescape.ArrowEscapePuzzleEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

private val json = Json { ignoreUnknownKeys = true }

private const val BANK_ASSET = "arrowescape_puzzles.json"
private const val HALLOWEEN_BANK_ASSET = "arrowescape_halloween_puzzles.json"

object ArrowEscapePuzzleBank {
    private val cache = ConcurrentHashMap<String, Map<String, List<ArrowEscapePuzzleEntry>>>()

    private suspend fun load(context: Context, asset: String): Map<String, List<ArrowEscapePuzzleEntry>> {
        cache[asset]?.let { return it }
        return withContext(Dispatchers.IO) {
            val text = context.assets.open(asset).bufferedReader().use { it.readText() }
            json.decodeFromString<List<ArrowEscapePuzzleEntry>>(text).groupBy { it.difficulty }.also { cache[asset] = it }
        }
    }

    suspend fun randomPuzzle(context: Context, difficulty: Difficulty, recentlyPlayedIds: Set<String> = emptySet()): ArrowEscapePuzzleEntry? {
        val pool = load(context, BANK_ASSET)[difficulty.key] ?: return null
        if (pool.isEmpty()) return null
        val candidates = pool.filter { it.id !in recentlyPlayedIds }
        return candidates.ifEmpty { pool }.random()
    }

    suspend fun halloweenPuzzle(context: Context, difficulty: Difficulty, playedIds: Set<String>): ArrowEscapePuzzleEntry? =
        load(context, HALLOWEEN_BANK_ASSET)[difficulty.key]?.firstOrNull { it.id !in playedIds }
}
