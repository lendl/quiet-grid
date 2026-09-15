package com.quietgrid.app.games.starbattle

import com.quietgrid.engine.starbattle.StarBattlePuzzleEntry
import kotlinx.serialization.Serializable

const val STARBATTLE_STARTING_LIVES = 3

enum class StarBattleCellState { EMPTY, MARKED, LOCKED_CORRECT, LOCKED_WRONG }

enum class StarBattleStatus { PLAYING, WON, LOST }

data class StarBattleSession(
    val puzzle: StarBattlePuzzleEntry,
    val cells: List<List<StarBattleCellState>>,
    val lives: Int,
    val status: StarBattleStatus,
)

@Serializable
data class StarBattlePersistedSession(
    val puzzle: StarBattlePuzzleEntry,
    val cells: List<Int>,
    val lives: Int,
    val status: String,
)

fun createStarBattleSession(puzzle: StarBattlePuzzleEntry): StarBattleSession = StarBattleSession(
    puzzle = puzzle,
    cells = List(puzzle.size) { List(puzzle.size) { StarBattleCellState.EMPTY } },
    lives = STARBATTLE_STARTING_LIVES,
    status = StarBattleStatus.PLAYING,
)
