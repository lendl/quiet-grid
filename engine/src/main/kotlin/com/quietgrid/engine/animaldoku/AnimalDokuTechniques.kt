package com.quietgrid.engine.animaldoku

enum class AnimalDokuTechnique { SINGLETON, STRUCTURAL_CONFINEMENT, CONFINEMENT, PAIRING_2, PAIRING_3, PAIRING_4_PLUS, CHAIN }

sealed class AnimalDokuStep {
    abstract val technique: AnimalDokuTechnique

    data class Placement(val row: Int, val col: Int, override val technique: AnimalDokuTechnique) : AnimalDokuStep()

    data class Elimination(
        val cells: List<Pair<Int, Int>>,
        override val technique: AnimalDokuTechnique,
        val chainDepth: Int = 0,
    ) : AnimalDokuStep()
}

fun findSingleton(state: AnimalDokuSolverState): AnimalDokuStep.Placement? {
    for (region in 0 until state.size) {
        if (state.regionSolved[region]) continue
        val cells = state.candidatesInRegion(region)
        if (cells.size == 1) return AnimalDokuStep.Placement(cells[0].first, cells[0].second, AnimalDokuTechnique.SINGLETON)
    }
    for (row in 0 until state.size) {
        if (state.rowSolved[row]) continue
        val cells = state.candidatesInRow(row)
        if (cells.size == 1) return AnimalDokuStep.Placement(cells[0].first, cells[0].second, AnimalDokuTechnique.SINGLETON)
    }
    for (col in 0 until state.size) {
        if (state.colSolved[col]) continue
        val cells = state.candidatesInCol(col)
        if (cells.size == 1) return AnimalDokuStep.Placement(cells[0].first, cells[0].second, AnimalDokuTechnique.SINGLETON)
    }
    return null
}

private fun regionCellsRaw(regionOf: List<List<Int>>, size: Int, region: Int): List<Pair<Int, Int>> =
    (0 until size).flatMap { r -> (0 until size).mapNotNull { c -> if (regionOf[r][c] == region) r to c else null } }

private fun rowIsMonochrome(regionOf: List<List<Int>>, size: Int, row: Int): Boolean =
    (1 until size).all { c -> regionOf[row][c] == regionOf[row][0] }

private fun colIsMonochrome(regionOf: List<List<Int>>, size: Int, col: Int): Boolean =
    (1 until size).all { r -> regionOf[r][col] == regionOf[0][col] }

