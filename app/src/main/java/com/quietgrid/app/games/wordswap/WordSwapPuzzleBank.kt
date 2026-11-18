package com.quietgrid.app.games.wordswap

import android.content.Context
import com.quietgrid.app.core.Difficulty
import com.quietgrid.engine.wordswap.WordSwapPuzzleEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

private val json = Json { ignoreUnknownKeys = true }

object WordSwapPuzzleBank {
    private val cache = ConcurrentHashMap<String, Map<String, List<WordSwapPuzzleEntry>>>()

    private suspend fun load(context: Context, locale: String): Map<String, List<WordSwapPuzzleEntry>> {
        cache[locale]?.let { return it }
        return withContext(Dispatchers.IO) {
            val text = context.assets.open(wordSwapBankAsset(locale)).bufferedReader().use { it.readText() }
            json.decodeFromString<List<WordSwapPuzzleEntry>>(text).groupBy { it.difficulty }.also { cache[locale] = it }
        }
    }

    suspend fun puzzleById(context: Context, difficulty: Difficulty, id: String): WordSwapPuzzleEntry? =
        load(context, wordSwapLocaleOfId(id))[difficulty.key].orEmpty().firstOrNull { it.id == id }

    suspend fun ratedPool(context: Context, locale: String, difficulty: Difficulty): List<Pair<String, Int>> =
        load(context, locale)[difficulty.key].orEmpty().mapNotNull { entry -> entry.rating?.let { entry.id to it } }

    suspend fun randomPuzzle(
        context: Context,
        locale: String,
        difficulty: Difficulty,
        recentlyPlayedIds: Set<String> = emptySet(),
    ): WordSwapPuzzleEntry? {
        val pool = load(context, locale)[difficulty.key] ?: return null
        if (pool.isEmpty()) return null
        val candidates = pool.filter { it.id !in recentlyPlayedIds }
        return candidates.ifEmpty { pool }.random()
    }
}
