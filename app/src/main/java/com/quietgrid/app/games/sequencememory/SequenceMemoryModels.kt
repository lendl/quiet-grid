package com.quietgrid.app.games.sequencememory

import kotlinx.serialization.Serializable

enum class SequenceMemoryPhase { SHOWING, INPUT, FAILED, WON }

data class SequenceMemoryDifficultyConfig(
    val padCount: Int,
    val columns: Int,
    val targetLength: Int,
    val flashMs: Long,
    val gapMs: Long,
)

@Serializable
data class SequenceMemorySession(
    val difficulty: String,
    val sequence: List<Int>,
    val completedRounds: Int = 0,
    val inputIndex: Int = 0,
    val phase: SequenceMemoryPhase = SequenceMemoryPhase.SHOWING,
    val wrongPad: Int? = null,
)
