package com.quietgrid.app.games.wordswap

import com.quietgrid.app.platform.BundledFiles
import com.quietgrid.app.platform.readText
import com.quietgrid.app.core.Difficulty
import com.quietgrid.engine.wordswap.WordSwapPuzzleEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val json = Json { ignoreUnknownKeys = true }

object WordSwapPuzzleBank {
    private val cache = HashMap<String, Map<String, List<WordSwapPuzzleEntry>>>()
    private val cacheLock = Mutex()

    private suspend fun load(files: BundledFiles, locale: String): Map<String, List<WordSwapPuzzleEntry>> {
        cacheLock.withLock { cache[locale] }?.let { return it }
        return withContext(Dispatchers.IO) {
            val text = files.readText(wordSwapBankAsset(locale))
            json.decodeFromString<List<WordSwapPuzzleEntry>>(text).groupBy { it.difficulty }.also { cacheLock.withLock { cache[locale] = it } }
        }
    }

    suspend fun puzzleById(files: BundledFiles, difficulty: Difficulty, id: String): WordSwapPuzzleEntry? =
        load(files, wordSwapLocaleOfId(id))[difficulty.key].orEmpty().firstOrNull { it.id == id }

    suspend fun ratedPool(files: BundledFiles, locale: String, difficulty: Difficulty): List<Pair<String, Int>> =
        load(files, locale)[difficulty.key].orEmpty().mapNotNull { entry -> entry.rating?.let { entry.id to it } }

    suspend fun randomPuzzle(
        files: BundledFiles,
        locale: String,
        difficulty: Difficulty,
        recentlyPlayedIds: Set<String> = emptySet(),
    ): WordSwapPuzzleEntry? {
        val pool = load(files, locale)[difficulty.key] ?: return null
        if (pool.isEmpty()) return null
        val candidates = pool.filter { it.id !in recentlyPlayedIds }
        return candidates.ifEmpty { pool }.random()
    }
}
