// cli/src/main/kotlin/com/quietgrid/cli/arrowescape/ArrowEscapeGenerator.kt
package com.quietgrid.cli.arrowescape

import com.quietgrid.engine.arrowescape.ArrowEscapePiece
import com.quietgrid.engine.arrowescape.ArrowEscapePieceData
import com.quietgrid.engine.arrowescape.ArrowEscapePuzzleEntry
import com.quietgrid.engine.arrowescape.buildDependencyGraph
import com.quietgrid.engine.arrowescape.chainLengthForDifficulty
import com.quietgrid.engine.arrowescape.classifyArrowEscapeDifficulty
import com.quietgrid.engine.arrowescape.key
import com.quietgrid.engine.arrowescape.measureArrowEscapePuzzle
import com.quietgrid.engine.core.Difficulty
import kotlin.random.Random

const val ARROW_ESCAPE_EMPTY_CELL_TOLERANCE = 0.03
private const val MAX_GENERATE_ATTEMPTS = 20

data class ArrowEscapeGeneratedPuzzle(
    val rows: Int,
    val cols: Int,
    val difficulty: Difficulty,
    val pieces: List<ArrowEscapePiece>,
    val dedupeKey: String,
)

fun buildPuzzleFingerprint(pieces: List<ArrowEscapePiece>): String {
    val perPiece = pieces.map { piece ->
        val sortedCellKeys = piece.cells.map { "${it.row},${it.col}" }.sorted()
        "${sortedCellKeys.joinToString("|")}:${piece.headDirection.key}"
    }
    return perPiece.sorted().joinToString(";")
}

val ARROW_ESCAPE_SIZE_RANGE_BY_DIFFICULTY: Map<Difficulty, IntRange> = mapOf(
    Difficulty.EASY to 10..12,
    Difficulty.MEDIUM to 12..14,
    Difficulty.HARD to 14..18,
    Difficulty.EXPERT to 18..30,
)

fun arrowEscapeSizesForDifficulty(difficulty: Difficulty): List<Int> =
    ARROW_ESCAPE_SIZE_RANGE_BY_DIFFICULTY.getValue(difficulty).toList()

fun generateArrowEscapePuzzle(
    rows: Int,
    cols: Int,
    targetDifficulty: Difficulty,
    random: Random = Random.Default,
): ArrowEscapeGeneratedPuzzle? {
    repeat(MAX_GENERATE_ATTEMPTS) {
        val occupied = mutableSetOf<Pair<Int, Int>>()
        val chainLength = chainLengthForDifficulty(targetDifficulty)
        val chain = placeChain(rows, cols, chainLength, occupied, random) ?: return@repeat
        chain.forEach { piece -> piece.cells.forEach { occupied.add(it.row to it.col) } }

        val fillResult = fillCoverage(rows, cols, occupied, ARROW_ESCAPE_EMPTY_CELL_TOLERANCE, random) ?: return@repeat

        val allPieces = chain + fillResult.pieces
        val graph = buildDependencyGraph(allPieces, rows, cols)
        val metrics = measureArrowEscapePuzzle(allPieces, graph)
        val difficulty = classifyArrowEscapeDifficulty(metrics)
        if (difficulty != targetDifficulty) return@repeat

        return ArrowEscapeGeneratedPuzzle(rows, cols, difficulty, allPieces, buildPuzzleFingerprint(allPieces))
    }
    return null
}

fun toEntry(id: String, generated: ArrowEscapeGeneratedPuzzle): ArrowEscapePuzzleEntry = ArrowEscapePuzzleEntry(
    id = id,
    difficulty = generated.difficulty.key,
    rows = generated.rows,
    cols = generated.cols,
    pieces = generated.pieces.map { piece ->
        ArrowEscapePieceData(
            cells = piece.cells.map { listOf(it.row, it.col) },
            headDirection = piece.headDirection.key,
        )
    },
)
