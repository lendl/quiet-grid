package com.quietgrid.app.games.wordsearch

import android.content.Context
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
import kotlinx.serialization.json.decodeToSequence
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

private val json = Json { ignoreUnknownKeys = true }

@OptIn(ExperimentalSerializationApi::class)
private fun decodeEntries(stream: InputStream): Sequence<WordSearchBankEntry> =
    json.decodeToSequence(stream, WordSearchBankEntry.serializer(), DecodeSequenceMode.ARRAY_WRAPPED)

internal fun decodeWordSearchPool(open: () -> InputStream, locale: String, difficulty: String): List<WordSearchPuzzleEntry> {
    val matching = mutableListOf<WordSearchBankEntry>()
    val english = mutableListOf<WordSearchBankEntry>()
    open().use { stream ->
        decodeEntries(stream).forEach { entry ->
            if (entry.locale == locale) {
                matching += entry
                english.clear()
            } else if (entry.locale == "en" && matching.isEmpty()) {
                english += entry
            }
        }
    }
    return matching.ifEmpty { english }
        .ifEmpty { open().use { decodeEntries(it).toList() } }
        .map { it.toPuzzleEntry(difficulty) }
}

object WordSearchPuzzleBank {
    private val cache = ConcurrentHashMap<String, List<WordSearchPuzzleEntry>>()

    suspend fun dailyPool(context: Context, locale: String, difficulty: Difficulty): List<WordSearchPuzzleEntry> {
        val key = "${difficulty.key}:$locale"
        cache[key]?.let { return it }
        return withContext(Dispatchers.IO) {
            decodeWordSearchPool({ context.assets.open("wordsearch_puzzles_${difficulty.key}.json") }, locale, difficulty.key)
                .also { cache[key] = it }
        }
    }

    suspend fun dailyPoolSize(context: Context, locale: String, difficulty: Difficulty): Int =
        themeCounts(context, locale).sumOf { it.perTier[difficulty] ?: 0 }

    suspend fun randomPuzzle(
        context: Context,
        locale: String,
        difficulty: Difficulty,
        recentlyPlayedIds: Set<String> = emptySet(),
        excludedThemes: Set<String> = emptySet(),
    ): WordSearchPuzzleEntry? {
        val pool = filterPoolByThemes(dailyPool(context, locale, difficulty), excludedThemes) { it.themeId }
        if (pool.isEmpty()) return null
        val candidates = pool.filter { it.id !in recentlyPlayedIds }
        return candidates.ifEmpty { pool }.random()
    }

    suspend fun themeCounts(context: Context, locale: String): List<ThemeCount> =
        themeCountsForLocale(ThemeCountIndex.forGame(context, "wordsearch"), locale, fallbackToAllLocales = true)
}
