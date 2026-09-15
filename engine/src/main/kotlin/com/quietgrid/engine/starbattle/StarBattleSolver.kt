package com.quietgrid.engine.starbattle

data class StarBattleSolveStep(val technique: StarBattleTechnique, val chainDepth: Int)

data class StarBattleSolveResult(val solved: Boolean, val steps: List<StarBattleSolveStep>)

fun solveStarBattle(size: Int, k: Int, regionOf: List<List<Int>>): StarBattleSolveResult {
    val state = StarBattleSolverState(size, k, regionOf)
    val steps = mutableListOf<StarBattleSolveStep>()

    while (!state.isSolved()) {
        val forced = findForcedPlacement(state)
        if (forced != null) {
            forced.cells.forEach { (r, c) -> state.place(r, c) }
            steps.add(StarBattleSolveStep(StarBattleTechnique.FORCED_PLACEMENT, 0))
            continue
        }

        val confinement = findConfinement(state)
        if (confinement != null) {
            confinement.cells.forEach { (r, c) -> state.eliminated[r][c] = true }
            steps.add(StarBattleSolveStep(confinement.technique, 0))
            continue
        }

        val chain = findChainContradiction(state)
        if (chain != null) {
            chain.cells.forEach { (r, c) -> state.eliminated[r][c] = true }
            steps.add(StarBattleSolveStep(StarBattleTechnique.CHAIN, chain.chainDepth))
            continue
        }

        return StarBattleSolveResult(solved = false, steps = steps)
    }

    return StarBattleSolveResult(solved = true, steps = steps)
}
