package com.quietgrid.app.games.wordguess

import android.content.Context
import com.quietgrid.app.core.Difficulty
import com.quietgrid.engine.wordguess.WordGuessDictionaryEntry
import com.quietgrid.engine.wordguess.WordGuessPuzzleEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

object WordGuessPuzzleBank {
    private var answerCache: Map<String, List<WordGuessPuzzleEntry>>? = null
    private var dictionaryCache: Map<String, Set<String>>? = null
    private val lastPickedWord = mutableMapOf<String, String>()

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
        val cache = dictionaryCache ?: withContext(Dispatchers.IO) {
            val text = context.assets.open("wordguess_dictionary.json").bufferedReader().use { it.readText() }
            val entries = json.decodeFromString<List<WordGuessDictionaryEntry>>(text)
            entries.groupBy { it.locale }.mapValues { (_, v) -> v.map { it.word }.toSet() }
        }.also { dictionaryCache = it }
        return cache[locale] ?: emptySet()
    }

    suspend fun randomPuzzle(context: Context, locale: String, difficulty: Difficulty): WordGuessPuzzleEntry? {
        val key = "$locale:${difficulty.key}"
        val pool = loadAnswers(context)[key] ?: return null
        if (pool.isEmpty()) return null
        val lastWord = lastPickedWord[key]
        val candidates = if (pool.size > 1 && lastWord != null) pool.filter { it.word != lastWord } else pool
        val choices = candidates.ifEmpty { pool }
        val chosen = choices.random()
        lastPickedWord[key] = chosen.word
        return chosen
    }
}
