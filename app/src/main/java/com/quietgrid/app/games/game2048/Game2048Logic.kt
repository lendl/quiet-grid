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

data class Game2048TileMove(val value: Int, val fromRow: Int, val fromCol: Int, val toRow: Int, val toCol: Int, val merged: Boolean)
data class Game2048MoveResult(val board: Game2048Board, val tileMoves: List<Game2048TileMove>)

private data class LocalMove(val fromIndex: Int, val toIndex: Int, val value: Int, val merged: Boolean)
private data class LineMergeResult(val line: List<Int?>, val scoreGained: Int, val moves: List<LocalMove>)

private fun mergeLine(line: List<Int?>): LineMergeResult {
    val indexed = line.withIndex().filter { it.value != null }.map { it.index to it.value!! }
    val moves = mutableListOf<LocalMove>()
    val merged = mutableListOf<Int>()
    var scoreGained = 0
    var i = 0
    while (i < indexed.size) {
        val (fromIndexA, valueA) = indexed[i]
        val toIndex = merged.size
        if (i + 1 < indexed.size && indexed[i + 1].second == valueA) {
            val (fromIndexB, _) = indexed[i + 1]
            val mergedValue = valueA * 2
            moves.add(LocalMove(fromIndexA, toIndex, valueA, merged = false))
            moves.add(LocalMove(fromIndexB, toIndex, valueA, merged = true))
            merged.add(mergedValue)
            scoreGained += mergedValue
            i += 2
        } else {
            moves.add(LocalMove(fromIndexA, toIndex, valueA, merged = false))
            merged.add(valueA)
            i += 1
        }
    }
    val padded: List<Int?> = merged + List(line.size - merged.size) { null }
    return LineMergeResult(padded, scoreGained, moves)
}

private fun transpose(tiles: List<List<Int?>>): List<List<Int?>> {
    val size = tiles.size
    return (0 until size).map { c -> (0 until size).map { r -> tiles[r][c] } }
}

private fun localToGrid(direction: Game2048Direction, lineIndex: Int, pos: Int, size: Int): Pair<Int, Int> = when (direction) {
    Game2048Direction.LEFT -> lineIndex to pos
    Game2048Direction.RIGHT -> lineIndex to (size - 1 - pos)
    Game2048Direction.UP -> pos to lineIndex
    Game2048Direction.DOWN -> (size - 1 - pos) to lineIndex
}

private fun slideAndMerge(tiles: List<List<Int?>>, direction: Game2048Direction): Triple<List<List<Int?>>, Int, List<Game2048TileMove>> {
    val size = tiles.size
    val lines: List<List<Int?>> = when (direction) {
        Game2048Direction.LEFT -> tiles
        Game2048Direction.RIGHT -> tiles.map { it.reversed() }
        Game2048Direction.UP -> transpose(tiles)
        Game2048Direction.DOWN -> transpose(tiles).map { it.reversed() }
    }

    var scoreGained = 0
    val tileMoves = mutableListOf<Game2048TileMove>()
    val mergedLines = lines.mapIndexed { lineIndex, line ->
        val result = mergeLine(line)
        scoreGained += result.scoreGained
        result.moves.forEach { move ->
            val (fromRow, fromCol) = localToGrid(direction, lineIndex, move.fromIndex, size)
            val (toRow, toCol) = localToGrid(direction, lineIndex, move.toIndex, size)
            tileMoves.add(Game2048TileMove(move.value, fromRow, fromCol, toRow, toCol, move.merged))
        }
        result.line
    }

    val restored: List<List<Int?>> = when (direction) {
        Game2048Direction.LEFT -> mergedLines
        Game2048Direction.RIGHT -> mergedLines.map { it.reversed() }
        Game2048Direction.UP -> transpose(mergedLines)
        Game2048Direction.DOWN -> transpose(mergedLines.map { it.reversed() })
    }

    return Triple(restored, scoreGained, tileMoves)
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

fun applyGame2048MoveDetailed(board: Game2048Board, puzzle: Game2048Puzzle, direction: Game2048Direction): Game2048MoveResult {
    if (board.status != Game2048Status.PLAYING) return Game2048MoveResult(board, emptyList())

    val (moved, scoreGained, tileMoves) = slideAndMerge(board.tiles, direction)
    if (moved == board.tiles) return Game2048MoveResult(board, emptyList())

    val spawned = spawnRandomTile(moved, puzzle.fourSpawnChance)
    val reachedTarget = spawned.any { row -> row.any { it != null && it >= puzzle.targetTile } }
    val status = when {
        reachedTarget -> Game2048Status.WON
        !hasAnyMove(spawned) -> Game2048Status.LOST
        else -> Game2048Status.PLAYING
    }

    val newBoard = board.copy(
        tiles = spawned,
        score = board.score + scoreGained,
        moveCount = board.moveCount + 1,
        status = status,
    )
    return Game2048MoveResult(newBoard, tileMoves)
}

fun applyGame2048Move(board: Game2048Board, puzzle: Game2048Puzzle, direction: Game2048Direction): Game2048Board =
    applyGame2048MoveDetailed(board, puzzle, direction).board

fun game2048BestTile(board: Game2048Board): Int =
    board.tiles.flatten().filterNotNull().maxOrNull() ?: 0

fun game2048HasMeaningfulProgress(session: Game2048Session): Boolean =
    session.board.moveCount > 0
