package com.quietgrid.app.core.journey

import android.content.Context
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.games.GameRegistry
import com.quietgrid.app.games.animaldoku.AnimalDokuPuzzleBank
import com.quietgrid.app.games.arrowescape.ArrowEscapePuzzleBank
import com.quietgrid.app.games.blockescape.BlockEscapePuzzleBank
import com.quietgrid.app.games.circuit.CircuitPuzzleBank
import com.quietgrid.app.games.killersudoku.KillerSudokuPuzzleBank
import com.quietgrid.app.games.links.LinksPuzzleBank
import com.quietgrid.app.games.minisudoku.MiniSudokuPuzzleBank
import com.quietgrid.app.games.nonogram.NonogramPuzzleBank
import com.quietgrid.app.games.starbattle.StarBattlePuzzleBank
import com.quietgrid.app.games.sudoku.SudokuPuzzleBank
import com.quietgrid.app.games.takuzu.TakuzuPuzzleBank
import com.quietgrid.app.games.wordswap.WordSwapPuzzleBank
import com.quietgrid.engine.journey.JourneyChapter
import com.quietgrid.engine.journey.JourneyFile
import com.quietgrid.engine.journey.JourneyStop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

val JOURNEY_GAMES: Set<GameId> = setOf(
    GameId.ANIMALDOKU, GameId.ARROWESCAPE, GameId.STARBATTLE, GameId.SUDOKU, GameId.MINISUDOKU, GameId.KILLERSUDOKU, GameId.TAKUZU, GameId.NONOGRAM,
    GameId.BLOCKESCAPE, GameId.CIRCUIT, GameId.LINKS, GameId.WORDSWAP,
)

data class JourneyTrack(val gameId: GameId, val locale: String) {
    val scope: String = if (locale.isEmpty() || locale == "en") gameId.key else "${gameId.key}_$locale"
    val assetName: String = "journey_$scope.json"
}

fun journeyTrack(gameId: GameId, puzzleLanguage: String): JourneyTrack =
    JourneyTrack(gameId, GameRegistry.of(gameId).journeyLocale(puzzleLanguage))

fun journeyTrackOfStop(gameId: GameId, stopId: String): JourneyTrack? =
    GameRegistry.of(gameId).journeyLocaleOfStop(stopId)?.let { JourneyTrack(gameId, it) }

data class JourneyRequest(val stopId: String, val puzzleId: String, val transform: Int)

private val journeyJson = Json { ignoreUnknownKeys = true }

private val journeyFiles = ConcurrentHashMap<String, JourneyFile>()

suspend fun loadJourneyFile(context: Context, track: JourneyTrack): JourneyFile? {
    if (track.gameId !in JOURNEY_GAMES) return null
    journeyFiles[track.scope]?.let { return it }
    return withContext(Dispatchers.IO) {
        try {
            val text = context.assets.open(track.assetName).bufferedReader().use { it.readText() }
            journeyJson.decodeFromString(JourneyFile.serializer(), text).also { journeyFiles[track.scope] = it }
        } catch (_: IOException) {
            null
        }
    }
}

suspend fun journeyRatedPool(context: Context, track: JourneyTrack, difficulty: Difficulty): List<Pair<String, Int>> = when (track.gameId) {
    GameId.ANIMALDOKU -> AnimalDokuPuzzleBank.ratedPool(context, difficulty)
    GameId.ARROWESCAPE -> ArrowEscapePuzzleBank.ratedPool(context, difficulty)
    GameId.STARBATTLE -> StarBattlePuzzleBank.ratedPool(context, difficulty)
    GameId.SUDOKU -> SudokuPuzzleBank.ratedPool(context, difficulty)
    GameId.MINISUDOKU -> MiniSudokuPuzzleBank.ratedPool(context, difficulty)
    GameId.KILLERSUDOKU -> KillerSudokuPuzzleBank.ratedPool(context, difficulty)
    GameId.TAKUZU -> TakuzuPuzzleBank.ratedPool(context, difficulty)
    GameId.NONOGRAM -> NonogramPuzzleBank.ratedPool(context, difficulty)
    GameId.BLOCKESCAPE -> BlockEscapePuzzleBank.ratedPool(context, difficulty)
    GameId.CIRCUIT -> CircuitPuzzleBank.ratedPool(context, difficulty)
    GameId.LINKS -> LinksPuzzleBank.ratedPool(context, difficulty)
    GameId.WORDSWAP -> WordSwapPuzzleBank.ratedPool(context, track.locale, difficulty)
    else -> emptyList()
}

fun JourneyFile.locate(stopId: String): Pair<JourneyChapter, JourneyStop>? =
    chapters.firstNotNullOfOrNull { chapter -> chapter.stops.firstOrNull { it.puzzleId == stopId }?.let { chapter to it } }
