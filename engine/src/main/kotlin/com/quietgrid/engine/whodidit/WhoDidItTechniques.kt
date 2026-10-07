package com.quietgrid.engine.whodidit

private fun unknownChange(state: WhoDidItGridState, a: WhoDidItRef, b: WhoDidItRef, mark: WhoDidItMark): WhoDidItCellChange? =
    if (a.category != b.category && state.mark(a, b) == WhoDidItMark.UNKNOWN) WhoDidItCellChange(a, b, mark) else null

fun findClueDirect(spec: WhoDidItPuzzleSpec, state: WhoDidItGridState): WhoDidItStep? {
    for ((clue, source) in effectiveClues(spec, state)) {
        var changes = emptyList<WhoDidItCellChange>()
        var removals = emptySet<Int>()
        when (clue) {
            is WhoDidItClue.Same -> changes = listOfNotNull(unknownChange(state, clue.a, clue.b, WhoDidItMark.YES))
            is WhoDidItClue.NotSame -> changes = listOfNotNull(unknownChange(state, clue.a, clue.b, WhoDidItMark.NO))
            is WhoDidItClue.Crime -> {
                if (clue.e.category == 0) {
                    removals = state.culpritCandidates.filter { it != clue.e.slot }.toSet()
                } else {
                    removals = state.culpritCandidates
                        .filter { state.mark(WhoDidItRef(0, it), clue.e) == WhoDidItMark.NO }
                        .toSet()
                    val remaining = state.culpritCandidates - removals
                    if (remaining.size == 1) {
                        changes = listOfNotNull(unknownChange(state, WhoDidItRef(0, remaining.first()), clue.e, WhoDidItMark.YES))
                    }
                }
            }
            else -> Unit
        }
        if (changes.isNotEmpty() || removals.isNotEmpty()) {
            return WhoDidItStep(WhoDidItTechnique.CLUE_DIRECT, listOf(source), changes, removals)
        }
    }
    return null
}

fun findTraitDirect(spec: WhoDidItPuzzleSpec, state: WhoDidItGridState): WhoDidItStep? {
    for ((clue, source) in effectiveClues(spec, state)) {
        val (a, trait, wantTrait) = when (clue) {
            is WhoDidItClue.HasTrait -> Triple(clue.a, clue.trait, true)
            is WhoDidItClue.LacksTrait -> Triple(clue.a, clue.trait, false)
            else -> continue
        }
        val category = trait.category.ordinal
        if (a.category == category) continue
        val changes = (0 until spec.size).mapNotNull { slot ->
            val holds = trait in spec.traits[category][slot]
            if (holds != wantTrait) unknownChange(state, a, WhoDidItRef(category, slot), WhoDidItMark.NO) else null
        }
        if (changes.isNotEmpty()) return WhoDidItStep(WhoDidItTechnique.TRAIT_DIRECT, listOf(source), changes)
    }
    return null
}

fun findSingle(spec: WhoDidItPuzzleSpec, state: WhoDidItGridState): WhoDidItStep? {
    val size = spec.size
    for ((rowCategory, colCategory) in whoDidItCategoryPairs(spec.categoryCount)) {
        for (row in 0 until size) {
            val open = (0 until size).filter {
                state.mark(WhoDidItRef(rowCategory, row), WhoDidItRef(colCategory, it)) != WhoDidItMark.NO
            }
            if (open.size == 1) {
                val change = unknownChange(state, WhoDidItRef(rowCategory, row), WhoDidItRef(colCategory, open[0]), WhoDidItMark.YES)
                if (change != null) return WhoDidItStep(WhoDidItTechnique.SINGLE, changes = listOf(change))
            }
        }
        for (col in 0 until size) {
            val open = (0 until size).filter {
                state.mark(WhoDidItRef(rowCategory, it), WhoDidItRef(colCategory, col)) != WhoDidItMark.NO
            }
            if (open.size == 1) {
                val change = unknownChange(state, WhoDidItRef(rowCategory, open[0]), WhoDidItRef(colCategory, col), WhoDidItMark.YES)
                if (change != null) return WhoDidItStep(WhoDidItTechnique.SINGLE, changes = listOf(change))
            }
        }
    }
    return null
}

