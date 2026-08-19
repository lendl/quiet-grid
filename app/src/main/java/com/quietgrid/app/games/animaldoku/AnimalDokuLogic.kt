// app/src/main/java/com/quietgrid/app/games/animaldoku/AnimalDokuLogic.kt
package com.quietgrid.app.games.animaldoku

private fun List<List<AnimalDokuCellState>>.replaceCell(row: Int, col: Int, value: AnimalDokuCellState): List<List<AnimalDokuCellState>> =
    mapIndexed { r, line -> if (r != row) line else line.mapIndexed { c, cell -> if (c != col) cell else value } }

fun applyAnimalDokuTap(session: AnimalDokuSession, row: Int, col: Int): AnimalDokuSession? {
    if (session.status != AnimalDokuStatus.PLAYING) return null
    val next = when (session.cells[row][col]) {
        AnimalDokuCellState.EMPTY -> AnimalDokuCellState.MARKED
        AnimalDokuCellState.MARKED -> AnimalDokuCellState.EMPTY
        else -> return null
    }
    return session.copy(cells = session.cells.replaceCell(row, col, next))
}

/**
 * Batches a mark/unmark drag across [visited] cells, all toward [markAll] (true = mark, false =
 * unmark). [markAll] must be the direction decided once at gesture start from the start cell's
 * original pre-drag state -- never re-derived here from the session's current (possibly
 * already-mutated-by-an-earlier-call-in-the-same-gesture) state, or the direction could flip
 * mid-swipe.
 */
fun applyAnimalDokuDrag(session: AnimalDokuSession, markAll: Boolean, visited: List<Pair<Int, Int>>): AnimalDokuSession? {
    if (session.status != AnimalDokuStatus.PLAYING) return null

    var cells = session.cells
    for ((row, col) in visited) {
        val current = cells[row][col]
        if (current != AnimalDokuCellState.EMPTY && current != AnimalDokuCellState.MARKED) continue
        cells = cells.replaceCell(row, col, if (markAll) AnimalDokuCellState.MARKED else AnimalDokuCellState.EMPTY)
    }
    return session.copy(cells = cells)
}

data class AnimalDokuOpenResult(val session: AnimalDokuSession, val wasCorrect: Boolean)

fun applyAnimalDokuOpen(session: AnimalDokuSession, row: Int, col: Int): AnimalDokuOpenResult? {
    if (session.status != AnimalDokuStatus.PLAYING) return null
    val current = session.cells[row][col]
    if (current != AnimalDokuCellState.EMPTY && current != AnimalDokuCellState.MARKED) return null

    val isCorrect = session.puzzle.solution[row] == col
    val cells = session.cells.replaceCell(row, col, if (isCorrect) AnimalDokuCellState.LOCKED_CORRECT else AnimalDokuCellState.LOCKED_WRONG)
    val lives = if (isCorrect) session.lives else session.lives - 1
    val solvedRows = cells.count { line -> line.any { it == AnimalDokuCellState.LOCKED_CORRECT } }

    val status = when {
        solvedRows == session.puzzle.size -> AnimalDokuStatus.WON
        lives <= 0 -> AnimalDokuStatus.LOST
        else -> AnimalDokuStatus.PLAYING
    }

    return AnimalDokuOpenResult(session.copy(cells = cells, lives = lives, status = status), isCorrect)
}
