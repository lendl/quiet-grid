package com.quietgrid.app.games.wordsearch

import android.content.Context
import com.quietgrid.app.core.Difficulty
import com.quietgrid.engine.wordsearch.WordSearchPuzzleEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

object WordSearchPuzzleBank {
    private val cache = mutableMapOf<String, List<WordSearchPuzzleEntry>>()

    private suspend fun load(context: Context, difficulty: Difficulty): List<WordSearchPuzzleEntry> {
        cache[difficulty.key]?.let { return it }
        return withContext(Dispatchers.IO) {
            val text = context.assets.open("wordsearch_puzzles_${difficulty.key}.json").bufferedReader().use { it.readText() }
            val entries = json.decodeFromString<List<WordSearchPuzzleEntry>>(text)
            cache[difficulty.key] = entries
            entries
        }
    }

    suspend fun randomPuzzle(
        context: Context,
        locale: String,
        difficulty: Difficulty,
        recentlyPlayedIds: Set<String> = emptySet(),
    ): WordSearchPuzzleEntry? {
        val all = load(context, difficulty)
        if (all.isEmpty()) return null
        val pool = all.filter { it.locale == locale }.ifEmpty { all.filter { it.locale == "en" } }.ifEmpty { all }
        val candidates = pool.filter { it.id !in recentlyPlayedIds }
        return candidates.ifEmpty { pool }.random()
    }
}