fun findTransitive(spec: WhoDidItPuzzleSpec, state: WhoDidItGridState): WhoDidItStep? {
    val size = spec.size
    val categories = spec.categoryCount
    for (ca in 0 until categories) for (cb in 0 until categories) for (cc in 0 until categories) {
        if (ca == cb || cb == cc || ca == cc) continue
        for (i in 0 until size) for (j in 0 until size) {
            val a = WhoDidItRef(ca, i)
            val b = WhoDidItRef(cb, j)
            if (state.mark(a, b) != WhoDidItMark.YES) continue
            for (k in 0 until size) {
                val c = WhoDidItRef(cc, k)
                val implied = when (state.mark(b, c)) {
                    WhoDidItMark.YES -> WhoDidItMark.YES
                    WhoDidItMark.NO -> WhoDidItMark.NO
                    WhoDidItMark.UNKNOWN -> continue
                }
                val change = unknownChange(state, a, c, implied)
                if (change != null) return WhoDidItStep(WhoDidItTechnique.TRANSITIVE, changes = listOf(change))
            }
        }
    }
    return null
}

fun findCluePropagation(spec: WhoDidItPuzzleSpec, state: WhoDidItGridState): WhoDidItStep? {
    val size = spec.size
    for ((clue, source) in effectiveClues(spec, state)) {
        var changes = emptyList<WhoDidItCellChange>()
        var removals = emptySet<Int>()
        when (clue) {
            is WhoDidItClue.Either -> {
                if (clue.a.category == clue.b.category || clue.b.category != clue.c.category) continue
                val yes = when {
                    state.mark(clue.a, clue.b) == WhoDidItMark.NO -> unknownChange(state, clue.a, clue.c, WhoDidItMark.YES)
                    state.mark(clue.a, clue.c) == WhoDidItMark.NO -> unknownChange(state, clue.a, clue.b, WhoDidItMark.YES)
                    else -> null
                }
                val others = (0 until size)
                    .filter { it != clue.b.slot && it != clue.c.slot }
                    .mapNotNull { unknownChange(state, clue.a, WhoDidItRef(clue.b.category, it), WhoDidItMark.NO) }
                changes = listOfNotNull(yes) + others
            }
            is WhoDidItClue.HasTrait, is WhoDidItClue.LacksTrait -> {
                val a = if (clue is WhoDidItClue.HasTrait) clue.a else (clue as WhoDidItClue.LacksTrait).a
                val trait = if (clue is WhoDidItClue.HasTrait) clue.trait else (clue as WhoDidItClue.LacksTrait).trait
                val wantTrait = clue is WhoDidItClue.HasTrait
                val traitCategory = trait.category.ordinal
                if (a.category == traitCategory) continue
                changes = (0 until spec.categoryCount)
                    .filter { it != a.category && it != traitCategory }
                    .flatMap { other ->
                        (0 until size).mapNotNull { slot ->
                            val y = WhoDidItRef(other, slot)
                            val partners = (0 until size).filter {
                                state.mark(y, WhoDidItRef(traitCategory, it)) != WhoDidItMark.NO
                            }
                            val ruledOut = partners.isNotEmpty() &&
                                partners.all { (trait in spec.traits[traitCategory][it]) != wantTrait }
                            if (ruledOut) unknownChange(state, a, y, WhoDidItMark.NO) else null
                        }
                    }
            }
            is WhoDidItClue.Crime -> {
                if (clue.e.category == 0) continue
                changes = (0 until size)
                    .filter { it !in state.culpritCandidates }
                    .mapNotNull { unknownChange(state, WhoDidItRef(0, it), clue.e, WhoDidItMark.NO) }
            }
            is WhoDidItClue.CrimeTrait -> {
                val traitCategory = clue.trait.category.ordinal
                removals = state.culpritCandidates.filter { suspect ->
                    val partners = if (traitCategory == 0) {
                        listOf(suspect)
                    } else {
                        (0 until size).filter {
                            state.mark(WhoDidItRef(0, suspect), WhoDidItRef(traitCategory, it)) != WhoDidItMark.NO
                        }
                    }
                    partners.isNotEmpty() && partners.none { clue.trait in spec.traits[traitCategory][it] }
                }.toSet()
                if (traitCategory != 0 && state.culpritCandidates.size == 1) {
                    val culprit = WhoDidItRef(0, state.culpritCandidates.first())
                    changes = (0 until size)
                        .filter { clue.trait !in spec.traits[traitCategory][it] }
                        .mapNotNull { unknownChange(state, culprit, WhoDidItRef(traitCategory, it), WhoDidItMark.NO) }
                }
            }
            else -> Unit
        }
        if (changes.isNotEmpty() || removals.isNotEmpty()) {
            return WhoDidItStep(WhoDidItTechnique.CLUE_PROPAGATION, listOf(source), changes, removals)
        }
    }
    return null
}

