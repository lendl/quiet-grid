package com.quietgrid.engine.blockescape

import com.quietgrid.engine.core.Difficulty
import kotlin.random.Random

private val BLOCKESCAPE_BLOCK_COUNT_BY_DIFFICULTY: Map<Difficulty, IntRange> = mapOf(
    Difficulty.EASY to 7..10,
    Difficulty.MEDIUM to 9..12,
    Difficulty.HARD to 10..13,
    Difficulty.EXPERT to 11..14,
)

private val BLOCKESCAPE_CLIMB_STEPS_BY_DIFFICULTY: Map<Difficulty, Int> = mapOf(
    Difficulty.EASY to 30,
    Difficulty.MEDIUM to 150,
    Difficulty.HARD to 500,
    Difficulty.EXPERT to 1500,
)

private val BLOCKESCAPE_TARGET_CAP_BY_DIFFICULTY: Map<Difficulty, Int> = mapOf(
    Difficulty.EASY to 8,
    Difficulty.MEDIUM to 16,
    Difficulty.HARD to 26,
    Difficulty.EXPERT to 42,
)

private fun addRandomBlock(blocks: MutableList<BlockEscapeBlock>, random: Random): Boolean {
    val owners = blockEscapeOccupancy(blocks)
    repeat(200) {
        val horizontal = random.nextBoolean()
        val length = if (random.nextInt(3) == 0) 3 else 2
        val lane = random.nextInt(BLOCKESCAPE_SIZE)
        if (horizontal && lane == BLOCKESCAPE_EXIT_ROW) return@repeat
        val along = random.nextInt(BLOCKESCAPE_SIZE - length + 1)
        val block = if (horizontal) BlockEscapeBlock(lane, along, length, true) else BlockEscapeBlock(along, lane, length, false)
        if (block.cells().any { owners[it] >= 0 }) return@repeat
        blocks += block
        return true
    }
    return false
}

fun randomBlockEscapeLayout(blockCount: Int, random: Random): List<BlockEscapeBlock> {
    val blocks = mutableListOf(BlockEscapeBlock(BLOCKESCAPE_EXIT_ROW, random.nextInt(3), 2, true))
    while (blocks.size < blockCount) {
        if (!addRandomBlock(blocks, random)) break
    }
    return blocks
}

private fun mutateBlockEscapeLayout(blocks: List<BlockEscapeBlock>, minBlocks: Int, maxBlocks: Int, random: Random): List<BlockEscapeBlock> {
    val next = blocks.toMutableList()
    val roll = random.nextDouble()
    if (next.size > minBlocks && (roll < 0.6 || next.size >= maxBlocks)) next.removeAt(1 + random.nextInt(next.size - 1))
    if (roll > 0.3 || next.size < minBlocks) addRandomBlock(next, random)
    return next
}

fun generateBlockEscapePuzzle(
    difficulty: Difficulty,
    random: Random = Random.Default,
    maxAttempts: Int = 40,
): Pair<List<BlockEscapeBlock>, Int>? {
    val range = BLOCKESCAPE_MOVES_BY_DIFFICULTY.getValue(difficulty)
    val counts = BLOCKESCAPE_BLOCK_COUNT_BY_DIFFICULTY.getValue(difficulty)
    val climbSteps = BLOCKESCAPE_CLIMB_STEPS_BY_DIFFICULTY.getValue(difficulty)
    val targetCap = BLOCKESCAPE_TARGET_CAP_BY_DIFFICULTY.getValue(difficulty)
    repeat(maxAttempts) {
        val target = (range.first..targetCap).random(random)
        var layout = randomBlockEscapeLayout(counts.random(random), random)
        var distances = blockEscapeDistances(layout) ?: return@repeat
        var step = 0
        while (distances.farthest < target && step < climbSteps) {
            step++
            val candidate = mutateBlockEscapeLayout(layout, counts.first, counts.last, random)
            val candidateDistances = blockEscapeDistances(candidate) ?: continue
            if (candidateDistances.farthest >= distances.farthest) {
                layout = candidate
                distances = candidateDistances
            }
        }
        val states = distances.statesAt(target..range.last)
        if (states.isEmpty()) return@repeat
        val chosen = states[random.nextInt(states.size)]
        val blocks = distances.blocksOf(chosen)
        if (blocks.size !in counts) return@repeat
        val minMoves = distances.distances.getValue(chosen)
        return blocks to minMoves
    }
    return null
}
