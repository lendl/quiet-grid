package com.quietgrid.cli.hashi

import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.hashi.HashiBoard
import com.quietgrid.engine.hashi.HashiPuzzleEntry
import com.quietgrid.engine.hashi.classifyHashiDifficulty
import com.quietgrid.engine.hashi.solveHashi
import java.security.MessageDigest
import kotlin.random.Random

private const val HASHI_MAX_SINGLE_SHARE = 0.3
private const val HASHI_MIN_DOUBLE_BRIDGE_SHARE = 0.15
private const val HASHI_BIG_ISLAND_COUNT = 4
private const val HASHI_MIN_BIG_ISLAND_SHARE = 1.0 / 6
private const val HASHI_SECTORS_PER_SIDE = 3

fun hashiLayoutParamsFor(difficulty: Difficulty): HashiLayoutParams = when (difficulty) {
    Difficulty.EASY -> HashiLayoutParams(7, 7, 10..14, doubleBridgeChance = 0.35, loopChance = 0.3)
    Difficulty.MEDIUM -> HashiLayoutParams(9, 9, 16..22, doubleBridgeChance = 0.3, loopChance = 0.4)
    Difficulty.HARD -> HashiLayoutParams(10, 10, 22..28, doubleBridgeChance = 0.25, loopChance = 0.4)
    Difficulty.EXPERT -> HashiLayoutParams(12, 12, 30..38, doubleBridgeChance = 0.2, loopChance = 0.5)
}

fun passesHashiQuality(layout: HashiLayout, params: HashiLayoutParams): Boolean {
    val islands = layout.islands
    if (islands.size !in params.islandCount) return false
    if (islands.minOf { it.row } > 1 || islands.maxOf { it.row } < layout.rows - 2) return false
    if (islands.minOf { it.col } > 1 || islands.maxOf { it.col } < layout.cols - 2) return false
    val occupiedSectors = islands.map { (it.row * HASHI_SECTORS_PER_SIDE / layout.rows) to (it.col * HASHI_SECTORS_PER_SIDE / layout.cols) }.toSet()
    if (occupiedSectors.size < HASHI_SECTORS_PER_SIDE * HASHI_SECTORS_PER_SIDE) return false
    if (islands.count { it.count == 1 } > islands.size * HASHI_MAX_SINGLE_SHARE) return false
    if (islands.count { it.count >= HASHI_BIG_ISLAND_COUNT } < islands.size * HASHI_MIN_BIG_ISLAND_SHARE) return false
    if (layout.bridges.count { it.count == 2 } < layout.bridges.size * HASHI_MIN_DOUBLE_BRIDGE_SHARE) return false
    return true
}

fun hashiPuzzleKey(layout: HashiLayout): String =
    "${layout.rows}x${layout.cols}:" + layout.islands.joinToString(";") { "${it.row},${it.col},${it.count}" }

fun generateHashiPuzzle(difficulty: Difficulty, random: Random = Random.Default, maxAttempts: Int = 2000): HashiPuzzleEntry? {
    val params = hashiLayoutParamsFor(difficulty)
    repeat(maxAttempts) {
        val layout = generateHashiLayout(params, random) ?: return@repeat
        if (!passesHashiQuality(layout, params)) return@repeat
        val board = HashiBoard(layout.rows, layout.cols, layout.islands)
        val result = solveHashi(board)
        if (classifyHashiDifficulty(result) != difficulty) return@repeat
        val counts = result.counts ?: return@repeat
        return HashiPuzzleEntry(
            id = "h${layout.rows}-${shortDigest(hashiPuzzleKey(layout))}",
            rows = layout.rows,
            cols = layout.cols,
            difficulty = difficulty.key,
            islands = layout.islands,
            solution = board.bridgesFromCounts(counts),
        )
    }
    return null
}

private fun shortDigest(text: String): String =
    MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).take(6).joinToString("") { "%02x".format(it) }
