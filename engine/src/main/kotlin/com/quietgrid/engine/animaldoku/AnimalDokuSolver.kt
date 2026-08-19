package com.quietgrid.engine.animaldoku

data class AnimalDokuSolveStep(val technique: AnimalDokuTechnique, val chainDepth: Int)

data class AnimalDokuSolveResult(val solved: Boolean, val steps: List<AnimalDokuSolveStep>)

/**
 * Repeatedly applies the easiest technique that currently produces a move (singleton, then
 * confinement, then pairing K=2..[maxPairingK], then chain/contradiction) until the puzzle is
 * fully solved or nothing applies. `solved = false` means either the layout isn't fully
 * human-solvable by these techniques, or it has more than one valid solution (see
 * [AnimalDokuSolveResult] doc on the public function this backs).
 */
fun solveAnimalDoku(size: Int, regionOf: List<List<Int>>, maxPairingK: Int = 4): AnimalDokuSolveResult {
    val state = AnimalDokuSolverState(size, regionOf)
    val steps = mutableListOf<AnimalDokuSolveStep>()

    while (!state.isSolved()) {
        val singleton = findSingleton(state)
        if (singleton != null) {
            state.place(singleton.row, singleton.col)
            steps.add(AnimalDokuSolveStep(AnimalDokuTechnique.SINGLETON, 0))
            continue
        }

        val confinement = findConfinement(state)
        if (confinement != null) {
            confinement.cells.forEach { (r, c) -> state.eliminated[r][c] = true }
            steps.add(AnimalDokuSolveStep(AnimalDokuTechnique.CONFINEMENT, 0))
            continue
        }

        val pairing = (2..maxPairingK).asSequence().mapNotNull { k -> findPairing(state, k) }.firstOrNull()
        if (pairing != null) {
            pairing.cells.forEach { (r, c) -> state.eliminated[r][c] = true }
            steps.add(AnimalDokuSolveStep(pairing.technique, 0))
            continue
        }

        val chain = findChainContradiction(state)
        if (chain != null) {
            chain.cells.forEach { (r, c) -> state.eliminated[r][c] = true }
            steps.add(AnimalDokuSolveStep(AnimalDokuTechnique.CHAIN, chain.chainDepth))
            continue
        }

        return AnimalDokuSolveResult(solved = false, steps = steps)
    }

    return AnimalDokuSolveResult(solved = true, steps = steps)
}
