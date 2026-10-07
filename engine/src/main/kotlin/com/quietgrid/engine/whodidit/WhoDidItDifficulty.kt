package com.quietgrid.engine.whodidit

import com.quietgrid.engine.core.Difficulty

const val WHODIDIT_EASY_MIN_CLUES = 6
const val WHODIDIT_MEDIUM_PROPAGATION_FLOOR = 3
const val WHODIDIT_MEDIUM_MAX_EXTRA_CLUES = 2
const val WHODIDIT_HARD_CONTRADICTION_DEPTH_CEILING = 3
const val WHODIDIT_HARD_INCOMPATIBLE_FLOOR = 2
const val WHODIDIT_EXPERT_CONTRADICTION_DEPTH_FLOOR = 3
const val WHODIDIT_EXPERT_CONTRADICTION_REPEATS_FLOOR = 2
const val WHODIDIT_EXPERT_MAX_DIRECT_SAME = 2

private val EASY_CLUE_TYPES = setOf(
    WhoDidItClueType.SAME,
    WhoDidItClueType.NOT_SAME,
    WhoDidItClueType.TRAIT,
    WhoDidItClueType.CRIME,
)

private val MEDIUM_CLUE_TYPES = EASY_CLUE_TYPES + setOf(
    WhoDidItClueType.EITHER,
    WhoDidItClueType.NOT_TRAIT,
    WhoDidItClueType.CRIME_TRAIT,
)

fun whoDidItAllowedClueTypes(difficulty: Difficulty): Set<WhoDidItClueType> = when (difficulty) {
    Difficulty.EASY -> EASY_CLUE_TYPES
    Difficulty.MEDIUM -> MEDIUM_CLUE_TYPES
    Difficulty.HARD, Difficulty.EXPERT -> WhoDidItClueType.entries.toSet()
}

data class WhoDidItProfile(
    val peak: WhoDidItTechnique?,
    val opening: WhoDidItTechnique?,
    val counts: Map<WhoDidItTechnique, Int>,
    val maxContradictionDepth: Int,
    val stepCount: Int,
)

fun profileWhoDidIt(result: WhoDidItSolveResult): WhoDidItProfile {
    val steps = result.steps
    return WhoDidItProfile(
        peak = steps.maxOfOrNull { it.technique },
        opening = steps.firstOrNull()?.technique,
        counts = steps.groupingBy { it.technique }.eachCount(),
        maxContradictionDepth = steps
            .filter { it.technique == WhoDidItTechnique.CONTRADICTION }
            .maxOfOrNull { it.depth } ?: 0,
        stepCount = steps.size,
    )
}

private fun WhoDidItProfile.count(technique: WhoDidItTechnique): Int = counts[technique] ?: 0

fun whoDidItTraceScore(profile: WhoDidItProfile): Int =
    profile.maxContradictionDepth * 10 +
        profile.count(WhoDidItTechnique.CONTRADICTION) * 5 +
        profile.count(WhoDidItTechnique.INCOMPATIBLE) * 2 +
        profile.count(WhoDidItTechnique.LIAR_HYPOTHESIS) * 20 +
        profile.count(WhoDidItTechnique.STATEMENT_CHECK) * 5 +
        profile.count(WhoDidItTechnique.CLUE_PROPAGATION)

private fun meetsTierRules(difficulty: Difficulty, profile: WhoDidItProfile, spec: WhoDidItPuzzleSpec, extraClues: Int): Boolean {
    val contradictions = profile.count(WhoDidItTechnique.CONTRADICTION)
    val liars = profile.count(WhoDidItTechnique.LIAR_HYPOTHESIS)
    val incompatibles = profile.count(WhoDidItTechnique.INCOMPATIBLE)
    val propagations = profile.count(WhoDidItTechnique.CLUE_PROPAGATION)
    return when (difficulty) {
        Difficulty.EASY -> {
            val peak = profile.peak
            (peak == null || peak <= WhoDidItTechnique.TRANSITIVE) && profile.opening == WhoDidItTechnique.CLUE_DIRECT
        }
        Difficulty.MEDIUM ->
            contradictions == 0 && liars == 0 &&
                incompatibles <= 1 &&
                propagations + incompatibles >= WHODIDIT_MEDIUM_PROPAGATION_FLOOR &&
                extraClues <= WHODIDIT_MEDIUM_MAX_EXTRA_CLUES
        Difficulty.HARD ->
            liars == 0 && extraClues == 0 && (
                (contradictions >= 1 && profile.maxContradictionDepth <= WHODIDIT_HARD_CONTRADICTION_DEPTH_CEILING) ||
                    (contradictions == 0 && incompatibles >= WHODIDIT_HARD_INCOMPATIBLE_FLOOR)
                )
        Difficulty.EXPERT ->
            extraClues == 0 && liars >= 1 &&
                (
                    profile.maxContradictionDepth >= WHODIDIT_EXPERT_CONTRADICTION_DEPTH_FLOOR ||
                        contradictions >= WHODIDIT_EXPERT_CONTRADICTION_REPEATS_FLOOR
                    ) &&
                spec.clues.count { it is WhoDidItClue.Same } <= WHODIDIT_EXPERT_MAX_DIRECT_SAME
    }
}

fun classifyWhoDidIt(spec: WhoDidItPuzzleSpec, result: WhoDidItSolveResult, extraClues: Int): Difficulty? {
    if (!result.solved) return null
    val shape = WhoDidItShape(spec.size, spec.categoryCount)
    val difficulty = WHODIDIT_SHAPES.entries.firstOrNull { it.value == shape }?.key ?: return null
    if (!whoDidItAllowedClueTypes(difficulty).containsAll(spec.clues.map { it.type })) return null
    if (spec.statements.isNotEmpty() != (difficulty == Difficulty.EXPERT)) return null
    return if (meetsTierRules(difficulty, profileWhoDidIt(result), spec, extraClues)) difficulty else null
}
