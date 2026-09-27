package com.quietgrid.app.games.themeclear

import android.content.Context
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.themes.ThemeCount
import com.quietgrid.app.core.themes.ThemeCountIndex
import com.quietgrid.app.core.themes.filterPoolByThemes
import com.quietgrid.app.core.themes.themeCountsForLocale
import com.quietgrid.engine.themeclear.ThemeClearDictionary
import com.quietgrid.engine.themeclear.ThemeClearPuzzleEntry
import com.quietgrid.engine.themeclear.parseThemeClearThemes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

object ThemeClearPuzzleBank {
    private var puzzles: List<ThemeClearPuzzleEntry>? = null
    private var dictionaries: Map<String, Map<String, ThemeClearDictionary>>? = null

    private suspend fun loadPuzzles(context: Context): List<ThemeClearPuzzleEntry> {
        puzzles?.let { return it }
        return withContext(Dispatchers.IO) {
            val text = context.assets.open("themeclear_puzzles.json").bufferedReader().use { it.readText() }
            json.decodeFromString(ListSerializer(ThemeClearPuzzleEntry.serializer()), text).also { puzzles = it }
        }
    }

    private suspend fun loadDictionaries(context: Context): Map<String, Map<String, ThemeClearDictionary>> {
        dictionaries?.let { return it }
        return withContext(Dispatchers.IO) {
            val text = context.assets.open("themes.json").bufferedReader().use { it.readText() }
            parseThemeClearThemes(text)
                .mapValues { (_, themes) -> themes.associate { it.themeId to ThemeClearDictionary(it.words) } }
                .also { dictionaries = it }
        }
    }

    suspend fun dailyPool(context: Context, locale: String, difficulty: Difficulty): List<ThemeClearPuzzleEntry> {
        val tier = loadPuzzles(context).filter { it.difficulty == difficulty.key }
        return tier.filter { it.locale == locale }.ifEmpty { tier.filter { it.locale == "en" } }
    }

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

    suspend fun dictionary(context: Context, locale: String, themeId: String): ThemeClearDictionary? =
        loadDictionaries(context)[locale]?.get(themeId)
}
