package com.quietgrid.app.games.sequencememory

import com.quietgrid.app.core.Difficulty
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class SequenceMemoryLogicTest {
    private fun inputSession(difficulty: Difficulty, sequence: List<Int>, inputIndex: Int = 0) = SequenceMemorySession(
        difficulty = difficulty.key,
        sequence = sequence,
        completedRounds = sequence.size - 1,
        inputIndex = inputIndex,
        phase = SequenceMemoryPhase.INPUT,
    )

    @Test
    fun `a fresh session starts with one pad and plays it back first`() {
        Difficulty.entries.forEach { difficulty ->
            val session = createSequenceMemorySession(difficulty, Random(1))
            assertEquals(1, session.sequence.size)
            assertTrue(session.sequence.single() in 0 until sequenceMemoryConfig(difficulty).padCount)
            assertEquals(SequenceMemoryPhase.SHOWING, session.phase)
            assertEquals(0, session.completedRounds)
        }
    }

    @Test
    fun `harder tiers use more pads, faster flashes and longer sequences`() {
        val configs = Difficulty.entries.map(::sequenceMemoryConfig)
        configs.zipWithNext().forEach { (easier, harder) ->
            assertTrue(harder.padCount >= easier.padCount)
            assertTrue(harder.targetLength > easier.targetLength)
            assertTrue(harder.flashMs < easier.flashMs)
        }
    }

    @Test
    fun `a correct tap mid-sequence advances the input`() {
        val next = applySequenceMemoryTap(inputSession(Difficulty.EASY, listOf(0, 3, 1)), pad = 0)
        assertEquals(1, next.inputIndex)
        assertEquals(SequenceMemoryPhase.INPUT, next.phase)
    }

    @Test
    fun `finishing a round grows the sequence by one and replays it`() {
        val next = applySequenceMemoryTap(inputSession(Difficulty.EASY, listOf(0, 3, 1), inputIndex = 2), pad = 1, random = Random(3))
        assertEquals(listOf(0, 3, 1), next.sequence.take(3))
        assertEquals(4, next.sequence.size)
        assertEquals(3, next.completedRounds)
        assertEquals(0, next.inputIndex)
        assertEquals(SequenceMemoryPhase.SHOWING, next.phase)
    }

    @Test
    fun `finishing the target length wins`() {
        val sequence = List(sequenceMemoryConfig(Difficulty.EASY).targetLength) { it % 4 }
        val next = applySequenceMemoryTap(inputSession(Difficulty.EASY, sequence, inputIndex = sequence.lastIndex), pad = sequence.last())
        assertEquals(SequenceMemoryPhase.WON, next.phase)
        assertEquals(sequence.size, next.completedRounds)
        assertEquals(sequence.size, next.sequence.size)
    }

    @Test
    fun `a wrong tap fails and remembers the pad and the expected pad`() {
        val next = applySequenceMemoryTap(inputSession(Difficulty.MEDIUM, listOf(2, 2, 0), inputIndex = 1), pad = 3)
        assertEquals(SequenceMemoryPhase.FAILED, next.phase)
        assertEquals(3, next.wrongPad)
        assertEquals(2, sequenceMemoryExpectedPad(next))
    }

    @Test
    fun `taps are ignored while the sequence is playing`() {
        val showing = inputSession(Difficulty.EASY, listOf(1)).copy(phase = SequenceMemoryPhase.SHOWING)
        assertEquals(showing, applySequenceMemoryTap(showing, pad = 0))
    }

    @Test
    fun `a saved session replays its current round from the start`() {
        val failedMidRound = inputSession(Difficulty.HARD, listOf(5, 4, 3), inputIndex = 2).copy(phase = SequenceMemoryPhase.FAILED, wrongPad = 1)
        val json = Json { ignoreUnknownKeys = true }
        val restored = json.decodeFromString(SequenceMemorySession.serializer(), json.encodeToString(SequenceMemorySession.serializer(), sequenceMemorySessionForReplay(failedMidRound)))
        assertEquals(listOf(5, 4, 3), restored.sequence)
        assertEquals(0, restored.inputIndex)
        assertEquals(SequenceMemoryPhase.SHOWING, restored.phase)
        assertNull(restored.wrongPad)
    }

    @Test
    fun `saved sessions with an unknown tier or out-of-range pads are not restored`() {
        assertTrue(isRestorableSequenceMemorySession(SequenceMemorySession(difficulty = "easy", sequence = listOf(0, 3))))
        assertFalse(isRestorableSequenceMemorySession(SequenceMemorySession(difficulty = "legendary", sequence = listOf(0))))
        assertFalse(isRestorableSequenceMemorySession(SequenceMemorySession(difficulty = "easy", sequence = listOf(0, 8))))
        assertFalse(isRestorableSequenceMemorySession(SequenceMemorySession(difficulty = "easy", sequence = emptyList())))
    }
}
