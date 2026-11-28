package com.quietgrid.app.games.sudoku

import com.quietgrid.app.platform.BundledFiles
import com.quietgrid.app.platform.readText
import com.quietgrid.app.core.Difficulty
import com.quietgrid.engine.sudoku.SudokuPuzzleEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

open class SudokuEntryBank(private val assetName: String) {
    private var cache: Map<String, List<SudokuPuzzleEntry>>? = null

    private suspend fun load(files: BundledFiles): Map<String, List<SudokuPuzzleEntry>> {
        cache?.let { return it }
        return withContext(Dispatchers.IO) {
            val text = files.readText(assetName)
            val entries = json.decodeFromString<List<SudokuPuzzleEntry>>(text)
            val grouped = entries.groupBy { it.difficulty }
            cache = grouped
            grouped
        }
    }

    suspend fun dailyPool(files: BundledFiles, difficulty: Difficulty): List<SudokuPuzzleEntry> =
        load(files)[difficulty.key].orEmpty()

    suspend fun puzzleById(files: BundledFiles, difficulty: Difficulty, id: String): SudokuPuzzleEntry? =
        dailyPool(files, difficulty).firstOrNull { it.id == id }

    suspend fun ratedPool(files: BundledFiles, difficulty: Difficulty): List<Pair<String, Int>> =
        dailyPool(files, difficulty).mapNotNull { entry -> entry.rating?.let { entry.id to it } }

    suspend fun randomPuzzle(files: BundledFiles, difficulty: Difficulty, recentlyPlayedIds: Set<String> = emptySet()): SudokuPuzzleEntry? {
        val pool = dailyPool(files, difficulty)
        if (pool.isEmpty()) return null
        val candidates = pool.filter { it.id !in recentlyPlayedIds }
        return candidates.ifEmpty { pool }.random()
    }
}

object SudokuPuzzleBank : SudokuEntryBank("sudoku_puzzles.json")
