package com.quietgrid.engine.wordsearch

private fun normalizeWordToken(value: String): String = value.trim().uppercase().replace(Regex("[^A-Z]"), "")

fun buildHiddenWordPool(themeWords: List<String>): List<String> {
    val seen = mutableSetOf<String>()
    val pool = mutableListOf<String>()
    for (word in themeWords) {
        val normalized = normalizeWordToken(word)
        if (normalized.length >= 3 && seen.add(normalized)) pool.add(normalized)
    }
    return pool
}

fun pickHiddenWord(pool: List<String>, rows: Int, cols: Int): String? {
    val maxLength = rows * cols
    val candidates = pool.filter { it.length <= maxLength }
    if (candidates.isEmpty()) return null
    return candidates.random()
}

data class ReservedHiddenWord(val word: String, val positions: List<WSCellRef>)

fun reserveHiddenWordCells(word: String, rows: Int, cols: Int): ReservedHiddenWord {
    val allCells = mutableListOf<WSCellRef>()
    for (row in 0 until rows) for (col in 0 until cols) allCells.add(WSCellRef(row, col))
    allCells.shuffle()
    val positions = allCells.take(word.length).sortedWith(compareBy({ it.row }, { it.col }))
    return ReservedHiddenWord(word, positions)
}
