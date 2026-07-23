package com.quietgrid.app.games.sudoku

/**
 * Ports the RN app's Sudoku next-move hint (src/games/sudoku/gameplay/analysis). RN's solver
 * tries 12 techniques ranked by complexity score to find the single best move across all of
 * them; this port covers the 6 "basic + intersection" techniques (naked/hidden single,
 * naked/hidden pair, pointing pair/triple, box-line reduction) that solve easy-to-hard puzzles,
 * tried in a fixed priority order (first valid move wins, no cross-technique scoring). The 6
 * advanced fish/wing/chain techniques (x-wing, swordfish, xy-wing, xyz-wing, coloring, chains —
 * expert-tier deductions) are not ported; positions that only have those left fall back to a
 * generic "no supported move" hint, same as RN's own "unsupported" fallback copy.
 */

private const val FULL_MASK = 0b111111111

private fun digitToBit(digit: Int) = 1 shl (digit - 1)
private fun bitToDigit(mask: Int): Int = Integer.numberOfTrailingZeros(mask) + 1

private fun cellIndex(row: Int, col: Int) = row * 9 + col
private fun cellRow(index: Int) = index / 9
private fun cellCol(index: Int) = index % 9
private fun cellBox(row: Int, col: Int) = (row / 3) * 3 + (col / 3)

private val rowCellIndexes: List<List<Int>> = (0 until 9).map { row -> (0 until 9).map { col -> cellIndex(row, col) } }
private val columnCellIndexes: List<List<Int>> = (0 until 9).map { col -> (0 until 9).map { row -> cellIndex(row, col) } }
private val boxCellIndexes: List<List<Int>> = (0 until 9).map { box ->
    val rowStart = (box / 3) * 3
    val colStart = (box % 3) * 3
    (0 until 9).map { offset -> cellIndex(rowStart + offset / 3, colStart + offset % 3) }
}
private val cellBoxIndexes: List<Int> = (0 until 81).map { index -> cellBox(cellRow(index), cellCol(index)) }

private fun cellRef(index: Int): Pair<Int, Int> = cellRow(index) to cellCol(index)

enum class SudokuTechnique { NAKED_SINGLE, HIDDEN_SINGLE, NAKED_PAIR, HIDDEN_PAIR, POINTING_PAIR_TRIPLE, BOX_LINE_REDUCTION }

data class SudokuHouseRef(val kind: String, val index: Int)

sealed interface SudokuNextMoveHint {
    val evidenceCells: List<Pair<Int, Int>>
    val targetCells: List<Pair<Int, Int>>
    val highlightRows: List<Int>
    val highlightCols: List<Int>
    val highlightBoxes: List<Int>
}

data class SudokuInvalidConflict(
    val house: SudokuHouseRef,
    val digit: Int,
    override val evidenceCells: List<Pair<Int, Int>>,
    override val highlightRows: List<Int>,
    override val highlightCols: List<Int>,
    override val highlightBoxes: List<Int>,
) : SudokuNextMoveHint {
    override val targetCells = emptyList<Pair<Int, Int>>()
}

data class SudokuInvalidDeadCell(
    val row: Int,
    val col: Int,
    override val evidenceCells: List<Pair<Int, Int>>,
    override val highlightRows: List<Int>,
    override val highlightCols: List<Int>,
    override val highlightBoxes: List<Int>,
) : SudokuNextMoveHint {
    override val targetCells = emptyList<Pair<Int, Int>>()
}

data class SudokuPlacementHint(
    val technique: SudokuTechnique,
    val row: Int,
    val col: Int,
    val digit: Int,
    val house: SudokuHouseRef?,
    override val evidenceCells: List<Pair<Int, Int>>,
    override val targetCells: List<Pair<Int, Int>>,
    override val highlightRows: List<Int>,
    override val highlightCols: List<Int>,
    override val highlightBoxes: List<Int>,
) : SudokuNextMoveHint

data class SudokuEliminationHint(
    val technique: SudokuTechnique,
    val digits: List<Int>,
    val sourceHouse: SudokuHouseRef,
    val targetHouse: SudokuHouseRef?,
    override val evidenceCells: List<Pair<Int, Int>>,
    override val targetCells: List<Pair<Int, Int>>,
    override val highlightRows: List<Int>,
    override val highlightCols: List<Int>,
    override val highlightBoxes: List<Int>,
) : SudokuNextMoveHint

