package com.quietgrid.app.games.game2048

import kotlinx.serialization.Serializable

enum class Game2048Direction { UP, DOWN, LEFT, RIGHT }
enum class Game2048Status { PLAYING, WON, LOST }

@Serializable
data class Game2048Puzzle(
    val difficulty: String,
    val size: Int,
    val fourSpawnChance: Double,
    val targetTile: Int,
)

@Serializable
data class Game2048Board(
    val size: Int,
    val tiles: List<List<Int?>>,
    val score: Int,
    val moveCount: Int,
    val status: Game2048Status,
)

data class Game2048Session(
    val puzzle: Game2048Puzzle,
    val board: Game2048Board,
)

@Serializable
data class Game2048PersistedSession(
    val puzzle: Game2048Puzzle,
    val board: Game2048Board,
)
