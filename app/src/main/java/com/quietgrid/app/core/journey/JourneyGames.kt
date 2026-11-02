package com.quietgrid.app.core.journey

import android.content.Context
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.games.animaldoku.AnimalDokuPuzzleBank
import com.quietgrid.app.games.arrowescape.ArrowEscapePuzzleBank
import com.quietgrid.engine.journey.JourneyChapter
import com.quietgrid.engine.journey.JourneyFile
import com.quietgrid.engine.journey.JourneyStop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

val JOURNEY_GAMES: Set<GameId> = setOf(GameId.ANIMALDOKU, GameId.ARROWESCAPE)

fun journeyScope(gameId: GameId): String = gameId.key

data class JourneyRequest(val stopId: String, val puzzleId: String, val transform: Int)

private val journeyJson = Json { ignoreUnknownKeys = true }

private val journeyFiles = ConcurrentHashMap<GameId, JourneyFile>()

suspend fun loadJourneyFile(context: Context, gameId: GameId): JourneyFile? {
    if (gameId !in JOURNEY_GAMES) return null
    journeyFiles[gameId]?.let { return it }
    return withContext(Dispatchers.IO) {
        try {
            val text = context.assets.open("journey_${gameId.key}.json").bufferedReader().use { it.readText() }
            journeyJson.decodeFromString(JourneyFile.serializer(), text).also { journeyFiles[gameId] = it }
        } catch (_: IOException) {
            null
        }
    }
}

suspend fun journeyRatedPool(context: Context, gameId: GameId, difficulty: Difficulty): List<Pair<String, Int>> = when (gameId) {
    GameId.ANIMALDOKU -> AnimalDokuPuzzleBank.ratedPool(context, difficulty)
    GameId.ARROWESCAPE -> ArrowEscapePuzzleBank.ratedPool(context, difficulty)
    else -> emptyList()
}

fun JourneyFile.locate(stopId: String): Pair<JourneyChapter, JourneyStop>? =
    chapters.firstNotNullOfOrNull { chapter -> chapter.stops.firstOrNull { it.puzzleId == stopId }?.let { chapter to it } }
