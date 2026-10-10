package com.quietgrid.app.games.hashi

import com.quietgrid.engine.hashi.HashiBoard
import com.quietgrid.engine.hashi.HashiPuzzleEntry
import com.quietgrid.engine.hashi.toBoard
import kotlinx.serialization.Serializable

enum class HashiStatus { PLAYING, WON }

data class HashiHighlight(val edge: Int, val count: Int, val isMistake: Boolean)

data class HashiSession(
    val puzzle: HashiPuzzleEntry,
    val board: HashiBoard,
    val bridges: List<Int>,
    val hintsUsed: Int,
    val highlight: HashiHighlight?,
    val status: HashiStatus,
)

@Serializable
data class HashiPersistedSession(
    val puzzle: HashiPuzzleEntry,
    val bridges: List<Int>,
    val hintsUsed: Int,
    val status: String,
)

fun createHashiSession(puzzle: HashiPuzzleEntry): HashiSession {
    val board = puzzle.toBoard()
    return HashiSession(
        puzzle = puzzle,
        board = board,
        bridges = List(board.edges.size) { 0 },
        hintsUsed = 0,
        highlight = null,
        status = HashiStatus.PLAYING,
    )
}
