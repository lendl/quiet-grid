package com.quietgrid.engine.links

data class LinksMove(val color: Int, val side: Int, val cell: Int)

class LinksSolverState private constructor(
    val size: Int,
    private val neighbors: Array<IntArray>,
    private val owner: IntArray,
    private val heads: IntArray,
    private val done: BooleanArray,
    emptyCount: Int,
) {
    var emptyCount: Int = emptyCount
        private set

    val colorCount: Int get() = done.size
    val cellCount: Int get() = owner.size

    fun ownerOf(cell: Int): Int = owner[cell]

    fun head(color: Int, side: Int): Int = heads[color * 2 + side]

    fun isDone(color: Int): Boolean = done[color]

    fun isSolved(): Boolean = emptyCount == 0 && done.all { it }

    fun neighborsOf(cell: Int): IntArray = neighbors[cell]

    fun isOpenHead(cell: Int): Boolean {
        val color = owner[cell]
        if (color < 0 || done[color]) return false
        return heads[color * 2] == cell || heads[color * 2 + 1] == cell
    }

    fun sideOfHead(cell: Int): Int = if (heads[owner[cell] * 2] == cell) 0 else 1

    fun moves(color: Int, side: Int): List<Int> {
        val from = head(color, side)
        val partner = head(color, 1 - side)
        val result = ArrayList<Int>(4)
        for (next in neighbors[from]) {
            if (owner[next] < 0 || next == partner) result += next
        }
        return result
    }

    fun apply(move: LinksMove) {
        if (move.cell == head(move.color, 1 - move.side)) {
            done[move.color] = true
            return
        }
        owner[move.cell] = move.color
        heads[move.color * 2 + move.side] = move.cell
        emptyCount--
    }

    fun extendAlong(color: Int, side: Int, cells: List<Int>) {
        for (cell in cells) apply(LinksMove(color, side, cell))
    }

    fun copy(): LinksSolverState =
        LinksSolverState(size, neighbors, owner.copyOf(), heads.copyOf(), done.copyOf(), emptyCount)

    fun isFeasible(): Boolean {
        for (color in 0 until colorCount) {
            if (done[color]) continue
            if (moves(color, 0).isEmpty() || moves(color, 1).isEmpty()) return false
        }
        for (cell in owner.indices) {
            if (owner[cell] < 0 && !cellCanBeThreaded(cell)) return false
        }
        return regionsServed()
    }

    private fun cellCanBeThreaded(cell: Int): Boolean {
        var empty = 0
        val headColors = ArrayList<Int>(4)
        for (next in neighbors[cell]) {
            if (owner[next] < 0) empty++ else if (isOpenHead(next)) headColors += owner[next]
        }
        return when (empty) {
            0 -> headColors.size != headColors.distinct().size
            1 -> headColors.isNotEmpty()
            else -> true
        }
    }

    private fun regionsServed(): Boolean {
        val region = IntArray(owner.size) { -1 }
        val queue = IntArray(owner.size)
        var regions = 0
        for (start in owner.indices) {
            if (owner[start] >= 0 || region[start] >= 0) continue
            var read = 0
            var write = 0
            queue[write++] = start
            region[start] = regions
            while (read < write) {
                val current = queue[read++]
                for (next in neighbors[current]) {
                    if (owner[next] < 0 && region[next] < 0) {
                        region[next] = regions
                        queue[write++] = next
                    }
                }
            }
            regions++
        }
        val served = BooleanArray(regions)
        for (color in 0 until colorCount) {
            if (done[color]) continue
            val shared = markSharedRegions(color, region, served)
            if (!shared && !linksAreAdjacent(head(color, 0), head(color, 1), size)) return false
        }
        return served.all { it }
    }

    private fun markSharedRegions(color: Int, region: IntArray, served: BooleanArray): Boolean {
        var shared = false
        for (a in neighbors[head(color, 0)]) {
            if (owner[a] >= 0) continue
            for (b in neighbors[head(color, 1)]) {
                if (owner[b] < 0 && region[b] == region[a]) {
                    served[region[a]] = true
                    shared = true
                }
            }
        }
        return shared
    }

    companion object {
        fun of(size: Int, endpoints: List<List<Int>>): LinksSolverState {
            val cellCount = size * size
            val owner = IntArray(cellCount) { -1 }
            endpoints.forEachIndexed { color, ends -> ends.forEach { owner[it] = color } }
            return LinksSolverState(
                size = size,
                neighbors = Array(cellCount) { linksNeighbors(it, size).toIntArray() },
                owner = owner,
                heads = IntArray(endpoints.size * 2) { endpoints[it / 2][it % 2] },
                done = BooleanArray(endpoints.size),
                emptyCount = owner.count { it < 0 },
            )
        }
    }
}
