package com.quietgrid.app.games.starbattle

private fun List<List<StarBattleCellState>>.replaceCell(row: Int, col: Int, value: StarBattleCellState): List<List<StarBattleCellState>> =
    mapIndexed { r, line -> if (r != row) line else line.mapIndexed { c, cell -> if (c != col) cell else value } }

fun applyStarBattleTap(session: StarBattleSession, row: Int, col: Int): StarBattleSession? {
    if (session.status != StarBattleStatus.PLAYING) return null
    val next = when (session.cells[row][col]) {
        StarBattleCellState.EMPTY -> StarBattleCellState.MARKED
        StarBattleCellState.MARKED -> StarBattleCellState.EMPTY
        else -> return null
    }
    return session.copy(cells = session.cells.replaceCell(row, col, next))
}

fun applyStarBattleDrag(session: StarBattleSession, markAll: Boolean, visited: List<Pair<Int, Int>>): StarBattleSession? {
    if (session.status != StarBattleStatus.PLAYING) return null

    var cells = session.cells
    for ((row, col) in visited) {
        val current = cells[row][col]
        if (current != StarBattleCellState.EMPTY && current != StarBattleCellState.MARKED) continue
        cells = cells.replaceCell(row, col, if (markAll) StarBattleCellState.MARKED else StarBattleCellState.EMPTY)
    }
    return session.copy(cells = cells)
}

data class StarBattleOpenResult(val session: StarBattleSession, val wasCorrect: Boolean)

fun applyStarBattleOpen(session: StarBattleSession, row: Int, col: Int): StarBattleOpenResult? {
    if (session.status != StarBattleStatus.PLAYING) return null
    val current = session.cells[row][col]
    if (current != StarBattleCellState.EMPTY && current != StarBattleCellState.MARKED) return null

    val isCorrect = col in session.puzzle.solution[row]
    val cells = session.cells.replaceCell(row, col, if (isCorrect) StarBattleCellState.LOCKED_CORRECT else StarBattleCellState.LOCKED_WRONG)
    val lives = if (isCorrect) session.lives else session.lives - 1
    val correctCount = cells.sumOf { line -> line.count { it == StarBattleCellState.LOCKED_CORRECT } }
    val targetCount = session.puzzle.size * session.puzzle.k

    val status = when {
        correctCount == targetCount -> StarBattleStatus.WON
        lives <= 0 -> StarBattleStatus.LOST
        else -> StarBattleStatus.PLAYING
    }

    return StarBattleOpenResult(session.copy(cells = cells, lives = lives, status = status), isCorrect)
}
