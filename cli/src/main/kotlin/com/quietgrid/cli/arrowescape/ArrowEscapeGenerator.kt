// cli/src/main/kotlin/com/quietgrid/cli/arrowescape/ArrowEscapeGenerator.kt
package com.quietgrid.cli.arrowescape

import com.quietgrid.engine.arrowescape.ArrowEscapeDifficultyProfile
import com.quietgrid.engine.arrowescape.ArrowEscapePiece
import com.quietgrid.engine.arrowescape.ArrowEscapePieceData
import com.quietgrid.engine.arrowescape.ArrowEscapePuzzleEntry
import com.quietgrid.engine.arrowescape.classifyArrowEscapeDifficulty
import com.quietgrid.engine.arrowescape.key
import com.quietgrid.engine.arrowescape.profileArrowEscapePuzzle
import com.quietgrid.engine.arrowescape.solveArrowEscape
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
    val profile: ArrowEscapeDifficultyProfile,
    val shape: String? = null,
    val mask: ArrowEscapeBoardMask? = null,
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
    Difficulty.EXPERT to 18..24,
)

val ARROW_ESCAPE_SILHOUETTE_CELL_RANGE_BY_DIFFICULTY: Map<Difficulty, IntRange> = mapOf(
    Difficulty.EASY to 100..144,
    Difficulty.MEDIUM to 324..576,
    Difficulty.HARD to 625..900,
    Difficulty.EXPERT to 961..1369,
)

val ARROW_ESCAPE_FILL_PARAMS_BY_DIFFICULTY: Map<Difficulty, ArrowEscapeFillParams> = mapOf(
    Difficulty.EASY to ArrowEscapeFillParams(borderExitRate = 1.0, depthGreed = 0.0),
    Difficulty.MEDIUM to ArrowEscapeFillParams(borderExitRate = 0.3, depthGreed = 0.0),
    Difficulty.HARD to ArrowEscapeFillParams(borderExitRate = 0.1, depthGreed = 0.2),
    Difficulty.EXPERT to ArrowEscapeFillParams(borderExitRate = 0.05, depthGreed = 0.7),
)

val ARROW_ESCAPE_SILHOUETTE_FILL_PARAMS_BY_DIFFICULTY: Map<Difficulty, ArrowEscapeFillParams> =
    ARROW_ESCAPE_FILL_PARAMS_BY_DIFFICULTY + (Difficulty.HARD to ArrowEscapeFillParams(borderExitRate = 0.5, depthGreed = 0.0))

fun arrowEscapeSizesForDifficulty(difficulty: Difficulty): List<Int> =
    ARROW_ESCAPE_SIZE_RANGE_BY_DIFFICULTY.getValue(difficulty).toList()

fun generateArrowEscapePuzzle(
    rows: Int,
    cols: Int,
    targetDifficulty: Difficulty,
    random: Random = Random.Default,
): ArrowEscapeGeneratedPuzzle? = generateArrowEscapePuzzleOnBoard(
    rows, cols, null, null, ARROW_ESCAPE_FILL_PARAMS_BY_DIFFICULTY.getValue(targetDifficulty), targetDifficulty, random,
)

fun generateArrowEscapeSilhouettePuzzle(
    silhouette: ArrowEscapeSilhouette,
    targetDifficulty: Difficulty,
    random: Random = Random.Default,
): ArrowEscapeGeneratedPuzzle? {
    val targetCells = ARROW_ESCAPE_SILHOUETTE_CELL_RANGE_BY_DIFFICULTY.getValue(targetDifficulty).random(random)
    val mask = silhouetteBoardForCellTarget(silhouette, targetCells)
    val params = ARROW_ESCAPE_SILHOUETTE_FILL_PARAMS_BY_DIFFICULTY.getValue(targetDifficulty)
    return generateArrowEscapePuzzleOnBoard(mask.rows, mask.cols, silhouette.id, mask, params, targetDifficulty, random)
}

private fun generateArrowEscapePuzzleOnBoard(
    rows: Int,
    cols: Int,
    shape: String?,
    mask: ArrowEscapeBoardMask?,
    params: ArrowEscapeFillParams,
    targetDifficulty: Difficulty,
    random: Random,
): ArrowEscapeGeneratedPuzzle? {
    val activeCells = mask?.active?.map { it.row to it.col }?.toSet()
    repeat(MAX_GENERATE_ATTEMPTS) {
        val fillResult = fillCoverage(rows, cols, params, ARROW_ESCAPE_EMPTY_CELL_TOLERANCE, random, activeCells) ?: return@repeat
        val pieces = fillResult.pieces
        if (!solveArrowEscape(pieces, rows, cols).solvable) return@repeat
        val profile = profileArrowEscapePuzzle(pieces, rows, cols, activeCells = mask?.active)
        if (classifyArrowEscapeDifficulty(profile) != targetDifficulty) return@repeat
        return ArrowEscapeGeneratedPuzzle(rows, cols, targetDifficulty, pieces, buildPuzzleFingerprint(pieces), profile, shape, mask)
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
    shape = generated.shape,
    mask = generated.mask?.toMaskRows(),
)

fun loadArrowEscapeSilhouettes(): List<ArrowEscapeSilhouette> {
    val text = object {}.javaClass.getResourceAsStream("/arrowescape_silhouettes.json")
        ?.bufferedReader()?.use { it.readText() }
        ?: error("arrowescape_silhouettes.json missing from cli resources")
    return parseArrowEscapeSilhouettes(text)
}
