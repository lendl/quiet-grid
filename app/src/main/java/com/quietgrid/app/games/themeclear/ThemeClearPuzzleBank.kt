package com.quietgrid.app.games.themeclear

import android.content.Context
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.themes.ThemeCount
import com.quietgrid.app.core.themes.ThemeCountIndex
import com.quietgrid.app.core.themes.filterPoolByThemes
import com.quietgrid.app.core.themes.themeCountsForLocale
import com.quietgrid.engine.themeclear.ThemeClearBankEntry
import com.quietgrid.engine.themeclear.ThemeClearDictionary
import com.quietgrid.engine.themeclear.ThemeClearPuzzleEntry
import com.quietgrid.engine.themeclear.parseThemeClearThemes
import com.quietgrid.engine.themeclear.toPuzzleEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

private val json = Json { ignoreUnknownKeys = true }

object ThemeClearPuzzleBank {
    private var puzzles: Pair<String, List<ThemeClearPuzzleEntry>>? = null
    private var themeWords: Map<String, Map<String, List<String>>>? = null
    private val dictionaries = ConcurrentHashMap<String, ThemeClearDictionary>()

    private suspend fun loadPuzzles(context: Context, locale: String): List<ThemeClearPuzzleEntry> {
        puzzles?.takeIf { it.first == locale }?.let { return it.second }
        return withContext(Dispatchers.IO) {
            val text = context.assets.open("themeclear_puzzles.json").bufferedReader().use { it.readText() }
            json.decodeFromString(ListSerializer(ThemeClearBankEntry.serializer()), text)
                .groupBy { it.difficulty }
                .values
                .flatMap { tier -> tier.filter { it.locale == locale }.ifEmpty { tier.filter { it.locale == "en" } } }
                .map { it.toPuzzleEntry() }
                .also { puzzles = locale to it }
        }
    }

    private suspend fun loadThemeWords(context: Context): Map<String, Map<String, List<String>>> {
        themeWords?.let { return it }
        return withContext(Dispatchers.IO) {
            val text = context.assets.open("themes.json").bufferedReader().use { it.readText() }
            parseThemeClearThemes(text)
                .mapValues { (_, themes) -> themes.associate { it.themeId to it.words } }
                .also { themeWords = it }
        }
    }

    suspend fun dailyPool(context: Context, locale: String, difficulty: Difficulty): List<ThemeClearPuzzleEntry> =
        loadPuzzles(context, locale).filter { it.difficulty == difficulty.key }

    suspend fun randomPuzzle(
        context: Context,
        locale: String,
        difficulty: Difficulty,
        recentlyPlayedIds: Set<String> = emptySet(),
        excludedThemes: Set<String> = emptySet(),
    ): ThemeClearPuzzleEntry? {
        val pool = filterPoolByThemes(dailyPool(context, locale, difficulty), excludedThemes) { it.themeId }
        if (pool.isEmpty()) return null
        return pool.filter { it.id !in recentlyPlayedIds }.ifEmpty { pool }.random()
    }

    suspend fun themeCounts(context: Context, locale: String): List<ThemeCount> =
        themeCountsForLocale(ThemeCountIndex.forGame(context, "themeclear"), locale, fallbackToAllLocales = false)

    suspend fun dictionary(context: Context, locale: String, themeId: String): ThemeClearDictionary? {
        val key = "$locale:$themeId"
        dictionaries[key]?.let { return it }
        val words = loadThemeWords(context)[locale]?.get(themeId) ?: return null
        return withContext(Dispatchers.Default) { ThemeClearDictionary(words) }.also { dictionaries[key] = it }
    }
}
