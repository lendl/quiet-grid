package com.quietgrid.engine.links

import com.quietgrid.engine.journey.packRawScore
import com.quietgrid.engine.journey.tieredRawScore

private const val LINKS_UNSOLVED_SCORE = 99
private const val LINKS_PAIR_CEILING = 99

fun linksRawScore(entry: LinksPuzzleEntry): Long {
    val result = solveLinks(entry.size, entry.endpoints)
    val fine = if (!result.solved) {
        packRawScore(LINKS_UNSOLVED_SCORE to 2)
    } else {
        val profile = analyzeLinksSolveResult(result)
        packRawScore(
            profile.deepChainSteps to 2,
            profile.chainSteps to 2,
            profile.maxChainLength to 2,
            profile.lookaheadSteps to 2,
            result.steps.count { it.technique == LinksTechnique.POCKET } to 2,
            LINKS_PAIR_CEILING - entry.pairCount to 2,
        )
    }
    return tieredRawScore(entry.difficulty, fine)
}
