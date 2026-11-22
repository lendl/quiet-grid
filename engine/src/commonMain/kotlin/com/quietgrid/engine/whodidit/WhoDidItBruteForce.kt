package com.quietgrid.engine.whodidit

private fun permutations(n: Int): List<List<Int>> {
    if (n == 0) return listOf(emptyList())
    return permutations(n - 1).flatMap { rest ->
        (0..rest.size).map { at -> rest.toMutableList().apply { add(at, n - 1) } }
    }
}

private fun isCulpritDependent(clue: WhoDidItClue) = clue is WhoDidItClue.Crime || clue is WhoDidItClue.CrimeTrait

private fun completionLevel(clue: WhoDidItClue): Int = when (clue) {
    is WhoDidItClue.Same -> maxOf(clue.a.category, clue.b.category)
    is WhoDidItClue.NotSame -> maxOf(clue.a.category, clue.b.category)
    is WhoDidItClue.Either -> maxOf(clue.a.category, clue.b.category, clue.c.category)
    is WhoDidItClue.HasTrait -> maxOf(clue.a.category, clue.trait.category.ordinal)
    is WhoDidItClue.LacksTrait -> maxOf(clue.a.category, clue.trait.category.ordinal)
    is WhoDidItClue.Crime, is WhoDidItClue.CrimeTrait -> Int.MAX_VALUE
}

private fun enumerateSolutions(
    spec: WhoDidItPuzzleSpec,
    cap: Int,
    perms: List<List<Int>> = permutations(spec.size),
): List<WhoDidItSolution> {
    require(spec.statements.none { isCulpritDependent(it.clue) }) {
        "statement clues must not depend on the culprit (Crime/CrimeTrait): " +
            spec.statements.filter { isCulpritDependent(it.clue) }
    }
    if (cap <= 0) return emptyList()
    val identity = List(spec.size) { it }
    val current = MutableList(spec.categoryCount) { identity }
    val pruning = spec.clues.filterNot(::isCulpritDependent)
    val levelClues = List(spec.categoryCount) { level -> pruning.filter { completionLevel(it) == level } }
    val levelStatements = List(spec.categoryCount) { level ->
        spec.statements.filter { completionLevel(it.clue) == level }
    }
    val culpritClues = spec.clues.filter(::isCulpritDependent)
    val hasStatements = spec.statements.isNotEmpty()
    val probe = WhoDidItSolution(current, 0)
    val found = mutableListOf<WhoDidItSolution>()
    var falseSpeakers = emptyList<Int>()

    fun passes(level: Int): Boolean {
        val clues = levelClues[level]
        val statements = levelStatements[level]
        if (clues.isEmpty() && statements.isEmpty()) return true
        if (!clues.all { it.isTrue(spec, probe) }) return false
        if (statements.isNotEmpty()) {
            val lying = statements.filterNot { it.clue.isTrue(spec, probe) }.map { it.s }
            falseSpeakers = (falseSpeakers + lying).distinct()
            if (falseSpeakers.size > 1) return false
        }
        return true
    }

    fun finish() {
        val assignment = current.toList()
        val candidates = if (hasStatements) falseSpeakers else (0 until spec.size).toList()
        for (culprit in candidates) {
            val candidate = WhoDidItSolution(assignment, culprit)
            if (culpritClues.all { it.isTrue(spec, candidate) } &&
                spec.statements.all { it.clue.isTrue(spec, candidate) == (it.s != culprit) }
            ) {
                found.add(candidate)
                if (found.size >= cap) return
            }
        }
    }

    fun assign(category: Int) {
        if (category == spec.categoryCount) {
            finish()
            return
        }
        for (perm in perms) {
            val savedFalse = falseSpeakers
            current[category] = perm
            if (passes(category)) assign(category + 1)
            falseSpeakers = savedFalse
            if (found.size >= cap) return
        }
        current[category] = identity
    }

    val savedFalse = falseSpeakers
    if (passes(0)) assign(1)
    falseSpeakers = savedFalse
    return found
}

fun countWhoDidItSolutions(spec: WhoDidItPuzzleSpec, cap: Int = 2): Int = enumerateSolutions(spec, cap).size

fun findWhoDidItSolution(spec: WhoDidItPuzzleSpec): WhoDidItSolution? = enumerateSolutions(spec, 1).firstOrNull()

fun countWhoDidItExtraClues(spec: WhoDidItPuzzleSpec): Int {
    val perms = permutations(spec.size)
    return spec.clues.indices.count { index ->
        val reduced = spec.copy(clues = spec.clues.filterIndexed { i, _ -> i != index })
        enumerateSolutions(reduced, 2, perms).size == 1
    }
}
