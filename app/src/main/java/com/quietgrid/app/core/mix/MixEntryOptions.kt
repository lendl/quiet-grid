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

private fun MixEntry.isPuzzle(gameId: GameId, difficulty: Difficulty): Boolean =
    this.gameId == gameId.key && mode == MixEntryMode.PUZZLE && this.difficulty == difficulty.key

fun mixIdsContainingPuzzle(mixes: List<Mix>, gameId: GameId, difficulty: Difficulty): Set<String> =
    mixes.filter { mix -> mix.entries.any { it.isPuzzle(gameId, difficulty) } }.map { it.id }.toSet()

fun applyPuzzleMembership(mixes: List<Mix>, gameId: GameId, difficulty: Difficulty, selectedMixIds: Set<String>): List<Mix> =
    mixes.map { mix ->
        val hasEntry = mix.entries.any { it.isPuzzle(gameId, difficulty) }
        val selected = mix.id in selectedMixIds
        when {
            selected && !hasEntry -> mix.copy(
                entries = mix.entries + MixEntry(gameId = gameId.key, mode = MixEntryMode.PUZZLE, difficulty = difficulty.key, weight = 1),
            )
            !selected && hasEntry -> mix.copy(entries = mix.entries.filterNot { it.isPuzzle(gameId, difficulty) })
            else -> mix
        }
    }
