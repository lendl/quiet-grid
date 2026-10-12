package com.quietgrid.engine.sudoku

const val FULL_MASK = 0b111111111

val digitToBit: IntArray = IntArray(10) { digit -> if (digit in 1..9) 1 shl (digit - 1) else 0 }

val popcount: IntArray = IntArray(512) { mask -> Integer.bitCount(mask) }

val bitToDigit: IntArray = IntArray(512) { mask -> if (popcount[mask] == 1) (31 - Integer.numberOfLeadingZeros(mask)) + 1 else 0 }

fun getCellIndex(row: Int, col: Int): Int = SudokuLayout.CLASSIC.index(row, col)
fun getCellRow(index: Int): Int = SudokuLayout.CLASSIC.rowOf(index)
fun getCellCol(index: Int): Int = SudokuLayout.CLASSIC.colOf(index)
fun getCellBox(row: Int, col: Int): Int = SudokuLayout.CLASSIC.boxOf(row, col)

val rowCellIndexes: List<List<Int>> get() = SudokuLayout.CLASSIC.rowCellIndexes
val columnCellIndexes: List<List<Int>> get() = SudokuLayout.CLASSIC.columnCellIndexes
val boxCellIndexes: List<List<Int>> get() = SudokuLayout.CLASSIC.boxCellIndexes

val cellRowIndexes: List<Int> get() = SudokuLayout.CLASSIC.cellRowIndexes
val cellColIndexes: List<Int> get() = SudokuLayout.CLASSIC.cellColIndexes
val cellBoxIndexes: List<Int> get() = SudokuLayout.CLASSIC.cellBoxIndexes

val cellPeers: List<List<Int>> get() = SudokuLayout.CLASSIC.cellPeers

class SudokuBitmaskState(
    val board: IntArray,
    val candidateMask: IntArray,
    val rowMask: IntArray,
    val colMask: IntArray,
    val boxMask: IntArray,
    var unresolvedCount: Int,
    val layout: SudokuLayout = SudokuLayout.CLASSIC,
)

fun flattenBoard(board: SudokuGrid): IntArray {
    val layout = SudokuLayout.forSize(board.size)
    return IntArray(layout.cellCount) { index -> board[layout.rowOf(index)][layout.colOf(index)] ?: 0 }
}

fun createBitmaskStateFromFlatBoard(flatBoard: IntArray, layout: SudokuLayout = SudokuLayout.CLASSIC): SudokuBitmaskState {
    val boardCopy = flatBoard.copyOf()
    val candidateMask = IntArray(layout.cellCount)
    val rowMask = IntArray(layout.size)
    val colMask = IntArray(layout.size)
    val boxMask = IntArray(layout.size)

    for (index in 0 until layout.cellCount) {
        val value = boardCopy[index]
        if (value == 0) continue
        check(value in layout.digits) { "Invalid sudoku board: digit $value is out of range." }
        val bit = digitToBit[value]
        val row = layout.cellRowIndexes[index]
        val col = layout.cellColIndexes[index]
        val box = layout.cellBoxIndexes[index]
        check((rowMask[row] and bit) == 0 && (colMask[col] and bit) == 0 && (boxMask[box] and bit) == 0) {
            "Invalid sudoku board: duplicate digit $value at row ${row + 1}, col ${col + 1}."
        }
        rowMask[row] = rowMask[row] or bit
        colMask[col] = colMask[col] or bit
        boxMask[box] = boxMask[box] or bit
    }

    var unresolvedCount = 0
    for (index in 0 until layout.cellCount) {
        if (boardCopy[index] != 0) continue
        val row = layout.cellRowIndexes[index]
        val col = layout.cellColIndexes[index]
        val box = layout.cellBoxIndexes[index]
        candidateMask[index] = layout.fullMask and (rowMask[row] or colMask[col] or boxMask[box]).inv()
        unresolvedCount += 1
    }

    return SudokuBitmaskState(boardCopy, candidateMask, rowMask, colMask, boxMask, unresolvedCount, layout)
}

fun createBitmaskStateFromBoard(board: SudokuGrid): SudokuBitmaskState =
    createBitmaskStateFromFlatBoard(flattenBoard(board), SudokuLayout.forSize(board.size))

fun hasDuplicateGivens(board: SudokuGrid): Boolean {
    val layout = SudokuLayout.forSize(board.size)
    val flatBoard = flattenBoard(board)
    val houseMasks = IntArray(layout.size * 3)
    for (index in 0 until layout.cellCount) {
        val value = flatBoard[index]
        if (value == 0) continue
        val bit = digitToBit[value]
        val houses = intArrayOf(
            layout.cellRowIndexes[index],
            layout.size + layout.cellColIndexes[index],
            layout.size * 2 + layout.cellBoxIndexes[index],
        )
        if (houses.any { (houseMasks[it] and bit) != 0 }) return true
        houses.forEach { houseMasks[it] = houseMasks[it] or bit }
    }
    return false
}

fun cloneBitmaskState(state: SudokuBitmaskState): SudokuBitmaskState = SudokuBitmaskState(
    state.board.copyOf(), state.candidateMask.copyOf(), state.rowMask.copyOf(), state.colMask.copyOf(), state.boxMask.copyOf(), state.unresolvedCount, state.layout,
)

fun encodeBitmaskState(state: SudokuBitmaskState): String =
    "${state.board.joinToString("")}:${state.candidateMask.joinToString(",")}"

fun hasCandidateAtIndex(state: SudokuBitmaskState, index: Int, digit: Int): Boolean =
    state.board[index] == 0 && (state.candidateMask[index] and digitToBit[digit]) != 0

fun iterateMaskDigits(mask: Int): List<Int> = (1..9).filter { (mask and digitToBit[it]) != 0 }

fun placeDigit(state: SudokuBitmaskState, index: Int, digit: Int) {
    val current = state.board[index]
    if (current == digit) return
    check(current == 0) { "Cannot place digit $digit into filled sudoku cell $index." }

    val layout = state.layout
    val bit = digitToBit[digit]
    val row = layout.cellRowIndexes[index]
    val col = layout.cellColIndexes[index]
    val box = layout.cellBoxIndexes[index]
    check((state.rowMask[row] and bit) == 0 && (state.colMask[col] and bit) == 0 && (state.boxMask[box] and bit) == 0) {
        "Cannot place conflicting digit $digit at row ${row + 1}, col ${col + 1}."
    }

    state.board[index] = digit
    state.candidateMask[index] = 0
    state.rowMask[row] = state.rowMask[row] or bit
    state.colMask[col] = state.colMask[col] or bit
    state.boxMask[box] = state.boxMask[box] or bit
    state.unresolvedCount -= 1

    layout.cellPeers[index].forEach { peerIndex ->
        if (state.board[peerIndex] == 0) state.candidateMask[peerIndex] = state.candidateMask[peerIndex] and bit.inv()
    }
}

fun eliminateCandidate(state: SudokuBitmaskState, index: Int, digit: Int) {
    if (state.board[index] != 0) return
    state.candidateMask[index] = state.candidateMask[index] and digitToBit[digit].inv()
}

fun isSolved(state: SudokuBitmaskState): Boolean = state.unresolvedCount == 0
