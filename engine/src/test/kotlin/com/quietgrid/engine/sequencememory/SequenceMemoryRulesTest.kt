package com.quietgrid.engine.sequencememory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SequenceMemoryRulesTest {
    @Test
    fun `extending keeps the existing sequence and adds one pad in range`() {
        val random = Random(7)
        var sequence = emptyList<Int>()
        repeat(50) {
            val next = extendMemorySequence(sequence, padCount = 4, random = random)
            assertEquals(sequence.size + 1, next.size)
            assertEquals(sequence, next.dropLast(1))
            assertTrue(next.last() in 0 until 4)
            sequence = next
        }
    }

    @Test
    fun `a matching tap before the end of the sequence is correct`() {
        assertEquals(SequenceMemoryTapOutcome.CORRECT, evaluateSequenceMemoryTap(listOf(2, 0, 3), inputIndex = 1, pad = 0, targetLength = 8))
    }

    @Test
    fun `a wrong pad at any position fails`() {
        assertEquals(SequenceMemoryTapOutcome.WRONG, evaluateSequenceMemoryTap(listOf(2, 0, 3), inputIndex = 0, pad = 1, targetLength = 8))
        assertEquals(SequenceMemoryTapOutcome.WRONG, evaluateSequenceMemoryTap(listOf(2, 0, 3), inputIndex = 2, pad = 0, targetLength = 8))
    }

    @Test
    fun `finishing a sequence shorter than the target completes the round`() {
        assertEquals(SequenceMemoryTapOutcome.ROUND_COMPLETE, evaluateSequenceMemoryTap(listOf(2, 0, 3), inputIndex = 2, pad = 3, targetLength = 8))
    }

    @Test
    fun `finishing a sequence at the target length completes the puzzle`() {
        val sequence = listOf(1, 1, 0, 2)
        assertEquals(SequenceMemoryTapOutcome.SEQUENCE_COMPLETE, evaluateSequenceMemoryTap(sequence, inputIndex = 3, pad = 2, targetLength = 4))
    }

    @Test
    fun `score grows with completed rounds and never goes negative`() {
        assertEquals(0, sequenceMemoryScore(0))
        assertEquals(0, sequenceMemoryScore(-3))
        assertTrue(sequenceMemoryScore(5) > sequenceMemoryScore(4))
    }
}
