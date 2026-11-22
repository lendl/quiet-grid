package com.quietgrid.engine.links

data class LinksSolveCount(val solutions: Int, val nodes: Int, val exhausted: Boolean)

fun countLinksSolutions(
    size: Int,
    endpoints: List<List<Int>>,
    cap: Int = 2,
    maxNodes: Int = 400_000,
): LinksSolveCount = LinksCounter(size, endpoints, cap, maxNodes).run()

private class LinksCounter(
    private val size: Int,
    private val endpoints: List<List<Int>>,
    private val cap: Int,
    private val maxNodes: Int,
) {
    private val cellCount = size * size
    private val colors = endpoints.size
    private val neighbors = Array(cellCount) { linksNeighbors(it, size).toIntArray() }
    private val owner = IntArray(cellCount) { -1 }
    private val head = IntArray(colors) { endpoints[it][0] }
    private val target = IntArray(colors) { endpoints[it][1] }
    private val done = BooleanArray(colors)
    private val region = IntArray(cellCount)
    private val queue = IntArray(cellCount)
    private var emptyCount = 0
    private var found = 0
    private var nodes = 0
    private var exhausted = true

    fun run(): LinksSolveCount {
        endpoints.forEachIndexed { color, ends -> ends.forEach { owner[it] = color } }
        emptyCount = owner.count { it < 0 }
        if (feasible()) search()
        return LinksSolveCount(found, nodes, exhausted)
    }

    private fun search() {
        if (found >= cap) return
        if (++nodes > maxNodes) {
            exhausted = false
            return
        }
        var chosen = -1
        var chosenMoves = Int.MAX_VALUE
        for (color in 0 until colors) {
            if (done[color]) continue
            val moves = moveCount(color)
            if (moves == 0) return
            if (moves < chosenMoves) {
                chosen = color
                chosenMoves = moves
            }
        }
        if (chosen < 0) {
            if (emptyCount == 0) found++
            return
        }
        val from = head[chosen]
        for (next in neighbors[from]) {
            if (found >= cap || !exhausted) return
            if (next == target[chosen]) {
                done[chosen] = true
                if (feasible()) search()
                done[chosen] = false
            } else if (owner[next] < 0) {
                owner[next] = chosen
                head[chosen] = next
                emptyCount--
                if (feasible()) search()
                emptyCount++
                head[chosen] = from
                owner[next] = -1
            }
        }
    }

    private fun moveCount(color: Int): Int {
        var count = 0
        for (next in neighbors[head[color]]) {
            if (owner[next] < 0 || next == target[color]) count++
        }
        return count
    }

    private fun isOpenEnd(cell: Int): Boolean {
        val color = owner[cell]
        if (color < 0 || done[color]) return false
        return head[color] == cell || target[color] == cell
    }

    private fun feasible(): Boolean {
        for (cell in 0 until cellCount) {
            if (owner[cell] >= 0) continue
            var open = 0
            for (next in neighbors[cell]) if (owner[next] < 0 || isOpenEnd(next)) open++
            if (open < 2) return false
        }
        region.fill(-1)
        var regions = 0
        for (cell in 0 until cellCount) {
            if (owner[cell] >= 0 || region[cell] >= 0) continue
            var read = 0
            var write = 0
            queue[write++] = cell
            region[cell] = regions
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
        val regionServed = BooleanArray(regions)
        for (color in 0 until colors) {
            if (done[color]) continue
            if (linksAreAdjacent(head[color], target[color], size)) {
                markShared(color, regionServed)
                continue
            }
            if (!markShared(color, regionServed)) return false
        }
        return regionServed.all { it }
    }

    private fun markShared(color: Int, served: BooleanArray): Boolean {
        var shared = false
        for (a in neighbors[head[color]]) {
            val ra = region[a]
            if (owner[a] >= 0 || ra < 0) continue
            for (b in neighbors[target[color]]) {
                if (owner[b] < 0 && region[b] == ra) {
                    served[ra] = true
                    shared = true
                }
            }
        }
        return shared
    }
}
