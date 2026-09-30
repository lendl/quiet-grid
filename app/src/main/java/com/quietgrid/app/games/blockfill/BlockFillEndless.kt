package com.quietgrid.app.games.blockfill

import kotlin.random.Random

data class BlockFillEndlessLevel(
    val rules: BlockFillDealRules,
    val steerGrowthPerSet: Int,
)

const val BLOCKFILL_ENDLESS_STEER_AFTER_MOVES = 18
const val BLOCKFILL_ENDLESS_STEER_CAP_PERCENT = 80
val BLOCKFILL_ENDLESS_START_BLOCKS = 8..12
private const val MOVES_PER_SET = 3
private const val HELPER_PIECES = 2
private const val SECOND_HELPER_TOP_OPTIONS = 2
private const val FILLER_ATTEMPTS = 6

fun blendWeights(a: Map<BlockFillShapeFamily, Int>, b: Map<BlockFillShapeFamily, Int>): Map<BlockFillShapeFamily, Int> =
    (a.keys + b.keys).associateWith { family -> (a[family] ?: 0) + (b[family] ?: 0) }

private fun weightsOf(difficulty: String): Map<BlockFillShapeFamily, Int> = SHAPE_WEIGHTS_BY_DIFFICULTY.getValue(difficulty)

private fun endlessLevel(
    weights: Map<BlockFillShapeFamily, Int>,
    candidates: Int,
    guarantee: BlockFillGuarantee,
    steerGrowthPerSet: Int,
) = BlockFillEndlessLevel(BlockFillDealRules(weights, candidates, guarantee), steerGrowthPerSet)

val BLOCKFILL_ENDLESS_LEVELS: List<BlockFillEndlessLevel> = listOf(
    endlessLevel(weightsOf("easy"), 1, BlockFillGuarantee.ALL_THREE, 10),
    endlessLevel(blendWeights(weightsOf("easy"), weightsOf("medium")), 1, BlockFillGuarantee.ALL_THREE, 10),
    endlessLevel(weightsOf("medium"), 1, BlockFillGuarantee.ALL_THREE, 10),
    endlessLevel(blendWeights(weightsOf("medium"), weightsOf("hard")), 2, BlockFillGuarantee.ALL_THREE, 8),
    endlessLevel(weightsOf("hard"), 2, BlockFillGuarantee.ALL_THREE, 8),
    endlessLevel(blendWeights(weightsOf("hard"), weightsOf("expert")), 3, BlockFillGuarantee.ALL_THREE, 8),
    endlessLevel(weightsOf("expert"), 3, BlockFillGuarantee.AT_LEAST_TWO, 6),
    endlessLevel(weightsOf("expert"), 4, BlockFillGuarantee.AT_LEAST_TWO, 6),
    endlessLevel(weightsOf("expert"), 5, BlockFillGuarantee.AT_LEAST_ONE, 6),
    endlessLevel(weightsOf("expert"), 6, BlockFillGuarantee.AT_LEAST_ONE, 6),
)

fun endlessLevelFor(multiplier: Int): BlockFillEndlessLevel =
    BLOCKFILL_ENDLESS_LEVELS[multiplier.coerceIn(1, BLOCKFILL_ENDLESS_LEVELS.size) - 1]

fun endlessSteerChancePercent(movesSinceClear: Int, level: BlockFillEndlessLevel): Int {
    if (movesSinceClear < BLOCKFILL_ENDLESS_STEER_AFTER_MOVES) return 0
    val setsPastWindow = (movesSinceClear - BLOCKFILL_ENDLESS_STEER_AFTER_MOVES) / MOVES_PER_SET + 1
    return minOf(BLOCKFILL_ENDLESS_STEER_CAP_PERCENT, setsPastWindow * level.steerGrowthPerSet)
}

fun drawEndlessTray(board: BlockFillBoard, multiplier: Int, movesSinceClear: Int, random: Random): List<BlockFillPiece?> {
    val chance = endlessSteerChancePercent(movesSinceClear, endlessLevelFor(multiplier))
    val steer = chance > 0 && random.nextInt(100) < chance
    return drawEndlessTray(board, multiplier, random, steer)
}

internal fun drawEndlessTray(board: BlockFillBoard, multiplier: Int, random: Random, steer: Boolean): List<BlockFillPiece?> {
    val rules = endlessLevelFor(multiplier).rules
    if (!steer) return drawTray(rules, board, random = random)
    return constructClearingSet(board, rules, random) ?: drawTray(rules, board, random = random)
}

private fun lineFillScore(board: BlockFillBoard): Int {
    val rows = (0 until BLOCKFILL_BOARD_SIZE).sumOf { row -> board[row].count { it != null }.let { it * it } }
    val cols = (0 until BLOCKFILL_BOARD_SIZE).sumOf { col -> board.count { it[col] != null }.let { it * it } }
    return rows + cols
}

