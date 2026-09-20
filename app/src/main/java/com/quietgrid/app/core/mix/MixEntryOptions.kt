package com.quietgrid.app.core.mix

import com.quietgrid.app.core.CHALLENGER_CAPABLE_GAMES
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId

data class MixEntryOption(val mode: MixEntryMode, val difficulty: String?)

fun allModeOptionsFor(gameId: GameId): List<MixEntryOption> =
    Difficulty.entries.map { MixEntryOption(MixEntryMode.PUZZLE, it.key) } +
        if (gameId in CHALLENGER_CAPABLE_GAMES) listOf(MixEntryOption(MixEntryMode.CHALLENGER, null)) else emptyList()

fun missingModeOptionsFor(gameId: GameId, existingEntries: List<MixEntry>): List<MixEntryOption> {
    val used = existingEntries.map { MixEntryOption(it.mode, it.difficulty) }.toSet()
    return allModeOptionsFor(gameId).filterNot { it in used }
}

fun mixesEligibleForQuickAdd(mixes: List<Mix>, gameId: GameId, difficulty: Difficulty): List<Mix> =
    mixes.filterNot { mix ->
        mix.entries.any { it.gameId == gameId.key && it.mode == MixEntryMode.PUZZLE && it.difficulty == difficulty.key }
    }
