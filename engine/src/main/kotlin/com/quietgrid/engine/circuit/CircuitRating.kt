package com.quietgrid.engine.circuit

import com.quietgrid.engine.journey.packRawScore
import com.quietgrid.engine.journey.tieredRawScore

private const val CIRCUIT_UNSOLVED_SCORE = 99

fun circuitRawScore(entry: CircuitPuzzleEntry): Long {
    val result = solveCircuit(entry.toBoard(), circuitShapes(entry))
    val fine = if (!result.solved) {
        packRawScore(CIRCUIT_UNSOLVED_SCORE to 2)
    } else {
        val profile = analyzeCircuitSolveResult(result)
        packRawScore(
            profile.longWhatIfSteps to 2,
            profile.contradictionSteps to 2,
            profile.maxChainLength to 2,
            profile.loopSteps + profile.deadEndSteps to 3,
            result.steps.size to 3,
        )
    }
    return tieredRawScore(entry.difficulty, fine)
}
