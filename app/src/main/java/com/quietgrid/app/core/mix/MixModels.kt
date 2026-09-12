package com.quietgrid.app.core.mix

import kotlinx.serialization.Serializable

enum class MixEntryMode { PUZZLE, CHALLENGER }

@Serializable
data class MixEntry(
    val gameId: String,
    val mode: MixEntryMode,
    val difficulty: String? = null,
    val weight: Int,
)

@Serializable
data class Mix(
    val id: String,
    val name: String,
    val entries: List<MixEntry>,
)
