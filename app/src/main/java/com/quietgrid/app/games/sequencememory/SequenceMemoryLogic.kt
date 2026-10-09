package com.quietgrid.app.games.sequencememory

import com.quietgrid.app.core.Difficulty
import com.quietgrid.engine.sequencememory.SequenceMemoryTapOutcome
import com.quietgrid.engine.sequencememory.evaluateSequenceMemoryTap
import com.quietgrid.engine.sequencememory.extendMemorySequence
import kotlin.random.Random

private val SEQUENCE_MEMORY_TIER_CONFIG = mapOf(
    Difficulty.EASY to SequenceMemoryDifficultyConfig(padCount = 4, columns = 2, targetLength = 8, flashMs = 600L, gapMs = 250L),
    Difficulty.MEDIUM to SequenceMemoryDifficultyConfig(padCount = 4, columns = 2, targetLength = 12, flashMs = 450L, gapMs = 200L),
    Difficulty.HARD to SequenceMemoryDifficultyConfig(padCount = 6, columns = 3, targetLength = 14, flashMs = 380L, gapMs = 160L),
    Difficulty.EXPERT to SequenceMemoryDifficultyConfig(padCount = 9, columns = 3, targetLength = 16, flashMs = 320L, gapMs = 130L),
)

fun sequenceMemoryConfig(difficulty: Difficulty): SequenceMemoryDifficultyConfig = SEQUENCE_MEMORY_TIER_CONFIG.getValue(difficulty)

fun sequenceMemoryConfig(session: SequenceMemorySession): SequenceMemoryDifficultyConfig = sequenceMemoryConfig(Difficulty.fromKey(session.difficulty))

fun createSequenceMemorySession(difficulty: Difficulty, random: Random = Random.Default): SequenceMemorySession = SequenceMemorySession(
    difficulty = difficulty.key,
    sequence = extendMemorySequence(emptyList(), sequenceMemoryConfig(difficulty).padCount, random),
)

fun applySequenceMemoryTap(session: SequenceMemorySession, pad: Int, random: Random = Random.Default): SequenceMemorySession {
    if (session.phase != SequenceMemoryPhase.INPUT) return session
    val config = sequenceMemoryConfig(session)
    return when (evaluateSequenceMemoryTap(session.sequence, session.inputIndex, pad, config.targetLength)) {
        SequenceMemoryTapOutcome.CORRECT -> session.copy(inputIndex = session.inputIndex + 1)
        SequenceMemoryTapOutcome.ROUND_COMPLETE -> session.copy(
            sequence = extendMemorySequence(session.sequence, config.padCount, random),
            completedRounds = session.sequence.size,
            inputIndex = 0,
            phase = SequenceMemoryPhase.SHOWING,
        )
        SequenceMemoryTapOutcome.SEQUENCE_COMPLETE -> session.copy(
            completedRounds = session.sequence.size,
            inputIndex = 0,
            phase = SequenceMemoryPhase.WON,
        )
        SequenceMemoryTapOutcome.WRONG -> session.copy(phase = SequenceMemoryPhase.FAILED, wrongPad = pad)
    }
}

fun sequenceMemoryExpectedPad(session: SequenceMemorySession): Int? = session.sequence.getOrNull(session.inputIndex)

fun sequenceMemorySessionForReplay(session: SequenceMemorySession): SequenceMemorySession =
    session.copy(inputIndex = 0, phase = SequenceMemoryPhase.SHOWING, wrongPad = null)

fun isRestorableSequenceMemorySession(session: SequenceMemorySession): Boolean {
    val difficulty = Difficulty.entries.firstOrNull { it.key == session.difficulty } ?: return false
    val config = sequenceMemoryConfig(difficulty)
    return session.sequence.isNotEmpty() &&
        session.sequence.size <= config.targetLength &&
        session.sequence.all { it in 0 until config.padCount }
}
