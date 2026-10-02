package com.quietgrid.app.games.blockfill

import kotlin.random.Random

const val BLOCKFILL_REFILL_RETRY_CAP = 25

val BLOCKFILL_SCORE_TARGETS: Map<String, Int> = mapOf(
    "easy" to 1000,
    "medium" to 1500,
    "hard" to 2000,
    "expert" to 2500,
)

val BLOCKFILL_STARTING_BLOCKS: Map<String, IntRange> = mapOf(
    "easy" to 4..8,
    "medium" to 6..10,
    "hard" to 8..12,
    "expert" to 10..14,
)

val BLOCKFILL_SET_CANDIDATES: Map<String, Int> = mapOf(
    "easy" to 1,
    "medium" to 1,
    "hard" to 2,
    "expert" to 4,
)

private const val SAFE_FIRST_MOVE_CAP = 24
private const val MAX_GENERATION_ATTEMPTS = 500
private const val CLUTTER_PLACEMENT_ATTEMPTS = 2000
private const val HALF_WIDTH = BLOCKFILL_BOARD_SIZE / 2
private const val LAST_INDEX = BLOCKFILL_BOARD_SIZE - 1

private val CLUTTER_SHAPES: List<List<Pair<Int, Int>>> = listOf(
    "domino-h", "domino-v",
    "corner-tromino-a", "corner-tromino-b", "corner-tromino-c", "corner-tromino-d",
    "straight3-h", "straight3-v", "square2x2",
).map { id -> ALL_SHAPES.first { it.id == id }.cells }

fun pickClutterStyle(random: Random): BlockFillClutterStyle = BlockFillClutterStyle.entries.random(random)

fun clutterCountsFor(style: BlockFillClutterStyle, range: IntRange): List<Int> = range.filter { count ->
    when (style) {
        BlockFillClutterStyle.DIAGONAL_MIRROR -> true
        BlockFillClutterStyle.FOUR_WAY -> count % 4 == 0
        BlockFillClutterStyle.MIRRORED_SHAPES -> count % 2 == 0 && count != 2
        BlockFillClutterStyle.MIRROR, BlockFillClutterStyle.ROTATIONAL -> count % 2 == 0
    }
}

private fun mirrored(cells: List<Pair<Int, Int>>): List<Pair<Int, Int>> = cells + cells.map { (row, col) -> row to LAST_INDEX - col }

private fun clutterCandidate(style: BlockFillClutterStyle, remaining: Int, random: Random): List<Pair<Int, Int>>? = when (style) {
    BlockFillClutterStyle.MIRROR -> mirrored(listOf(random.nextInt(BLOCKFILL_BOARD_SIZE) to random.nextInt(HALF_WIDTH)))
    BlockFillClutterStyle.ROTATIONAL -> {
        val row = random.nextInt(BLOCKFILL_BOARD_SIZE)
        val col = random.nextInt(BLOCKFILL_BOARD_SIZE)
        listOf(row to col, LAST_INDEX - row to LAST_INDEX - col)
    }
    BlockFillClutterStyle.MIRRORED_SHAPES -> {
        val fitting = CLUTTER_SHAPES.filter { shape -> shape.size * 2 <= remaining && remaining - shape.size * 2 != 2 }
        if (fitting.isEmpty()) {
            null
        } else {
            val shape = fitting.random(random)
            val anchorRow = random.nextInt(BLOCKFILL_BOARD_SIZE)
            val anchorCol = random.nextInt(HALF_WIDTH)
            val half = shape.map { (dr, dc) -> anchorRow + dr to anchorCol + dc }
            if (half.any { (row, col) -> row >= BLOCKFILL_BOARD_SIZE || col >= HALF_WIDTH }) null else mirrored(half)
        }
    }
    BlockFillClutterStyle.DIAGONAL_MIRROR -> {
        if (remaining == 1) {
            val onDiagonal = random.nextInt(BLOCKFILL_BOARD_SIZE)
            listOf(onDiagonal to onDiagonal)
        } else {
            val row = random.nextInt(BLOCKFILL_BOARD_SIZE)
            val col = random.nextInt(BLOCKFILL_BOARD_SIZE)
            if (row == col) listOf(row to col) else listOf(row to col, col to row)
        }
    }
    BlockFillClutterStyle.FOUR_WAY -> {
        val row = random.nextInt(HALF_WIDTH)
        val col = random.nextInt(HALF_WIDTH)
        listOf(row to col, row to LAST_INDEX - col, LAST_INDEX - row to col, LAST_INDEX - row to LAST_INDEX - col)
    }
}

