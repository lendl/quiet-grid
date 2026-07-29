package com.quietgrid.engine.sudoku

const val FULL_MASK = 0b111111111

val digitToBit: IntArray = IntArray(10) { digit -> if (digit in 1..9) 1 shl (digit - 1) else 0 }

val popcount: IntArray = IntArray(512) { mask -> Integer.bitCount(mask) }

val bitToDigit: IntArray = IntArray(512) { mask -> if (popcount[mask] == 1) (31 - Integer.numberOfLeadingZeros(mask)) + 1 else 0 }

fun getCellIndex(row: Int, col: Int): Int = row * SUDOKU_SIZE + col
fun getCellRow(index: Int): Int = index / SUDOKU_SIZE
fun getCellCol(index: Int): Int = index % SUDOKU_SIZE
fun getCellBox(row: Int, col: Int): Int = (row / SUDOKU_BOX_SIZE) * SUDOKU_BOX_SIZE + col / SUDOKU_BOX_SIZE

val rowCellIndexes: List<List<Int>> = (0 until SUDOKU_SIZE).map { row -> (0 until SUDOKU_SIZE).map { col -> getCellIndex(row, col) } }
val columnCellIndexes: List<List<Int>> = (0 until SUDOKU_SIZE).map { col -> (0 until SUDOKU_SIZE).map { row -> getCellIndex(row, col) } }
val boxCellIndexes: List<List<Int>> = (0 until SUDOKU_SIZE).map { box ->
    val rowStart = (box / SUDOKU_BOX_SIZE) * SUDOKU_BOX_SIZE
    val colStart = (box % SUDOKU_BOX_SIZE) * SUDOKU_BOX_SIZE
    (0 until SUDOKU_SIZE).map { offset -> getCellIndex(rowStart + offset / SUDOKU_BOX_SIZE, colStart + offset % SUDOKU_BOX_SIZE) }
}

val cellRowIndexes: List<Int> = (0 until SUDOKU_SIZE * SUDOKU_SIZE).map { getCellRow(it) }
val cellColIndexes: List<Int> = (0 until SUDOKU_SIZE * SUDOKU_SIZE).map { getCellCol(it) }
val cellBoxIndexes: List<Int> = (0 until SUDOKU_SIZE * SUDOKU_SIZE).map { getCellBox(cellRowIndexes[it], cellColIndexes[it]) }

val cellPeers: List<List<Int>> = (0 until SUDOKU_SIZE * SUDOKU_SIZE).map { index ->
    val peers = sortedSetOf<Int>()
    rowCellIndexes[cellRowIndexes[index]].forEach { peers.add(it) }
    columnCellIndexes[cellColIndexes[index]].forEach { peers.add(it) }
    boxCellIndexes[cellBoxIndexes[index]].forEach { peers.add(it) }
    peers.remove(index)
    peers.toList()
}

class SudokuBitmaskState(
    val board: IntArray,
    val candidateMask: IntArray,
    val rowMask: IntArray,
    val colMask: IntArray,
    val boxMask: IntArray,
    var unresolvedCount: Int,
)

fun flattenBoard(board: SudokuGrid): IntArray = IntArray(81) { index -> board[cellRowIndexes[index]][cellColIndexes[index]] ?: 0 }

fun createBitmaskStateFromFlatBoard(flatBoard: IntArray): SudokuBitmaskState {
    val boardCopy = flatBoard.copyOf()
    val candidateMask = IntArray(81)
    val rowMask = IntArray(9)
    val colMask = IntArray(9)
    val boxMask = IntArray(9)

    for (index in 0 until 81) {
        val value = boardCopy[index]
        if (value == 0) continue
        val bit = digitToBit[value]
        val row = cellRowIndexes[index]
        val col = cellColIndexes[index]
        val box = cellBoxIndexes[index]
        check((rowMask[row] and bit) == 0 && (colMask[col] and bit) == 0 && (boxMask[box] and bit) == 0) {
            "Invalid sudoku board: duplicate digit $value at row ${row + 1}, col ${col + 1}."
        }
        rowMask[row] = rowMask[row] or bit
        colMask[col] = colMask[col] or bit
        boxMask[box] = boxMask[box] or bit
    }

    var unresolvedCount = 0
    for (index in 0 until 81) {
        if (boardCopy[index] != 0) continue
        val row = cellRowIndexes[index]
        val col = cellColIndexes[index]
        val box = cellBoxIndexes[index]
        candidateMask[index] = FULL_MASK and (rowMask[row] or colMask[col] or boxMask[box]).inv()
        unresolvedCount += 1
    }

    return SudokuBitmaskState(boardCopy, candidateMask, rowMask, colMask, boxMask, unresolvedCount)
}

fun createBitmaskStateFromBoard(board: SudokuGrid): SudokuBitmaskState = createBitmaskStateFromFlatBoard(flattenBoard(board))

fun cloneBitmaskState(state: SudokuBitmaskState): SudokuBitmaskState = SudokuBitmaskState(
    state.board.copyOf(), state.candidateMask.copyOf(), state.rowMask.copyOf(), state.colMask.copyOf(), state.boxMask.copyOf(), state.unresolvedCount,
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

    val bit = digitToBit[digit]
    val row = cellRowIndexes[index]
    val col = cellColIndexes[index]
    val box = cellBoxIndexes[index]
    check((state.rowMask[row] and bit) == 0 && (state.colMask[col] and bit) == 0 && (state.boxMask[box] and bit) == 0) {
        "Cannot place conflicting digit $digit at row ${row + 1}, col ${col + 1}."
    }

    state.board[index] = digit
    state.candidateMask[index] = 0
    state.rowMask[row] = state.rowMask[row] or bit
    state.colMask[col] = state.colMask[col] or bit
    state.boxMask[box] = state.boxMask[box] or bit
    state.unresolvedCount -= 1

    cellPeers[index].forEach { peerIndex ->
        if (state.board[peerIndex] == 0) state.candidateMask[peerIndex] = state.candidateMask[peerIndex] and bit.inv()
    }
}

fun eliminateCandidate(state: SudokuBitmaskState, index: Int, digit: Int) {
    if (state.board[index] != 0) return
    state.candidateMask[index] = state.candidateMask[index] and digitToBit[digit].inv()
}

fun isSolved(state: SudokuBitmaskState): Boolean = state.unresolvedCount == 0
