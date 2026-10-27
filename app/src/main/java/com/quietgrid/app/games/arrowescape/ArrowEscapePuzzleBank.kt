package com.quietgrid.app.games.arrowescape

import android.content.Context
import com.quietgrid.app.core.Difficulty
import com.quietgrid.engine.arrowescape.ArrowEscapePuzzleEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.DecodeSequenceMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeToSequence
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap

private val json = Json { ignoreUnknownKeys = true }

private const val HALLOWEEN_BANK_ASSET = "arrowescape_halloween_puzzles.json"

private fun tierBankAsset(difficulty: Difficulty): String = "arrowescape_puzzles_${difficulty.key}.json"

@OptIn(ExperimentalSerializationApi::class)
internal fun decodeArrowEscapePool(stream: InputStream, difficulty: Difficulty): List<ArrowEscapePuzzleEntry> =
    stream.use {
        json.decodeToSequence(it, ArrowEscapePuzzleEntry.serializer(), DecodeSequenceMode.ARRAY_WRAPPED)
            .filter { entry -> entry.difficulty == difficulty.key }
            .toList()
    }

object ArrowEscapePuzzleBank {
    private val cache = ConcurrentHashMap<String, List<ArrowEscapePuzzleEntry>>()

    private suspend fun pool(context: Context, asset: String, difficulty: Difficulty): List<ArrowEscapePuzzleEntry> {
        val key = "$asset:${difficulty.key}"
        cache[key]?.let { return it }
        return withContext(Dispatchers.IO) {
            decodeArrowEscapePool(context.assets.open(asset), difficulty).also { cache[key] = it }
        }
    }

    suspend fun randomPuzzle(context: Context, difficulty: Difficulty, recentlyPlayedIds: Set<String> = emptySet()): ArrowEscapePuzzleEntry? {
        val pool = pool(context, tierBankAsset(difficulty), difficulty)
        if (pool.isEmpty()) return null
        val candidates = pool.filter { it.id !in recentlyPlayedIds }
        return candidates.ifEmpty { pool }.random()
    }

    suspend fun halloweenPuzzle(context: Context, difficulty: Difficulty, playedIds: Set<String>): ArrowEscapePuzzleEntry? =
        pool(context, HALLOWEEN_BANK_ASSET, difficulty).firstOrNull { it.id !in playedIds }
}
