package com.quietgrid.app.core.mix

import com.quietgrid.app.core.GameId
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

fun nextMixName(existingMixCount: Int): String = "My Mix #${existingMixCount + 1}"

fun groupEntriesByKnownGame(entries: List<MixEntry>): List<Pair<GameId, List<MixEntry>>> =
    entries.groupBy { it.gameId }
        .mapNotNull { (gameIdKey, groupEntries) ->
            val gameId = GameId.entries.firstOrNull { it.key == gameIdKey } ?: return@mapNotNull null
            gameId to groupEntries
        }
