package com.quietgrid.cli.whodidit

import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.whodidit.WHODIDIT_EASY_MIN_CLUES
import com.quietgrid.engine.whodidit.WHODIDIT_POOL_SIZE
import com.quietgrid.engine.whodidit.WHODIDIT_SHAPES
import com.quietgrid.engine.whodidit.WHODIDIT_THEMES
import com.quietgrid.engine.whodidit.WhoDidItCategory
import com.quietgrid.engine.whodidit.WhoDidItClue
import com.quietgrid.engine.whodidit.WhoDidItClueType
import com.quietgrid.engine.whodidit.WhoDidItPuzzleEntry
import com.quietgrid.engine.whodidit.WhoDidItPuzzleSpec
import com.quietgrid.engine.whodidit.WhoDidItRef
import com.quietgrid.engine.whodidit.WhoDidItShape
import com.quietgrid.engine.whodidit.WhoDidItSolution
import com.quietgrid.engine.whodidit.WhoDidItStatement
import com.quietgrid.engine.whodidit.WhoDidItTechnique
import com.quietgrid.engine.whodidit.WhoDidItThemeDef
import com.quietgrid.engine.whodidit.WhoDidItTrait
import com.quietgrid.engine.whodidit.classifyWhoDidIt
import com.quietgrid.engine.whodidit.countWhoDidItExtraClues
import com.quietgrid.engine.whodidit.countWhoDidItSolutions
import com.quietgrid.engine.whodidit.isTrue
import com.quietgrid.engine.whodidit.profileWhoDidIt
import com.quietgrid.engine.whodidit.solveWhoDidIt
import com.quietgrid.engine.whodidit.whoDidItAllowedClueTypes
import com.quietgrid.engine.whodidit.whoDidItTheme
import kotlin.math.ln
import kotlin.random.Random

private const val SETUP_RETRIES = 50
private const val HEAVY_REMOVAL_WEIGHT = 2.5
private const val EITHER_REMOVAL_WEIGHT = 1.5
private const val EASY_MEDIUM_MAX_ATTEMPTS = 200

data class WhoDidItSetup(
    val themeId: String,
    val case: Int,
    val shape: WhoDidItShape,
    val entries: List<List<Int>>,
    val solution: WhoDidItSolution,
)

private fun hasConstantDimension(theme: WhoDidItThemeDef, shape: WhoDidItShape, entries: List<List<Int>>): Boolean =
    WhoDidItCategory.entries.take(shape.categoryCount).any { category ->
        val pool = theme.pools.getValue(category)
        val dimensions = pool.flatten().map { it.dimension }.toSet()
        dimensions.any { dimension ->
            entries[category.ordinal].flatMap { pool[it] }.filter { it.dimension == dimension }.toSet().size < 2
        }
    }

fun pickWhoDidItSetup(theme: WhoDidItThemeDef, shape: WhoDidItShape, random: Random): WhoDidItSetup? {
    repeat(SETUP_RETRIES) {
        val entries = List(shape.categoryCount) { (0 until WHODIDIT_POOL_SIZE).shuffled(random).take(shape.size) }
        if (hasConstantDimension(theme, shape, entries)) return@repeat
        val identity = List(shape.size) { it }
        val slotOf = listOf(identity) + List(shape.categoryCount - 1) { identity.shuffled(random) }
        return WhoDidItSetup(
            themeId = theme.id,
            case = random.nextInt(theme.caseCount),
            shape = shape,
            entries = entries,
            solution = WhoDidItSolution(slotOf, random.nextInt(shape.size)),
        )
    }
    return null
}

fun WhoDidItSetup.toSpec(clues: List<WhoDidItClue>, statements: List<WhoDidItStatement>): WhoDidItPuzzleSpec {
    val pools = whoDidItTheme(themeId).pools
    val traits = (0 until shape.categoryCount).map { category ->
        val pool = pools.getValue(WhoDidItCategory.entries[category])
        entries[category].map { pool[it] }
    }
    return WhoDidItPuzzleSpec(shape.size, shape.categoryCount, traits, clues, statements)
}

fun enumerateTrueWhoDidItClues(setup: WhoDidItSetup, allowed: Set<WhoDidItClueType>): List<WhoDidItClue> {
    val spec = setup.toSpec(emptyList(), emptyList())
    val shape = setup.shape
    val refs = (0 until shape.categoryCount).flatMap { category -> (0 until shape.size).map { WhoDidItRef(category, it) } }
    val traitsInPlay = WhoDidItTrait.entries.filter { it.category.ordinal < shape.categoryCount }
    val informative = traitsInPlay.filter { trait ->
        spec.traits[trait.category.ordinal].count { trait in it } in 1 until shape.size
    }.toSet()
    val result = mutableListOf<WhoDidItClue>()
    fun add(clue: WhoDidItClue) {
        if (clue.type in allowed && clue.isTrue(spec, setup.solution)) result += clue
    }
    for (a in refs) {
        for (b in refs) {
            if (a.category < b.category) {
                add(WhoDidItClue.Same(a, b))
                add(WhoDidItClue.NotSame(a, b))
            }
            if (b.category != a.category) {
                for (c in refs) {
                    if (c.category == b.category && b.slot < c.slot) add(WhoDidItClue.Either(a, b, c))
                }
            }
        }
        if (a.category != 0) add(WhoDidItClue.Crime(a))
        for (trait in informative) {
            if (a.category == trait.category.ordinal) continue
            add(WhoDidItClue.HasTrait(a, trait))
            add(WhoDidItClue.LacksTrait(a, trait))
        }
    }
    informative.forEach { add(WhoDidItClue.CrimeTrait(it)) }
    return result
}