fun findIncompatible(spec: WhoDidItPuzzleSpec, state: WhoDidItGridState): WhoDidItStep? {
    val size = spec.size
    for ((rowCategory, colCategory) in whoDidItCategoryPairs(spec.categoryCount)) {
        for (row in 0 until size) for (col in 0 until size) {
            val x = WhoDidItRef(rowCategory, row)
            val y = WhoDidItRef(colCategory, col)
            if (state.mark(x, y) != WhoDidItMark.UNKNOWN) continue
            for (third in 0 until spec.categoryCount) {
                if (third == rowCategory || third == colCategory) continue
                val xOptions = (0 until size).filter { state.mark(x, WhoDidItRef(third, it)) != WhoDidItMark.NO }.toSet()
                val yOptions = (0 until size).filter { state.mark(y, WhoDidItRef(third, it)) != WhoDidItMark.NO }.toSet()
                if (xOptions.isNotEmpty() && yOptions.isNotEmpty() && xOptions.intersect(yOptions).isEmpty()) {
                    return WhoDidItStep(
                        WhoDidItTechnique.INCOMPATIBLE,
                        changes = listOf(WhoDidItCellChange(x, y, WhoDidItMark.NO)),
                    )
                }
            }
        }
    }
    return null
}

const val WHODIDIT_HYPOTHESIS_DEPTH_CAP = 6

internal fun findPropagationStep(spec: WhoDidItPuzzleSpec, state: WhoDidItGridState): WhoDidItStep? =
    findClueDirect(spec, state)
        ?: findTraitDirect(spec, state)
        ?: findSingle(spec, state)
        ?: findTransitive(spec, state)
        ?: findCluePropagation(spec, state)
        ?: findIncompatible(spec, state)
        ?: findStatementCheck(spec, state)

private fun relation(state: WhoDidItGridState, a: WhoDidItRef, b: WhoDidItRef): WhoDidItMark = when {
    a.category != b.category -> state.mark(a, b)
    a.slot == b.slot -> WhoDidItMark.YES
    else -> WhoDidItMark.NO
}

private fun possibleSlots(state: WhoDidItGridState, a: WhoDidItRef, category: Int): List<Int> =
    if (a.category == category) {
        listOf(a.slot)
    } else {
        (0 until state.size).filter { state.mark(a, WhoDidItRef(category, it)) != WhoDidItMark.NO }
    }

private fun traitRefuted(
    spec: WhoDidItPuzzleSpec,
    state: WhoDidItGridState,
    a: WhoDidItRef,
    trait: WhoDidItTrait,
    wantTrait: Boolean,
): Boolean {
    val category = trait.category.ordinal
    return possibleSlots(state, a, category).none { (trait in spec.traits[category][it]) == wantTrait }
}

