package com.quietgrid.app.games.killersudoku

import com.quietgrid.app.platform.BundledFiles
import com.quietgrid.app.platform.readText
import com.quietgrid.app.core.Difficulty
import com.quietgrid.engine.killersudoku.KillerSudokuPuzzleEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

object KillerSudokuPuzzleBank {
    private var cache: Map<String, List<KillerSudokuPuzzleEntry>>? = null

    private suspend fun load(files: BundledFiles): Map<String, List<KillerSudokuPuzzleEntry>> {
        cache?.let { return it }
        return withContext(Dispatchers.IO) {
            val text = files.readText("killersudoku_puzzles.json")
            val grouped = json.decodeFromString<List<KillerSudokuPuzzleEntry>>(text).groupBy { it.difficulty }
            cache = grouped
            grouped
        }
    }

    suspend fun puzzleById(files: BundledFiles, difficulty: Difficulty, id: String): KillerSudokuPuzzleEntry? =
        load(files)[difficulty.key]?.firstOrNull { it.id == id }

    suspend fun ratedPool(files: BundledFiles, difficulty: Difficulty): List<Pair<String, Int>> =
        load(files)[difficulty.key].orEmpty().mapNotNull { entry -> entry.rating?.let { entry.id to it } }

    suspend fun randomPuzzle(files: BundledFiles, difficulty: Difficulty, recentlyPlayedIds: Set<String> = emptySet()): KillerSudokuPuzzleEntry? {
        val pool = load(files)[difficulty.key].orEmpty()
        if (pool.isEmpty()) return null
        return pool.filter { it.id !in recentlyPlayedIds }.ifEmpty { pool }.random()
    }
}
