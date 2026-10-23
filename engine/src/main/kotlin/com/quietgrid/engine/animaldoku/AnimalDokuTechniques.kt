package com.quietgrid.engine.animaldoku

enum class AnimalDokuTechnique { SINGLETON, STRUCTURAL_CONFINEMENT, CONFINEMENT, PAIRING_2, PAIRING_3, PAIRING_4_PLUS, CHAIN }

enum class AnimalDokuUnitKind { ROW, COLUMN, REGION }

data class AnimalDokuUnit(val kind: AnimalDokuUnitKind, val index: Int)

fun AnimalDokuUnit.cells(regionOf: List<List<Int>>): List<Pair<Int, Int>> {
    val size = regionOf.size
    return when (kind) {
        AnimalDokuUnitKind.ROW -> (0 until size).map { col -> index to col }
        AnimalDokuUnitKind.COLUMN -> (0 until size).map { row -> row to index }
        AnimalDokuUnitKind.REGION -> regionCellsRaw(regionOf, size, index)
    }
}

fun AnimalDokuSolverState.candidatesIn(unit: AnimalDokuUnit): List<Pair<Int, Int>> =
    unit.cells(regionOf).filter { (row, col) -> isCandidate(row, col) }

sealed class AnimalDokuStep {
    abstract val technique: AnimalDokuTechnique

    data class Placement(
        val row: Int,
        val col: Int,
        override val technique: AnimalDokuTechnique,
        val unit: AnimalDokuUnit,
    ) : AnimalDokuStep()

    data class Elimination(
        val cells: List<Pair<Int, Int>>,
        override val technique: AnimalDokuTechnique,
        val sources: List<AnimalDokuUnit>,
        val targets: List<AnimalDokuUnit>,
        val chainDepth: Int = 0,
    ) : AnimalDokuStep()
}

private fun rowUnit(index: Int) = AnimalDokuUnit(AnimalDokuUnitKind.ROW, index)
private fun columnUnit(index: Int) = AnimalDokuUnit(AnimalDokuUnitKind.COLUMN, index)
private fun regionUnit(index: Int) = AnimalDokuUnit(AnimalDokuUnitKind.REGION, index)

private fun Int.maskIndices(size: Int): List<Int> = (0 until size).filter { this and (1 shl it) != 0 }

