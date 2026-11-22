package com.quietgrid.engine.sequencememory

import kotlin.random.Random

const val SEQUENCE_MEMORY_POINTS_PER_ROUND = 100

enum class SequenceMemoryTapOutcome { CORRECT, ROUND_COMPLETE, SEQUENCE_COMPLETE, WRONG }

fun extendMemorySequence(sequence: List<Int>, padCount: Int, random: Random = Random.Default): List<Int> {
    require(padCount > 0)
    return sequence + random.nextInt(padCount)
}

fun evaluateSequenceMemoryTap(sequence: List<Int>, inputIndex: Int, pad: Int, targetLength: Int): SequenceMemoryTapOutcome {
    require(inputIndex in sequence.indices)
    if (sequence[inputIndex] != pad) return SequenceMemoryTapOutcome.WRONG
    if (inputIndex < sequence.lastIndex) return SequenceMemoryTapOutcome.CORRECT
    return if (sequence.size >= targetLength) SequenceMemoryTapOutcome.SEQUENCE_COMPLETE else SequenceMemoryTapOutcome.ROUND_COMPLETE
}

fun sequenceMemoryScore(completedRounds: Int): Int = completedRounds.coerceAtLeast(0) * SEQUENCE_MEMORY_POINTS_PER_ROUND
