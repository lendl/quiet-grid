package com.quietgrid.app.games.blockfill

import kotlinx.serialization.Serializable

const val BLOCKFILL_BOARD_SIZE = 8

@Serializable
enum class BlockFillShapeFamily {
    SINGLE, DOMINO, DIAGONAL_DOMINO, STRAIGHT3, CORNER_TROMINO, DIAGONAL_STAIRCASE3,
    SQUARE2X2, STRAIGHT4, T_TETROMINO, SZ, LJ, PLUS, STRAIGHT5, RECTANGLE, SQUARE3X3,
}

data class BlockFillShapeDef(
    val id: String,
    val family: BlockFillShapeFamily,
    val cells: List<Pair<Int, Int>>,
)

@Serializable
data class BlockFillPiece(
    val shapeId: String,
    val family: BlockFillShapeFamily,
    val cells: List<Pair<Int, Int>>,
)

typealias BlockFillBoard = List<List<BlockFillShapeFamily?>>

@Serializable
enum class BlockFillStatus { PLAYING, WON, LOST }

@Serializable
data class BlockFillPuzzle(
    val id: String,
    val difficulty: String,
    val scoreTarget: Int,
)

data class BlockFillSession(
    val puzzle: BlockFillPuzzle,
    val board: BlockFillBoard,
    val tray: List<BlockFillPiece?>,
    val score: Int,
    val comboStreak: Int,
    val status: BlockFillStatus,
)

@Serializable
data class BlockFillPersistedSession(
    val puzzle: BlockFillPuzzle,
    val board: BlockFillBoard,
    val tray: List<BlockFillPiece?>,
    val score: Int,
    val comboStreak: Int,
    val status: BlockFillStatus,
)
