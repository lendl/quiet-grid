package com.quietgrid.engine.starbattle

enum class StarBattleTechnique { FORCED_PLACEMENT, STRUCTURAL_CONFINEMENT, CONFINEMENT, PAIRING, CHAIN }

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

private fun List<Int>.combinationsOfSize(k: Int): List<List<Int>> {
    if (k > size) return emptyList()
    val result = mutableListOf<List<Int>>()
    fun recurse(start: Int, current: MutableList<Int>) {
        if (current.size == k) {
            result.add(current.toList())
            return
        }
        for (i in start until size) {
            current.add(this[i])
            recurse(i + 1, current)
            current.removeAt(current.size - 1)
        }
    }
    recurse(0, mutableListOf())
    return result
}

fun findPairing(state: StarBattleSolverState, groupSize: Int): StarBattleStep.Elimination? {
    val size = state.size
    val regionRows = IntArray(size)
    val regionCols = IntArray(size)
    val rowRegions = IntArray(size)
    val colRegions = IntArray(size)
    for (row in 0 until size) for (col in 0 until size) {
        if (!state.isCandidate(row, col)) continue
        val region = state.regionOf[row][col]
        regionRows[region] = regionRows[region] or (1 shl row)
        regionCols[region] = regionCols[region] or (1 shl col)
        rowRegions[row] = rowRegions[row] or (1 shl region)
        colRegions[col] = colRegions[col] or (1 shl region)
    }

    fun remainingIn(mask: Int, remaining: (Int) -> Int): Int {
        var total = 0
        for (i in 0 until size) if (mask and (1 shl i) != 0) total += remaining(i)
        return total
    }

    fun candidatesWhere(predicate: (Int, Int) -> Boolean): List<Pair<Int, Int>> {
        val cells = mutableListOf<Pair<Int, Int>>()
        for (row in 0 until size) for (col in 0 until size) {
            if (state.isCandidate(row, col) && predicate(row, col)) cells.add(row to col)
        }
        return cells
    }

    val openRegions = (0 until size).filter { regionRows[it] != 0 && state.regionRemaining(it) > 0 }
    val regionCombos = openRegions.combinationsOfSize(groupSize)
    for (combo in regionCombos) {
        val regionMask = combo.fold(0) { mask, region -> mask or (1 shl region) }
        val need = combo.sumOf { state.regionRemaining(it) }
        val rows = combo.fold(0) { mask, region -> mask or regionRows[region] }
        if (Integer.bitCount(rows) <= groupSize && remainingIn(rows, state::rowRemaining) == need) {
            val toEliminate = candidatesWhere { row, col -> rows and (1 shl row) != 0 && regionMask and (1 shl state.regionOf[row][col]) == 0 }
            if (toEliminate.isNotEmpty()) return StarBattleStep.Elimination(toEliminate, StarBattleTechnique.PAIRING)
        }
        val cols = combo.fold(0) { mask, region -> mask or regionCols[region] }
        if (Integer.bitCount(cols) <= groupSize && remainingIn(cols, state::colRemaining) == need) {
            val toEliminate = candidatesWhere { row, col -> cols and (1 shl col) != 0 && regionMask and (1 shl state.regionOf[row][col]) == 0 }
            if (toEliminate.isNotEmpty()) return StarBattleStep.Elimination(toEliminate, StarBattleTechnique.PAIRING)
        }
    }

    for (combo in (0 until size).filter { rowRegions[it] != 0 && state.rowRemaining(it) > 0 }.combinationsOfSize(groupSize)) {
        val rowMask = combo.fold(0) { mask, row -> mask or (1 shl row) }
        val regions = combo.fold(0) { mask, row -> mask or rowRegions[row] }
        if (Integer.bitCount(regions) <= groupSize && remainingIn(regions, state::regionRemaining) == combo.sumOf { state.rowRemaining(it) }) {
            val toEliminate = candidatesWhere { row, col -> rowMask and (1 shl row) == 0 && regions and (1 shl state.regionOf[row][col]) != 0 }
            if (toEliminate.isNotEmpty()) return StarBattleStep.Elimination(toEliminate, StarBattleTechnique.PAIRING)
        }
    }
    for (combo in (0 until size).filter { colRegions[it] != 0 && state.colRemaining(it) > 0 }.combinationsOfSize(groupSize)) {
        val colMask = combo.fold(0) { mask, col -> mask or (1 shl col) }
        val regions = combo.fold(0) { mask, col -> mask or colRegions[col] }
        if (Integer.bitCount(regions) <= groupSize && remainingIn(regions, state::regionRemaining) == combo.sumOf { state.colRemaining(it) }) {
            val toEliminate = candidatesWhere { row, col -> colMask and (1 shl col) == 0 && regions and (1 shl state.regionOf[row][col]) != 0 }
            if (toEliminate.isNotEmpty()) return StarBattleStep.Elimination(toEliminate, StarBattleTechnique.PAIRING)
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
