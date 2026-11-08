package com.quietgrid.engine.blockescape

const val BLOCKESCAPE_MAX_STATES = 250_000

internal class BlockEscapeSpace(blocks: List<BlockEscapeBlock>) {
    private val horizontal = BooleanArray(blocks.size) { blocks[it].horizontal }
    private val lengths = IntArray(blocks.size) { blocks[it].length }
    private val lanes = IntArray(blocks.size) { if (blocks[it].horizontal) blocks[it].row else blocks[it].col }
    val count = blocks.size

    fun encode(positions: IntArray): Long {
        var key = 0L
        for (index in positions.indices) key = key or (positions[index].toLong() shl (index * 3))
        return key
    }

    fun decode(key: Long): IntArray = IntArray(count) { ((key shr (it * 3)) and 7L).toInt() }

    fun isSolved(key: Long): Boolean =
        ((key and 7L).toInt()) + lengths[BLOCKESCAPE_KEY_BLOCK] == BLOCKESCAPE_SIZE

    fun forEachNeighbor(key: Long, action: (index: Int, to: Int, next: Long) -> Unit) {
        val positions = decode(key)
        val occupied = BooleanArray(BLOCKESCAPE_SIZE * BLOCKESCAPE_SIZE)
        for (index in 0 until count) {
            for (step in 0 until lengths[index]) occupied[cellOf(index, positions[index] + step)] = true
        }
        for (index in 0 until count) {
            val shift = index * 3
            val cleared = key and (7L shl shift).inv()
            var to = positions[index] - 1
            while (to >= 0 && !occupied[cellOf(index, to)]) {
                action(index, to, cleared or (to.toLong() shl shift))
                to--
            }
            to = positions[index] + 1
            while (to + lengths[index] - 1 < BLOCKESCAPE_SIZE && !occupied[cellOf(index, to + lengths[index] - 1)]) {
                action(index, to, cleared or (to.toLong() shl shift))
                to++
            }
        }
    }

    private fun cellOf(index: Int, along: Int): Int =
        if (horizontal[index]) lanes[index] * BLOCKESCAPE_SIZE + along else along * BLOCKESCAPE_SIZE + lanes[index]
}

private fun positionsOf(blocks: List<BlockEscapeBlock>): IntArray = IntArray(blocks.size) { blocks[it].position }

fun solveBlockEscape(blocks: List<BlockEscapeBlock>, maxStates: Int = BLOCKESCAPE_MAX_STATES): List<BlockEscapeMove>? {
    if (!isValidBlockEscapeLayout(blocks)) return null
    val space = BlockEscapeSpace(blocks)
    val start = space.encode(positionsOf(blocks))
    if (space.isSolved(start)) return emptyList()
    val parents = HashMap<Long, Long>()
    val moves = HashMap<Long, BlockEscapeMove>()
    parents[start] = start
    val queue = ArrayDeque<Long>()
    queue.addLast(start)
    while (queue.isNotEmpty()) {
        val key = queue.removeFirst()
        var found: Long? = null
        space.forEachNeighbor(key) { index, to, next ->
            if (found == null && next !in parents) {
                parents[next] = key
                moves[next] = BlockEscapeMove(index, to)
                if (space.isSolved(next)) found = next else queue.addLast(next)
            }
        }
        found?.let { goal ->
            val path = ArrayList<BlockEscapeMove>()
            var cursor = goal
            while (cursor != start) {
                path += moves.getValue(cursor)
                cursor = parents.getValue(cursor)
            }
            return path.reversed()
        }
        if (parents.size > maxStates) return null
    }
    return null
}

fun blockEscapeMinMoves(blocks: List<BlockEscapeBlock>): Int? = solveBlockEscape(blocks)?.size

fun findBlockEscapeHint(blocks: List<BlockEscapeBlock>): BlockEscapeMove? = solveBlockEscape(blocks)?.firstOrNull()

class BlockEscapeDistances internal constructor(
    private val space: BlockEscapeSpace,
    private val template: List<BlockEscapeBlock>,
    val distances: Map<Long, Int>,
) {
    val farthest: Int get() = distances.values.maxOrNull() ?: 0

    fun statesAt(range: IntRange): List<Long> = distances.filterValues { it in range }.keys.toList()

    fun blocksOf(key: Long): List<BlockEscapeBlock> {
        val positions = space.decode(key)
        return template.mapIndexed { index, block -> block.movedTo(positions[index]) }
    }
}

fun blockEscapeDistances(blocks: List<BlockEscapeBlock>, maxStates: Int = BLOCKESCAPE_MAX_STATES): BlockEscapeDistances? {
    if (!isValidBlockEscapeLayout(blocks)) return null
    val space = BlockEscapeSpace(blocks)
    val start = space.encode(positionsOf(blocks))
    val component = HashSet<Long>()
    component += start
    val queue = ArrayDeque<Long>()
    queue.addLast(start)
    while (queue.isNotEmpty()) {
        val key = queue.removeFirst()
        space.forEachNeighbor(key) { _, _, next -> if (component.add(next)) queue.addLast(next) }
        if (component.size > maxStates) return null
    }
    val distances = HashMap<Long, Int>()
    component.filter { space.isSolved(it) }.forEach {
        distances[it] = 0
        queue.addLast(it)
    }
    if (distances.isEmpty()) return null
    while (queue.isNotEmpty()) {
        val key = queue.removeFirst()
        val distance = distances.getValue(key) + 1
        space.forEachNeighbor(key) { _, _, next ->
            if (next !in distances) {
                distances[next] = distance
                queue.addLast(next)
            }
        }
    }
    return BlockEscapeDistances(space, blocks, distances)
}