private fun removalWeight(clue: WhoDidItClue): Double = when (clue.type) {
    WhoDidItClueType.NOT_SAME, WhoDidItClueType.NOT_TRAIT -> HEAVY_REMOVAL_WEIGHT
    WhoDidItClueType.EITHER -> EITHER_REMOVAL_WEIGHT
    else -> 1.0
}

private fun weightedShuffle(clues: List<WhoDidItClue>, random: Random): List<WhoDidItClue> =
    clues.map { it to -ln(1.0 - random.nextDouble()) / removalWeight(it) }.sortedBy { it.second }.map { it.first }

private fun traceAllows(difficulty: Difficulty, spec: WhoDidItPuzzleSpec): Boolean {
    if (difficulty == Difficulty.HARD || difficulty == Difficulty.EXPERT) return true
    val result = solveWhoDidIt(spec)
    if (!result.solved) return false
    val profile = profileWhoDidIt(result)
    return when (difficulty) {
        Difficulty.EASY -> (profile.peak ?: WhoDidItTechnique.CLUE_DIRECT) <= WhoDidItTechnique.TRANSITIVE
        else -> profile.counts.keys.none { it == WhoDidItTechnique.CONTRADICTION || it == WhoDidItTechnique.LIAR_HYPOTHESIS }
    }
}

fun reduceWhoDidItClues(
    setup: WhoDidItSetup,
    clues: List<WhoDidItClue>,
    statements: List<WhoDidItStatement>,
    difficulty: Difficulty,
    random: Random,
): List<WhoDidItClue> {
    val kept = clues.toMutableList()
    for (candidate in weightedShuffle(clues, random)) {
        if (difficulty == Difficulty.EASY && kept.size <= WHODIDIT_EASY_MIN_CLUES) break
        val spec = setup.toSpec(kept - candidate, statements)
        if (countWhoDidItSolutions(spec) == 1 && traceAllows(difficulty, spec)) kept.remove(candidate)
    }
    return kept
}

fun whoDidItEntryId(difficulty: Difficulty, entry: WhoDidItPuzzleEntry): String {
    val signature = "${entry.theme}|${entry.entries}|${entry.solution}|${entry.culprit}|${entry.clues}|${entry.statements}"
    return "whodidit-${difficulty.key}-" + signature.hashCode().toUInt().toString(16)
}

private fun WhoDidItSetup.toEntry(
    difficulty: Difficulty,
    clues: List<WhoDidItClue>,
    statements: List<WhoDidItStatement>,
): WhoDidItPuzzleEntry {
    val draft = WhoDidItPuzzleEntry(
        id = "",
        difficulty = difficulty.key,
        theme = themeId,
        case = case,
        size = shape.size,
        categories = WhoDidItCategory.entries.take(shape.categoryCount),
        entries = entries,
        solution = solution.slotOf,
        culprit = solution.culprit,
        clues = clues,
        statements = statements,
    )
    return draft.copy(id = whoDidItEntryId(difficulty, draft))
}

internal fun WhoDidItSetup.shuffledEntryOrNull(
    difficulty: Difficulty,
    clues: List<WhoDidItClue>,
    statements: List<WhoDidItStatement>,
    random: Random,
): WhoDidItPuzzleEntry? {
    val ordered = clues.shuffled(random)
    val spec = toSpec(ordered, statements)
    if (classifyWhoDidIt(spec, solveWhoDidIt(spec), countWhoDidItExtraClues(spec)) != difficulty) return null
    return toEntry(difficulty, ordered, statements)
}

internal fun whoDidItAttemptTheme(themeId: String?, random: Random): WhoDidItThemeDef =
    if (themeId != null) whoDidItTheme(themeId) else WHODIDIT_THEMES[random.nextInt(WHODIDIT_THEMES.size)]

fun generateWhoDidItPuzzle(
    difficulty: Difficulty,
    themeId: String?,
    random: Random,
    maxAttempts: Int? = null,
): WhoDidItPuzzleEntry? {
    if (difficulty == Difficulty.HARD || difficulty == Difficulty.EXPERT) {
        return generateWhoDidItHardPuzzle(difficulty, themeId, random, maxAttempts ?: WHODIDIT_HARD_MAX_ATTEMPTS)
    }
    val shape = WHODIDIT_SHAPES.getValue(difficulty)
    repeat(maxAttempts ?: EASY_MEDIUM_MAX_ATTEMPTS) {
        val setup = pickWhoDidItSetup(whoDidItAttemptTheme(themeId, random), shape, random) ?: return@repeat
        val all = enumerateTrueWhoDidItClues(setup, whoDidItAllowedClueTypes(difficulty))
        if (countWhoDidItSolutions(setup.toSpec(all, emptyList())) != 1) return@repeat
        val reduced = reduceWhoDidItClues(setup, all, emptyList(), difficulty, random)
        val entry = setup.shuffledEntryOrNull(difficulty, reduced, emptyList(), random) ?: return@repeat
        return entry
    }
    return null
}
