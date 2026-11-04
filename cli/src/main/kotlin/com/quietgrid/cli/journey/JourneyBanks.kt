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
import com.quietgrid.engine.killersudoku.KillerCageLayout
import com.quietgrid.engine.killersudoku.KillerSudokuPuzzleEntry
import com.quietgrid.engine.killersudoku.killerSudokuRawScore
import com.quietgrid.engine.killersudoku.killerSudokuReplayCandidates
import com.quietgrid.engine.killersudoku.traceKillerSolve
import com.quietgrid.engine.killersudoku.transformed
import com.quietgrid.engine.journey.GridTransform
import com.quietgrid.engine.journey.JourneyRatingScale
import com.quietgrid.engine.nonogram.NonogramPuzzleEntry
import com.quietgrid.engine.nonogram.analyzeNonogramEntry
import com.quietgrid.engine.nonogram.nonogramRawScore
import com.quietgrid.engine.nonogram.nonogramReplayCandidates
import com.quietgrid.engine.nonogram.transformed
import com.quietgrid.engine.starbattle.StarBattlePuzzleEntry
import com.quietgrid.engine.starbattle.countStarBattleSolutions
import com.quietgrid.engine.starbattle.starBattleRawScore
import com.quietgrid.engine.starbattle.starBattleReplayCandidates
import com.quietgrid.engine.starbattle.transformed
import com.quietgrid.engine.sudoku.SudokuPuzzleEntry
import com.quietgrid.engine.sudoku.gradeSudoku
import com.quietgrid.engine.sudoku.sudokuRawScore
import com.quietgrid.engine.sudoku.sudokuReplayCandidates
import com.quietgrid.engine.sudoku.transformed
import com.quietgrid.engine.takuzu.TakuzuPuzzleEntry
import com.quietgrid.engine.takuzu.takuzuHasOneSolution
import com.quietgrid.engine.takuzu.takuzuRawScore
import com.quietgrid.engine.takuzu.takuzuReplayCandidates
import com.quietgrid.engine.takuzu.transformed
import kotlinx.serialization.KSerializer

val JOURNEY_GAMES = listOf("animaldoku", "arrowescape", "starbattle", "sudoku", "minisudoku", "killersudoku", "takuzu", "nonogram")

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
    val keepSiblings: Boolean,
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
        keepSiblings = true,
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
        keepSiblings = true,
    )
    "starbattle" -> JourneyBank(
        game = game,
        files = listOf("starbattle_puzzles.json"),
        serializer = StarBattlePuzzleEntry.serializer(),
        id = { it.id },
        difficulty = { it.difficulty },
        rating = { it.rating },
        withRating = { entry, rating -> entry.copy(rating = rating) },
        rawScore = ::starBattleRawScore,
        replayCandidates = ::starBattleReplayCandidates,
        transformed = { entry, t -> entry.transformed(t) },
        solves = { countStarBattleSolutions(it.size, it.k, it.regions) == 1 },
        keepSiblings = true,
    )
    "sudoku", "minisudoku" -> JourneyBank(
        game = game,
        files = listOf("${game}_puzzles.json"),
        serializer = SudokuPuzzleEntry.serializer(),
        id = { it.id },
        difficulty = { it.difficulty },
        rating = { it.rating },
        withRating = { entry, rating -> entry.copy(rating = rating) },
        rawScore = ::sudokuRawScore,
        replayCandidates = ::sudokuReplayCandidates,
        transformed = { entry, t -> entry.transformed(t) },
        solves = { gradeSudoku(it.givens) != null },
        keepSiblings = false,
    )
    "killersudoku" -> JourneyBank(
        game = game,
        files = listOf("killersudoku_puzzles.json"),
        serializer = KillerSudokuPuzzleEntry.serializer(),
        id = { it.id },
        difficulty = { it.difficulty },
        rating = { it.rating },
        withRating = { entry, rating -> entry.copy(rating = rating) },
        rawScore = ::killerSudokuRawScore,
        replayCandidates = ::killerSudokuReplayCandidates,
        transformed = { entry, t -> entry.transformed(t) },
        solves = { traceKillerSolve(KillerCageLayout(it.cages)).solved },
        keepSiblings = false,
    )
    "takuzu" -> JourneyBank(
        game = game,
        files = listOf("takuzu_puzzles.json"),
        serializer = TakuzuPuzzleEntry.serializer(),
        id = { it.id },
        difficulty = { it.difficulty },
        rating = { it.rating },
        withRating = { entry, rating -> entry.copy(rating = rating) },
        rawScore = ::takuzuRawScore,
        replayCandidates = ::takuzuReplayCandidates,
        transformed = { entry, t -> entry.transformed(t) },
        solves = ::takuzuHasOneSolution,
        keepSiblings = false,
    )
    "nonogram" -> JourneyBank(
        game = game,
        files = listOf("nonogram_puzzles.json"),
        serializer = NonogramPuzzleEntry.serializer(),
        id = { it.id },
        difficulty = { it.difficulty },
        rating = { it.rating },
        withRating = { entry, rating -> entry.copy(rating = rating) },
        rawScore = ::nonogramRawScore,
        replayCandidates = ::nonogramReplayCandidates,
        transformed = { entry, t -> entry.transformed(t) },
        solves = { analyzeNonogramEntry(it) != null },
        keepSiblings = false,
    )
    else -> error("No journey for game '$game'. Expected one of: ${JOURNEY_GAMES.joinToString()}")
}
