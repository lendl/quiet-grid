package com.quietgrid.engine.blockescape

val BlockEscapeBlock.position: Int get() = if (horizontal) col else row

fun BlockEscapeBlock.movedTo(position: Int): BlockEscapeBlock = if (horizontal) copy(col = position) else copy(row = position)

fun BlockEscapeBlock.cells(): List<Int> = List(length) { step ->
    if (horizontal) row * BLOCKESCAPE_SIZE + col + step else (row + step) * BLOCKESCAPE_SIZE + col
}

fun blockEscapeOccupancy(blocks: List<BlockEscapeBlock>): IntArray {
    val owners = IntArray(BLOCKESCAPE_SIZE * BLOCKESCAPE_SIZE) { -1 }
    blocks.forEachIndexed { index, block -> block.cells().forEach { owners[it] = index } }
    return owners
}

fun blockEscapeBlockAt(blocks: List<BlockEscapeBlock>, row: Int, col: Int): Int? {
    if (row !in 0 until BLOCKESCAPE_SIZE || col !in 0 until BLOCKESCAPE_SIZE) return null
    return blockEscapeOccupancy(blocks)[row * BLOCKESCAPE_SIZE + col].takeIf { it >= 0 }
}

fun blockEscapeSlideRange(blocks: List<BlockEscapeBlock>, index: Int): IntRange {
    val owners = blockEscapeOccupancy(blocks)
    val block = blocks[index]
    fun free(position: Int): Boolean {
        if (position !in 0 until BLOCKESCAPE_SIZE) return false
        val cell = if (block.horizontal) block.row * BLOCKESCAPE_SIZE + position else position * BLOCKESCAPE_SIZE + block.col
        return owners[cell] < 0
    }
    var low = block.position
    while (free(low - 1)) low--
    var high = block.position
    while (free(high + block.length)) high++
    return low..high
}

fun applyBlockEscapeMove(blocks: List<BlockEscapeBlock>, move: BlockEscapeMove): List<BlockEscapeBlock>? {
    val block = blocks.getOrNull(move.block) ?: return null
    if (move.to == block.position) return null
    if (move.to !in blockEscapeSlideRange(blocks, move.block)) return null
    return blocks.mapIndexed { index, current -> if (index == move.block) current.movedTo(move.to) else current }
}

fun isBlockEscapeSolved(blocks: List<BlockEscapeBlock>): Boolean {
    val key = blocks[BLOCKESCAPE_KEY_BLOCK]
    return key.col + key.length == BLOCKESCAPE_SIZE
}

fun isValidBlockEscapeLayout(blocks: List<BlockEscapeBlock>): Boolean {
    if (blocks.isEmpty()) return false
    val key = blocks[BLOCKESCAPE_KEY_BLOCK]
    if (!key.horizontal || key.row != BLOCKESCAPE_EXIT_ROW || key.length != 2) return false
    val seen = HashSet<Int>()
    return blocks.all { block ->
        block.length in 2..3 &&
            block.row >= 0 && block.col >= 0 &&
            (if (block.horizontal) block.col + block.length else block.row + block.length) <= BLOCKESCAPE_SIZE &&
            (if (block.horizontal) block.row else block.col) in 0 until BLOCKESCAPE_SIZE &&
            block.cells().all { seen.add(it) }
    }
}

fun blockEscapeLanesMatch(start: List<BlockEscapeBlock>, now: List<BlockEscapeBlock>): Boolean {
    if (start.size != now.size) return false
    return now.zip(start).all { (moved, origin) ->
        moved.horizontal == origin.horizontal && moved.length == origin.length &&
            (if (origin.horizontal) moved.row == origin.row else moved.col == origin.col)
    }
}

private val nonKeyBlockOrder = compareBy<BlockEscapeBlock>({ it.row }, { it.col }, { it.length }, { it.horizontal })

fun blockEscapeLayoutKey(blocks: List<BlockEscapeBlock>): String {
    val ordered = blocks.take(BLOCKESCAPE_KEY_BLOCK + 1) + blocks.drop(BLOCKESCAPE_KEY_BLOCK + 1).sortedWith(nonKeyBlockOrder)
    return ordered.joinToString(";") { "${it.row},${it.col},${it.length},${if (it.horizontal) "h" else "v"}" }
}
