package com.quietgrid.engine.circuit

import kotlinx.serialization.Serializable

const val CIRCUIT_UP = 1
const val CIRCUIT_RIGHT = 2
const val CIRCUIT_DOWN = 4
const val CIRCUIT_LEFT = 8
const val CIRCUIT_ALL_ARMS = 15

val CIRCUIT_DIRECTIONS = intArrayOf(CIRCUIT_UP, CIRCUIT_RIGHT, CIRCUIT_DOWN, CIRCUIT_LEFT)

@Serializable
data class CircuitPuzzleEntry(
    val id: String,
    val rows: Int,
    val cols: Int,
    val difficulty: String,
    val source: Int,
    val wrap: Boolean = false,
    val solution: List<Int>,
    val start: List<Int>,
    val rating: Int? = null,
)

fun rotateCircuitMask(mask: Int, quarterTurns: Int = 1): Int {
    val turns = ((quarterTurns % 4) + 4) % 4
    if (turns == 0) return mask and CIRCUIT_ALL_ARMS
    return ((mask shl turns) or (mask ushr (4 - turns))) and CIRCUIT_ALL_ARMS
}

fun oppositeCircuitDirection(direction: Int): Int = rotateCircuitMask(direction, 2)

fun circuitArmCount(mask: Int): Int = Integer.bitCount(mask and CIRCUIT_ALL_ARMS)

fun circuitOrientations(mask: Int): IntArray = (0 until 4).map { rotateCircuitMask(mask, it) }.distinct().toIntArray()

fun circuitTurnsBetween(from: Int, to: Int): Int? = (0 until 4).firstOrNull { rotateCircuitMask(from, it) == to }

fun isCircuitStraight(mask: Int): Boolean = mask == (CIRCUIT_UP or CIRCUIT_DOWN) || mask == (CIRCUIT_LEFT or CIRCUIT_RIGHT)

class CircuitBoard(val rows: Int, val cols: Int, val source: Int, val wrap: Boolean = false) {
    val size: Int = rows * cols

    fun row(cell: Int): Int = cell / cols

    fun col(cell: Int): Int = cell % cols

    fun neighbor(cell: Int, direction: Int): Int {
        val row = row(cell)
        val col = col(cell)
        val (nextRow, nextCol) = when (direction) {
            CIRCUIT_UP -> row - 1 to col
            CIRCUIT_DOWN -> row + 1 to col
            CIRCUIT_LEFT -> row to col - 1
            CIRCUIT_RIGHT -> row to col + 1
            else -> return -1
        }
        if (wrap) return ((nextRow + rows) % rows) * cols + (nextCol + cols) % cols
        if (nextRow !in 0 until rows || nextCol !in 0 until cols) return -1
        return nextRow * cols + nextCol
    }

    fun wrapsAcross(cell: Int, direction: Int): Boolean {
        if (!wrap) return false
        return when (direction) {
            CIRCUIT_UP -> row(cell) == 0
            CIRCUIT_DOWN -> row(cell) == rows - 1
            CIRCUIT_LEFT -> col(cell) == 0
            CIRCUIT_RIGHT -> col(cell) == cols - 1
            else -> false
        }
    }
}

fun CircuitPuzzleEntry.toBoard(): CircuitBoard = CircuitBoard(rows, cols, source, wrap)

fun circuitArmMatched(board: CircuitBoard, masks: IntArray, cell: Int, direction: Int): Boolean {
    if (masks[cell] and direction == 0) return false
    val other = board.neighbor(cell, direction)
    return other >= 0 && masks[other] and oppositeCircuitDirection(direction) != 0
}

fun circuitLitCells(board: CircuitBoard, masks: IntArray): BooleanArray {
    val lit = BooleanArray(board.size)
    val queue = ArrayDeque(listOf(board.source))
    lit[board.source] = true
    while (queue.isNotEmpty()) {
        val cell = queue.removeFirst()
        for (direction in CIRCUIT_DIRECTIONS) {
            if (!circuitArmMatched(board, masks, cell, direction)) continue
            val other = board.neighbor(cell, direction)
            if (!lit[other]) {
                lit[other] = true
                queue.add(other)
            }
        }
    }
    return lit
}

fun isCircuitSolved(board: CircuitBoard, masks: IntArray): Boolean {
    if (masks.size != board.size) return false
    var links = 0
    for (cell in 0 until board.size) {
        for (direction in CIRCUIT_DIRECTIONS) {
            if (masks[cell] and direction == 0) continue
            if (!circuitArmMatched(board, masks, cell, direction)) return false
            links++
        }
    }
    if (links != 2 * (board.size - 1)) return false
    return circuitLitCells(board, masks).all { it }
}

fun circuitMasksFromTurns(start: List<Int>, turns: List<Int>): IntArray =
    IntArray(start.size) { rotateCircuitMask(start[it], turns[it]) }
