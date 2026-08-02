package com.quietgrid.app.games.blockfill

import kotlin.random.Random

val ALL_SHAPES: List<BlockFillShapeDef> = listOf(
    BlockFillShapeDef("single", BlockFillShapeFamily.SINGLE, listOf(0 to 0)),

    BlockFillShapeDef("domino-h", BlockFillShapeFamily.DOMINO, listOf(0 to 0, 0 to 1)),
    BlockFillShapeDef("domino-v", BlockFillShapeFamily.DOMINO, listOf(0 to 0, 1 to 0)),

    BlockFillShapeDef("diagonal-domino-a", BlockFillShapeFamily.DIAGONAL_DOMINO, listOf(0 to 0, 1 to 1)),
    BlockFillShapeDef("diagonal-domino-b", BlockFillShapeFamily.DIAGONAL_DOMINO, listOf(0 to 1, 1 to 0)),

    BlockFillShapeDef("straight3-h", BlockFillShapeFamily.STRAIGHT3, listOf(0 to 0, 0 to 1, 0 to 2)),
    BlockFillShapeDef("straight3-v", BlockFillShapeFamily.STRAIGHT3, listOf(0 to 0, 1 to 0, 2 to 0)),

    BlockFillShapeDef("corner-tromino-a", BlockFillShapeFamily.CORNER_TROMINO, listOf(0 to 0, 1 to 0, 1 to 1)),
    BlockFillShapeDef("corner-tromino-b", BlockFillShapeFamily.CORNER_TROMINO, listOf(0 to 1, 1 to 0, 1 to 1)),
    BlockFillShapeDef("corner-tromino-c", BlockFillShapeFamily.CORNER_TROMINO, listOf(0 to 0, 0 to 1, 1 to 0)),
    BlockFillShapeDef("corner-tromino-d", BlockFillShapeFamily.CORNER_TROMINO, listOf(0 to 0, 0 to 1, 1 to 1)),

    BlockFillShapeDef("diagonal-staircase3-a", BlockFillShapeFamily.DIAGONAL_STAIRCASE3, listOf(0 to 0, 1 to 1, 2 to 2)),
    BlockFillShapeDef("diagonal-staircase3-b", BlockFillShapeFamily.DIAGONAL_STAIRCASE3, listOf(0 to 2, 1 to 1, 2 to 0)),

    BlockFillShapeDef("square2x2", BlockFillShapeFamily.SQUARE2X2, listOf(0 to 0, 0 to 1, 1 to 0, 1 to 1)),

    BlockFillShapeDef("straight4-h", BlockFillShapeFamily.STRAIGHT4, listOf(0 to 0, 0 to 1, 0 to 2, 0 to 3)),
    BlockFillShapeDef("straight4-v", BlockFillShapeFamily.STRAIGHT4, listOf(0 to 0, 1 to 0, 2 to 0, 3 to 0)),

    BlockFillShapeDef("t-down", BlockFillShapeFamily.T_TETROMINO, listOf(0 to 0, 0 to 1, 0 to 2, 1 to 1)),
    BlockFillShapeDef("t-up", BlockFillShapeFamily.T_TETROMINO, listOf(0 to 1, 1 to 0, 1 to 1, 1 to 2)),
    BlockFillShapeDef("t-right", BlockFillShapeFamily.T_TETROMINO, listOf(0 to 0, 1 to 0, 1 to 1, 2 to 0)),
    BlockFillShapeDef("t-left", BlockFillShapeFamily.T_TETROMINO, listOf(0 to 1, 1 to 0, 1 to 1, 2 to 1)),

    BlockFillShapeDef("s-h", BlockFillShapeFamily.SZ, listOf(0 to 1, 0 to 2, 1 to 0, 1 to 1)),
    BlockFillShapeDef("s-v", BlockFillShapeFamily.SZ, listOf(0 to 0, 1 to 0, 1 to 1, 2 to 1)),
    BlockFillShapeDef("z-h", BlockFillShapeFamily.SZ, listOf(0 to 0, 0 to 1, 1 to 1, 1 to 2)),
    BlockFillShapeDef("z-v", BlockFillShapeFamily.SZ, listOf(0 to 1, 1 to 0, 1 to 1, 2 to 0)),

    BlockFillShapeDef("l-0", BlockFillShapeFamily.LJ, listOf(0 to 0, 1 to 0, 2 to 0, 2 to 1)),
    BlockFillShapeDef("l-90", BlockFillShapeFamily.LJ, listOf(0 to 0, 0 to 1, 0 to 2, 1 to 0)),
    BlockFillShapeDef("l-180", BlockFillShapeFamily.LJ, listOf(0 to 0, 0 to 1, 1 to 1, 2 to 1)),
    BlockFillShapeDef("l-270", BlockFillShapeFamily.LJ, listOf(0 to 2, 1 to 0, 1 to 1, 1 to 2)),
    BlockFillShapeDef("j-0", BlockFillShapeFamily.LJ, listOf(0 to 1, 1 to 1, 2 to 0, 2 to 1)),
    BlockFillShapeDef("j-90", BlockFillShapeFamily.LJ, listOf(0 to 0, 1 to 0, 1 to 1, 1 to 2)),
    BlockFillShapeDef("j-180", BlockFillShapeFamily.LJ, listOf(0 to 0, 0 to 1, 1 to 0, 2 to 0)),
    BlockFillShapeDef("j-270", BlockFillShapeFamily.LJ, listOf(0 to 0, 0 to 1, 0 to 2, 1 to 2)),

    BlockFillShapeDef("plus", BlockFillShapeFamily.PLUS, listOf(0 to 1, 1 to 0, 1 to 1, 1 to 2, 2 to 1)),

    BlockFillShapeDef("straight5-h", BlockFillShapeFamily.STRAIGHT5, listOf(0 to 0, 0 to 1, 0 to 2, 0 to 3, 0 to 4)),
    BlockFillShapeDef("straight5-v", BlockFillShapeFamily.STRAIGHT5, listOf(0 to 0, 1 to 0, 2 to 0, 3 to 0, 4 to 0)),

    BlockFillShapeDef("rect2x3", BlockFillShapeFamily.RECTANGLE, listOf(0 to 0, 0 to 1, 0 to 2, 1 to 0, 1 to 1, 1 to 2)),
    BlockFillShapeDef("rect3x2", BlockFillShapeFamily.RECTANGLE, listOf(0 to 0, 0 to 1, 1 to 0, 1 to 1, 2 to 0, 2 to 1)),

    BlockFillShapeDef(
        "square3x3",
        BlockFillShapeFamily.SQUARE3X3,
        listOf(0 to 0, 0 to 1, 0 to 2, 1 to 0, 1 to 1, 1 to 2, 2 to 0, 2 to 1, 2 to 2),
    ),
)

