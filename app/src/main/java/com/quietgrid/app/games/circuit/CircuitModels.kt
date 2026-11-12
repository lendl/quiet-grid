package com.quietgrid.app.games.circuit

import com.quietgrid.engine.circuit.CircuitBoard
import com.quietgrid.engine.circuit.CircuitHint
import com.quietgrid.engine.circuit.CircuitPuzzleEntry
import com.quietgrid.engine.circuit.circuitMasksFromTurns
import com.quietgrid.engine.circuit.toBoard
import kotlinx.serialization.Serializable

enum class CircuitStatus { PLAYING, WON }

data class CircuitTapEvent(val id: Int, val cell: Int, val blocked: Boolean)

data class CircuitSession(
    val puzzle: CircuitPuzzleEntry,
    val board: CircuitBoard,
    val turns: List<Int>,
    val locked: List<Boolean>,
    val moves: Int,
    val hintsUsed: Int,
    val status: CircuitStatus,
) {
    val masks: IntArray get() = circuitMasksFromTurns(puzzle.start, turns)
}

data class CircuitHintResult(val session: CircuitSession, val hint: CircuitHint)

@Serializable
data class CircuitPersistedSession(
    val puzzle: CircuitPuzzleEntry,
    val turns: List<Int>,
    val locked: List<Boolean>,
    val moves: Int,
    val hintsUsed: Int,
    val status: String,
)

fun createCircuitSession(puzzle: CircuitPuzzleEntry): CircuitSession {
    val board = puzzle.toBoard()
    return CircuitSession(
        puzzle = puzzle,
        board = board,
        turns = List(board.size) { 0 },
        locked = List(board.size) { false },
        moves = 0,
        hintsUsed = 0,
        status = CircuitStatus.PLAYING,
    )
}
