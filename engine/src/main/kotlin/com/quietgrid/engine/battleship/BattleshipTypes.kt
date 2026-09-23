package com.quietgrid.engine.battleship

import kotlinx.serialization.Serializable

enum class BattleshipCell { UNKNOWN, WATER, SHIP }

typealias BattleshipBoard = List<List<BattleshipCell>>

@Serializable
data class BattleshipPuzzleEntry(
    val id: String,
    val size: Int,
    val difficulty: String,
    val rowClues: List<Int>,
    val colClues: List<Int>,
    val fleet: List<Int>,
    val solution: String,
    val givens: String,
)

fun encodeBattleshipSolution(shipCells: List<List<Boolean>>): String =
    shipCells.joinToString("") { row -> row.joinToString("") { if (it) "1" else "0" } }

fun decodeBattleshipSolutionCells(solution: String, size: Int): Set<Pair<Int, Int>> {
    val cells = mutableSetOf<Pair<Int, Int>>()
    for (row in 0 until size) {
        for (col in 0 until size) {
            if (solution[row * size + col] == '1') cells.add(row to col)
        }
    }
    return cells
}

fun encodeBattleshipGivens(givens: List<Triple<Int, Int, BattleshipCell>>, size: Int): String {
    val chars = CharArray(size * size) { '.' }
    givens.forEach { (row, col, cell) ->
        chars[row * size + col] = when (cell) {
            BattleshipCell.SHIP -> '1'
            BattleshipCell.WATER -> '0'
            BattleshipCell.UNKNOWN -> '.'
        }
    }
    return String(chars)
}

fun decodeBattleshipGivens(givens: String, size: Int): List<Triple<Int, Int, BattleshipCell>> {
    val result = mutableListOf<Triple<Int, Int, BattleshipCell>>()
    for (row in 0 until size) {
        for (col in 0 until size) {
            when (givens[row * size + col]) {
                '1' -> result.add(Triple(row, col, BattleshipCell.SHIP))
                '0' -> result.add(Triple(row, col, BattleshipCell.WATER))
            }
        }
    }
    return result
}
