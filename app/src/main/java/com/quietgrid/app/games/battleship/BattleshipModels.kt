package com.quietgrid.app.games.battleship

import com.quietgrid.engine.battleship.BattleshipBoard
import com.quietgrid.engine.battleship.BattleshipPuzzleEntry
import kotlinx.serialization.Serializable

data class BattleshipSession(
    val puzzle: BattleshipPuzzleEntry,
    val board: BattleshipBoard,
    val solutionShipCells: Set<Pair<Int, Int>>,
    val givenCells: Set<Pair<Int, Int>>,
)

@Serializable
data class BattleshipPersistedSession(
    val puzzle: BattleshipPuzzleEntry,
    val board: String,
)
