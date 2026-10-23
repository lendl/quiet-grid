package com.quietgrid.engine.links

import com.quietgrid.engine.core.Difficulty
import kotlinx.serialization.Serializable

@Serializable
data class LinksPuzzleEntry(
    val id: String,
    val size: Int,
    val difficulty: String,
    val endpoints: List<List<Int>>,
    val solution: List<List<Int>>,
) {
    val pairCount: Int get() = endpoints.size
    val cellCount: Int get() = size * size
}

val LINKS_SIZE_BY_DIFFICULTY: Map<Difficulty, Int> = mapOf(
    Difficulty.EASY to 5,
    Difficulty.MEDIUM to 7,
    Difficulty.HARD to 9,
    Difficulty.EXPERT to 11,
)

val LINKS_PAIR_RANGE_BY_DIFFICULTY: Map<Difficulty, IntRange> = mapOf(
    Difficulty.EASY to 4..6,
    Difficulty.MEDIUM to 6..8,
    Difficulty.HARD to 8..11,
    Difficulty.EXPERT to 10..14,
)

const val LINKS_MIN_PATH_LENGTH = 3

fun linksNeighbors(cell: Int, size: Int): List<Int> {
    val row = cell / size
    val col = cell % size
    val result = ArrayList<Int>(4)
    if (row > 0) result += cell - size
    if (col < size - 1) result += cell + 1
    if (row < size - 1) result += cell + size
    if (col > 0) result += cell - 1
    return result
}

fun linksAreAdjacent(a: Int, b: Int, size: Int): Boolean {
    val dr = kotlin.math.abs(a / size - b / size)
    val dc = kotlin.math.abs(a % size - b % size)
    return dr + dc == 1
}

fun LinksPuzzleEntry.endpointColorAt(cell: Int): Int? {
    endpoints.forEachIndexed { color, ends -> if (cell in ends) return color }
    return null
}
