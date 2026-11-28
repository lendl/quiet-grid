package com.quietgrid.app.games.themeclear

import com.quietgrid.app.platform.BundledFiles
import com.quietgrid.app.platform.readText
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.themes.ThemeCount
import com.quietgrid.app.core.themes.ThemeCountIndex
import com.quietgrid.app.core.themes.ThemeWordIndex
import com.quietgrid.app.core.themes.filterPoolByThemes
import com.quietgrid.app.core.themes.themeCountsForLocale
import com.quietgrid.engine.themeclear.ThemeClearBankEntry
import com.quietgrid.engine.themeclear.ThemeClearDictionary
import com.quietgrid.engine.themeclear.ThemeClearPuzzleEntry
import com.quietgrid.engine.themeclear.toPuzzleEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val json = Json { ignoreUnknownKeys = true }

object ThemeClearPuzzleBank {
    private var puzzles: Pair<String, List<ThemeClearPuzzleEntry>>? = null
    private val dictionaries = HashMap<String, ThemeClearDictionary>()
    private val dictionariesLock = Mutex()

    private suspend fun loadPuzzles(files: BundledFiles, locale: String): List<ThemeClearPuzzleEntry> {
        puzzles?.takeIf { it.first == locale }?.let { return it.second }
        return withContext(Dispatchers.IO) {
            val text = files.readText("themeclear_puzzles.json")
            json.decodeFromString(ListSerializer(ThemeClearBankEntry.serializer()), text)
                .groupBy { it.difficulty }
                .values
                .flatMap { tier -> tier.filter { it.locale == locale }.ifEmpty { tier.filter { it.locale == "en" } } }
                .map { it.toPuzzleEntry() }
                .also { puzzles = locale to it }
        }
    }

    suspend fun dailyPool(files: BundledFiles, locale: String, difficulty: Difficulty): List<ThemeClearPuzzleEntry> =
        loadPuzzles(files, locale).filter { it.difficulty == difficulty.key }

    suspend fun randomPuzzle(
        files: BundledFiles,
        locale: String,
        difficulty: Difficulty,
        recentlyPlayedIds: Set<String> = emptySet(),
        excludedThemes: Set<String> = emptySet(),
    ): ThemeClearPuzzleEntry? {
        val pool = filterPoolByThemes(dailyPool(files, locale, difficulty), excludedThemes) { it.themeId }
        if (pool.isEmpty()) return null
        return pool.filter { it.id !in recentlyPlayedIds }.ifEmpty { pool }.random()
    }

    suspend fun themeCounts(files: BundledFiles, locale: String): List<ThemeCount> =
        themeCountsForLocale(ThemeCountIndex.forGame(files, "themeclear"), locale, fallbackToAllLocales = false)

    suspend fun dictionary(files: BundledFiles, locale: String, themeId: String): ThemeClearDictionary? {
        val key = "$locale:$themeId"
        dictionariesLock.withLock { dictionaries[key] }?.let { return it }
        val words = ThemeWordIndex.all(files)[locale]?.get(themeId) ?: return null
        return withContext(Dispatchers.Default) { ThemeClearDictionary(words) }.also { dictionariesLock.withLock { dictionaries[key] = it } }
    }
}
