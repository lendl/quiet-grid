package com.quietgrid.app.games.wordguess

import android.content.Context
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

    private suspend fun loadAnswers(context: Context): Map<String, List<WordGuessPuzzleEntry>> {
        answerCache?.let { return it }
        return withContext(Dispatchers.IO) {
            val text = context.assets.open("wordguess_puzzles.json").bufferedReader().use { it.readText() }
            val entries = json.decodeFromString<List<WordGuessPuzzleEntry>>(text)
            val grouped = entries.groupBy { "${it.locale}:${it.difficulty}" }
            answerCache = grouped
            grouped
        }
    }

    suspend fun loadDictionary(context: Context, locale: String): Set<String> {
        dictionaryCache?.takeIf { it.first == locale }?.let { return it.second }
        return withContext(Dispatchers.IO) {
            val text = context.assets.open("wordguess_dictionary.json").bufferedReader().use { it.readText() }
            json.decodeFromString(wordGuessDictionarySerializer, text)[locale].orEmpty().toHashSet()
                .also { dictionaryCache = locale to it }
        }
    }

    suspend fun dailyPool(context: Context, locale: String, difficulty: Difficulty): List<WordGuessPuzzleEntry> =
        loadAnswers(context)["$locale:${difficulty.key}"].orEmpty()

    suspend fun randomPuzzle(
        context: Context,
        locale: String,
        difficulty: Difficulty,
        recentlyPlayedIds: Set<String> = emptySet(),
    ): WordGuessPuzzleEntry? {
        val pool = dailyPool(context, locale, difficulty)
        if (pool.isEmpty()) return null
        val candidates = pool.filter { it.id !in recentlyPlayedIds }
        return candidates.ifEmpty { pool }.random()
    }
}