private fun withClutter(board: BlockFillBoard, cells: List<Pair<Int, Int>>): BlockFillBoard? {
    if (cells.any { (row, col) -> board[row][col] != null }) return null
    var next = board
    for ((row, col) in cells) next = placePieceAt(next, listOf(0 to 0), row, col, BlockFillShapeFamily.STONE)
    return if (fullLines(next).let { it.rows.isEmpty() && it.cols.isEmpty() }) next else null
}

fun generateClutter(style: BlockFillClutterStyle, blockCount: Int, random: Random): BlockFillBoard {
    var board = createEmptyBoard()
    var placed = 0
    repeat(CLUTTER_PLACEMENT_ATTEMPTS) {
        if (placed >= blockCount) return board
        val cells = clutterCandidate(style, blockCount - placed, random) ?: return@repeat
        if (cells.size > blockCount - placed) return@repeat
        board = withClutter(board, cells) ?: return@repeat
        placed += cells.size
    }
    if (style != BlockFillClutterStyle.MIRRORED_SHAPES) return board
    repeat(CLUTTER_PLACEMENT_ATTEMPTS) {
        if (placed >= blockCount) return board
        val cells = mirrored(listOf(random.nextInt(BLOCKFILL_BOARD_SIZE) to random.nextInt(HALF_WIDTH)))
        board = withClutter(board, cells) ?: return@repeat
        placed += cells.size
    }
    return board
}

fun canPlaceTrayInSomeOrder(board: BlockFillBoard, tray: List<BlockFillPiece>): Boolean {
    if (tray.isEmpty()) return true
    if (tray.size == 1) return pieceFitsAnywhere(board, tray.single().cells)
    return tray.indices.any { index ->
        val piece = tray[index]
        val rest = tray.filterIndexed { other, _ -> other != index }
        findValidPlacements(board, piece.cells).any { (row, col) ->
            val (afterClear, _) = clearFullLines(placePieceAt(board, piece.cells, row, col, piece.family))
            canPlaceTrayInSomeOrder(afterClear, rest)
        }
    }
}

fun safeFirstMoveCount(board: BlockFillBoard, tray: List<BlockFillPiece>, cap: Int): Int {
    var count = 0
    tray.forEachIndexed { index, piece ->
        val rest = tray.filterIndexed { other, _ -> other != index }
        for ((row, col) in findValidPlacements(board, piece.cells)) {
            val (afterClear, _) = clearFullLines(placePieceAt(board, piece.cells, row, col, piece.family))
            if (canPlaceTrayInSomeOrder(afterClear, rest)) {
                count++
                if (count >= cap) return count
            }
        }
    }
    return count
}

enum class BlockFillGuarantee(val placeableCount: Int) { ALL_THREE(3), AT_LEAST_TWO(2), AT_LEAST_ONE(1) }

data class BlockFillDealRules(
    val weights: Map<BlockFillShapeFamily, Int>,
    val candidateCount: Int,
    val guarantee: BlockFillGuarantee,
)

fun blockFillRulesFor(difficulty: String): BlockFillDealRules = BlockFillDealRules(
    weights = SHAPE_WEIGHTS_BY_DIFFICULTY.getValue(difficulty),
    candidateCount = BLOCKFILL_SET_CANDIDATES.getValue(difficulty),
    guarantee = BlockFillGuarantee.ALL_THREE,
)

private fun subsetsOfSize(items: List<BlockFillPiece>, size: Int): List<List<BlockFillPiece>> = when {
    size == 0 -> listOf(emptyList())
    items.size < size -> emptyList()
    else -> subsetsOfSize(items.drop(1), size - 1).map { listOf(items.first()) + it } + subsetsOfSize(items.drop(1), size)
}

