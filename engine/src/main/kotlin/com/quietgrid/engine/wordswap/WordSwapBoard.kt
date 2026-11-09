package com.quietgrid.engine.wordswap

fun WordSwapSlot.cells(cols: Int): List<Int> =
    List(length) { step -> (row + step * direction.rowStep) * cols + col + step * direction.colStep }

fun WordSwapSlot.read(board: String, cols: Int): String = cells(cols).map { board[it] }.joinToString("")

fun wordSwapLineSlots(layout: String, rows: Int, cols: Int): List<WordSwapSlot> {
    val slots = mutableListOf<WordSwapSlot>()
    fun isTile(r: Int, c: Int) = layout[r * cols + c] != WORDSWAP_HOLE
    for (r in 0 until rows) {
        var c = 0
        while (c < cols) {
            if (!isTile(r, c)) { c++; continue }
            val start = c
            while (c < cols && isTile(r, c)) c++
            if (c - start >= WORDSWAP_MIN_WORD_LENGTH) slots += WordSwapSlot(r, start, c - start, WordSwapDirection.ACROSS)
        }
    }
    for (c in 0 until cols) {
        var r = 0
        while (r < rows) {
            if (!isTile(r, c)) { r++; continue }
            val start = r
            while (r < rows && isTile(r, c)) r++
            if (r - start >= WORDSWAP_MIN_WORD_LENGTH) slots += WordSwapSlot(start, c, r - start, WordSwapDirection.DOWN)
        }
    }
    return slots
}

fun wordSwapDiagonalSlot(layout: String, size: Int, direction: WordSwapDirection): WordSwapSlot? {
    val slot = when (direction) {
        WordSwapDirection.DIAGONAL -> WordSwapSlot(0, 0, size, direction)
        WordSwapDirection.ANTI_DIAGONAL -> WordSwapSlot(0, size - 1, size, direction)
        else -> return null
    }
    return slot.takeIf { s -> s.cells(size).none { layout[it] == WORDSWAP_HOLE } }
}

fun wordSwapTileCells(board: String): List<Int> = board.indices.filter { board[it] != WORDSWAP_HOLE }

fun wordSwapCoversAllTiles(layout: String, cols: Int, slots: List<WordSwapSlot>): Boolean {
    val covered = slots.flatMapTo(HashSet()) { it.cells(cols) }
    return wordSwapTileCells(layout).all { it in covered }
}

fun WordSwapPuzzleEntry.words(): List<String> = slots.map { it.read(solution, cols) }

fun isWordSwapSolved(puzzle: WordSwapPuzzleEntry, board: String): Boolean = board == puzzle.solution

fun wordSwapSolvedSlots(puzzle: WordSwapPuzzleEntry, board: String): Set<Int> =
    puzzle.slots.indices.filterTo(HashSet()) { index ->
        val slot = puzzle.slots[index]
        slot.read(board, puzzle.cols) == slot.read(puzzle.solution, puzzle.cols)
    }

fun applyWordSwapMove(board: String, move: WordSwapMove): String? {
    val (a, b) = move
    if (a == b || a !in board.indices || b !in board.indices) return null
    if (board[a] == WORDSWAP_HOLE || board[b] == WORDSWAP_HOLE) return null
    val chars = board.toCharArray()
    chars[a] = board[b]
    chars[b] = board[a]
    return String(chars)
}

fun isValidWordSwapBoard(puzzle: WordSwapPuzzleEntry, board: String): Boolean {
    if (board.length != puzzle.rows * puzzle.cols || puzzle.solution.length != board.length) return false
    if (board.indices.any { (board[it] == WORDSWAP_HOLE) != (puzzle.solution[it] == WORDSWAP_HOLE) }) return false
    return board.toList().sorted() == puzzle.solution.toList().sorted()
}

fun countWordSwapArrangements(
    layout: String,
    cols: Int,
    slots: List<WordSwapSlot>,
    words: List<String>,
    limit: Int = 2,
): Int {
    require(words.size == slots.size)
    val tileLetters = wordSwapTileCells(layout).map { layout[it] }.groupingBy { it }.eachCount()
    val slotCells = slots.map { it.cells(cols) }
    val order = slots.indices.sortedByDescending { slots[it].length }
    val letters = HashMap<Int, Char>()
    val used = BooleanArray(words.size)
    var count = 0

    fun place(depth: Int) {
        if (count >= limit) return
        if (depth == order.size) {
            val placed = letters.values.groupingBy { it }.eachCount()
            if (placed == tileLetters) count++
            return
        }
        val slotIndex = order[depth]
        val cells = slotCells[slotIndex]
        val tried = HashSet<String>()
        for (w in words.indices) {
            if (used[w]) continue
            val word = words[w]
            if (slots[w].direction != slots[slotIndex].direction) continue
            if (word.length != cells.size || !tried.add(word)) continue
            if (cells.indices.any { i -> letters[cells[i]]?.let { it != word[i] } == true }) continue
            val added = cells.indices.filter { letters[cells[it]] == null }
            added.forEach { i -> letters[cells[i]] = word[i] }
            used[w] = true
            place(depth + 1)
            used[w] = false
            added.forEach { i -> letters.remove(cells[i]) }
            if (count >= limit) return
        }
    }

    place(0)
    return count
}
