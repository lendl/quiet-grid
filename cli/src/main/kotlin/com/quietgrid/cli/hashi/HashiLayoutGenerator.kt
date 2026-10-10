package com.quietgrid.cli.hashi

import com.quietgrid.engine.hashi.HashiBoard
import com.quietgrid.engine.hashi.HashiBridge
import com.quietgrid.engine.hashi.HashiIsland
import kotlin.random.Random

data class HashiLayout(val rows: Int, val cols: Int, val islands: List<HashiIsland>, val bridges: List<HashiBridge>)

data class HashiLayoutParams(
    val rows: Int,
    val cols: Int,
    val islandCount: IntRange,
    val doubleBridgeChance: Double,
    val loopChance: Double,
)

private val DIRECTIONS = listOf(0 to 1, 1 to 0, 0 to -1, -1 to 0)

private const val EMPTY = 0
private const val ISLAND = 1
private const val BRIDGE = 2

fun generateHashiLayout(params: HashiLayoutParams, random: Random = Random.Default): HashiLayout? {
    val rows = params.rows
    val cols = params.cols
    val target = params.islandCount.random(random)
    val cells = Array(rows) { IntArray(cols) }
    val positions = mutableListOf<Pair<Int, Int>>()
    val bridgeCounts = HashMap<Pair<Int, Int>, Int>()

    fun inBounds(row: Int, col: Int) = row in 0 until rows && col in 0 until cols

    fun touchesIsland(row: Int, col: Int): Boolean =
        DIRECTIONS.any { (dr, dc) -> inBounds(row + dr, col + dc) && cells[row + dr][col + dc] == ISLAND }

    fun randomBridgeCount(): Int = if (random.nextDouble() < params.doubleBridgeChance) 2 else 1

    val startRow = random.nextInt(rows)
    val startCol = random.nextInt(cols)
    cells[startRow][startCol] = ISLAND
    positions += startRow to startCol

    var attempts = 0
    while (positions.size < target && attempts < target * 200) {
        attempts++
        val from = random.nextInt(positions.size)
        val (row, col) = positions[from]
        val (dr, dc) = DIRECTIONS[random.nextInt(DIRECTIONS.size)]
        val maxReach = if (dr == 0) cols else rows
        val length = 2 + random.nextInt(maxOf(1, minOf(maxReach / 2, 5) - 1))
        val targetRow = row + dr * length
        val targetCol = col + dc * length
        if (!inBounds(targetRow, targetCol) || cells[targetRow][targetCol] != EMPTY) continue
        if ((1 until length).any { step -> cells[row + dr * step][col + dc * step] != EMPTY }) continue
        if (touchesIsland(targetRow, targetCol)) continue
        cells[targetRow][targetCol] = ISLAND
        (1 until length).forEach { step -> cells[row + dr * step][col + dc * step] = BRIDGE }
        positions += targetRow to targetCol
        bridgeCounts[from to positions.lastIndex] = randomBridgeCount()
    }
    if (positions.size < params.islandCount.first) return null

    val sorted = positions.withIndex().sortedWith(compareBy({ it.value.first }, { it.value.second }))
    val remap = IntArray(positions.size).also { map -> sorted.forEachIndexed { newIndex, old -> map[old.index] = newIndex } }
    val placeholderIslands = sorted.map { HashiIsland(it.value.first, it.value.second, 1) }
    val board = HashiBoard(rows, cols, placeholderIslands)
    val counts = IntArray(board.edges.size)
    for ((pair, count) in bridgeCounts) {
        val edge = board.edgeIndex(remap[pair.first], remap[pair.second])
        if (edge < 0) return null
        counts[edge] = count
    }

    fun pathCells(edge: Int): List<Pair<Int, Int>> {
        val ends = board.edges[edge]
        val a = placeholderIslands[ends.a]
        val b = placeholderIslands[ends.b]
        return if (ends.horizontal) (minOf(a.col, b.col) + 1 until maxOf(a.col, b.col)).map { a.row to it }
        else (minOf(a.row, b.row) + 1 until maxOf(a.row, b.row)).map { it to a.col }
    }

    for (edge in board.edges.indices.shuffled(random)) {
        if (counts[edge] > 0 || random.nextDouble() >= params.loopChance) continue
        val path = pathCells(edge)
        if (path.any { (r, c) -> cells[r][c] != EMPTY }) continue
        path.forEach { (r, c) -> cells[r][c] = BRIDGE }
        counts[edge] = randomBridgeCount()
    }

    val totals = IntArray(placeholderIslands.size)
    board.edges.forEachIndexed { index, edge ->
        totals[edge.a] += counts[index]
        totals[edge.b] += counts[index]
    }
    val islands = placeholderIslands.mapIndexed { index, island -> island.copy(count = totals[index]) }
    return HashiLayout(rows, cols, islands, HashiBoard(rows, cols, islands).bridgesFromCounts(counts))
}
