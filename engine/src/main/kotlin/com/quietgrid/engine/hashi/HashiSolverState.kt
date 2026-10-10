package com.quietgrid.engine.hashi

private const val FULL_DOMAIN = (1 shl (HASHI_MAX_BRIDGES_PER_EDGE + 1)) - 1

class HashiSolverState private constructor(val board: HashiBoard, val domains: IntArray) {

    constructor(board: HashiBoard) : this(
        board,
        IntArray(board.edges.size) { edge ->
            val ends = board.edges[edge]
            val cap = minOf(HASHI_MAX_BRIDGES_PER_EDGE, board.islands[ends.a].count, board.islands[ends.b].count)
            FULL_DOMAIN and ((1 shl (cap + 1)) - 1)
        },
    )

    fun lo(edge: Int): Int = Integer.numberOfTrailingZeros(domains[edge])

    fun hi(edge: Int): Int = 31 - Integer.numberOfLeadingZeros(domains[edge])

    fun allows(edge: Int, value: Int): Boolean = domains[edge] and (1 shl value) != 0

    fun isDecided(edge: Int): Boolean = Integer.bitCount(domains[edge]) == 1

    fun isSolved(): Boolean = domains.indices.all { isDecided(it) }

    fun hasEmptyDomain(): Boolean = domains.any { it == 0 }

    fun restrict(edge: Int, mask: Int): Boolean {
        val next = domains[edge] and mask
        if (next == domains[edge]) return false
        domains[edge] = next
        return true
    }

    fun fix(edge: Int, value: Int): Boolean = restrict(edge, 1 shl value)

    fun atLeast(edge: Int, value: Int): Boolean = restrict(edge, FULL_DOMAIN and ((1 shl value) - 1).inv())

    fun loTotals(): IntArray {
        val totals = IntArray(board.islands.size)
        board.edges.forEachIndexed { index, edge ->
            val value = lo(index)
            totals[edge.a] += value
            totals[edge.b] += value
        }
        return totals
    }

    fun hiTotals(): IntArray {
        val totals = IntArray(board.islands.size)
        board.edges.forEachIndexed { index, edge ->
            val value = hi(index)
            totals[edge.a] += value
            totals[edge.b] += value
        }
        return totals
    }

    fun values(): IntArray = IntArray(domains.size) { lo(it) }

    fun copy(): HashiSolverState = HashiSolverState(board, domains.copyOf())

    companion object {
        fun fromPlaced(board: HashiBoard, placed: IntArray): HashiSolverState {
            val state = HashiSolverState(board)
            placed.forEachIndexed { edge, value -> if (value > 0) state.atLeast(edge, value) }
            return state
        }
    }
}
