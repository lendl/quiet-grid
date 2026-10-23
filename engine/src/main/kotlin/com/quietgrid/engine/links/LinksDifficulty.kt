package com.quietgrid.engine.links

import com.quietgrid.engine.core.Difficulty

const val LINKS_MEDIUM_LOOKAHEAD_FLOOR = 2
const val LINKS_EXPERT_CHAIN_FLOOR = 3

data class LinksSolveStep(val technique: LinksTechnique, val chainLength: Int)

data class LinksSolveResult(val solved: Boolean, val steps: List<LinksSolveStep>)

data class LinksSolveProfile(
    val lookaheadSteps: Int,
    val chainSteps: Int,
    val deepChainSteps: Int,
    val maxChainLength: Int,
)

fun nextLinksDeduction(state: LinksSolverState): LinksDeduction? =
    findLinksForced(state)
        ?: findLinksPocket(state)
        ?: findLinksLookahead(state)
        ?: findLinksChain(state)
        ?: findLinksDeepChain(state)

fun solveLinks(size: Int, endpoints: List<List<Int>>): LinksSolveResult {
    val state = LinksSolverState.of(size, endpoints)
    val steps = mutableListOf<LinksSolveStep>()
    while (!state.isSolved()) {
        if (!state.isFeasible()) return LinksSolveResult(false, steps)
        val deduction = nextLinksDeduction(state) ?: return LinksSolveResult(false, steps)
        state.apply(deduction.move)
        steps += LinksSolveStep(deduction.technique, deduction.chainLength)
    }
    return LinksSolveResult(true, steps)
}

fun analyzeLinksSolveResult(result: LinksSolveResult): LinksSolveProfile {
    val chains = result.steps.filter { it.technique == LinksTechnique.CHAIN || it.technique == LinksTechnique.DEEP_CHAIN }
    return LinksSolveProfile(
        lookaheadSteps = result.steps.count { it.technique == LinksTechnique.LOOKAHEAD },
        chainSteps = result.steps.count { it.technique == LinksTechnique.CHAIN },
        deepChainSteps = result.steps.count { it.technique == LinksTechnique.DEEP_CHAIN },
        maxChainLength = chains.maxOfOrNull { it.chainLength } ?: 0,
    )
}

fun classifyLinksDifficulty(result: LinksSolveResult): Difficulty? {
    if (!result.solved) return null
    val profile = analyzeLinksSolveResult(result)
    val chains = profile.chainSteps + profile.deepChainSteps
    return when {
        chains >= LINKS_EXPERT_CHAIN_FLOOR && profile.deepChainSteps > 0 -> Difficulty.EXPERT
        chains > 0 -> Difficulty.HARD
        profile.lookaheadSteps >= LINKS_MEDIUM_LOOKAHEAD_FLOOR -> Difficulty.MEDIUM
        else -> Difficulty.EASY
    }
}

fun gradeLinksPuzzle(size: Int, endpoints: List<List<Int>>): Difficulty? =
    classifyLinksDifficulty(solveLinks(size, endpoints))