private class HelperOption(val shape: BlockFillShapeDef, val after: BlockFillBoard, val removed: Int, val remaining: Int, val fill: Int)

private fun bestHelperOptions(board: BlockFillBoard): List<HelperOption> {
    val filledBefore = countFilledCells(board)
    return ALL_SHAPES.flatMap { shape ->
        findValidPlacements(board, shape.cells).map { (row, col) ->
            val (after, _) = clearFullLines(placePieceAt(board, shape.cells, row, col, shape.family))
            val remaining = countFilledCells(after)
            HelperOption(shape, after, filledBefore + shape.cells.size - remaining, remaining, lineFillScore(after))
        }
    }.filter { it.shape.family != BlockFillShapeFamily.SINGLE || it.removed > 1 }.sortedWith(
        compareByDescending<HelperOption> { it.removed }
            .thenBy { if (it.removed > 0) it.remaining else 0 }
            .thenByDescending { it.fill },
    )
}

fun constructClearingSet(board: BlockFillBoard, rules: BlockFillDealRules, random: Random): List<BlockFillPiece>? {
    var working = board
    val helpers = mutableListOf<BlockFillPiece>()
    repeat(HELPER_PIECES) { index ->
        val options = bestHelperOptions(working)
        if (options.isEmpty()) return null
        val pick = if (index == 0) options.first() else options.take(SECOND_HELPER_TOP_OPTIONS).random(random)
        helpers.add(shapeDefToPiece(pick.shape))
        working = pick.after
    }
    val fillerWeights = rules.weights - BlockFillShapeFamily.SINGLE
    repeat(FILLER_ATTEMPTS) {
        val set = (helpers + drawWeightedPiece(fillerWeights, random)).shuffled(random)
        if (canPlaceAtLeast(board, set, rules.guarantee.placeableCount)) return set
    }
    return null
}

private const val ENDLESS_START_ATTEMPTS = 500

fun createBlockFillEndlessSession(bestAtStart: Int, random: Random = Random.Default): BlockFillEndlessSession {
    repeat(ENDLESS_START_ATTEMPTS) {
        val style = pickClutterStyle(random)
        val counts = clutterCountsFor(style, BLOCKFILL_ENDLESS_START_BLOCKS)
        if (counts.isEmpty()) return@repeat
        val board = generateClutter(style, counts.random(random), random)
        val tray = drawEndlessTray(board, 1, 0, random)
        if (!canPlaceTrayInSomeOrder(board, tray.filterNotNull())) return@repeat
        return BlockFillEndlessSession(
            board = board,
            tray = tray,
            score = 0,
            comboStreak = 0,
            multiplier = 1,
            moves = 0,
            movesSinceClear = 0,
            linesCleared = 0,
            bestAtStart = bestAtStart,
            status = BlockFillStatus.PLAYING,
        )
    }
    throw IllegalStateException("Block Fill: failed to start an endless run after $ENDLESS_START_ATTEMPTS attempts")
}

fun applyBlockFillEndlessPlacement(
    session: BlockFillEndlessSession,
    pieceIndex: Int,
    anchorRow: Int,
    anchorCol: Int,
    random: Random = Random.Default,
): BlockFillEndlessSession? {
    if (session.status != BlockFillStatus.PLAYING) return null
    val piece = session.tray.getOrNull(pieceIndex) ?: return null
    val outcome = resolvePlacement(session.board, piece, anchorRow, anchorCol, session.comboStreak, session.multiplier) ?: return null

    val trayAfterRemoval = session.tray.mapIndexed { index, p -> if (index == pieceIndex) null else p }
    val nextMultiplier = if (outcome.boardEmptied) session.multiplier + 1 else session.multiplier
    val nextMovesSinceClear = if (outcome.boardEmptied) 0 else session.movesSinceClear + 1
    val nextTray = if (trayAfterRemoval.all { it == null }) {
        drawEndlessTray(outcome.board, nextMultiplier, nextMovesSinceClear, random)
    } else {
        trayAfterRemoval
    }
    val status = if (nextTray.any { it != null && pieceFitsAnywhere(outcome.board, it.cells) }) BlockFillStatus.PLAYING else BlockFillStatus.LOST

    return session.copy(
        board = outcome.board,
        tray = nextTray,
        score = session.score + outcome.gained,
        comboStreak = if (outcome.linesCleared > 0) session.comboStreak + 1 else 0,
        multiplier = nextMultiplier,
        moves = session.moves + 1,
        movesSinceClear = nextMovesSinceClear,
        linesCleared = session.linesCleared + outcome.linesCleared,
        status = status,
    )
}
