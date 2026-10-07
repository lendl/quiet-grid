package com.quietgrid.app.games.whodidit

import android.content.Context
import com.quietgrid.app.core.Difficulty
import com.quietgrid.engine.whodidit.WHODIDIT_THEMES
import com.quietgrid.engine.whodidit.WhoDidItPuzzleEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

internal fun isPlayable(entry: WhoDidItPuzzleEntry): Boolean {
    if (WHODIDIT_THEMES.none { it.id == entry.theme }) return false
    val themeRes = WHODIDIT_THEME_RES[entry.theme] ?: return false
    return entry.case >= 0 && entry.case < themeRes.cases.size
}

object WhoDidItPuzzleBank {
    private var cache: Map<String, List<WhoDidItPuzzleEntry>>? = null

    private suspend fun load(context: Context): Map<String, List<WhoDidItPuzzleEntry>> {
        cache?.let { return it }
        return withContext(Dispatchers.IO) {
            val text = context.assets.open("whodidit_puzzles.json").bufferedReader().use { it.readText() }
            val entries = json.decodeFromString<List<WhoDidItPuzzleEntry>>(text).filter(::isPlayable)
            val grouped = entries.groupBy { it.difficulty }
            cache = grouped
            grouped
        }
    }

    suspend fun dailyPool(context: Context, difficulty: Difficulty): List<WhoDidItPuzzleEntry> =
        load(context)[difficulty.key].orEmpty()

    suspend fun randomPuzzle(context: Context, difficulty: Difficulty, recentlyPlayedIds: Set<String> = emptySet()): WhoDidItPuzzleEntry? {
        val pool = dailyPool(context, difficulty)
        if (pool.isEmpty()) return null
        val candidates = pool.filter { it.id !in recentlyPlayedIds }
        return candidates.ifEmpty { pool }.random()
    }
}
