package com.quietgrid.engine.circuit

private fun maskBit(mask: Int): Int = 1 shl mask

fun circuitDomainOf(mask: Int): Int = circuitOrientations(mask).fold(0) { acc, orientation -> acc or maskBit(orientation) }

class CircuitSolverState private constructor(val board: CircuitBoard, val domains: IntArray) {

    fun options(cell: Int): IntArray {
        val domain = domains[cell]
        val result = IntArray(Integer.bitCount(domain))
        var index = 0
        for (mask in 0..CIRCUIT_ALL_ARMS) if (domain and maskBit(mask) != 0) result[index++] = mask
        return result
    }

    fun allows(cell: Int, mask: Int): Boolean = domains[cell] and maskBit(mask) != 0

    fun isDecided(cell: Int): Boolean = Integer.bitCount(domains[cell]) == 1

    fun decided(cell: Int): Int = Integer.numberOfTrailingZeros(domains[cell])

    fun isSolved(): Boolean = domains.indices.all { isDecided(it) }

    fun hasEmptyDomain(): Boolean = domains.any { it == 0 }

    fun canHave(cell: Int, direction: Int): Boolean {
        val domain = domains[cell]
        for (mask in 0..CIRCUIT_ALL_ARMS) if (domain and maskBit(mask) != 0 && mask and direction != 0) return true
        return false
    }

    fun mustHave(cell: Int, direction: Int): Boolean {
        val domain = domains[cell]
        if (domain == 0) return false
        for (mask in 0..CIRCUIT_ALL_ARMS) if (domain and maskBit(mask) != 0 && mask and direction == 0) return false
        return true
    }

    fun linkForced(cell: Int, direction: Int): Boolean {
        val other = board.neighbor(cell, direction)
        if (other < 0) return false
        return mustHave(cell, direction) || mustHave(other, oppositeCircuitDirection(direction))
    }

    fun linkPossible(cell: Int, direction: Int): Boolean {
        val other = board.neighbor(cell, direction)
        if (other < 0) return false
        return canHave(cell, direction) && canHave(other, oppositeCircuitDirection(direction))
    }

    fun restrict(cell: Int, domain: Int): Boolean {
        val next = domains[cell] and domain
        if (next == domains[cell]) return false
        domains[cell] = next
        return true
    }

    fun fix(cell: Int, mask: Int): Boolean = restrict(cell, maskBit(mask))

    fun remove(cell: Int, mask: Int): Boolean = restrict(cell, maskBit(mask).inv())

    fun values(): IntArray = IntArray(domains.size) { decided(it) }

    fun copy(): CircuitSolverState = CircuitSolverState(board, domains.copyOf())

    companion object {
        fun fromShapes(board: CircuitBoard, shapes: IntArray): CircuitSolverState =
            CircuitSolverState(board, IntArray(board.size) { circuitDomainOf(shapes[it]) })

        fun withFixed(board: CircuitBoard, shapes: IntArray, fixed: Map<Int, Int>): CircuitSolverState {
            val state = fromShapes(board, shapes)
            fixed.forEach { (cell, mask) -> state.fix(cell, mask) }
            return state
        }
    }
}

internal class CircuitUnionFind(size: Int) {
    private val parent = IntArray(size) { it }
    private val rank = IntArray(size)
    val sizes = IntArray(size) { 1 }

    fun find(cell: Int): Int {
        var root = cell
        while (parent[root] != root) root = parent[root]
        var walk = cell
        while (parent[walk] != root) {
            val next = parent[walk]
            parent[walk] = root
            walk = next
        }
        return root
    }

    fun union(a: Int, b: Int): Boolean {
        val rootA = find(a)
        val rootB = find(b)
        if (rootA == rootB) return false
        if (rank[rootA] < rank[rootB]) {
            parent[rootA] = rootB
            sizes[rootB] += sizes[rootA]
        } else {
            parent[rootB] = rootA
            sizes[rootA] += sizes[rootB]
            if (rank[rootA] == rank[rootB]) rank[rootA]++
        }
        return true
    }
}

internal fun CircuitSolverState.forcedComponents(): Pair<CircuitUnionFind, Boolean> {
    val groups = CircuitUnionFind(board.size)
    var looped = false
    for (cell in 0 until board.size) {
        for (direction in intArrayOf(CIRCUIT_RIGHT, CIRCUIT_DOWN)) {
            if (!linkForced(cell, direction)) continue
            if (!groups.union(cell, board.neighbor(cell, direction))) looped = true
        }
    }
    return groups to looped
}