private fun WhoDidItClue.isRefuted(spec: WhoDidItPuzzleSpec, state: WhoDidItGridState): Boolean = when (this) {
    is WhoDidItClue.Same -> relation(state, a, b) == WhoDidItMark.NO
    is WhoDidItClue.NotSame -> relation(state, a, b) == WhoDidItMark.YES
    is WhoDidItClue.Either -> {
        val withB = relation(state, a, b)
        val withC = relation(state, a, c)
        withB != WhoDidItMark.UNKNOWN && withC != WhoDidItMark.UNKNOWN && (withB == WhoDidItMark.YES) == (withC == WhoDidItMark.YES)
    }
    is WhoDidItClue.HasTrait -> traitRefuted(spec, state, a, trait, true)
    is WhoDidItClue.LacksTrait -> traitRefuted(spec, state, a, trait, false)
    is WhoDidItClue.Crime -> state.culpritCandidates.none { relation(state, WhoDidItRef(0, it), e) != WhoDidItMark.NO }
    is WhoDidItClue.CrimeTrait -> state.culpritCandidates.none { suspect ->
        val category = trait.category.ordinal
        possibleSlots(state, WhoDidItRef(0, suspect), category).any { trait in spec.traits[category][it] }
    }
}

fun findStatementCheck(spec: WhoDidItPuzzleSpec, state: WhoDidItGridState): WhoDidItStep? {
    if (state.culpritCandidates.size < 2) return null
    for (statement in spec.statements.sortedBy { it.s }) {
        if (statement.s !in state.culpritCandidates) continue
        val removals = when {
            statement.clue.isRefuted(spec, state) -> state.culpritCandidates - statement.s
            statement.clue.negated().isRefuted(spec, state) -> setOf(statement.s)
            else -> continue
        }
        return WhoDidItStep(
            WhoDidItTechnique.STATEMENT_CHECK,
            sources = listOf(WhoDidItSource.Statement(statement.s)),
            culpritRemovals = removals,
        )
    }
    return null
}

internal fun isWhoDidItContradicted(spec: WhoDidItPuzzleSpec, state: WhoDidItGridState): Boolean =
    state.hasContradiction() || effectiveClues(spec, state).any { (clue, _) -> clue.isRefuted(spec, state) }

private fun stepsToContradiction(spec: WhoDidItPuzzleSpec, trial: WhoDidItGridState, limit: Int): Int? {
    if (isWhoDidItContradicted(spec, trial)) return 0
    for (applied in 1..limit) {
        val step = findPropagationStep(spec, trial) ?: return null
        trial.apply(step)
        if (isWhoDidItContradicted(spec, trial)) return applied
    }
    return null
}

private fun WhoDidItStep?.depthLimit(): Int = if (this == null) WHODIDIT_HYPOTHESIS_DEPTH_CAP else depth - 1

fun findLiarHypothesis(spec: WhoDidItPuzzleSpec, state: WhoDidItGridState): WhoDidItStep? {
    if (spec.statements.isEmpty() || state.culpritCandidates.size < 2) return null
    var best: WhoDidItStep? = null
    for (suspect in state.culpritCandidates.sorted()) {
        val trial = state.copy()
        trial.culpritCandidates.retainAll(setOf(suspect))
        val depth = stepsToContradiction(spec, trial, best.depthLimit()) ?: continue
        if (depth == 0) continue
        best = WhoDidItStep(
            WhoDidItTechnique.LIAR_HYPOTHESIS,
            sources = listOf(WhoDidItSource.Statement(suspect)),
            culpritRemovals = setOf(suspect),
            depth = depth,
        )
        if (depth == 1) break
    }
    return best
}

fun findContradiction(spec: WhoDidItPuzzleSpec, state: WhoDidItGridState): WhoDidItStep? {
    var best: WhoDidItStep? = null
    for ((rowCategory, colCategory) in whoDidItCategoryPairs(spec.categoryCount)) {
        for (row in 0 until spec.size) for (col in 0 until spec.size) {
            val x = WhoDidItRef(rowCategory, row)
            val y = WhoDidItRef(colCategory, col)
            if (state.mark(x, y) != WhoDidItMark.UNKNOWN) continue
            val trial = state.copy()
            trial.set(x, y, WhoDidItMark.YES)
            trial.autoFill()
            val depth = stepsToContradiction(spec, trial, best.depthLimit()) ?: continue
            best = WhoDidItStep(
                WhoDidItTechnique.CONTRADICTION,
                changes = listOf(WhoDidItCellChange(x, y, WhoDidItMark.NO)),
                depth = depth,
            )
            if (depth == 0) return best
        }
    }
    return best
}
