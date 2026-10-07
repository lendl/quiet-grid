package com.quietgrid.cli.whodidit

import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.whodidit.WHODIDIT_EXPERT_MAX_DIRECT_SAME
import com.quietgrid.engine.whodidit.WHODIDIT_SHAPES
import com.quietgrid.engine.whodidit.WhoDidItClue
import com.quietgrid.engine.whodidit.WhoDidItClueType
import com.quietgrid.engine.whodidit.WhoDidItPuzzleEntry
import com.quietgrid.engine.whodidit.WhoDidItPuzzleSpec
import com.quietgrid.engine.whodidit.WhoDidItSolveResult
import com.quietgrid.engine.whodidit.WhoDidItStatement
import com.quietgrid.engine.whodidit.classifyWhoDidIt
import com.quietgrid.engine.whodidit.countWhoDidItExtraClues
import com.quietgrid.engine.whodidit.countWhoDidItSolutions
import com.quietgrid.engine.whodidit.negated
import com.quietgrid.engine.whodidit.profileWhoDidIt
import com.quietgrid.engine.whodidit.solveWhoDidIt
import com.quietgrid.engine.whodidit.whoDidItAllowedClueTypes
import com.quietgrid.engine.whodidit.whoDidItTraceScore
import kotlin.random.Random

const val WHODIDIT_HARDEN_STALL_BUDGET = 40
internal const val WHODIDIT_HARD_MAX_ATTEMPTS = 300

private val STATEMENT_SOURCE_TYPES = setOf(
    WhoDidItClueType.SAME,
    WhoDidItClueType.NOT_SAME,
    WhoDidItClueType.TRAIT,
    WhoDidItClueType.NOT_TRAIT,
)

private val STRONG_CLUE_TYPES = setOf(WhoDidItClueType.SAME, WhoDidItClueType.CRIME)

private val WEAK_CLUE_TYPES = setOf(
    WhoDidItClueType.NOT_SAME,
    WhoDidItClueType.NOT_TRAIT,
    WhoDidItClueType.TRAIT,
    WhoDidItClueType.EITHER,
    WhoDidItClueType.CRIME_TRAIT,
)

private val STATEMENT_CONTENT_TYPES = listOf(WhoDidItClueType.SAME, WhoDidItClueType.NOT_SAME, WhoDidItClueType.TRAIT)

fun assignWhoDidItStatements(setup: WhoDidItSetup, random: Random): List<WhoDidItStatement> {
    val pool = enumerateTrueWhoDidItClues(setup, STATEMENT_SOURCE_TYPES).shuffled(random)
    val truths = pool.filter { it.type in STATEMENT_CONTENT_TYPES }
    val lies = pool.filter { it.type != WhoDidItClueType.TRAIT }.map { it.negated() }
    val used = mutableSetOf<WhoDidItClue>()
    return (0 until setup.shape.size).map { suspect ->
        val options = if (suspect == setup.solution.culprit) lies else truths
        val type = STATEMENT_CONTENT_TYPES.random(random)
        val clue = options.firstOrNull { it.type == type && it !in used } ?: options.first { it !in used }
        used += clue
        WhoDidItStatement(suspect, clue)
    }
}

private class ScoredClues(val clues: List<WhoDidItClue>, val spec: WhoDidItPuzzleSpec, val result: WhoDidItSolveResult) {
    val score: Int = whoDidItTraceScore(profileWhoDidIt(result))

    fun classifiesAs(target: Difficulty): Boolean =
        result.solved && classifyWhoDidIt(spec, result, countWhoDidItExtraClues(spec)) == target
}

private fun WhoDidItSetup.scored(clues: List<WhoDidItClue>, statements: List<WhoDidItStatement>): ScoredClues {
    val spec = toSpec(clues, statements)
    return ScoredClues(clues, spec, solveWhoDidIt(spec))
}

