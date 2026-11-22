package com.quietgrid.engine.sudoku

data class SudokuHouseCellsRef(val house: SudokuHouseRef, val cells: List<Int>)

val allHouseRefs: List<SudokuHouseCellsRef> get() = SudokuLayout.CLASSIC.allHouseRefs

fun buildPlacementHouseRefs(row: Int, col: Int, layout: SudokuLayout = SudokuLayout.CLASSIC): List<SudokuHouseRef> = listOf(
    SudokuHouseRef("row", row),
    SudokuHouseRef("column", col),
    SudokuHouseRef("box", layout.boxOf(row, col)),
)

fun collectHousesFromIndexes(indexes: List<Int>, layout: SudokuLayout = SudokuLayout.CLASSIC): List<SudokuHouseRef> {
    val seen = LinkedHashMap<String, SudokuHouseRef>()
    indexes.forEach { index ->
        val row = layout.cellRowIndexes[index]
        val col = layout.cellColIndexes[index]
        val box = layout.cellBoxIndexes[index]
        seen["r:$row"] = SudokuHouseRef("row", row)
        seen["c:$col"] = SudokuHouseRef("column", col)
        seen["b:$box"] = SudokuHouseRef("box", box)
    }
    return seen.values.sortedWith(compareBy({ it.kind }, { it.index }))
}

fun cellRefFromIndex(index: Int, layout: SudokuLayout = SudokuLayout.CLASSIC): SudokuCellRef = SudokuCellRef(layout.rowOf(index), layout.colOf(index))

fun getCellCandidates(state: SudokuBitmaskState, row: Int, col: Int): List<Int> = iterateMaskDigits(state.candidateMask[state.layout.index(row, col)])

fun getCellCandidatesByIndex(state: SudokuBitmaskState, index: Int): List<Int> = iterateMaskDigits(state.candidateMask[index])

fun isSameCell(left: SudokuCellRef, right: SudokuCellRef): Boolean = left.row == right.row && left.col == right.col

fun arePeerCells(left: SudokuCellRef, right: SudokuCellRef, layout: SudokuLayout = SudokuLayout.CLASSIC): Boolean =
    layout.arePeers(layout.index(left.row, left.col), layout.index(right.row, right.col))

fun arePeerIndexes(left: Int, right: Int, layout: SudokuLayout = SudokuLayout.CLASSIC): Boolean = layout.arePeers(left, right)

fun buildPlacementMove(
    technique: SudokuTechnique,
    row: Int,
    col: Int,
    digit: Int,
    evidenceCells: List<Int>,
    houses: List<SudokuHouseRef>? = null,
    complexity: Int,
    layout: SudokuLayout = SudokuLayout.CLASSIC,
): SudokuPlacementMove = SudokuPlacementMove(
    technique = technique,
    complexity = complexity,
    targetRow = row,
    targetCol = col,
    digit = digit,
    evidenceCells = evidenceCells.map { cellRefFromIndex(it, layout) }.sortedWith(compareBy({ it.row }, { it.col })),
    houses = houses ?: buildPlacementHouseRefs(row, col, layout),
)

fun buildCandidateEliminationMove(
    technique: SudokuTechnique,
    eliminations: List<Pair<Int, Int>>,
    evidenceCells: List<Int>,
    houses: List<SudokuHouseRef>? = null,
    complexity: Int,
    layout: SudokuLayout = SudokuLayout.CLASSIC,
): SudokuCandidateEliminationMove? {
    val uniqueEliminations = eliminations
        .map { (index, digit) -> cellRefFromIndex(index, layout) to digit }
        .distinct()
        .sortedWith(compareBy({ it.first.row }, { it.first.col }, { it.second }))
    if (uniqueEliminations.isEmpty()) return null

    return SudokuCandidateEliminationMove(
        technique = technique,
        complexity = complexity,
        eliminations = uniqueEliminations.map { Triple(it.first.row, it.first.col, it.second) },
        evidenceCells = evidenceCells.map { cellRefFromIndex(it, layout) }.distinct().sortedWith(compareBy({ it.row }, { it.col })),
        houses = houses ?: collectHousesFromIndexes(evidenceCells, layout),
    )
}

fun peerIntersectionIndexes(indexes: List<Int>, layout: SudokuLayout = SudokuLayout.CLASSIC): List<Int> {
    if (indexes.isEmpty()) return emptyList()
    val first = indexes.first()
    val rest = indexes.drop(1)
    return layout.cellPeers[first].filter { candidate -> rest.all { layout.arePeers(candidate, it) } }
}

fun getHouseDigitMatches(state: SudokuBitmaskState, cells: List<Int>, digit: Int): List<Int> =
    cells.filter { hasCandidateAtIndex(state, it, digit) }