private class BitmaskState(val board: IntArray, val candidateMask: IntArray)

private fun buildBitmaskState(board: SudokuGrid): BitmaskState {
    val flat = IntArray(81)
    for (r in 0 until 9) for (c in 0 until 9) flat[cellIndex(r, c)] = board[r][c] ?: 0
    val rowMask = IntArray(9)
    val colMask = IntArray(9)
    val boxMask = IntArray(9)
    for (index in 0 until 81) {
        val v = flat[index]
        if (v == 0) continue
        val bit = digitToBit(v)
        val row = cellRow(index); val col = cellCol(index); val box = cellBoxIndexes[index]
        rowMask[row] = rowMask[row] or bit
        colMask[col] = colMask[col] or bit
        boxMask[box] = boxMask[box] or bit
    }
    val candidateMask = IntArray(81)
    for (index in 0 until 81) {
        if (flat[index] != 0) continue
        val row = cellRow(index); val col = cellCol(index); val box = cellBoxIndexes[index]
        candidateMask[index] = FULL_MASK and (rowMask[row] or colMask[col] or boxMask[box]).inv()
    }
    return BitmaskState(flat, candidateMask)
}

private fun houseDigitMatches(state: BitmaskState, cells: List<Int>, digit: Int): List<Int> {
    val bit = digitToBit(digit)
    return cells.filter { state.board[it] == 0 && (state.candidateMask[it] and bit) != 0 }
}

private fun cellCandidates(state: BitmaskState, index: Int): List<Int> =
    (1..9).filter { (state.candidateMask[index] and digitToBit(it)) != 0 }

private data class House(val ref: SudokuHouseRef, val cells: List<Int>)

private val allHouses: List<House> =
    rowCellIndexes.mapIndexed { i, cells -> House(SudokuHouseRef("row", i), cells) } +
        columnCellIndexes.mapIndexed { i, cells -> House(SudokuHouseRef("column", i), cells) } +
        boxCellIndexes.mapIndexed { i, cells -> House(SudokuHouseRef("box", i), cells) }

private fun highlightsFor(houses: List<SudokuHouseRef>): Triple<List<Int>, List<Int>, List<Int>> {
    val rows = houses.filter { it.kind == "row" }.map { it.index }.distinct().sorted()
    val cols = houses.filter { it.kind == "column" }.map { it.index }.distinct().sorted()
    val boxes = houses.filter { it.kind == "box" }.map { it.index }.distinct().sorted()
    return Triple(rows, cols, boxes)
}

// ---- Invalid-board detection (checked before any technique runs) ----

private fun findDuplicateConflict(board: SudokuGrid): SudokuInvalidConflict? {
    for (house in allHouses) {
        val byDigit = HashMap<Int, MutableList<Int>>()
        for (index in house.cells) {
            val digit = board[cellRow(index)][cellCol(index)] ?: continue
            byDigit.getOrPut(digit) { mutableListOf() }.add(index)
        }
        for ((digit, cells) in byDigit) {
            if (cells.size < 2) continue
            val (rows, cols, boxes) = highlightsFor(listOf(house.ref))
            return SudokuInvalidConflict(house.ref, digit, cells.map(::cellRef).sortedWith(compareBy({ it.first }, { it.second })), rows, cols, boxes)
        }
    }
    return null
}

private fun findDeadCellConflict(board: SudokuGrid): SudokuInvalidDeadCell? {
    val state = buildBitmaskState(board)
    for (index in 0 until 81) {
        if (state.board[index] != 0 || state.candidateMask[index] != 0) continue
        val row = cellRow(index); val col = cellCol(index); val box = cellBoxIndexes[index]
        val evidence = (rowCellIndexes[row] + columnCellIndexes[col] + boxCellIndexes[box])
            .filter { board[cellRow(it)][cellCol(it)] != null }
            .distinct()
            .map(::cellRef)
            .sortedWith(compareBy({ it.first }, { it.second }))
        return SudokuInvalidDeadCell(row, col, evidence, listOf(row), listOf(col), listOf(box))
    }
    return null
}

fun findSudokuInvalidState(board: SudokuGrid): SudokuNextMoveHint? =
    findDuplicateConflict(board) ?: findDeadCellConflict(board)

// ---- Core techniques (first valid move wins, in RN's priority order) ----

