package com.quietgrid.app.games.themeclear

import com.quietgrid.engine.themeclear.ThemeClearPuzzleEntry
import kotlin.random.Random

private const val THEMECLEAR_BASE_SCORE = 1000
private const val THEMECLEAR_HINT_PENALTY = 100
private const val THEMECLEAR_TIME_PENALTY_PER_SECOND = 2
private const val THEMECLEAR_POINTS_PER_DISCOVERED_LETTER = 10

fun themeClearInitialColumns(puzzle: ThemeClearPuzzleEntry): List<List<TCTile>> =
    List(puzzle.cols) { col ->
        (puzzle.rows - 1 downTo 0).map { row -> TCTile(row * puzzle.cols + col, puzzle.grid[row][col]) }
    }

fun themeClearNewSession(puzzle: ThemeClearPuzzleEntry): ThemeClearSession = ThemeClearSession(
    puzzle = puzzle,
    columns = themeClearInitialColumns(puzzle),
    foundWords = emptyList(),
    selection = emptyList(),
    hintsUsed = 0,
)

fun tcTileById(session: ThemeClearSession, id: Int): TCTile? =
    session.columns.firstNotNullOfOrNull { column -> column.firstOrNull { it.id == id } }

fun tcSelectedWord(session: ThemeClearSession): String =
    session.selection.mapNotNull { tcTileById(session, it)?.letter }.joinToString("")

fun tcBoardLetters(session: ThemeClearSession): String =
    session.columns.flatten().map { it.letter }.joinToString("")

fun tcUnselectedLetters(session: ThemeClearSession): String =
    session.columns.flatten().filter { it.id !in session.selection }.map { it.letter }.joinToString("")

fun tcToggleTile(session: ThemeClearSession, tileId: Int): ThemeClearSession {
    if (tcTileById(session, tileId) == null) return session
    val selection = if (tileId in session.selection) session.selection - tileId else session.selection + tileId
    return session.copy(selection = selection)
}

fun tcClearSelection(session: ThemeClearSession): ThemeClearSession = session.copy(selection = emptyList())

fun tcAcceptSelection(session: ThemeClearSession): ThemeClearSession {
    if (session.selection.isEmpty()) return session
    val picked = session.selection.mapNotNull { tcTileById(session, it) }
    val selected = session.selection.toSet()
    val word = picked.map { it.letter }.joinToString("")
    return session.copy(
        columns = session.columns.map { column -> column.filter { it.id !in selected } },
        foundWords = session.foundWords + TCFoundWord(word, picked),
        selection = emptyList(),
        discoveredWords = session.discoveredWords + word,
    )
}

fun tcRemoveFoundWord(session: ThemeClearSession, index: Int, random: Random): ThemeClearSession {
    val removed = session.foundWords.getOrNull(index) ?: return session
    val columns = session.columns.map { it.toMutableList() }
    removed.tiles.shuffled(random).forEach { tile ->
        val open = columns.indices.filter { columns[it].size < session.puzzle.rows }
        columns[open.random(random)].add(tile)
    }
    return session.copy(
        columns = columns,
        foundWords = session.foundWords.filterIndexed { i, _ -> i != index },
        selection = emptyList(),
    )
}

fun tcShuffle(session: ThemeClearSession, random: Random): ThemeClearSession {
    val columns = List(session.puzzle.cols) { mutableListOf<TCTile>() }
    session.columns.flatten().shuffled(random).forEachIndexed { index, tile ->
        columns[index % session.puzzle.cols].add(tile)
    }
    return session.copy(
        columns = columns,
        selection = emptyList(),
    )
}

fun tcIsCleared(session: ThemeClearSession): Boolean = session.columns.all { it.isEmpty() }

fun tcTilePositions(session: ThemeClearSession): Map<Int, TCTilePosition> = buildMap {
    session.columns.forEachIndexed { col, column ->
        column.forEachIndexed { height, tile -> put(tile.id, TCTilePosition(session.puzzle.rows - 1 - height, col)) }
    }
}

fun tcTilesForWord(session: ThemeClearSession, word: String): List<Int> {
    val available = session.columns.flatten().toMutableList()
    return word.mapNotNull { letter ->
        available.firstOrNull { it.letter == letter }?.also { available.remove(it) }?.id
    }
}

fun themeClearHasMeaningfulProgress(session: ThemeClearSession): Boolean =
    session.foundWords.isNotEmpty() || session.hintsUsed > 0 || session.columns != themeClearInitialColumns(session.puzzle)

fun themeClearScore(hintsUsed: Int, elapsedSeconds: Int, discoveredWords: Set<String>): Int = maxOf(
    0,
    THEMECLEAR_BASE_SCORE - hintsUsed * THEMECLEAR_HINT_PENALTY - elapsedSeconds * THEMECLEAR_TIME_PENALTY_PER_SECOND,
) + discoveredWords.sumOf { it.length } * THEMECLEAR_POINTS_PER_DISCOVERED_LETTER
