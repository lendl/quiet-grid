package com.quietgrid.engine.battleship

enum class BattleshipTechnique { STRUCTURAL_WATER, LINE_SATURATION, LINE_EXHAUSTION, FLEET_ELIMINATION, FLEET_LENGTH_MATCH, PROBING }

sealed class BattleshipStep {
    abstract val technique: BattleshipTechnique

    data class Water(val cells: List<Pair<Int, Int>>, override val technique: BattleshipTechnique) : BattleshipStep()
    data class Ship(val cells: List<Pair<Int, Int>>, override val technique: BattleshipTechnique) : BattleshipStep()
}

fun applyStep(state: BattleshipSolverState, step: BattleshipStep) {
    when (step) {
        is BattleshipStep.Water -> step.cells.forEach { (r, c) -> state.grid[r][c] = BattleshipCell.WATER }
        is BattleshipStep.Ship -> step.cells.forEach { (r, c) -> state.grid[r][c] = BattleshipCell.SHIP }
    }
}

private val DIAGONAL_DELTAS = listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)
private val ORTHOGONAL_DELTAS = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)

fun findStructuralWater(state: BattleshipSolverState): BattleshipStep.Water? {
    val size = state.size
    val visited = Array(size) { BooleanArray(size) }
    val cells = mutableListOf<Pair<Int, Int>>()
    val seen = mutableSetOf<Pair<Int, Int>>()
    val maxRemainingLength = state.remainingFleetCounts().filterValues { it > 0 }.keys.maxOrNull() ?: state.fleet.maxOrNull() ?: 0

    fun addIfUnknown(r: Int, c: Int) {
        if (r !in 0 until size || c !in 0 until size) return
        if (state.grid[r][c] == BattleshipCell.UNKNOWN && (r to c) !in seen) {
            seen.add(r to c)
            cells.add(r to c)
        }
    }

    for (row in 0 until size) {
        for (col in 0 until size) {
            if (state.grid[row][col] != BattleshipCell.SHIP || visited[row][col]) continue
            val run = mutableListOf(row to col)
            visited[row][col] = true
            var idx = 0
            while (idx < run.size) {
                val (r, c) = run[idx]
                idx++
                for ((dr, dc) in ORTHOGONAL_DELTAS) {
                    val nr = r + dr
                    val nc = c + dc
                    if (nr in 0 until size && nc in 0 until size && !visited[nr][nc] && state.grid[nr][nc] == BattleshipCell.SHIP) {
                        visited[nr][nc] = true
                        run.add(nr to nc)
                    }
                }
            }
            val rows = run.map { it.first }.toSet()
            val cols = run.map { it.second }.toSet()
            for ((r, c) in run) {
                for ((dr, dc) in DIAGONAL_DELTAS) addIfUnknown(r + dr, c + dc)
            }
            when {
                run.size == 1 -> {
                    if (run.size >= maxRemainingLength) {
                        val (r, c) = run[0]
                        addIfUnknown(r - 1, c)
                        addIfUnknown(r + 1, c)
                        addIfUnknown(r, c - 1)
                        addIfUnknown(r, c + 1)
                    }
                }
                rows.size == 1 -> {
                    val r0 = rows.first()
                    for (c in cols) {
                        addIfUnknown(r0 - 1, c)
                        addIfUnknown(r0 + 1, c)
                    }
                    if (run.size >= maxRemainingLength) {
                        addIfUnknown(r0, cols.min() - 1)
                        addIfUnknown(r0, cols.max() + 1)
                    }
                }
                else -> {
                    val c0 = cols.first()
                    for (r in rows) {
                        addIfUnknown(r, c0 - 1)
                        addIfUnknown(r, c0 + 1)
                    }
                    if (run.size >= maxRemainingLength) {
                        addIfUnknown(rows.min() - 1, c0)
                        addIfUnknown(rows.max() + 1, c0)
                    }
                }
            }
        }
    }
    return if (cells.isEmpty()) null else BattleshipStep.Water(cells, BattleshipTechnique.STRUCTURAL_WATER)
}

fun findLineSaturation(state: BattleshipSolverState): BattleshipStep.Water? {
    for (row in 0 until state.size) {
        if (state.shipCountInRow(row) == state.rowClues[row] && state.unknownCountInRow(row) > 0) {
            return BattleshipStep.Water(state.unknownColsInRow(row).map { row to it }, BattleshipTechnique.LINE_SATURATION)
        }
    }
    for (col in 0 until state.size) {
        if (state.shipCountInCol(col) == state.colClues[col] && state.unknownCountInCol(col) > 0) {
            return BattleshipStep.Water(state.unknownRowsInCol(col).map { it to col }, BattleshipTechnique.LINE_SATURATION)
        }
    }
    return null
}