fun findSingleton(state: AnimalDokuSolverState): AnimalDokuStep.Placement? {
    for (region in 0 until state.size) {
        if (state.regionSolved[region]) continue
        val cells = state.candidatesInRegion(region)
        if (cells.size == 1) return AnimalDokuStep.Placement(cells[0].first, cells[0].second, AnimalDokuTechnique.SINGLETON, regionUnit(region))
    }
    for (row in 0 until state.size) {
        if (state.rowSolved[row]) continue
        val cells = state.candidatesInRow(row)
        if (cells.size == 1) return AnimalDokuStep.Placement(cells[0].first, cells[0].second, AnimalDokuTechnique.SINGLETON, rowUnit(row))
    }
    for (col in 0 until state.size) {
        if (state.colSolved[col]) continue
        val cells = state.candidatesInCol(col)
        if (cells.size == 1) return AnimalDokuStep.Placement(cells[0].first, cells[0].second, AnimalDokuTechnique.SINGLETON, columnUnit(col))
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
                return AnimalDokuStep.Elimination(toEliminate, technique, sources = listOf(regionUnit(region)), targets = listOf(rowUnit(rows.first())))
            }
        }

        val cols = cells.map { it.second }.toSet()
        if (cols.size == 1) {
            val toEliminate = state.candidatesInCol(cols.first()).filter { state.regionOf[it.first][it.second] != region }
            if (toEliminate.isNotEmpty()) {
                val structural = regionCellsRaw(state.regionOf, size, region).all { it.second == cols.first() }
                val technique = if (structural) AnimalDokuTechnique.STRUCTURAL_CONFINEMENT else AnimalDokuTechnique.CONFINEMENT
                return AnimalDokuStep.Elimination(toEliminate, technique, sources = listOf(regionUnit(region)), targets = listOf(columnUnit(cols.first())))
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
                return AnimalDokuStep.Elimination(toEliminate, technique, sources = listOf(rowUnit(row)), targets = listOf(regionUnit(regions.first())))
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
                return AnimalDokuStep.Elimination(toEliminate, technique, sources = listOf(columnUnit(col)), targets = listOf(regionUnit(regions.first())))
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
    val technique = pairingTechniqueFor(k)
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

    fun candidatesWhere(predicate: (Int, Int) -> Boolean): List<Pair<Int, Int>> {
        val cells = mutableListOf<Pair<Int, Int>>()
        for (row in 0 until size) for (col in 0 until size) {
            if (state.isCandidate(row, col) && predicate(row, col)) cells.add(row to col)
        }
        return cells
    }

    val unsolvedRegions = (0 until size).filter { regionRows[it] != 0 }
    val regionCombos = unsolvedRegions.combinationsOfSize(k)
    for (combo in regionCombos) {
        val regionMask = combo.fold(0) { mask, region -> mask or (1 shl region) }
        val rows = combo.fold(0) { mask, region -> mask or regionRows[region] }
        if (Integer.bitCount(rows) == k) {
            val toEliminate = candidatesWhere { row, col -> rows and (1 shl row) != 0 && regionMask and (1 shl state.regionOf[row][col]) == 0 }
            if (toEliminate.isNotEmpty()) {
                return AnimalDokuStep.Elimination(toEliminate, technique, sources = combo.map(::regionUnit), targets = rows.maskIndices(size).map(::rowUnit))
            }
        }
    }
    for (combo in regionCombos) {
        val regionMask = combo.fold(0) { mask, region -> mask or (1 shl region) }
        val cols = combo.fold(0) { mask, region -> mask or regionCols[region] }
        if (Integer.bitCount(cols) == k) {
            val toEliminate = candidatesWhere { row, col -> cols and (1 shl col) != 0 && regionMask and (1 shl state.regionOf[row][col]) == 0 }
            if (toEliminate.isNotEmpty()) {
                return AnimalDokuStep.Elimination(toEliminate, technique, sources = combo.map(::regionUnit), targets = cols.maskIndices(size).map(::columnUnit))
            }
        }
    }

    for (combo in (0 until size).filter { rowRegions[it] != 0 }.combinationsOfSize(k)) {
        val rowMask = combo.fold(0) { mask, row -> mask or (1 shl row) }
        val regions = combo.fold(0) { mask, row -> mask or rowRegions[row] }
        if (Integer.bitCount(regions) == k) {
            val toEliminate = candidatesWhere { row, col -> rowMask and (1 shl row) == 0 && regions and (1 shl state.regionOf[row][col]) != 0 }
            if (toEliminate.isNotEmpty()) {
                return AnimalDokuStep.Elimination(toEliminate, technique, sources = combo.map(::rowUnit), targets = regions.maskIndices(size).map(::regionUnit))
            }
        }
    }
    for (combo in (0 until size).filter { colRegions[it] != 0 }.combinationsOfSize(k)) {
        val colMask = combo.fold(0) { mask, col -> mask or (1 shl col) }
        val regions = combo.fold(0) { mask, col -> mask or colRegions[col] }
        if (Integer.bitCount(regions) == k) {
            val toEliminate = candidatesWhere { row, col -> colMask and (1 shl col) == 0 && regions and (1 shl state.regionOf[row][col]) != 0 }
            if (toEliminate.isNotEmpty()) {
                return AnimalDokuStep.Elimination(toEliminate, technique, sources = combo.map(::columnUnit), targets = regions.maskIndices(size).map(::regionUnit))
            }
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

private fun findEmptyUnit(state: AnimalDokuSolverState): AnimalDokuUnit? {
    for (region in 0 until state.size) if (!state.regionSolved[region] && state.candidatesInRegion(region).isEmpty()) return regionUnit(region)
    for (row in 0 until state.size) if (!state.rowSolved[row] && state.candidatesInRow(row).isEmpty()) return rowUnit(row)
    for (col in 0 until state.size) if (!state.colSolved[col] && state.candidatesInCol(col).isEmpty()) return columnUnit(col)
    return null
}

fun findChainContradiction(state: AnimalDokuSolverState): AnimalDokuStep.Elimination? {
    for (region in 0 until state.size) {
        if (state.regionSolved[region]) continue
        for ((row, col) in state.candidatesInRegion(region)) {
            val hypothesis = state.copy()
            hypothesis.place(row, col)
            val intermediatePlacements = propagateSingletons(hypothesis, MAX_CHAIN_PROPAGATION_DEPTH)
            val emptyUnit = findEmptyUnit(hypothesis)
            if (emptyUnit != null) {
                return AnimalDokuStep.Elimination(
                    listOf(row to col),
                    AnimalDokuTechnique.CHAIN,
                    sources = listOf(emptyUnit),
                    targets = emptyList(),
                    chainDepth = 1 + intermediatePlacements,
                )
            }
        }
    }
    return null
}
