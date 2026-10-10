package com.quietgrid.engine.hashi

import kotlinx.serialization.Serializable

const val HASHI_MAX_BRIDGES_PER_EDGE = 2

@Serializable
data class HashiIsland(val row: Int, val col: Int, val count: Int)

@Serializable
data class HashiBridge(val from: Int, val to: Int, val count: Int)

@Serializable
data class HashiPuzzleEntry(
    val id: String,
    val rows: Int,
    val cols: Int,
    val difficulty: String,
    val islands: List<HashiIsland>,
    val solution: List<HashiBridge>,
)

data class HashiEdge(val a: Int, val b: Int, val horizontal: Boolean)

class HashiBoard(val rows: Int, val cols: Int, val islands: List<HashiIsland>) {
    val edges: List<HashiEdge>
    val crossings: List<IntArray>
    val edgesOfIsland: List<IntArray>
    private val edgeByPair: Map<Long, Int>

    init {
        val islandAt = HashMap<Int, Int>()
        islands.forEachIndexed { index, island -> islandAt[island.row * cols + island.col] = index }
        val found = mutableListOf<HashiEdge>()
        islands.forEachIndexed { index, island ->
            var col = island.col + 1
            while (col < cols) {
                val other = islandAt[island.row * cols + col]
                if (other != null) {
                    found += HashiEdge(minOf(index, other), maxOf(index, other), horizontal = true)
                    break
                }
                col++
            }
            var row = island.row + 1
            while (row < rows) {
                val other = islandAt[row * cols + island.col]
                if (other != null) {
                    found += HashiEdge(minOf(index, other), maxOf(index, other), horizontal = false)
                    break
                }
                row++
            }
        }
        edges = found
        edgeByPair = found.withIndex().associate { (index, edge) -> pairKey(edge.a, edge.b) to index }
        crossings = found.indices.map { e -> found.indices.filter { f -> edgesCross(found[e], found[f]) }.toIntArray() }
        edgesOfIsland = islands.indices.map { island -> found.indices.filter { found[it].a == island || found[it].b == island }.toIntArray() }
    }

    fun edgeIndex(a: Int, b: Int): Int = edgeByPair[pairKey(minOf(a, b), maxOf(a, b))] ?: -1

    fun otherEnd(edge: Int, island: Int): Int = edges[edge].let { if (it.a == island) it.b else it.a }

    fun countsFromBridges(bridges: List<HashiBridge>): IntArray {
        val counts = IntArray(edges.size)
        for (bridge in bridges) {
            val edge = edgeIndex(bridge.from, bridge.to)
            require(edge >= 0) { "Islands ${bridge.from} and ${bridge.to} cannot be bridged." }
            counts[edge] = bridge.count
        }
        return counts
    }

    fun bridgesFromCounts(counts: IntArray): List<HashiBridge> =
        edges.indices.filter { counts[it] > 0 }.map { HashiBridge(edges[it].a, edges[it].b, counts[it]) }

    private fun edgesCross(first: HashiEdge, second: HashiEdge): Boolean {
        if (first.horizontal == second.horizontal) return false
        val horizontal = if (first.horizontal) first else second
        val vertical = if (first.horizontal) second else first
        val row = islands[horizontal.a].row
        val col = islands[vertical.a].col
        val colRange = minOf(islands[horizontal.a].col, islands[horizontal.b].col)..maxOf(islands[horizontal.a].col, islands[horizontal.b].col)
        val rowRange = minOf(islands[vertical.a].row, islands[vertical.b].row)..maxOf(islands[vertical.a].row, islands[vertical.b].row)
        return col > colRange.first && col < colRange.last && row > rowRange.first && row < rowRange.last
    }

    private fun pairKey(a: Int, b: Int): Long = a.toLong() * islands.size + b
}

fun HashiPuzzleEntry.toBoard(): HashiBoard = HashiBoard(rows, cols, islands)

fun hashiIslandTotals(board: HashiBoard, counts: IntArray): IntArray {
    val totals = IntArray(board.islands.size)
    board.edges.forEachIndexed { index, edge ->
        totals[edge.a] += counts[index]
        totals[edge.b] += counts[index]
    }
    return totals
}

fun hashiBridgesCross(board: HashiBoard, counts: IntArray, edge: Int): Boolean =
    board.crossings[edge].any { counts[it] > 0 }

fun isHashiConnected(board: HashiBoard, counts: IntArray): Boolean {
    if (board.islands.isEmpty()) return true
    val visited = BooleanArray(board.islands.size)
    val queue = ArrayDeque(listOf(0))
    visited[0] = true
    var reached = 1
    while (queue.isNotEmpty()) {
        val island = queue.removeFirst()
        for (edge in board.edgesOfIsland[island]) {
            if (counts[edge] == 0) continue
            val other = board.otherEnd(edge, island)
            if (!visited[other]) {
                visited[other] = true
                reached++
                queue.add(other)
            }
        }
    }
    return reached == board.islands.size
}

fun isHashiSolved(board: HashiBoard, counts: IntArray): Boolean {
    if (counts.size != board.edges.size) return false
    if (counts.any { it !in 0..HASHI_MAX_BRIDGES_PER_EDGE }) return false
    if (board.edges.indices.any { counts[it] > 0 && hashiBridgesCross(board, counts, it) }) return false
    val totals = hashiIslandTotals(board, counts)
    if (board.islands.indices.any { totals[it] != board.islands[it].count }) return false
    return isHashiConnected(board, counts)
}
