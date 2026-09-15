package com.quietgrid.engine.starbattle

enum class StarBattleTechnique { FORCED_PLACEMENT, STRUCTURAL_CONFINEMENT, CONFINEMENT, CHAIN }

sealed class StarBattleStep {
    abstract val technique: StarBattleTechnique

    data class Placement(val cells: List<Pair<Int, Int>>, override val technique: StarBattleTechnique) : StarBattleStep()

    data class Elimination(
        val cells: List<Pair<Int, Int>>,
        override val technique: StarBattleTechnique,
        val chainDepth: Int = 0,
    ) : StarBattleStep()
}

fun findForcedPlacement(state: StarBattleSolverState): StarBattleStep.Placement? {
    for (region in 0 until state.size) {
        val remaining = state.regionRemaining(region)
        if (remaining <= 0) continue
        val cells = state.candidatesInRegion(region)
        if (cells.size == remaining) return StarBattleStep.Placement(cells, StarBattleTechnique.FORCED_PLACEMENT)
    }
    for (row in 0 until state.size) {
        val remaining = state.rowRemaining(row)
        if (remaining <= 0) continue
        val cells = state.candidatesInRow(row)
        if (cells.size == remaining) return StarBattleStep.Placement(cells, StarBattleTechnique.FORCED_PLACEMENT)
    }
    for (col in 0 until state.size) {
        val remaining = state.colRemaining(col)
        if (remaining <= 0) continue
        val cells = state.candidatesInCol(col)
        if (cells.size == remaining) return StarBattleStep.Placement(cells, StarBattleTechnique.FORCED_PLACEMENT)
    }
    return null
}

private fun regionCellsRaw(regionOf: List<List<Int>>, size: Int, region: Int): List<Pair<Int, Int>> =
    (0 until size).flatMap { r -> (0 until size).mapNotNull { c -> if (regionOf[r][c] == region) r to c else null } }

private fun rowIsMonochrome(regionOf: List<List<Int>>, size: Int, row: Int): Boolean =
    (1 until size).all { c -> regionOf[row][c] == regionOf[row][0] }

private fun colIsMonochrome(regionOf: List<List<Int>>, size: Int, col: Int): Boolean =
    (1 until size).all { r -> regionOf[r][col] == regionOf[0][col] }

fun findConfinement(state: StarBattleSolverState): StarBattleStep.Elimination? {
    val size = state.size

    for (region in 0 until size) {
        val remaining = state.regionRemaining(region)
        if (remaining <= 0) continue
        val cells = state.candidatesInRegion(region)
        if (cells.isEmpty()) continue

        val rows = cells.map { it.first }.toSet()
        if (rows.size == 1 && state.rowRemaining(rows.first()) == remaining) {
            val toEliminate = state.candidatesInRow(rows.first()).filter { state.regionOf[it.first][it.second] != region }
            if (toEliminate.isNotEmpty()) {
                val structural = regionCellsRaw(state.regionOf, size, region).all { it.first == rows.first() }
                val technique = if (structural) StarBattleTechnique.STRUCTURAL_CONFINEMENT else StarBattleTechnique.CONFINEMENT
                return StarBattleStep.Elimination(toEliminate, technique)
            }
        }

        val cols = cells.map { it.second }.toSet()
        if (cols.size == 1 && state.colRemaining(cols.first()) == remaining) {
            val toEliminate = state.candidatesInCol(cols.first()).filter { state.regionOf[it.first][it.second] != region }
            if (toEliminate.isNotEmpty()) {
                val structural = regionCellsRaw(state.regionOf, size, region).all { it.second == cols.first() }
                val technique = if (structural) StarBattleTechnique.STRUCTURAL_CONFINEMENT else StarBattleTechnique.CONFINEMENT
                return StarBattleStep.Elimination(toEliminate, technique)
            }
        }
    }

    for (row in 0 until size) {
        val remaining = state.rowRemaining(row)
        if (remaining <= 0) continue
        val cells = state.candidatesInRow(row)
        if (cells.isEmpty()) continue
        val regions = cells.map { state.regionOf[it.first][it.second] }.toSet()
        if (regions.size == 1 && state.regionRemaining(regions.first()) == remaining) {
            val toEliminate = state.candidatesInRegion(regions.first()).filter { it.first != row }
            if (toEliminate.isNotEmpty()) {
                val structural = rowIsMonochrome(state.regionOf, size, row)
                val technique = if (structural) StarBattleTechnique.STRUCTURAL_CONFINEMENT else StarBattleTechnique.CONFINEMENT
                return StarBattleStep.Elimination(toEliminate, technique)
            }
        }
    }

    for (col in 0 until size) {
        val remaining = state.colRemaining(col)
        if (remaining <= 0) continue
        val cells = state.candidatesInCol(col)
        if (cells.isEmpty()) continue
        val regions = cells.map { state.regionOf[it.first][it.second] }.toSet()
        if (regions.size == 1 && state.regionRemaining(regions.first()) == remaining) {
            val toEliminate = state.candidatesInRegion(regions.first()).filter { it.second != col }
            if (toEliminate.isNotEmpty()) {
                val structural = colIsMonochrome(state.regionOf, size, col)
                val technique = if (structural) StarBattleTechnique.STRUCTURAL_CONFINEMENT else StarBattleTechnique.CONFINEMENT
                return StarBattleStep.Elimination(toEliminate, technique)
            }
        }
    }

    return null
}

private const val MAX_CHAIN_PROPAGATION_DEPTH = 10

private fun propagateForcedPlacements(state: StarBattleSolverState, maxDepth: Int): Int {
    var depth = 0
    while (depth < maxDepth) {
        val move = findForcedPlacement(state) ?: break
        move.cells.forEach { (r, c) -> state.place(r, c) }
        depth++
    }
    return depth
}

private fun hasContradiction(state: StarBattleSolverState): Boolean {
    fun unitIsContradictory(remaining: Int, cells: List<Pair<Int, Int>>): Boolean {
        if (remaining <= 0) return false
        if (cells.size < remaining) return true
        if (remaining == 2 && !hasNonTouchingPair(cells)) return true
        return false
    }
    for (region in 0 until state.size) {
        if (unitIsContradictory(state.regionRemaining(region), state.candidatesInRegion(region))) return true
    }
    for (row in 0 until state.size) {
        if (unitIsContradictory(state.rowRemaining(row), state.candidatesInRow(row))) return true
    }
    for (col in 0 until state.size) {
        if (unitIsContradictory(state.colRemaining(col), state.candidatesInCol(col))) return true
    }
    return false
}

fun findChainContradiction(state: StarBattleSolverState): StarBattleStep.Elimination? {
    for (region in 0 until state.size) {
        if (state.regionRemaining(region) <= 0) continue
        for ((row, col) in state.candidatesInRegion(region)) {
            val hypothesis = state.copy()
            hypothesis.place(row, col)
            val intermediatePlacements = propagateForcedPlacements(hypothesis, MAX_CHAIN_PROPAGATION_DEPTH)
            if (hasContradiction(hypothesis)) {
                return StarBattleStep.Elimination(listOf(row to col), StarBattleTechnique.CHAIN, chainDepth = 1 + intermediatePlacements)
            }
        }
    }
    return null
}
