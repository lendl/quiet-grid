package com.quietgrid.app.core.mix

import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import kotlin.random.Random

sealed interface MixMode {
    data class Puzzle(val difficulty: Difficulty) : MixMode
    data object Challenger : MixMode
}

data class MixCandidate(
    val gameId: GameId,
    val mode: MixMode,
    val weight: Int,
)

fun Mix.resolvedCandidates(): List<MixCandidate> = entries.mapNotNull { entry ->
    if (entry.weight < 1) return@mapNotNull null
    val gameId = GameId.entries.firstOrNull { it.key == entry.gameId } ?: return@mapNotNull null
    val mode = when (entry.mode) {
        MixEntryMode.CHALLENGER -> MixMode.Challenger
        MixEntryMode.PUZZLE -> {
            val difficulty = entry.difficulty?.let { key -> Difficulty.entries.firstOrNull { it.key == key } }
                ?: return@mapNotNull null
            MixMode.Puzzle(difficulty)
        }
    }
    MixCandidate(gameId, mode, entry.weight)
}

fun drawWeighted(candidates: List<MixCandidate>, random: Random = Random): MixCandidate? {
    val total = candidates.sumOf { it.weight }
    if (total <= 0) return null
    var roll = random.nextInt(total)
    for (candidate in candidates) {
        if (roll < candidate.weight) return candidate
        roll -= candidate.weight
    }
    return null
}