fun findConfinement(state: AnimalDokuSolverState): AnimalDokuStep.Elimination? {
    val size = state.size

    for (region in 0 until size) {
        if (state.regionSolved[region]) continue
        val cells = state.candidatesInRegion(region)
        if (cells.isEmpty()) continue

        val rows = cells.map { it.first }.toSet()
        if (rows.size == 1) {
            val toEliminate = state.candidatesInRow(rows.first()).filter { state.regionOf[it.first][it.second] != region }
            if (toEliminate.isNotEmpty()) {
                val structural = regionCellsRaw(state.regionOf, size, region).all { it.first == rows.first() }
                val technique = if (structural) AnimalDokuTechnique.STRUCTURAL_CONFINEMENT else AnimalDokuTechnique.CONFINEMENT
                return AnimalDokuStep.Elimination(toEliminate, technique)
            }
        }

        val cols = cells.map { it.second }.toSet()
        if (cols.size == 1) {
            val toEliminate = state.candidatesInCol(cols.first()).filter { state.regionOf[it.first][it.second] != region }
            if (toEliminate.isNotEmpty()) {
                val structural = regionCellsRaw(state.regionOf, size, region).all { it.second == cols.first() }
                val technique = if (structural) AnimalDokuTechnique.STRUCTURAL_CONFINEMENT else AnimalDokuTechnique.CONFINEMENT
                return AnimalDokuStep.Elimination(toEliminate, technique)
            }
        }
    }

    for (row in 0 until size) {
        if (state.rowSolved[row]) continue
        val cells = state.candidatesInRow(row)
        if (cells.isEmpty()) continue
        val regions = cells.map { state.regionOf[it.first][it.second] }.toSet()
        if (regions.size == 1) {
            val toEliminate = state.candidatesInRegion(regions.first()).filter { it.first != row }
            if (toEliminate.isNotEmpty()) {
                val structural = rowIsMonochrome(state.regionOf, size, row)
                val technique = if (structural) AnimalDokuTechnique.STRUCTURAL_CONFINEMENT else AnimalDokuTechnique.CONFINEMENT
                return AnimalDokuStep.Elimination(toEliminate, technique)
            }
        }
    }

    for (col in 0 until size) {
        if (state.colSolved[col]) continue
        val cells = state.candidatesInCol(col)
        if (cells.isEmpty()) continue
        val regions = cells.map { state.regionOf[it.first][it.second] }.toSet()
        if (regions.size == 1) {
            val toEliminate = state.candidatesInRegion(regions.first()).filter { it.second != col }
            if (toEliminate.isNotEmpty()) {
                val structural = colIsMonochrome(state.regionOf, size, col)
                val technique = if (structural) AnimalDokuTechnique.STRUCTURAL_CONFINEMENT else AnimalDokuTechnique.CONFINEMENT
                return AnimalDokuStep.Elimination(toEliminate, technique)
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

private fun pairingTechniqueFor(k: Int): AnimalDokuTechnique = when {
    k == 2 -> AnimalDokuTechnique.PAIRING_2
    k == 3 -> AnimalDokuTechnique.PAIRING_3
    else -> AnimalDokuTechnique.PAIRING_4_PLUS
}

fun findPairing(state: AnimalDokuSolverState, k: Int): AnimalDokuStep.Elimination? {
    val size = state.size
    val unsolvedRegions = (0 until size).filter { !state.regionSolved[it] && state.candidatesInRegion(it).isNotEmpty() }
    if (unsolvedRegions.size < k) return null
    val technique = pairingTechniqueFor(k)

    for (combo in unsolvedRegions.combinationsOfSize(k)) {
        val rows = combo.flatMap { region -> state.candidatesInRegion(region).map { it.first } }.toSet()
        if (rows.size == k) {
            val toEliminate = rows.flatMap { row -> state.candidatesInRow(row) }
                .filter { state.regionOf[it.first][it.second] !in combo }
            if (toEliminate.isNotEmpty()) return AnimalDokuStep.Elimination(toEliminate, technique)
        }
    }

    for (combo in unsolvedRegions.combinationsOfSize(k)) {
        val cols = combo.flatMap { region -> state.candidatesInRegion(region).map { it.second } }.toSet()
        if (cols.size == k) {
            val toEliminate = cols.flatMap { col -> state.candidatesInCol(col) }
                .filter { state.regionOf[it.first][it.second] !in combo }
            if (toEliminate.isNotEmpty()) return AnimalDokuStep.Elimination(toEliminate, technique)
        }
    }

    return null
}

private const val MAX_CHAIN_PROPAGATION_DEPTH = 10

private fun propagateSingletons(state: AnimalDokuSolverState, maxDepth: Int): Int {
    var depth = 0
    while (depth < maxDepth) {
        val move = findSingleton(state) ?: break
        state.place(move.row, move.col)
        depth++
    }
    return depth
}

private fun hasContradiction(state: AnimalDokuSolverState): Boolean {
    for (region in 0 until state.size) if (!state.regionSolved[region] && state.candidatesInRegion(region).isEmpty()) return true
    for (row in 0 until state.size) if (!state.rowSolved[row] && state.candidatesInRow(row).isEmpty()) return true
    for (col in 0 until state.size) if (!state.colSolved[col] && state.candidatesInCol(col).isEmpty()) return true
    return false
}

fun findChainContradiction(state: AnimalDokuSolverState): AnimalDokuStep.Elimination? {
    for (region in 0 until state.size) {
        if (state.regionSolved[region]) continue
        for ((row, col) in state.candidatesInRegion(region)) {
            val hypothesis = state.copy()
            hypothesis.place(row, col)
            val intermediatePlacements = propagateSingletons(hypothesis, MAX_CHAIN_PROPAGATION_DEPTH)
            if (hasContradiction(hypothesis)) {
                return AnimalDokuStep.Elimination(listOf(row to col), AnimalDokuTechnique.CHAIN, chainDepth = 1 + intermediatePlacements)
            }
        }
    }
    return null
}
