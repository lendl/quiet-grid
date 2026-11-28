package com.quietgrid.app.games.wordsearch

import com.quietgrid.app.platform.BundledFiles
import com.quietgrid.app.platform.readText
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.themes.ThemeCount
import com.quietgrid.app.core.themes.ThemeCountIndex
import com.quietgrid.app.core.themes.filterPoolByThemes
import com.quietgrid.app.core.themes.themeCountsForLocale
import com.quietgrid.engine.wordsearch.WordSearchBankEntry
import com.quietgrid.engine.wordsearch.WordSearchPuzzleEntry
import com.quietgrid.engine.wordsearch.toPuzzleEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.DecodeSequenceMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.okio.decodeBufferedSourceToSequence
import okio.BufferedSource
import okio.Source
import okio.buffer
import okio.use
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val json = Json { ignoreUnknownKeys = true }

@OptIn(ExperimentalSerializationApi::class)
private fun decodeEntries(source: BufferedSource): Sequence<WordSearchBankEntry> =
    json.decodeBufferedSourceToSequence(source, WordSearchBankEntry.serializer(), DecodeSequenceMode.ARRAY_WRAPPED)

internal fun decodeWordSearchPool(open: () -> Source, locale: String, difficulty: String): List<WordSearchPuzzleEntry> {
    val matching = mutableListOf<WordSearchBankEntry>()
    val english = mutableListOf<WordSearchBankEntry>()
    open().buffer().use { source ->
        decodeEntries(source).forEach { entry ->
            if (entry.locale == locale) {
                matching += entry
                english.clear()
            } else if (entry.locale == "en" && matching.isEmpty()) {
                english += entry
            }
        }
    }
    return matching.ifEmpty { english }
        .ifEmpty { open().buffer().use { decodeEntries(it).toList() } }
        .map { it.toPuzzleEntry(difficulty) }
}

object WordSearchPuzzleBank {
    private val cache = HashMap<String, List<WordSearchPuzzleEntry>>()
    private val cacheLock = Mutex()

    suspend fun dailyPool(files: BundledFiles, locale: String, difficulty: Difficulty): List<WordSearchPuzzleEntry> {
        val key = "${difficulty.key}:$locale"
        cacheLock.withLock { cache[key] }?.let { return it }
        return withContext(Dispatchers.IO) {
            decodeWordSearchPool({ files.open("wordsearch_puzzles_${difficulty.key}.json") }, locale, difficulty.key)
                .also { cacheLock.withLock { cache[key] = it } }
        }
    }

    suspend fun dailyPoolSize(files: BundledFiles, locale: String, difficulty: Difficulty): Int =
        themeCounts(files, locale).sumOf { it.perTier[difficulty] ?: 0 }

    suspend fun randomPuzzle(
        files: BundledFiles,
        locale: String,
        difficulty: Difficulty,
        recentlyPlayedIds: Set<String> = emptySet(),
        excludedThemes: Set<String> = emptySet(),
    ): WordSearchPuzzleEntry? {
        val pool = filterPoolByThemes(dailyPool(files, locale, difficulty), excludedThemes) { it.themeId }
        if (pool.isEmpty()) return null
        val candidates = pool.filter { it.id !in recentlyPlayedIds }
        return candidates.ifEmpty { pool }.random()
    }

    suspend fun themeCounts(files: BundledFiles, locale: String): List<ThemeCount> =
        themeCountsForLocale(ThemeCountIndex.forGame(files, "wordsearch"), locale, fallbackToAllLocales = true)

    suspend fun halloweenPuzzle(files: BundledFiles, locale: String, difficulty: Difficulty, playedIds: Set<String>): WordSearchPuzzleEntry? {
        val key = "halloween:${difficulty.key}:$locale"
        val pool = cacheLock.withLock { cache[key] } ?: withContext(Dispatchers.IO) {
            decodeWordSearchPool({ files.open("wordsearch_halloween_puzzles_${difficulty.key}.json") }, locale, difficulty.key)
                .also { cacheLock.withLock { cache[key] = it } }
        }
        return pool.firstOrNull { it.id !in playedIds }
    }
}
