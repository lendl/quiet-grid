package com.quietgrid.app.games.game2048

import com.quietgrid.app.core.Difficulty
import kotlin.random.Random

private data class Game2048TierConfig(val size: Int, val fourSpawnChance: Double, val targetTile: Int)

private val TIER_CONFIG = mapOf(
    Difficulty.EASY to Game2048TierConfig(size = 5, fourSpawnChance = 0.10, targetTile = 2048),
    Difficulty.MEDIUM to Game2048TierConfig(size = 4, fourSpawnChance = 0.10, targetTile = 2048),
    Difficulty.HARD to Game2048TierConfig(size = 4, fourSpawnChance = 0.25, targetTile = 2048),
    Difficulty.EXPERT to Game2048TierConfig(size = 3, fourSpawnChance = 0.25, targetTile = 1024),
)

private fun emptyTiles(size: Int): List<List<Int?>> = List(size) { List<Int?>(size) { null } }

private fun spawnRandomTile(tiles: List<List<Int?>>, fourSpawnChance: Double): List<List<Int?>> {
    val size = tiles.size
    val emptyCoords = buildList {
        for (r in 0 until size) for (c in 0 until size) if (tiles[r][c] == null) add(r to c)
    }
    if (emptyCoords.isEmpty()) return tiles
    val (row, col) = emptyCoords.random()
    val value = if (Random.nextDouble() < fourSpawnChance) 4 else 2
    return tiles.mapIndexed { r, line ->
        line.mapIndexed { c, cell -> if (r == row && c == col) value else cell }
    }
}

fun createGame2048Session(difficulty: Difficulty): Game2048Session {
    val config = TIER_CONFIG.getValue(difficulty)
    val puzzle = Game2048Puzzle(difficulty = difficulty.key, size = config.size, fourSpawnChance = config.fourSpawnChance, targetTile = config.targetTile)
    var tiles = emptyTiles(config.size)
    tiles = spawnRandomTile(tiles, config.fourSpawnChance)
    tiles = spawnRandomTile(tiles, config.fourSpawnChance)
    val board = Game2048Board(size = config.size, tiles = tiles, score = 0, moveCount = 0, status = Game2048Status.PLAYING)
    return Game2048Session(puzzle = puzzle, board = board)
}

private fun mergeLine(line: List<Int?>): Pair<List<Int?>, Int> {
    val values = line.filterNotNull().toMutableList()
    val merged = mutableListOf<Int>()
    var scoreGained = 0
    var i = 0
    while (i < values.size) {
        if (i + 1 < values.size && values[i] == values[i + 1]) {
            val mergedValue = values[i] * 2
            merged.add(mergedValue)
            scoreGained += mergedValue
            i += 2
        } else {
            merged.add(values[i])
            i += 1
        }
    }
    val padded: List<Int?> = merged + List(line.size - merged.size) { null }
    return padded to scoreGained
}

private fun transpose(tiles: List<List<Int?>>): List<List<Int?>> {
    val size = tiles.size
    return (0 until size).map { c -> (0 until size).map { r -> tiles[r][c] } }
}

private fun slideAndMerge(tiles: List<List<Int?>>, direction: Game2048Direction): Pair<List<List<Int?>>, Int> {
    val lines: List<List<Int?>> = when (direction) {
        Game2048Direction.LEFT -> tiles
        Game2048Direction.RIGHT -> tiles.map { it.reversed() }
        Game2048Direction.UP -> transpose(tiles)
        Game2048Direction.DOWN -> transpose(tiles).map { it.reversed() }
    }

    var scoreGained = 0
    val mergedLines = lines.map { line ->
        val (newLine, gained) = mergeLine(line)
        scoreGained += gained
        newLine
    }

    val restored: List<List<Int?>> = when (direction) {
        Game2048Direction.LEFT -> mergedLines
        Game2048Direction.RIGHT -> mergedLines.map { it.reversed() }
        Game2048Direction.UP -> transpose(mergedLines)
        Game2048Direction.DOWN -> transpose(mergedLines.map { it.reversed() })
    }

    return restored to scoreGained
}

private fun hasAnyMove(tiles: List<List<Int?>>): Boolean {
    val size = tiles.size
    for (r in 0 until size) {
        for (c in 0 until size) {
            if (tiles[r][c] == null) return true
            if (c + 1 < size && tiles[r][c] == tiles[r][c + 1]) return true
            if (r + 1 < size && tiles[r][c] == tiles[r + 1][c]) return true
        }
    }
    return false
}

fun applyGame2048Move(board: Game2048Board, puzzle: Game2048Puzzle, direction: Game2048Direction): Game2048Board {
    if (board.status != Game2048Status.PLAYING) return board

    val (moved, scoreGained) = slideAndMerge(board.tiles, direction)
    if (moved == board.tiles) return board

    val spawned = spawnRandomTile(moved, puzzle.fourSpawnChance)
    val reachedTarget = spawned.any { row -> row.any { it != null && it >= puzzle.targetTile } }
    val status = when {
        reachedTarget -> Game2048Status.WON
        !hasAnyMove(spawned) -> Game2048Status.LOST
        else -> Game2048Status.PLAYING
    }

    return board.copy(
        tiles = spawned,
        score = board.score + scoreGained,
        moveCount = board.moveCount + 1,
        status = status,
    )
}

fun game2048BestTile(board: Game2048Board): Int =
    board.tiles.flatten().filterNotNull().maxOrNull() ?: 0

fun game2048HasMeaningfulProgress(session: Game2048Session): Boolean =
    session.board.moveCount > 0
