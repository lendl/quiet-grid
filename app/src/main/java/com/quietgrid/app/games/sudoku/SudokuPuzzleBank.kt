package com.quietgrid.app.games.sudoku

import android.content.Context
import com.quietgrid.app.core.Difficulty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

object SudokuPuzzleBank {
    private var cache: Map<String, List<SudokuPuzzleEntry>>? = null
    private val lastPickedId = mutableMapOf<String, String>()

    private suspend fun load(context: Context): Map<String, List<SudokuPuzzleEntry>> {
        cache?.let { return it }
        return withContext(Dispatchers.IO) {
            val text = context.assets.open("sudoku_puzzles.json").bufferedReader().use { it.readText() }
            val entries = json.decodeFromString<List<SudokuPuzzleEntry>>(text)
            val grouped = entries.groupBy { it.difficulty }
            cache = grouped
            grouped
        }
    }

    suspend fun randomPuzzle(context: Context, difficulty: Difficulty): SudokuPuzzleEntry? {
        val pool = load(context)[difficulty.key] ?: return null
        if (pool.isEmpty()) return null
        val lastId = lastPickedId[difficulty.key]
        val candidates = if (pool.size > 1 && lastId != null) pool.filter { it.id != lastId } else pool
        val choices = candidates.ifEmpty { pool }
        val chosen = choices.random()
        lastPickedId[difficulty.key] = chosen.id
        return chosen
    }
}
