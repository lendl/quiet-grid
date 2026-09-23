package com.quietgrid.app.games.battleship

import android.content.Context
import com.quietgrid.app.core.Difficulty
import com.quietgrid.engine.battleship.BattleshipPuzzleEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

object BattleshipPuzzleBank {
    private var cache: Map<String, List<BattleshipPuzzleEntry>>? = null

    private suspend fun load(context: Context): Map<String, List<BattleshipPuzzleEntry>> {
        cache?.let { return it }
        return withContext(Dispatchers.IO) {
            val text = context.assets.open("battleship_puzzles.json").bufferedReader().use { it.readText() }
            val entries = json.decodeFromString<List<BattleshipPuzzleEntry>>(text)
            val grouped = entries.groupBy { it.difficulty }
            cache = grouped
            grouped
        }
    }

    suspend fun randomPuzzle(context: Context, difficulty: Difficulty, recentlyPlayedIds: Set<String> = emptySet()): BattleshipPuzzleEntry? {
        val pool = load(context)[difficulty.key] ?: return null
        if (pool.isEmpty()) return null
        val candidates = pool.filter { it.id !in recentlyPlayedIds }
        return candidates.ifEmpty { pool }.random()
    }
}