// Relative weights per shape family, per difficulty key ("easy"/"medium"/"hard"/"expert"). Arbitrary
// units, not required to sum to 100. Ported verbatim from the RN branch's design-spec numbers.
// Individual oriented pieces within a family split that family's weight evenly, so a family with more
// orientations (L/J has 8) doesn't implicitly dominate a family with fewer (plus has 1).
val SHAPE_WEIGHTS_BY_DIFFICULTY: Map<String, Map<BlockFillShapeFamily, Int>> = mapOf(
    "easy" to mapOf(
        BlockFillShapeFamily.SINGLE to 20, BlockFillShapeFamily.DOMINO to 16, BlockFillShapeFamily.STRAIGHT3 to 14,
        BlockFillShapeFamily.CORNER_TROMINO to 12, BlockFillShapeFamily.SQUARE2X2 to 10, BlockFillShapeFamily.STRAIGHT4 to 8,
        BlockFillShapeFamily.RECTANGLE to 6, BlockFillShapeFamily.STRAIGHT5 to 5, BlockFillShapeFamily.T_TETROMINO to 4,
        BlockFillShapeFamily.SQUARE3X3 to 3, BlockFillShapeFamily.DIAGONAL_DOMINO to 1, BlockFillShapeFamily.LJ to 1,
        BlockFillShapeFamily.SZ to 0, BlockFillShapeFamily.DIAGONAL_STAIRCASE3 to 0, BlockFillShapeFamily.PLUS to 0,
    ),
    "medium" to mapOf(
        BlockFillShapeFamily.SINGLE to 10, BlockFillShapeFamily.DOMINO to 9, BlockFillShapeFamily.STRAIGHT3 to 9,
        BlockFillShapeFamily.CORNER_TROMINO to 8, BlockFillShapeFamily.SQUARE2X2 to 8, BlockFillShapeFamily.STRAIGHT4 to 8,
        BlockFillShapeFamily.RECTANGLE to 7, BlockFillShapeFamily.STRAIGHT5 to 7, BlockFillShapeFamily.T_TETROMINO to 7,
        BlockFillShapeFamily.SQUARE3X3 to 6, BlockFillShapeFamily.DIAGONAL_DOMINO to 5, BlockFillShapeFamily.LJ to 5,
        BlockFillShapeFamily.SZ to 4, BlockFillShapeFamily.DIAGONAL_STAIRCASE3 to 2, BlockFillShapeFamily.PLUS to 1,
    ),
    "hard" to mapOf(
        BlockFillShapeFamily.SINGLE to 5, BlockFillShapeFamily.DOMINO to 5, BlockFillShapeFamily.STRAIGHT3 to 5,
        BlockFillShapeFamily.CORNER_TROMINO to 5, BlockFillShapeFamily.SQUARE2X2 to 6, BlockFillShapeFamily.STRAIGHT4 to 6,
        BlockFillShapeFamily.RECTANGLE to 6, BlockFillShapeFamily.STRAIGHT5 to 6, BlockFillShapeFamily.T_TETROMINO to 6,
        BlockFillShapeFamily.SQUARE3X3 to 7, BlockFillShapeFamily.DIAGONAL_DOMINO to 7, BlockFillShapeFamily.LJ to 8,
        BlockFillShapeFamily.SZ to 8, BlockFillShapeFamily.DIAGONAL_STAIRCASE3 to 7, BlockFillShapeFamily.PLUS to 8,
    ),
    "expert" to mapOf(
        BlockFillShapeFamily.SINGLE to 2, BlockFillShapeFamily.DOMINO to 2, BlockFillShapeFamily.STRAIGHT3 to 3,
        BlockFillShapeFamily.CORNER_TROMINO to 3, BlockFillShapeFamily.SQUARE2X2 to 3, BlockFillShapeFamily.STRAIGHT4 to 4,
        BlockFillShapeFamily.RECTANGLE to 4, BlockFillShapeFamily.STRAIGHT5 to 5, BlockFillShapeFamily.T_TETROMINO to 5,
        BlockFillShapeFamily.SQUARE3X3 to 6, BlockFillShapeFamily.DIAGONAL_DOMINO to 6, BlockFillShapeFamily.LJ to 8,
        BlockFillShapeFamily.SZ to 9, BlockFillShapeFamily.DIAGONAL_STAIRCASE3 to 10, BlockFillShapeFamily.PLUS to 12,
    ),
)

