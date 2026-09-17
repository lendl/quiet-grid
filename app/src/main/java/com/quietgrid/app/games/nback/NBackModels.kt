package com.quietgrid.app.games.nback

import com.quietgrid.app.core.Difficulty

enum class NBackModality { SPATIAL }

data class NBackDifficultyConfig(
    val n: Int,
    val intervalMs: Long,
    val modality: NBackModality,
)

data class NBackTrial(
    val position: Int,
    val isTargetMatch: Boolean,
    val responded: Boolean = false,
    val reactionTimeMs: Long? = null,
)

data class NBackSession(
    val difficulty: Difficulty,
    val config: NBackDifficultyConfig,
    val trials: List<NBackTrial>,
    val currentIndex: Int,
    val showStimulus: Boolean = true,
)
