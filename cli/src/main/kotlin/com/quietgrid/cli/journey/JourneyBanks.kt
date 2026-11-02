package com.quietgrid.cli.journey

import com.quietgrid.engine.animaldoku.AnimalDokuPuzzleEntry
import com.quietgrid.engine.animaldoku.animalDokuRawScore
import com.quietgrid.engine.animaldoku.animalDokuReplayCandidates
import com.quietgrid.engine.animaldoku.solveAnimalDoku
import com.quietgrid.engine.animaldoku.transformed
import com.quietgrid.engine.arrowescape.ArrowEscapePuzzleEntry
import com.quietgrid.engine.arrowescape.arrowEscapeRawScore
import com.quietgrid.engine.arrowescape.arrowEscapeReplayCandidates
import com.quietgrid.engine.arrowescape.solveArrowEscape
import com.quietgrid.engine.arrowescape.toPiece
import com.quietgrid.engine.arrowescape.transformed
import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.journey.GridTransform
import com.quietgrid.engine.journey.JourneyRatingScale
import kotlinx.serialization.KSerializer

val JOURNEY_GAMES = listOf("animaldoku", "arrowescape")

class JourneyBank<T>(
    val game: String,
    val files: List<String>,
    val serializer: KSerializer<T>,
    val id: (T) -> String,
    val difficulty: (T) -> String,
    val rating: (T) -> Int?,
    val withRating: (T, Int?) -> T,
    val rawScore: (T) -> Long,
    val replayCandidates: (T) -> List<GridTransform>,
    val transformed: (T, GridTransform) -> T,
    val solves: (T) -> Boolean,
)

fun <T> JourneyBank<T>.replayRating(entry: T, transform: GridTransform, scale: JourneyRatingScale): Int? {
    val moved = transformed(entry, transform)
    return if (solves(moved)) scale.rate(rawScore(moved)) else null
}

fun journeyBank(game: String): JourneyBank<*> = when (game) {
    "animaldoku" -> JourneyBank(
        game = game,
        files = listOf("animaldoku_puzzles.json"),
        serializer = AnimalDokuPuzzleEntry.serializer(),
        id = { it.id },
        difficulty = { it.difficulty },
        rating = { it.rating },
        withRating = { entry, rating -> entry.copy(rating = rating) },
        rawScore = ::animalDokuRawScore,
        replayCandidates = ::animalDokuReplayCandidates,
        transformed = { entry, t -> entry.transformed(t) },
        solves = { solveAnimalDoku(it.size, it.regions).solved },
    )
    "arrowescape" -> JourneyBank(
        game = game,
        files = Difficulty.entries.map { "arrowescape_puzzles_${it.key}.json" },
        serializer = ArrowEscapePuzzleEntry.serializer(),
        id = { it.id },
        difficulty = { it.difficulty },
        rating = { it.rating },
        withRating = { entry, rating -> entry.copy(rating = rating) },
        rawScore = ::arrowEscapeRawScore,
        replayCandidates = ::arrowEscapeReplayCandidates,
        transformed = { entry, t -> entry.transformed(t) },
        solves = { solveArrowEscape(it.pieces.map { piece -> piece.toPiece() }, it.rows, it.cols).solvable },
    )
    else -> error("No journey for game '$game'. Expected one of: ${JOURNEY_GAMES.joinToString()}")
}
