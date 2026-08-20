package com.quietgrid.engine.sudoku

data class SudokuHouseCellsRef(val house: SudokuHouseRef, val cells: List<Int>)

val allHouseRefs: List<SudokuHouseCellsRef> =
    rowCellIndexes.mapIndexed { index, cells -> SudokuHouseCellsRef(SudokuHouseRef("row", index), cells) } +
        columnCellIndexes.mapIndexed { index, cells -> SudokuHouseCellsRef(SudokuHouseRef("column", index), cells) } +
        boxCellIndexes.mapIndexed { index, cells -> SudokuHouseCellsRef(SudokuHouseRef("box", index), cells) }

fun buildPlacementHouseRefs(row: Int, col: Int): List<SudokuHouseRef> = listOf(
    SudokuHouseRef("row", row),
    SudokuHouseRef("column", col),
    SudokuHouseRef("box", cellBoxIndexes[getCellIndex(row, col)]),
)

fun collectHousesFromIndexes(indexes: List<Int>): List<SudokuHouseRef> {
    val seen = LinkedHashMap<String, SudokuHouseRef>()
    indexes.forEach { index ->
        val row = cellRowIndexes[index]
        val col = cellColIndexes[index]
        val box = cellBoxIndexes[index]
        seen["r:$row"] = SudokuHouseRef("row", row)
        seen["c:$col"] = SudokuHouseRef("column", col)
        seen["b:$box"] = SudokuHouseRef("box", box)
    }
    return seen.values.sortedWith(compareBy({ it.kind }, { it.index }))
}

fun cellRefFromIndex(index: Int): SudokuCellRef = SudokuCellRef(getCellRow(index), getCellCol(index))

fun getCellCandidates(state: SudokuBitmaskState, row: Int, col: Int): List<Int> = iterateMaskDigits(state.candidateMask[getCellIndex(row, col)])

fun getCellCandidatesByIndex(state: SudokuBitmaskState, index: Int): List<Int> = iterateMaskDigits(state.candidateMask[index])

fun isSameCell(left: SudokuCellRef, right: SudokuCellRef): Boolean = left.row == right.row && left.col == right.col

fun arePeerCells(left: SudokuCellRef, right: SudokuCellRef): Boolean = cellPeers[getCellIndex(left.row, left.col)].contains(getCellIndex(right.row, right.col))

fun arePeerIndexes(left: Int, right: Int): Boolean = cellPeers[left].contains(right)

fun buildPlacementMove(
    technique: SudokuTechnique,
    row: Int,
    col: Int,
    digit: Int,
    evidenceCells: List<Int>,
    houses: List<SudokuHouseRef>? = null,
    complexity: Int,
): SudokuPlacementMove = SudokuPlacementMove(
    technique = technique,
    complexity = complexity,
    targetRow = row,
    targetCol = col,
    digit = digit,
    evidenceCells = evidenceCells.map(::cellRefFromIndex).sortedWith(compareBy({ it.row }, { it.col })),
    houses = houses ?: buildPlacementHouseRefs(row, col),
)

fun buildCandidateEliminationMove(
    technique: SudokuTechnique,
    eliminations: List<Pair<Int, Int>>,
    evidenceCells: List<Int>,
    houses: List<SudokuHouseRef>? = null,
    complexity: Int,
): SudokuCandidateEliminationMove? {
    val uniqueEliminations = eliminations
        .map { (index, digit) -> cellRefFromIndex(index) to digit }
        .distinct()
        .sortedWith(compareBy({ it.first.row }, { it.first.col }, { it.second }))
    if (uniqueEliminations.isEmpty()) return null

    return SudokuCandidateEliminationMove(
        technique = technique,
        complexity = complexity,
        eliminations = uniqueEliminations.map { Triple(it.first.row, it.first.col, it.second) },
        evidenceCells = evidenceCells.map(::cellRefFromIndex).distinct().sortedWith(compareBy({ it.row }, { it.col })),
        houses = houses ?: collectHousesFromIndexes(evidenceCells),
    )
}

fun peerIntersectionIndexes(indexes: List<Int>): List<Int> {
    if (indexes.isEmpty()) return emptyList()
    val first = indexes.first()
    val rest = indexes.drop(1)
    return cellPeers[first].filter { candidate -> rest.all { arePeerIndexes(candidate, it) } }
}

fun getHouseDigitMatches(state: SudokuBitmaskState, cells: List<Int>, digit: Int): List<Int> =
    cells.filter { hasCandidateAtIndex(state, it, digit) }
