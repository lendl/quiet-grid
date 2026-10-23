package com.quietgrid.engine.animaldoku

data class AnimalDokuSolveStep(val technique: AnimalDokuTechnique, val chainDepth: Int)

data class AnimalDokuSolveResult(val solved: Boolean, val steps: List<AnimalDokuSolveStep>, val remainingCandidates: Int = 0)

fun nextAnimalDokuStep(state: AnimalDokuSolverState, maxPairingK: Int = 4): AnimalDokuStep? =
    findSingleton(state)
        ?: findConfinement(state)
        ?: (2..maxPairingK).asSequence().mapNotNull { k -> findPairing(state, k) }.firstOrNull()
        ?: findChainContradiction(state)

fun AnimalDokuSolverState.apply(step: AnimalDokuStep) {
    when (step) {
        is AnimalDokuStep.Placement -> place(step.row, step.col)
        is AnimalDokuStep.Elimination -> step.cells.forEach { (r, c) -> eliminated[r][c] = true }
    }
}

fun solveAnimalDoku(size: Int, regionOf: List<List<Int>>, maxPairingK: Int = 4): AnimalDokuSolveResult {
    val state = AnimalDokuSolverState(size, regionOf)
    val steps = mutableListOf<AnimalDokuSolveStep>()

    while (!state.isSolved()) {
        val step = nextAnimalDokuStep(state, maxPairingK)
            ?: return AnimalDokuSolveResult(solved = false, steps = steps, remainingCandidates = state.candidateCount())
        state.apply(step)
        steps.add(AnimalDokuSolveStep(step.technique, (step as? AnimalDokuStep.Elimination)?.chainDepth ?: 0))
    }

    return AnimalDokuSolveResult(solved = true, steps = steps)
}
