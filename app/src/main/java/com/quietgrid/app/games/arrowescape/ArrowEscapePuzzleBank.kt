package com.quietgrid.app.games.arrowescape

import com.quietgrid.app.platform.BundledFiles
import com.quietgrid.app.platform.readText
import com.quietgrid.app.core.Difficulty
import com.quietgrid.engine.arrowescape.ArrowEscapePuzzleEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.DecodeSequenceMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.okio.decodeBufferedSourceToSequence
import okio.Source
import okio.buffer
import okio.use
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val json = Json { ignoreUnknownKeys = true }

private const val HALLOWEEN_BANK_ASSET = "arrowescape_halloween_puzzles.json"

private fun tierBankAsset(difficulty: Difficulty): String = "arrowescape_puzzles_${difficulty.key}.json"

@OptIn(ExperimentalSerializationApi::class)
internal fun decodeArrowEscapePool(source: Source, difficulty: Difficulty): List<ArrowEscapePuzzleEntry> =
    source.buffer().use {
        json.decodeBufferedSourceToSequence(it, ArrowEscapePuzzleEntry.serializer(), DecodeSequenceMode.ARRAY_WRAPPED)
            .filter { entry -> entry.difficulty == difficulty.key }
            .toList()
    }

object ArrowEscapePuzzleBank {
    private val cache = HashMap<String, List<ArrowEscapePuzzleEntry>>()
    private val cacheLock = Mutex()

    private suspend fun pool(files: BundledFiles, asset: String, difficulty: Difficulty): List<ArrowEscapePuzzleEntry> {
        val key = "$asset:${difficulty.key}"
        cacheLock.withLock { cache[key] }?.let { return it }
        return withContext(Dispatchers.IO) {
            decodeArrowEscapePool(files.open(asset), difficulty).also { cacheLock.withLock { cache[key] = it } }
        }
    }

    suspend fun randomPuzzle(files: BundledFiles, difficulty: Difficulty, recentlyPlayedIds: Set<String> = emptySet()): ArrowEscapePuzzleEntry? {
        val pool = pool(files, tierBankAsset(difficulty), difficulty)
        if (pool.isEmpty()) return null
        val candidates = pool.filter { it.id !in recentlyPlayedIds }
        return candidates.ifEmpty { pool }.random()
    }

    suspend fun puzzleById(files: BundledFiles, difficulty: Difficulty, id: String): ArrowEscapePuzzleEntry? =
        pool(files, tierBankAsset(difficulty), difficulty).firstOrNull { it.id == id }
            ?: pool(files, HALLOWEEN_BANK_ASSET, difficulty).firstOrNull { it.id == id }

    suspend fun ratedPool(files: BundledFiles, difficulty: Difficulty): List<Pair<String, Int>> =
        pool(files, tierBankAsset(difficulty), difficulty).mapNotNull { entry -> entry.rating?.let { entry.id to it } }

    suspend fun halloweenPuzzle(files: BundledFiles, difficulty: Difficulty, playedIds: Set<String>): ArrowEscapePuzzleEntry? =
        pool(files, HALLOWEEN_BANK_ASSET, difficulty).firstOrNull { it.id !in playedIds }
}
