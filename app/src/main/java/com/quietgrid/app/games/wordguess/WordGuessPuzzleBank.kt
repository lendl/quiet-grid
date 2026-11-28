package com.quietgrid.app.games.wordguess

import com.quietgrid.app.platform.BundledFiles
import com.quietgrid.app.platform.readText
import com.quietgrid.app.core.Difficulty
import com.quietgrid.engine.wordguess.WordGuessPuzzleEntry
import com.quietgrid.engine.wordguess.wordGuessDictionarySerializer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

object WordGuessPuzzleBank {
    private var answerCache: Map<String, List<WordGuessPuzzleEntry>>? = null
    private var dictionaryCache: Pair<String, Set<String>>? = null

    private suspend fun loadAnswers(files: BundledFiles): Map<String, List<WordGuessPuzzleEntry>> {
        answerCache?.let { return it }
        return withContext(Dispatchers.IO) {
            val text = files.readText("wordguess_puzzles.json")
            val entries = json.decodeFromString<List<WordGuessPuzzleEntry>>(text)
            val grouped = entries.groupBy { "${it.locale}:${it.difficulty}" }
            answerCache = grouped
            grouped
        }
    }

    suspend fun loadDictionary(files: BundledFiles, locale: String): Set<String> {
        dictionaryCache?.takeIf { it.first == locale }?.let { return it.second }
        return withContext(Dispatchers.IO) {
            val text = files.readText("wordguess_dictionary.json")
            json.decodeFromString(wordGuessDictionarySerializer, text)[locale].orEmpty().toHashSet()
                .also { dictionaryCache = locale to it }
        }
    }

    suspend fun dailyPool(files: BundledFiles, locale: String, difficulty: Difficulty): List<WordGuessPuzzleEntry> =
        loadAnswers(files)["$locale:${difficulty.key}"].orEmpty()

    suspend fun randomPuzzle(
        files: BundledFiles,
        locale: String,
        difficulty: Difficulty,
        recentlyPlayedIds: Set<String> = emptySet(),
    ): WordGuessPuzzleEntry? {
        val pool = dailyPool(files, locale, difficulty)
        if (pool.isEmpty()) return null
        val candidates = pool.filter { it.id !in recentlyPlayedIds }
        return candidates.ifEmpty { pool }.random()
    }
}
