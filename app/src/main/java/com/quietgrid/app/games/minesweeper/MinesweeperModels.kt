package com.quietgrid.app.games.minesweeper

import kotlinx.serialization.Serializable

enum class MinesweeperCellState { HIDDEN, REVEALED, FLAGGED }
enum class MinesweeperStatus { PLAYING, WON, LOST }

@Serializable
data class MinesweeperCell(
    val isMine: Boolean,
    val adjacentMines: Int = 0,
    val state: MinesweeperCellState = MinesweeperCellState.HIDDEN,
)

@Serializable
data class MinesweeperBoard(
    val rows: Int,
    val cols: Int,
    val mines: Int,
    val generated: Boolean,
    val cells: List<List<MinesweeperCell>>,
    val status: MinesweeperStatus,
)

@Serializable
data class MinesweeperPuzzle(
    val difficulty: String,
    val profileId: String,
    val rows: Int,
    val cols: Int,
    val mines: Int,
)

data class MinesweeperSession(
    val puzzle: MinesweeperPuzzle,
    val board: MinesweeperBoard,
)

@Serializable
data class MinesweeperPersistedSession(
    val puzzle: MinesweeperPuzzle,
    val board: MinesweeperBoard,
)
