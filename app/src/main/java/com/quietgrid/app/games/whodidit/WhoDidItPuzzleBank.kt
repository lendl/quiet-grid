package com.quietgrid.app.games.whodidit

import com.quietgrid.app.platform.BundledFiles
import com.quietgrid.app.platform.readText
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

    private suspend fun load(files: BundledFiles): Map<String, List<WhoDidItPuzzleEntry>> {
        cache?.let { return it }
        return withContext(Dispatchers.IO) {
            val text = files.readText("whodidit_puzzles.json")
            val entries = json.decodeFromString<List<WhoDidItPuzzleEntry>>(text).filter(::isPlayable)
            val grouped = entries.groupBy { it.difficulty }
            cache = grouped
            grouped
        }
    }

    suspend fun dailyPool(files: BundledFiles, difficulty: Difficulty): List<WhoDidItPuzzleEntry> =
        load(files)[difficulty.key].orEmpty()

    suspend fun randomPuzzle(files: BundledFiles, difficulty: Difficulty, recentlyPlayedIds: Set<String> = emptySet()): WhoDidItPuzzleEntry? {
        val pool = dailyPool(files, difficulty)
        if (pool.isEmpty()) return null
        val candidates = pool.filter { it.id !in recentlyPlayedIds }
        return candidates.ifEmpty { pool }.random()
    }
}