private fun findNakedSingle(state: BitmaskState): SudokuPlacementHint? {
    for (index in 0 until 81) {
        if (state.board[index] != 0 || Integer.bitCount(state.candidateMask[index]) != 1) continue
        val row = cellRow(index); val col = cellCol(index)
        return SudokuPlacementHint(
            SudokuTechnique.NAKED_SINGLE, row, col, bitToDigit(state.candidateMask[index]), null,
            listOf(cellRef(index)), listOf(row to col), listOf(row), listOf(col), listOf(cellBoxIndexes[index]),
        )
    }
    return null
}

private fun findHiddenSingle(state: BitmaskState): SudokuPlacementHint? {
    for (house in allHouses) {
        for (digit in 1..9) {
            val matches = houseDigitMatches(state, house.cells, digit)
            if (matches.size != 1) continue
            val index = matches[0]
            val row = cellRow(index); val col = cellCol(index)
            val (rows, cols, boxes) = highlightsFor(listOf(house.ref, SudokuHouseRef("row", row), SudokuHouseRef("column", col), SudokuHouseRef("box", cellBoxIndexes[index])))
            return SudokuPlacementHint(
                SudokuTechnique.HIDDEN_SINGLE, row, col, digit, house.ref,
                house.cells.map(::cellRef), listOf(row to col), rows, cols, boxes,
            )
        }
    }
    return null
}

private fun findNakedPair(state: BitmaskState): SudokuEliminationHint? {
    for (house in allHouses) {
        val byMask = HashMap<Int, MutableList<Int>>()
        for (index in house.cells) {
            if (state.board[index] != 0 || Integer.bitCount(state.candidateMask[index]) != 2) continue
            byMask.getOrPut(state.candidateMask[index]) { mutableListOf() }.add(index)
        }
        for ((mask, pairCells) in byMask) {
            if (pairCells.size != 2) continue
            val pairDigits = cellCandidates(state, pairCells[0])
            val eliminations = house.cells
                .filter { it !in pairCells }
                .flatMap { index -> pairDigits.filter { d -> cellCandidates(state, index).contains(d) }.map { index to it } }
            if (eliminations.isEmpty()) continue
            val (rows, cols, boxes) = highlightsFor(listOf(house.ref))
            return SudokuEliminationHint(
                SudokuTechnique.NAKED_PAIR, pairDigits, house.ref, null,
                pairCells.map(::cellRef), eliminations.map { cellRef(it.first) }.distinct(), rows, cols, boxes,
            )
        }
    }
    return null
}

private fun findHiddenPair(state: BitmaskState): SudokuEliminationHint? {
    for (house in allHouses) {
        val positions = (1..9).associateWith { digit -> houseDigitMatches(state, house.cells, digit) }
        for (leftIndex in 1..9) {
            for (rightIndex in (leftIndex + 1)..9) {
                val leftPositions = positions[leftIndex] ?: continue
                val rightPositions = positions[rightIndex] ?: continue
                if (leftPositions.size != 2 || rightPositions.size != 2) continue
                if (leftPositions[0] != rightPositions[0] || leftPositions[1] != rightPositions[1]) continue
                val eliminations = leftPositions.flatMap { index ->
                    cellCandidates(state, index).filter { it != leftIndex && it != rightIndex }.map { index to it }
                }
                if (eliminations.isEmpty()) continue
                val (rows, cols, boxes) = highlightsFor(listOf(house.ref))
                return SudokuEliminationHint(
                    SudokuTechnique.HIDDEN_PAIR, listOf(leftIndex, rightIndex), house.ref, null,
                    leftPositions.map(::cellRef), eliminations.map { cellRef(it.first) }.distinct(), rows, cols, boxes,
                )
            }
        }
    }
    return null
}

