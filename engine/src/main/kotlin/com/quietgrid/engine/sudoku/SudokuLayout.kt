package com.quietgrid.engine.sudoku

class SudokuLayout private constructor(val size: Int, val boxRows: Int, val boxCols: Int) {
    val cellCount: Int = size * size
    val digits: IntRange = 1..size
    val fullMask: Int = (1 shl size) - 1

    fun index(row: Int, col: Int): Int = row * size + col
    fun rowOf(index: Int): Int = index / size
    fun colOf(index: Int): Int = index % size
    fun boxOf(row: Int, col: Int): Int = (row / boxRows) * (size / boxCols) + col / boxCols

    val rowCellIndexes: List<List<Int>> = (0 until size).map { row -> (0 until size).map { col -> index(row, col) } }
    val columnCellIndexes: List<List<Int>> = (0 until size).map { col -> (0 until size).map { row -> index(row, col) } }
    val boxCellIndexes: List<List<Int>> = (0 until size).map { box ->
        val rowStart = (box / (size / boxCols)) * boxRows
        val colStart = (box % (size / boxCols)) * boxCols
        (0 until size).map { offset -> index(rowStart + offset / boxCols, colStart + offset % boxCols) }
    }

    val cellRowIndexes: List<Int> = (0 until cellCount).map { rowOf(it) }
    val cellColIndexes: List<Int> = (0 until cellCount).map { colOf(it) }
    val cellBoxIndexes: List<Int> = (0 until cellCount).map { boxOf(cellRowIndexes[it], cellColIndexes[it]) }

    val cellPeers: List<List<Int>> = (0 until cellCount).map { index ->
        val peers = sortedSetOf<Int>()
        rowCellIndexes[cellRowIndexes[index]].forEach { peers.add(it) }
        columnCellIndexes[cellColIndexes[index]].forEach { peers.add(it) }
        boxCellIndexes[cellBoxIndexes[index]].forEach { peers.add(it) }
        peers.remove(index)
        peers.toList()
    }

    private val peerSets: List<Set<Int>> = cellPeers.map { it.toHashSet() }

    fun arePeers(left: Int, right: Int): Boolean = right in peerSets[left]

    val allHouseRefs: List<SudokuHouseCellsRef> =
        rowCellIndexes.mapIndexed { index, cells -> SudokuHouseCellsRef(SudokuHouseRef("row", index), cells) } +
            columnCellIndexes.mapIndexed { index, cells -> SudokuHouseCellsRef(SudokuHouseRef("column", index), cells) } +
            boxCellIndexes.mapIndexed { index, cells -> SudokuHouseCellsRef(SudokuHouseRef("box", index), cells) }

    companion object {
        val CLASSIC = SudokuLayout(9, 3, 3)
        val MINI_6 = SudokuLayout(6, 2, 3)
        val MINI_4 = SudokuLayout(4, 2, 2)

        fun forSize(size: Int): SudokuLayout = when (size) {
            9 -> CLASSIC
            6 -> MINI_6
            4 -> MINI_4
            else -> error("Unsupported sudoku size $size.")
        }
    }
}