private fun removableStrongClues(clues: List<WhoDidItClue>, target: Difficulty): List<WhoDidItClue> {
    val strong = clues.filter { it.type in STRONG_CLUE_TYPES }
    val same = strong.filterIsInstance<WhoDidItClue.Same>()
    val pushSame = target == Difficulty.EXPERT && same.size > WHODIDIT_EXPERT_MAX_DIRECT_SAME
    return if (pushSame) same else strong
}

internal fun whoDidItTypeFirstDraws(pool: List<WhoDidItClue>, random: Random): Iterator<WhoDidItClue> = iterator {
    val byType = pool.groupBy { it.type }.toSortedMap().mapValuesTo(LinkedHashMap()) { it.value.toMutableList() }
    while (byType.isNotEmpty()) {
        val type = byType.keys.elementAt(random.nextInt(byType.size))
        val clues = byType.getValue(type)
        yield(clues.removeAt(random.nextInt(clues.size)))
        if (clues.isEmpty()) byType.remove(type)
    }
}

private fun weakenOnce(
    setup: WhoDidItSetup,
    clues: List<WhoDidItClue>,
    statements: List<WhoDidItStatement>,
    weakPool: List<WhoDidItClue>,
    target: Difficulty,
    random: Random,
): List<WhoDidItClue>? {
    val removed = removableStrongClues(clues, target).randomOrNull(random) ?: return null
    val next = (clues - removed).toMutableList()
    val additions = whoDidItTypeFirstDraws(weakPool.filter { it !in next }, random)
    while (countWhoDidItSolutions(setup.toSpec(next, statements)) != 1) {
        if (!additions.hasNext()) return null
        next += additions.next()
    }
    return reduceWhoDidItClues(setup, next, statements, target, random)
}

fun hardenWhoDidIt(
    setup: WhoDidItSetup,
    clues: List<WhoDidItClue>,
    statements: List<WhoDidItStatement>,
    target: Difficulty,
    random: Random,
): List<WhoDidItClue>? {
    val weakPool = enumerateTrueWhoDidItClues(setup, whoDidItAllowedClueTypes(target) intersect WEAK_CLUE_TYPES)
    var current = setup.scored(clues, statements)
    if (!current.result.solved) return null
    var reached = current.classifiesAs(target)
    var stalls = 0
    while (!reached && stalls < WHODIDIT_HARDEN_STALL_BUDGET) {
        val candidate = weakenOnce(setup, current.clues, statements, weakPool, target, random)
            ?.let { setup.scored(it, statements) }
        if (candidate != null && candidate.result.solved && candidate.score > current.score) {
            current = candidate
            reached = current.classifiesAs(target)
            stalls = 0
        } else {
            stalls++
        }
    }
    return if (reached) current.clues else null
}

fun generateWhoDidItHardPuzzle(
    difficulty: Difficulty,
    themeId: String?,
    random: Random,
    maxAttempts: Int = WHODIDIT_HARD_MAX_ATTEMPTS,
): WhoDidItPuzzleEntry? {
    if (difficulty != Difficulty.HARD && difficulty != Difficulty.EXPERT) return null
    val shape = WHODIDIT_SHAPES.getValue(difficulty)
    val allowed = whoDidItAllowedClueTypes(difficulty)
    repeat(maxAttempts) {
        val setup = pickWhoDidItSetup(whoDidItAttemptTheme(themeId, random), shape, random) ?: return@repeat
        val statements = if (difficulty == Difficulty.EXPERT) assignWhoDidItStatements(setup, random) else emptyList()
        val all = enumerateTrueWhoDidItClues(setup, allowed)
        if (countWhoDidItSolutions(setup.toSpec(all, statements)) != 1) return@repeat
        val reduced = reduceWhoDidItClues(setup, all, statements, difficulty, random)
        val hardened = hardenWhoDidIt(setup, reduced, statements, difficulty, random) ?: return@repeat
        val entry = setup.shuffledEntryOrNull(difficulty, hardened, statements, random) ?: return@repeat
        return entry
    }
    return null
}