private fun findPointingPairTriple(state: BitmaskState): SudokuEliminationHint? {
    for (boxIndex in boxCellIndexes.indices) {
        val boxCells = boxCellIndexes[boxIndex]
        for (digit in 1..9) {
            val matches = houseDigitMatches(state, boxCells, digit)
            if (matches.size !in 2..3) continue

            val distinctRows = matches.map { cellRow(it) }.distinct()
            if (distinctRows.size == 1) {
                val targetRow = distinctRows[0]
                val eliminations = rowCellIndexes[targetRow]
                    .filter { cellBoxIndexes[it] != boxIndex && state.board[it] == 0 && (state.candidateMask[it] and digitToBit(digit)) != 0 }
                if (eliminations.isNotEmpty()) {
                    val (rows, cols, boxes) = highlightsFor(listOf(SudokuHouseRef("box", boxIndex), SudokuHouseRef("row", targetRow)))
                    return SudokuEliminationHint(
                        SudokuTechnique.POINTING_PAIR_TRIPLE, listOf(digit), SudokuHouseRef("box", boxIndex), SudokuHouseRef("row", targetRow),
                        matches.map(::cellRef), eliminations.map(::cellRef), rows, cols, boxes,
                    )
                }
            }

            val distinctCols = matches.map { cellCol(it) }.distinct()
            if (distinctCols.size == 1) {
                val targetCol = distinctCols[0]
                val eliminations = columnCellIndexes[targetCol]
                    .filter { cellBoxIndexes[it] != boxIndex && state.board[it] == 0 && (state.candidateMask[it] and digitToBit(digit)) != 0 }
                if (eliminations.isNotEmpty()) {
                    val (rows, cols, boxes) = highlightsFor(listOf(SudokuHouseRef("box", boxIndex), SudokuHouseRef("column", targetCol)))
                    return SudokuEliminationHint(
                        SudokuTechnique.POINTING_PAIR_TRIPLE, listOf(digit), SudokuHouseRef("box", boxIndex), SudokuHouseRef("column", targetCol),
                        matches.map(::cellRef), eliminations.map(::cellRef), rows, cols, boxes,
                    )
                }
            }
        }
    }
    return null
}

private fun findBoxLineReduction(state: BitmaskState): SudokuEliminationHint? {
    for (rowIndex in rowCellIndexes.indices) {
        val rowCells = rowCellIndexes[rowIndex]
        for (digit in 1..9) {
            val matches = houseDigitMatches(state, rowCells, digit)
            if (matches.size !in 2..3) continue
            val boxIndexes = matches.map { cellBoxIndexes[it] }.distinct()
            if (boxIndexes.size != 1) continue
            val boxIndex = boxIndexes[0]
            val eliminations = boxCellIndexes[boxIndex]
                .filter { cellRow(it) != rowIndex && state.board[it] == 0 && (state.candidateMask[it] and digitToBit(digit)) != 0 }
            if (eliminations.isEmpty()) continue
            val (rows, cols, boxes) = highlightsFor(listOf(SudokuHouseRef("row", rowIndex), SudokuHouseRef("box", boxIndex)))
            return SudokuEliminationHint(
                SudokuTechnique.BOX_LINE_REDUCTION, listOf(digit), SudokuHouseRef("row", rowIndex), SudokuHouseRef("box", boxIndex),
                matches.map(::cellRef), eliminations.map(::cellRef), rows, cols, boxes,
            )
        }
    }
    for (colIndex in columnCellIndexes.indices) {
        val colCells = columnCellIndexes[colIndex]
        for (digit in 1..9) {
            val matches = houseDigitMatches(state, colCells, digit)
            if (matches.size !in 2..3) continue
            val boxIndexes = matches.map { cellBoxIndexes[it] }.distinct()
            if (boxIndexes.size != 1) continue
            val boxIndex = boxIndexes[0]
            val eliminations = boxCellIndexes[boxIndex]
                .filter { cellCol(it) != colIndex && state.board[it] == 0 && (state.candidateMask[it] and digitToBit(digit)) != 0 }
            if (eliminations.isEmpty()) continue
            val (rows, cols, boxes) = highlightsFor(listOf(SudokuHouseRef("column", colIndex), SudokuHouseRef("box", boxIndex)))
            return SudokuEliminationHint(
                SudokuTechnique.BOX_LINE_REDUCTION, listOf(digit), SudokuHouseRef("column", colIndex), SudokuHouseRef("box", boxIndex),
                matches.map(::cellRef), eliminations.map(::cellRef), rows, cols, boxes,
            )
        }
    }
    return null
}

fun getSudokuNextMoveHint(board: SudokuGrid): SudokuNextMoveHint? {
    findSudokuInvalidState(board)?.let { return it }
    val state = buildBitmaskState(board)
    return findNakedSingle(state)
        ?: findHiddenSingle(state)
        ?: findNakedPair(state)
        ?: findHiddenPair(state)
        ?: findPointingPairTriple(state)
        ?: findBoxLineReduction(state)
}
