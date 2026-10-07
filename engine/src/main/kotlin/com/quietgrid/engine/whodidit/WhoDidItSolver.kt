package com.quietgrid.engine.whodidit

data class WhoDidItSolveResult(val solved: Boolean, val steps: List<WhoDidItStep>)

private fun findWhoDidItStep(spec: WhoDidItPuzzleSpec, state: WhoDidItGridState): WhoDidItStep? =
    findPropagationStep(spec, state)
        ?: findLiarHypothesis(spec, state)
        ?: findContradiction(spec, state)

fun solveWhoDidIt(spec: WhoDidItPuzzleSpec): WhoDidItSolveResult {
    val state = WhoDidItGridState(spec.size, spec.categoryCount)
    val steps = mutableListOf<WhoDidItStep>()
    while (!state.isSolved()) {
        val step = findWhoDidItStep(spec, state) ?: break
        state.apply(step)
        steps.add(step)
    }
    return WhoDidItSolveResult(state.isSolved(), steps)
}

private fun workingCopy(spec: WhoDidItPuzzleSpec, state: WhoDidItGridState): WhoDidItGridState? {
    val work = state.copy()
    work.autoFill()
    return if (isWhoDidItContradicted(spec, work)) null else work
}

fun nextWhoDidItStep(spec: WhoDidItPuzzleSpec, state: WhoDidItGridState): WhoDidItStep? {
    val work = workingCopy(spec, state) ?: return null
    return findWhoDidItStep(spec, work)
}

data class WhoDidItHintPath(val prelude: List<WhoDidItStep>, val step: WhoDidItStep?)

fun nextWhoDidItHintPath(spec: WhoDidItPuzzleSpec, state: WhoDidItGridState): WhoDidItHintPath {
    val work = workingCopy(spec, state) ?: return WhoDidItHintPath(emptyList(), null)
    val prelude = mutableListOf<WhoDidItStep>()
    while (true) {
        val step = findWhoDidItStep(spec, work) ?: return WhoDidItHintPath(prelude, null)
        if (step.changes.isNotEmpty()) return WhoDidItHintPath(prelude, step)
        prelude.add(step)
        work.apply(step)
    }
}

fun nextWhoDidItCellStep(spec: WhoDidItPuzzleSpec, state: WhoDidItGridState): WhoDidItStep? =
    nextWhoDidItHintPath(spec, state).step
