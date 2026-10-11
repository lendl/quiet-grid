package com.quietgrid.app.games.killersudoku

import android.content.Context
import com.quietgrid.app.core.Difficulty
import com.quietgrid.engine.killersudoku.KillerSudokuPuzzleEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

object KillerSudokuPuzzleBank {
    private var cache: Map<String, List<KillerSudokuPuzzleEntry>>? = null

    private suspend fun load(context: Context): Map<String, List<KillerSudokuPuzzleEntry>> {
        cache?.let { return it }
        return withContext(Dispatchers.IO) {
            val text = context.assets.open("killersudoku_puzzles.json").bufferedReader().use { it.readText() }
            val grouped = json.decodeFromString<List<KillerSudokuPuzzleEntry>>(text).groupBy { it.difficulty }
            cache = grouped
            grouped
        }
    }

    suspend fun randomPuzzle(context: Context, difficulty: Difficulty, recentlyPlayedIds: Set<String> = emptySet()): KillerSudokuPuzzleEntry? {
        val pool = load(context)[difficulty.key].orEmpty()
        if (pool.isEmpty()) return null
        return pool.filter { it.id !in recentlyPlayedIds }.ifEmpty { pool }.random()
    }
}