fun findLineExhaustion(state: BattleshipSolverState): BattleshipStep.Ship? {
    for (row in 0 until state.size) {
        val unknowns = state.unknownColsInRow(row)
        if (unknowns.isNotEmpty() && state.shipCountInRow(row) + unknowns.size == state.rowClues[row]) {
            return BattleshipStep.Ship(unknowns.map { row to it }, BattleshipTechnique.LINE_EXHAUSTION)
        }
    }
    for (col in 0 until state.size) {
        val unknowns = state.unknownRowsInCol(col)
        if (unknowns.isNotEmpty() && state.shipCountInCol(col) + unknowns.size == state.colClues[col]) {
            return BattleshipStep.Ship(unknowns.map { it to col }, BattleshipTechnique.LINE_EXHAUSTION)
        }
    }
    return null
}

private fun unknownRunsInRow(state: BattleshipSolverState, row: Int): List<List<Int>> {
    val runs = mutableListOf<List<Int>>()
    var start = -1
    for (col in 0 until state.size) {
        if (state.grid[row][col] == BattleshipCell.UNKNOWN) {
            if (start == -1) start = col
        } else if (start != -1) {
            runs.add((start until col).toList())
            start = -1
        }
    }
    if (start != -1) runs.add((start until state.size).toList())
    return runs
}

private fun unknownRunsInCol(state: BattleshipSolverState, col: Int): List<List<Int>> {
    val runs = mutableListOf<List<Int>>()
    var start = -1
    for (row in 0 until state.size) {
        if (state.grid[row][col] == BattleshipCell.UNKNOWN) {
            if (start == -1) start = row
        } else if (start != -1) {
            runs.add((start until row).toList())
            start = -1
        }
    }
    if (start != -1) runs.add((start until state.size).toList())
    return runs
}

fun findFleetElimination(state: BattleshipSolverState): BattleshipStep.Water? {
    val remaining = state.remainingFleetCounts().filterValues { it > 0 }
    if (remaining.isEmpty()) return null
    val minRemainingLength = remaining.keys.min()
    for (row in 0 until state.size) {
        for (run in unknownRunsInRow(state, row)) {
            if (run.size < minRemainingLength) return BattleshipStep.Water(run.map { row to it }, BattleshipTechnique.FLEET_ELIMINATION)
        }
    }
    for (col in 0 until state.size) {
        for (run in unknownRunsInCol(state, col)) {
            if (run.size < minRemainingLength) return BattleshipStep.Water(run.map { it to col }, BattleshipTechnique.FLEET_ELIMINATION)
        }
    }
    return null
}

fun findFleetLengthMatch(state: BattleshipSolverState): BattleshipStep.Ship? {
    val remaining = state.remainingFleetCounts().filterValues { it > 0 }
    for ((length, count) in remaining) {
        if (count != 1) continue
        val candidateRuns = mutableListOf<List<Pair<Int, Int>>>()
        for (row in 0 until state.size) {
            for (run in unknownRunsInRow(state, row)) {
                if (run.size == length) candidateRuns.add(run.map { row to it })
            }
        }
        for (col in 0 until state.size) {
            for (run in unknownRunsInCol(state, col)) {
                if (run.size == length) candidateRuns.add(run.map { it to col })
            }
        }
        if (candidateRuns.size == 1) return BattleshipStep.Ship(candidateRuns[0], BattleshipTechnique.FLEET_LENGTH_MATCH)
    }
    return null
}

private fun propagateBasicTechniques(state: BattleshipSolverState, maxSteps: Int): Int {
    var count = 0
    while (count < maxSteps) {
        val step = findStructuralWater(state)
            ?: findLineSaturation(state)
            ?: findLineExhaustion(state)
            ?: findFleetElimination(state)
            ?: findFleetLengthMatch(state)
            ?: break
        applyStep(state, step)
        count++
    }
    return count
}

fun findProbing(state: BattleshipSolverState, maxPropagation: Int = 200): BattleshipStep? {
    for (row in 0 until state.size) {
        for (col in 0 until state.size) {
            if (state.grid[row][col] != BattleshipCell.UNKNOWN) continue

            val shipHypothesis = state.copy()
            shipHypothesis.grid[row][col] = BattleshipCell.SHIP
            propagateBasicTechniques(shipHypothesis, maxPropagation)
            if (shipHypothesis.hasContradiction()) {
                return BattleshipStep.Water(listOf(row to col), BattleshipTechnique.PROBING)
            }

            val waterHypothesis = state.copy()
            waterHypothesis.grid[row][col] = BattleshipCell.WATER
            propagateBasicTechniques(waterHypothesis, maxPropagation)
            if (waterHypothesis.hasContradiction()) {
                return BattleshipStep.Ship(listOf(row to col), BattleshipTechnique.PROBING)
            }
        }
    }
    return null
}