fun canPlaceAtLeast(board: BlockFillBoard, tray: List<BlockFillPiece>, count: Int): Boolean =
    subsetsOfSize(tray, minOf(count, tray.size)).any { canPlaceTrayInSomeOrder(board, it) }

internal fun drawCandidateSets(
    rules: BlockFillDealRules,
    board: BlockFillBoard,
    random: Random,
    count: Int,
    refillRetryCap: Int = BLOCKFILL_REFILL_RETRY_CAP,
): List<List<BlockFillPiece>> {
    val candidates = mutableListOf<List<BlockFillPiece>>()
    repeat(refillRetryCap) {
        if (candidates.size >= count) return candidates
        val tray = drawBalancedSet(rules.weights, random)
        if (canPlaceAtLeast(board, tray, rules.guarantee.placeableCount)) candidates.add(tray)
    }
    return candidates
}

fun drawTray(
    rules: BlockFillDealRules,
    board: BlockFillBoard,
    refillRetryCap: Int = BLOCKFILL_REFILL_RETRY_CAP,
    random: Random = Random.Default,
): List<BlockFillPiece?> {
    val candidates = drawCandidateSets(rules, board, random, rules.candidateCount, refillRetryCap)
    if (candidates.isNotEmpty()) {
        if (candidates.size == 1) return candidates.single()
        return candidates.minBy { safeFirstMoveCount(board, it, SAFE_FIRST_MOVE_CAP) }
    }
    return drawBalancedSet(rules.weights, random, fixed = listOf(SINGLE_PIECE))
}

fun drawTray(
    difficulty: String,
    board: BlockFillBoard,
    refillRetryCap: Int = BLOCKFILL_REFILL_RETRY_CAP,
    random: Random = Random.Default,
    candidateCount: Int = BLOCKFILL_SET_CANDIDATES.getValue(difficulty),
): List<BlockFillPiece?> =
    drawTray(blockFillRulesFor(difficulty).copy(candidateCount = candidateCount), board, refillRetryCap, random)

private val HEAVY_FAMILIES = setOf(BlockFillShapeFamily.PLUS, BlockFillShapeFamily.DIAGONAL_STAIRCASE3, BlockFillShapeFamily.SQUARE3X3)
private val SINGLE_PIECE = BlockFillPiece(shapeId = "single", family = BlockFillShapeFamily.SINGLE, cells = listOf(0 to 0))
private const val SET_SIZE = 3
private const val BALANCED_SET_ATTEMPTS = 50

fun isBalancedSet(set: List<BlockFillPiece>): Boolean =
    set.count { it.family in HEAVY_FAMILIES } <= 1 && set.map { it.family }.distinct().size > 1

private fun drawBalancedSet(
    weights: Map<BlockFillShapeFamily, Int>,
    random: Random,
    fixed: List<BlockFillPiece> = emptyList(),
): List<BlockFillPiece> {
    var set = fixed
    repeat(BALANCED_SET_ATTEMPTS) {
        set = fixed + List(SET_SIZE - fixed.size) { drawWeightedPiece(weights, random) }
        if (isBalancedSet(set)) return set
    }
    return set
}

fun createBlockFillSession(difficulty: String, random: Random = Random.Default): BlockFillSession {
    val blockRange = BLOCKFILL_STARTING_BLOCKS.getValue(difficulty)

    repeat(MAX_GENERATION_ATTEMPTS) {
        val style = pickClutterStyle(random)
        val counts = clutterCountsFor(style, blockRange)
        if (counts.isEmpty()) return@repeat
        val board = generateClutter(style, counts.random(random), random)
        val tray = drawTray(difficulty, board, random = random)
        if (!canPlaceTrayInSomeOrder(board, tray.filterNotNull())) return@repeat

        val puzzle = BlockFillPuzzle(id = "$difficulty-${System.currentTimeMillis()}", difficulty = difficulty, scoreTarget = BLOCKFILL_SCORE_TARGETS.getValue(difficulty))
        return BlockFillSession(puzzle = puzzle, board = board, tray = tray, score = 0, comboStreak = 0, status = BlockFillStatus.PLAYING)
    }

    throw IllegalStateException("Block Fill: failed to generate a valid $difficulty puzzle after $MAX_GENERATION_ATTEMPTS attempts")
}
