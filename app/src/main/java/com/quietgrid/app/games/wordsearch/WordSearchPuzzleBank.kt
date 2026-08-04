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
    private val lastPickedId = mutableMapOf<String, String>()

    private suspend fun load(context: Context, difficulty: Difficulty): List<WordSearchPuzzleEntry> {
        cache[difficulty.key]?.let { return it }
        return withContext(Dispatchers.IO) {
            val text = context.assets.open("wordsearch_puzzles_${difficulty.key}.json").bufferedReader().use { it.readText() }
            val entries = json.decodeFromString<List<WordSearchPuzzleEntry>>(text)
            cache[difficulty.key] = entries
            entries
        }
    }

    suspend fun randomPuzzle(context: Context, difficulty: Difficulty): WordSearchPuzzleEntry? {
        val pool = load(context, difficulty)
        if (pool.isEmpty()) return null
        val lastId = lastPickedId[difficulty.key]
        val candidates = if (pool.size > 1 && lastId != null) pool.filter { it.id != lastId } else pool
        val choices = candidates.ifEmpty { pool }
        val chosen = choices.random()
        lastPickedId[difficulty.key] = chosen.id
        return chosen
    }
}
