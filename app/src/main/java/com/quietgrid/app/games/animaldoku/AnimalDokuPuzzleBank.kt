package com.quietgrid.app.games.animaldoku

import com.quietgrid.app.platform.BundledFiles
import com.quietgrid.app.platform.readText
import com.quietgrid.app.core.Difficulty
import com.quietgrid.engine.animaldoku.AnimalDokuPuzzleEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

object AnimalDokuPuzzleBank {
    private var cache: Map<String, List<AnimalDokuPuzzleEntry>>? = null

    private suspend fun load(files: BundledFiles): Map<String, List<AnimalDokuPuzzleEntry>> {
        cache?.let { return it }
        return withContext(Dispatchers.IO) {
            val text = files.readText("animaldoku_puzzles.json")
            val entries = json.decodeFromString<List<AnimalDokuPuzzleEntry>>(text)
            val grouped = entries.groupBy { it.difficulty }
            cache = grouped
            grouped
        }
    }

    suspend fun dailyPool(files: BundledFiles, difficulty: Difficulty): List<AnimalDokuPuzzleEntry> =
        load(files)[difficulty.key].orEmpty()

    suspend fun puzzleById(files: BundledFiles, difficulty: Difficulty, id: String): AnimalDokuPuzzleEntry? =
        dailyPool(files, difficulty).firstOrNull { it.id == id }

    suspend fun ratedPool(files: BundledFiles, difficulty: Difficulty): List<Pair<String, Int>> =
        dailyPool(files, difficulty).mapNotNull { entry -> entry.rating?.let { entry.id to it } }

    suspend fun randomPuzzle(files: BundledFiles, difficulty: Difficulty, recentlyPlayedIds: Set<String> = emptySet()): AnimalDokuPuzzleEntry? {
        val pool = dailyPool(files, difficulty)
        if (pool.isEmpty()) return null
        val candidates = pool.filter { it.id !in recentlyPlayedIds }
        return candidates.ifEmpty { pool }.random()
    }
}