private val SHAPES_BY_FAMILY: Map<BlockFillShapeFamily, List<BlockFillShapeDef>> =
    ALL_SHAPES.groupBy { it.family }

fun shapeDefToPiece(shape: BlockFillShapeDef): BlockFillPiece =
    BlockFillPiece(shapeId = shape.id, family = shape.family, cells = shape.cells)

/**
 * Weighted-random single piece draw for the given difficulty key. Family weight is split evenly
 * across that family's oriented variants so a family with more orientations doesn't implicitly
 * dominate one with fewer, beyond what the weight table itself intends.
 */
fun drawWeightedPiece(difficulty: String, random: Random = Random.Default): BlockFillPiece {
    val familyWeights = SHAPE_WEIGHTS_BY_DIFFICULTY.getValue(difficulty)
    val entries = mutableListOf<Pair<BlockFillShapeDef, Double>>()

    for ((family, weight) in familyWeights) {
        if (weight <= 0) continue
        val familyShapes = SHAPES_BY_FAMILY[family] ?: continue
        val perShapeWeight = weight.toDouble() / familyShapes.size
        for (shape in familyShapes) entries.add(shape to perShapeWeight)
    }

    val totalWeight = entries.sumOf { it.second }
    var roll = random.nextDouble() * totalWeight
    for ((shape, weight) in entries) {
        roll -= weight
        if (roll <= 0) return shapeDefToPiece(shape)
    }
    return shapeDefToPiece(entries.last().first)
}
