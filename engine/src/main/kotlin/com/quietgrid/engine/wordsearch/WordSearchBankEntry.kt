package com.quietgrid.engine.wordsearch

import kotlinx.serialization.Serializable

@Serializable
data class WordSearchBankEntry(
    val id: String,
    val themeId: String,
    val rows: Int,
    val cols: Int,
    val words: List<String>,
    val hiddenWord: String,
    val locale: String = "en",
)

private val deltaToDirection: Map<Pair<Int, Int>, WordSearchDirection> =
    directionToDelta.entries.associate { (direction, delta) -> delta to direction }

private fun encodeWordPlacement(entry: WSWordEntry): String {
    val start = entry.positions.first()
    val next = entry.positions.getOrElse(1) { start }
    val direction = deltaToDirection[(next.row - start.row) to (next.col - start.col)] ?: WordSearchDirection.RIGHT
    return "${entry.word} ${start.row} ${start.col} ${direction.name}"
}

fun WordSearchPuzzleEntry.toBankEntry(): WordSearchBankEntry {
    val bankEntry = WordSearchBankEntry(
        id = id,
        themeId = themeId,
        rows = rows,
        cols = cols,
        words = words.map(::encodeWordPlacement),
        hiddenWord = hiddenWord.word,
        locale = locale,
    )
    val restored = bankEntry.toPuzzleEntry(difficulty)
    val lossless = restored.grid == grid &&
        restored.hiddenWord == hiddenWord &&
        restored.words.map { it.word to it.positions } == words.map { it.word to it.positions }
    require(lossless) { "Word search puzzle $id cannot be stored as a bank entry without losing data" }
    return bankEntry
}

fun WordSearchBankEntry.toPuzzleEntry(difficulty: String): WordSearchPuzzleEntry {
    val grid = List(rows) { MutableList(cols) { "" } }
    val placedWords = words.mapIndexed { index, encoded ->
        val (word, row, col, direction) = encoded.split(' ')
        val (rowStep, colStep) = directionToDelta.getValue(WordSearchDirection.valueOf(direction))
        val positions = word.indices.map { WSCellRef(row.toInt() + it * rowStep, col.toInt() + it * colStep) }
        positions.forEachIndexed { letter, cell -> grid[cell.row][cell.col] = word[letter].toString() }
        WSWordEntry((index + 1).toString(), word, positions)
    }
    val hiddenPositions = mutableListOf<WSCellRef>()
    for (row in 0 until rows) for (col in 0 until cols) if (grid[row][col].isEmpty()) hiddenPositions += WSCellRef(row, col)
    require(hiddenPositions.size == hiddenWord.length) {
        "Word search puzzle $id leaves ${hiddenPositions.size} free cells for hidden word $hiddenWord"
    }
    hiddenPositions.forEachIndexed { letter, cell -> grid[cell.row][cell.col] = hiddenWord[letter].toString() }
    return WordSearchPuzzleEntry(
        id = id,
        difficulty = difficulty,
        rows = rows,
        cols = cols,
        themeId = themeId,
        grid = grid.map { it.toList() },
        words = placedWords,
        hiddenWord = WSHiddenWord(hiddenWord, themeId, hiddenPositions),
        locale = locale,
    )
}
