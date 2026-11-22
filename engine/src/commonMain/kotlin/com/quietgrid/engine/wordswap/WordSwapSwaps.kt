package com.quietgrid.engine.wordswap

private const val ALPHABET = 26

fun wordSwapMinSwaps(board: String, solution: String): Int {
    require(board.length == solution.length)
    val counts = Array(ALPHABET) { IntArray(ALPHABET) }
    var misplaced = 0
    for (i in board.indices) {
        val from = board[i]
        val to = solution[i]
        if (from == to || from == WORDSWAP_HOLE) continue
        counts[from - 'a'][to - 'a']++
        misplaced++
    }
    var cycles = 0
    for (a in 0 until ALPHABET) {
        for (b in a + 1 until ALPHABET) {
            val pairs = minOf(counts[a][b], counts[b][a])
            counts[a][b] -= pairs
            counts[b][a] -= pairs
            cycles += pairs
        }
    }
    cycles += maxCycles(counts, HashMap())
    return misplaced - cycles
}

private fun key(counts: Array<IntArray>): String {
    val sb = StringBuilder()
    for (a in 0 until ALPHABET) for (b in 0 until ALPHABET) if (counts[a][b] > 0) sb.append(a).append(',').append(b).append(':').append(counts[a][b]).append(';')
    return sb.toString()
}

private fun maxCycles(counts: Array<IntArray>, memo: HashMap<String, Int>): Int {
    val start = (0 until ALPHABET).firstOrNull { a -> counts[a].any { it > 0 } } ?: return 0
    val k = key(counts)
    memo[k]?.let { return it }
    var best = 0
    val path = ArrayList<Int>()
    val onPath = BooleanArray(ALPHABET)

    fun extend(vertex: Int) {
        for (next in 0 until ALPHABET) {
            if (counts[vertex][next] == 0) continue
            if (next == start) {
                counts[vertex][next]--
                val closed = path + vertex
                for (i in 0 until closed.size - 1) counts[closed[i]][closed[i + 1]]--
                best = maxOf(best, 1 + maxCycles(counts, memo))
                for (i in 0 until closed.size - 1) counts[closed[i]][closed[i + 1]]++
                counts[vertex][next]++
            } else if (!onPath[next]) {
                path += vertex
                onPath[next] = true
                extend(next)
                onPath[next] = false
                path.removeAt(path.size - 1)
            }
        }
    }

    onPath[start] = true
    extend(start)
    memo[k] = best
    return best
}

fun findWordSwapSwapHint(board: String, solution: String): WordSwapMove? {
    val current = wordSwapMinSwaps(board, solution)
    if (current == 0) return null
    val misplaced = board.indices.filter { board[it] != solution[it] }
    for (a in misplaced) for (b in misplaced) {
        if (a < b && board[a] == solution[b] && board[b] == solution[a]) return WordSwapMove(a, b)
    }
    for (a in misplaced) for (b in misplaced) {
        if (a == b || board[b] != solution[a]) continue
        val next = applyWordSwapMove(board, WordSwapMove(a, b)) ?: continue
        if (wordSwapMinSwaps(next, solution) == current - 1) return WordSwapMove(a, b)
    }
    return null
}
