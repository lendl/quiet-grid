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

fun nextMixName(existingNames: Collection<String>, nameForNumber: (Int) -> String): String =
    generateSequence(existingNames.size + 1) { it + 1 }
        .map(nameForNumber)
        .first { it !in existingNames }

fun groupEntriesByKnownGame(entries: List<MixEntry>): List<Pair<GameId, List<MixEntry>>> =
    entries.groupBy { it.gameId }
        .mapNotNull { (gameIdKey, groupEntries) ->
            val gameId = GameId.entries.firstOrNull { it.key == gameIdKey } ?: return@mapNotNull null
            gameId to groupEntries
        }
