package com.quietgrid.app.games.wordsearch

import kotlin.math.max
import kotlin.math.sign

private const val MAX_SCORE = 15_000
private const val MIN_SCORE = 1_000
private const val SKIPPED_WORD_BONUS = 300

fun wordSearchScore(elapsedSeconds: Int, session: WordSearchSession): Int {
    val base = max(MIN_SCORE, MAX_SCORE - elapsedSeconds * 8)
    val skipped = session.puzzle.words.size - session.foundWordIds.size
    return base + skipped * SKIPPED_WORD_BONUS
}

private fun isInsideGrid(puzzle: WordSearchPuzzleEntry, cell: WSCellRef): Boolean =
    cell.row in 0 until puzzle.rows && cell.col in 0 until puzzle.cols

private fun computeStepDelta(start: WSCellRef, end: WSCellRef): Pair<Int, Int>? {
    val rowDelta = end.row - start.row
    val colDelta = end.col - start.col
    val rowStep = rowDelta.sign
    val colStep = colDelta.sign
    if (rowDelta == 0 && colDelta == 0) return 0 to 0
    return if (rowDelta == 0 || colDelta == 0 || kotlin.math.abs(rowDelta) == kotlin.math.abs(colDelta)) {
        rowStep to colStep
    } else {
        null
    }
}

private fun buildStraightPath(start: WSCellRef, end: WSCellRef): List<WSCellRef>? {
    val (rowStep, colStep) = computeStepDelta(start, end) ?: return null
    val path = mutableListOf<WSCellRef>()
    var row = start.row
    var col = start.col
    path.add(WSCellRef(row, col))
    while (row != end.row || col != end.col) {
        row += rowStep
        col += colStep
        path.add(WSCellRef(row, col))
    }
    return path
}

private fun pathMatchesWord(path: List<WSCellRef>, positions: List<WSCellRef>): Boolean {
    if (path.size != positions.size) return false
    if (path.indices.all { path[it] == positions[it] }) return true
    return path.indices.all { path[it] == positions[positions.size - 1 - it] }
}

fun wsBeginSelection(session: WordSearchSession, cell: WSCellRef): WordSearchSession? {
    if (!isInsideGrid(session.puzzle, cell)) return null
    val path = buildStraightPath(cell, cell) ?: return null
    return session.copy(tempSelection = WSSelection(path))
}

fun wsUpdateSelection(session: WordSearchSession, cell: WSCellRef): WordSearchSession? {
    val current = session.tempSelection ?: return null
    if (!isInsideGrid(session.puzzle, cell)) return null
    val start = current.path.first()
    val nextPath = buildStraightPath(start, cell) ?: return null
    if (nextPath == current.path) return null
    return session.copy(tempSelection = WSSelection(nextPath))
}

fun wsClearSelection(session: WordSearchSession): WordSearchSession? {
    if (session.tempSelection == null) return null
    return session.copy(tempSelection = null)
}

fun wsCommitSelection(session: WordSearchSession): WordSearchSession? {
    val selection = session.tempSelection ?: return null
    val pendingWord = session.puzzle.words.firstOrNull {
        it.id !in session.foundWordIds && pathMatchesWord(selection.path, it.positions)
    } ?: return session.copy(tempSelection = null)

    val nextFoundWordIds = session.foundWordIds + pendingWord.id
    val allWordsFound = nextFoundWordIds.size >= session.puzzle.words.size
    return session.copy(
        foundWordIds = nextFoundWordIds,
        tempSelection = null,
        hiddenWordMode = !session.hiddenWordSolved && allWordsFound,
    )
}

fun wsToggleHiddenWordMode(session: WordSearchSession): WordSearchSession? {
    if (session.hiddenWordSolved) return null
    return session.copy(hiddenWordMode = !session.hiddenWordMode, tempSelection = null)
}

private fun nextHiddenWordCell(session: WordSearchSession): WSCellRef? {
    if (session.hiddenWordSolved) return null
    return session.puzzle.hiddenWord.positions.getOrNull(session.hiddenWordProgress.size)
}

fun wsInputHiddenWordCell(session: WordSearchSession, cell: WSCellRef): WordSearchSession? {
    if (!session.hiddenWordMode || session.hiddenWordSolved || !isInsideGrid(session.puzzle, cell)) return null
    val expected = nextHiddenWordCell(session) ?: return null

    if (expected != cell) {
        if (session.hiddenWordProgress.isEmpty()) return null
        return session.copy(hiddenWordProgress = emptyList(), tempSelection = null)
    }

    val progress = session.hiddenWordProgress + cell
    val solved = progress.size >= session.puzzle.hiddenWord.positions.size
    return session.copy(
        hiddenWordProgress = progress,
        hiddenWordSolved = solved,
        hiddenWordMode = if (solved) false else session.hiddenWordMode,
        tempSelection = null,
    )
}

sealed class WSNextMoveHint {
    data class FindWord(val word: String, val evidenceCells: List<WSCellRef>, val targetCells: List<WSCellRef>) : WSNextMoveHint()
    data class FindHiddenLetter(val clue: String, val evidenceCells: List<WSCellRef>, val targetCells: List<WSCellRef>) : WSNextMoveHint()
}

private fun nextHiddenLetterHint(session: WordSearchSession): WSNextMoveHint.FindHiddenLetter? {
    if (session.hiddenWordSolved) return null
    val cell = session.puzzle.hiddenWord.positions.getOrNull(session.hiddenWordProgress.size) ?: return null
    return WSNextMoveHint.FindHiddenLetter(session.puzzle.hiddenWord.clue, listOf(cell), listOf(cell))
}

fun wsNextMoveHint(session: WordSearchSession): WSNextMoveHint? {
    if (session.hiddenWordMode) {
        return nextHiddenLetterHint(session)
    }

    val foundIds = session.foundWordIds.toSet()
    val unfoundWords = session.puzzle.words.filter { it.id !in foundIds }

    if (unfoundWords.isNotEmpty()) {
        val foundCellKeys = session.puzzle.words
            .filter { it.id in foundIds }
            .flatMap { it.positions }
            .toSet()

        for (word in unfoundWords) {
            val overlapCell = word.positions.firstOrNull { it in foundCellKeys }
            if (overlapCell != null) {
                return WSNextMoveHint.FindWord(word.word, listOf(overlapCell), listOf(overlapCell))
            }
        }

        val targetWord = unfoundWords.first()
        val targetCell = targetWord.positions.firstOrNull() ?: return null
        return WSNextMoveHint.FindWord(targetWord.word, listOf(targetCell), listOf(targetCell))
    }

    return nextHiddenLetterHint(session)
}

fun isWordSearchSolved(session: WordSearchSession): Boolean = session.hiddenWordSolved

fun wordSearchHasMeaningfulProgress(session: WordSearchSession): Boolean =
    session.foundWordIds.isNotEmpty() || session.hiddenWordProgress.isNotEmpty() || session.hiddenWordSolved
