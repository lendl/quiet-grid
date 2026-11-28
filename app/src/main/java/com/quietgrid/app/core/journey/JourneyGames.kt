package com.quietgrid.app.core.journey

import com.quietgrid.app.platform.BundledFiles
import com.quietgrid.app.platform.readText
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okio.IOException

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

private val journeyFiles = HashMap<String, JourneyFile>()
private val journeyFilesLock = Mutex()

suspend fun loadJourneyFile(files: BundledFiles, track: JourneyTrack): JourneyFile? {
    if (track.gameId !in JOURNEY_GAMES) return null
    journeyFilesLock.withLock { journeyFiles[track.scope] }?.let { return it }
    return withContext(Dispatchers.IO) {
        try {
            val text = files.readText(track.assetName)
            journeyJson.decodeFromString(JourneyFile.serializer(), text).also { journeyFilesLock.withLock { journeyFiles[track.scope] = it } }
        } catch (_: IOException) {
            null
        }
    }
}

suspend fun journeyRatedPool(files: BundledFiles, track: JourneyTrack, difficulty: Difficulty): List<Pair<String, Int>> = when (track.gameId) {
    GameId.ANIMALDOKU -> AnimalDokuPuzzleBank.ratedPool(files, difficulty)
    GameId.ARROWESCAPE -> ArrowEscapePuzzleBank.ratedPool(files, difficulty)
    GameId.STARBATTLE -> StarBattlePuzzleBank.ratedPool(files, difficulty)
    GameId.SUDOKU -> SudokuPuzzleBank.ratedPool(files, difficulty)
    GameId.MINISUDOKU -> MiniSudokuPuzzleBank.ratedPool(files, difficulty)
    GameId.KILLERSUDOKU -> KillerSudokuPuzzleBank.ratedPool(files, difficulty)
    GameId.TAKUZU -> TakuzuPuzzleBank.ratedPool(files, difficulty)
    GameId.NONOGRAM -> NonogramPuzzleBank.ratedPool(files, difficulty)
    GameId.BLOCKESCAPE -> BlockEscapePuzzleBank.ratedPool(files, difficulty)
    GameId.CIRCUIT -> CircuitPuzzleBank.ratedPool(files, difficulty)
    GameId.LINKS -> LinksPuzzleBank.ratedPool(files, difficulty)
    GameId.WORDSWAP -> WordSwapPuzzleBank.ratedPool(files, track.locale, difficulty)
    else -> emptyList()
}

fun JourneyFile.locate(stopId: String): Pair<JourneyChapter, JourneyStop>? =
    chapters.firstNotNullOfOrNull { chapter -> chapter.stops.firstOrNull { it.puzzleId == stopId }?.let { chapter to it } }
