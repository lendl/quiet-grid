package com.quietgrid.engine.journey

import kotlinx.serialization.Serializable

@Serializable
data class JourneyStop(
    val puzzleId: String,
    val difficulty: String,
    val rating: Int,
    val targetSeconds: Int,
    val replaces: String? = null,
    val replayTransforms: List<Int> = emptyList(),
)

@Serializable
data class JourneyChapter(
    val index: Int,
    val minRating: Int,
    val maxRating: Int,
    val stops: List<JourneyStop>,
)

@Serializable
data class JourneyFile(
    val game: String,
    val chapters: List<JourneyChapter>,
)
