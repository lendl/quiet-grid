package com.quietgrid.app.games.blockfill

import kotlin.random.Random

data class BlockFillDifficultyConfig(
    val preFillDensity: Double,
    val feasibilityFloor: Double,
    val scoreTarget: Int,
    val refillRetryCap: Int,
)

val BLOCKFILL_DIFFICULTY_CONFIG: Map<String, BlockFillDifficultyConfig> = mapOf(
    "easy" to BlockFillDifficultyConfig(preFillDensity = 0.12, feasibilityFloor = 0.25, scoreTarget = 300, refillRetryCap = 50),
    "medium" to BlockFillDifficultyConfig(preFillDensity = 0.20, feasibilityFloor = 0.20, scoreTarget = 600, refillRetryCap = 25),
    "hard" to BlockFillDifficultyConfig(preFillDensity = 0.28, feasibilityFloor = 0.15, scoreTarget = 1000, refillRetryCap = 8),
    "expert" to BlockFillDifficultyConfig(preFillDensity = 0.35, feasibilityFloor = 0.10, scoreTarget = 1500, refillRetryCap = 3),
)

private const val TOTAL_CELLS = BLOCKFILL_BOARD_SIZE * BLOCKFILL_BOARD_SIZE
private const val MAX_GENERATION_ATTEMPTS = 500

private fun shuffledPositions(random: Random): List<Pair<Int, Int>> {
    val positions = mutableListOf<Pair<Int, Int>>()
    for (row in 0 until BLOCKFILL_BOARD_SIZE) {
        for (col in 0 until BLOCKFILL_BOARD_SIZE) positions.add(row to col)
    }
    positions.shuffle(random)
    return positions
}

/**
 * Fills random individual cells up to preFillDensity, never allowing a fully-filled row or column
 * to result (a puzzle must never start with an already-clearable line).
 */
private fun generatePreFilledBoard(preFillDensity: Double, random: Random): BlockFillBoard {
    val targetCount = Math.round(TOTAL_CELLS * preFillDensity).toInt()
    var board = createEmptyBoard()
    var filled = 0

    for ((row, col) in shuffledPositions(random)) {
        if (filled >= targetCount) break

        val candidate = placePieceAt(board, listOf(0 to 0), row, col, BlockFillShapeFamily.SINGLE)
        val rowWouldBeFull = candidate[row].all { it != null }
        val colWouldBeFull = candidate.all { it[col] != null }
        if (rowWouldBeFull || colWouldBeFull) continue

        board = candidate
        filled++
    }

    return board
}

private val PERMUTATIONS_OF_3: List<List<Int>> = listOf(
    listOf(0, 1, 2), listOf(0, 2, 1), listOf(1, 0, 2), listOf(1, 2, 0), listOf(2, 0, 1), listOf(2, 1, 0),
)

/**
 * Checks the start-fit guarantee and computes the feasibility-floor clear fraction in one pass:
 * tries every ordering of the 3 pieces, and within each ordering greedily places each piece at the
 * position that clears the most lines. Best-effort "optimal" placement, not an exhaustive search
 * over all placement combinations — with only 3 pieces and an 8x8 board this is cheap and good
 * enough to prove the puzzle is progressable. Returns null if no ordering can place all 3 pieces
 * at all (start-fit guarantee fails).
 */
fun bestClearFractionForTray(board: BlockFillBoard, tray: List<BlockFillPiece>): Double? {
    var bestClearFraction: Double? = null

    for (order in PERMUTATIONS_OF_3) {
        var workingBoard = board
        var orderSucceeded = true

        for (index in order) {
            val piece = tray[index]
            val placements = findValidPlacements(workingBoard, piece.cells)
            if (placements.isEmpty()) {
                orderSucceeded = false
                break
            }

            var bestLinesCleared = -1
            var bestBoardAfter = workingBoard
            for ((row, col) in placements) {
                val placed = placePieceAt(workingBoard, piece.cells, row, col, piece.family)
                val (cleared, linesCleared) = clearFullLines(placed)
                if (linesCleared > bestLinesCleared) {
                    bestLinesCleared = linesCleared
                    bestBoardAfter = cleared
                }
            }
            workingBoard = bestBoardAfter
        }

        if (!orderSucceeded) continue

        val clearFraction = 1.0 - countFilledCells(workingBoard).toDouble() / TOTAL_CELLS
        if (bestClearFraction == null || clearFraction > bestClearFraction!!) bestClearFraction = clearFraction
    }

    return bestClearFraction
}

/**
 * Draws 3 pieces such that at least one is placeable against the given board, retrying the whole
 * draw up to retryCap times. A higher retryCap yields a more consistently "comfortable" batch
 * (Easy/Medium); a low retryCap barely enforces the guarantee, preserving real risk on Hard/Expert.
 */
fun drawTray(difficulty: String, board: BlockFillBoard, refillRetryCap: Int, random: Random = Random.Default): List<BlockFillPiece?> {
    repeat(refillRetryCap) {
        val tray = listOf(drawWeightedPiece(difficulty, random), drawWeightedPiece(difficulty, random), drawWeightedPiece(difficulty, random))
        val anyFits = tray.any { piece -> findValidPlacements(board, piece.cells).isNotEmpty() }
        if (anyFits) return tray
    }

    // Retry budget exhausted (rare): force a guaranteed fit by replacing slot 0 with a single-cell
    // piece, which fits anywhere the board isn't completely full.
    return listOf(
        BlockFillPiece(shapeId = "single", family = BlockFillShapeFamily.SINGLE, cells = listOf(0 to 0)),
        drawWeightedPiece(difficulty, random),
        drawWeightedPiece(difficulty, random),
    )
}

fun createBlockFillSession(difficulty: String, random: Random = Random.Default): BlockFillSession {
    val config = BLOCKFILL_DIFFICULTY_CONFIG.getValue(difficulty)

    repeat(MAX_GENERATION_ATTEMPTS) {
        val board = generatePreFilledBoard(config.preFillDensity, random)
        val tray = listOf(drawWeightedPiece(difficulty, random), drawWeightedPiece(difficulty, random), drawWeightedPiece(difficulty, random))

        val clearFraction = bestClearFractionForTray(board, tray)
        if (clearFraction == null || clearFraction < config.feasibilityFloor) return@repeat

        val puzzle = BlockFillPuzzle(id = "$difficulty-${System.currentTimeMillis()}", difficulty = difficulty, scoreTarget = config.scoreTarget)
        return BlockFillSession(puzzle = puzzle, board = board, tray = tray, score = 0, comboStreak = 0, status = BlockFillStatus.PLAYING)
    }

    throw IllegalStateException("Block Fill: failed to generate a valid $difficulty puzzle after $MAX_GENERATION_ATTEMPTS attempts")
}
