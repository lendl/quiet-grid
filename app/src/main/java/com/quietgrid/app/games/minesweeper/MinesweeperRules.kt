package com.quietgrid.app.games.minesweeper

import com.quietgrid.app.core.Difficulty
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

data class MinesweeperSizeProfile(val id: String, val rows: Int, val cols: Int)

private val SIZE_PROFILES = mapOf(
    Difficulty.EASY to MinesweeperSizeProfile("easy-11x12", 12, 11),
    Difficulty.MEDIUM to MinesweeperSizeProfile("medium-11x14", 14, 11),
    Difficulty.HARD to MinesweeperSizeProfile("hard-11x16", 16, 11),
    Difficulty.EXPERT to MinesweeperSizeProfile("expert-11x18", 18, 11),
)

private enum class Distribution { CLUSTER_LIMITED, MOSTLY_UNIFORM, RANDOM, CLUSTERED }

private data class Config(
    val rows: Int,
    val cols: Int,
    val densityMin: Double,
    val densityMax: Double,
    val targetDensity: Double,
    val distribution: Distribution,
    val openingMinRatio: Double?,
    val openingMaxRatio: Double?,
    val protectNeighbors: Boolean,
    val retryLimit: Int,
)

private data class DifficultyRules(
    val densityMin: Double,
    val densityMax: Double,
    val targetDensity: Double,
    val distribution: Distribution,
    val openingMinRatio: Double?,
    val openingMaxRatio: Double?,
    val protectNeighbors: Boolean,
    val retryLimit: Int,
)

private val DIFFICULTY_RULES = mapOf(
    Difficulty.EASY to DifficultyRules(0.10, 0.13, 0.12, Distribution.CLUSTER_LIMITED, 0.18, null, true, 500),
    Difficulty.MEDIUM to DifficultyRules(0.15, 0.18, 0.165, Distribution.MOSTLY_UNIFORM, 0.10, 0.18, false, 500),
    Difficulty.HARD to DifficultyRules(0.18, 0.22, 0.20, Distribution.RANDOM, 0.04, 0.10, false, 700),
    Difficulty.EXPERT to DifficultyRules(0.20, 0.25, 0.21, Distribution.CLUSTERED, 0.03, null, true, 120),
)

private fun buildConfig(difficulty: Difficulty, rows: Int, cols: Int): Config {
    val rules = DIFFICULTY_RULES.getValue(difficulty)
    return Config(
        rows = rows,
        cols = cols,
        densityMin = rules.densityMin,
        densityMax = rules.densityMax,
        targetDensity = rules.targetDensity,
        distribution = rules.distribution,
        openingMinRatio = rules.openingMinRatio,
        openingMaxRatio = rules.openingMaxRatio,
        protectNeighbors = rules.protectNeighbors,
        retryLimit = rules.retryLimit,
    )
}

private fun getMineCount(config: Config): Int {
    val cellCount = config.rows * config.cols
    val minCount = ceil(cellCount * config.densityMin).toInt()
    val maxCount = floor(cellCount * config.densityMax).toInt()
    val targetCount = round(cellCount * config.targetDensity).toInt()
    return min(maxCount, max(minCount, targetCount))
}

fun createMinesweeperPuzzle(difficulty: Difficulty): MinesweeperPuzzle {
    val profile = SIZE_PROFILES.getValue(difficulty)
    val config = buildConfig(difficulty, profile.rows, profile.cols)
    return MinesweeperPuzzle(
        difficulty = difficulty.key,
        profileId = profile.id,
        rows = profile.rows,
        cols = profile.cols,
        mines = getMineCount(config),
    )
}

fun createMinesweeperBoard(puzzle: MinesweeperPuzzle): MinesweeperBoard = MinesweeperBoard(
    rows = puzzle.rows,
    cols = puzzle.cols,
    mines = puzzle.mines,
    generated = false,
    status = MinesweeperStatus.PLAYING,
    cells = List(puzzle.rows) { List(puzzle.cols) { MinesweeperCell(isMine = false) } },
)

// --- Working (mutable) board used only during generation/reveal, converted back to the immutable model. ---

private class WorkingCell(var isMine: Boolean, var adjacentMines: Int = 0, var state: MinesweeperCellState = MinesweeperCellState.HIDDEN)
private class WorkingBoard(val rows: Int, val cols: Int, var mines: Int, var generated: Boolean, val cells: Array<Array<WorkingCell>>, var status: MinesweeperStatus)

private fun MinesweeperBoard.toWorking(): WorkingBoard = WorkingBoard(
    rows, cols, mines, generated,
    Array(rows) { r -> Array(cols) { c -> cells[r][c].let { WorkingCell(it.isMine, it.adjacentMines, it.state) } } },
    status,
)

private fun WorkingBoard.toImmutable(): MinesweeperBoard = MinesweeperBoard(
    rows, cols, mines, generated,
    (0 until rows).map { r -> (0 until cols).map { c -> cells[r][c].let { MinesweeperCell(it.isMine, it.adjacentMines, it.state) } } },
    status,
)

private fun WorkingBoard.deepCopy(): WorkingBoard = WorkingBoard(
    rows, cols, mines, generated,
    Array(rows) { r -> Array(cols) { c -> cells[r][c].let { WorkingCell(it.isMine, it.adjacentMines, it.state) } } },
    status,
)

private fun isInBounds(board: WorkingBoard, row: Int, col: Int): Boolean =
    row in 0 until board.rows && col in 0 until board.cols

private fun neighborCoords(board: WorkingBoard, row: Int, col: Int): List<Pair<Int, Int>> {
    val result = mutableListOf<Pair<Int, Int>>()
    for (rOff in -1..1) {
        for (cOff in -1..1) {
            if (rOff == 0 && cOff == 0) continue
            val nr = row + rOff
            val nc = col + cOff
            if (isInBounds(board, nr, nc)) result.add(nr to nc)
        }
    }
    return result
}

private fun countAdjacentMines(board: WorkingBoard, row: Int, col: Int): Int =
    neighborCoords(board, row, col).count { (r, c) -> board.cells[r][c].isMine }

private fun revealAllMines(board: WorkingBoard) {
    for (row in board.cells) for (cell in row) if (cell.isMine) cell.state = MinesweeperCellState.REVEALED
}

private fun computeStatus(board: WorkingBoard): MinesweeperStatus {
    for (row in board.cells) {
        for (cell in row) {
            if (!cell.isMine && cell.state != MinesweeperCellState.REVEALED) return MinesweeperStatus.PLAYING
        }
    }
    return MinesweeperStatus.WON
}

private fun revealSafeArea(board: WorkingBoard, startRow: Int, startCol: Int) {
    val queue = ArrayDeque<Pair<Int, Int>>()
    queue.add(startRow to startCol)
    val seen = mutableSetOf<Pair<Int, Int>>()

    while (queue.isNotEmpty()) {
        val (row, col) = queue.removeFirst()
        if (!seen.add(row to col)) continue

        val cell = board.cells[row][col]
        if (cell.state == MinesweeperCellState.FLAGGED || cell.state == MinesweeperCellState.REVEALED || cell.isMine) continue

        cell.state = MinesweeperCellState.REVEALED
        if (cell.adjacentMines != 0) continue

        for ((nr, nc) in neighborCoords(board, row, col)) {
            val neighbor = board.cells[nr][nc]
            if (!neighbor.isMine && neighbor.state != MinesweeperCellState.REVEALED) queue.add(nr to nc)
        }
    }
}

private fun countRevealedCells(board: WorkingBoard): Int =
    board.cells.sumOf { row -> row.count { it.state == MinesweeperCellState.REVEALED } }

private fun countAdjacentMinePairs(board: WorkingBoard): Int {
    var pairs = 0
    for (row in 0 until board.rows) {
        for (col in 0 until board.cols) {
            if (!board.cells[row][col].isMine) continue
            for ((nr, nc) in neighborCoords(board, row, col)) {
                if (nr < row || (nr == row && nc <= col)) continue
                if (board.cells[nr][nc].isMine) pairs++
            }
        }
    }
    return pairs
}

private fun getMaxWindowMineCount(board: WorkingBoard, windowRows: Int, windowCols: Int): Int {
    var maxCount = 0
    for (row in 0..(board.rows - windowRows)) {
        for (col in 0..(board.cols - windowCols)) {
            var count = 0
            for (rOff in 0 until windowRows) {
                for (cOff in 0 until windowCols) {
                    if (board.cells[row + rOff][col + cOff].isMine) count++
                }
            }
            maxCount = max(maxCount, count)
        }
    }
    return maxCount
}

private fun getProtectedIndexes(rows: Int, cols: Int, targetRow: Int, targetCol: Int, protectNeighbors: Boolean): Set<Int> {
    val protectedIndexes = mutableSetOf<Int>()
    for (rOff in -1..1) {
        for (cOff in -1..1) {
            if (!protectNeighbors && (rOff != 0 || cOff != 0)) continue
            val row = targetRow + rOff
            val col = targetCol + cOff
            if (row in 0 until rows && col in 0 until cols) protectedIndexes.add(row * cols + col)
        }
    }
    return protectedIndexes
}

private fun getRandomMineIndexes(rows: Int, cols: Int, mineCount: Int, protectedIndexes: Set<Int>): Set<Int> {
    val positions = (0 until rows * cols).filter { it !in protectedIndexes }.toMutableList()
    for (i in positions.size - 1 downTo 1) {
        val j = (0..i).random()
        val tmp = positions[i]; positions[i] = positions[j]; positions[j] = tmp
    }
    return positions.take(mineCount).toSet()
}

private fun createResolvedBoard(config: Config, mineIndexes: Set<Int>, mineCount: Int): WorkingBoard {
    val board = WorkingBoard(
        rows = config.rows,
        cols = config.cols,
        mines = mineCount,
        generated = true,
        status = MinesweeperStatus.PLAYING,
        cells = Array(config.rows) { r ->
            Array(config.cols) { c ->
                val flatIndex = r * config.cols + c
                WorkingCell(isMine = mineIndexes.contains(flatIndex))
            }
        },
    )
    for (row in 0 until board.rows) {
        for (col in 0 until board.cols) {
            val cell = board.cells[row][col]
            if (!cell.isMine) cell.adjacentMines = countAdjacentMines(board, row, col)
        }
    }
    return board
}

private fun meetsDistributionRules(board: WorkingBoard, config: Config, mineCount: Int): Boolean {
    if (config.distribution == Distribution.RANDOM || config.distribution == Distribution.CLUSTERED) return true

    val adjacentPairs = countAdjacentMinePairs(board)
    val maxWindowCount = getMaxWindowMineCount(board, 3, 3)

    return if (config.distribution == Distribution.CLUSTER_LIMITED) {
        adjacentPairs <= floor(mineCount * 0.7).toInt() && maxWindowCount <= 3
    } else {
        adjacentPairs <= floor(mineCount * 1.2).toInt() && maxWindowCount <= 4
    }
}

private fun meetsOpeningRules(board: WorkingBoard, row: Int, col: Int, config: Config): Boolean {
    if (config.openingMinRatio == null && config.openingMaxRatio == null) return true

    val simulated = board.deepCopy()
    revealSafeArea(simulated, row, col)
    val revealedCount = countRevealedCells(simulated)
    val cellCount = board.rows * board.cols
    val min = config.openingMinRatio?.let { floor(cellCount * it).toInt() }
    val max = config.openingMaxRatio?.let { ceil(cellCount * it).toInt() }

    if (min != null && revealedCount < min) return false
    if (max != null && revealedCount > max) return false
    return true
}

private fun countOpeningFrontier(board: WorkingBoard): Int {
    var frontier = 0
    for (row in 0 until board.rows) {
        for (col in 0 until board.cols) {
            val cell = board.cells[row][col]
            if (cell.state != MinesweeperCellState.REVEALED || cell.adjacentMines == 0) continue
            val touchesHidden = neighborCoords(board, row, col).any { (nr, nc) -> board.cells[nr][nc].state == MinesweeperCellState.HIDDEN }
            if (touchesHidden) frontier++
        }
    }
    return frontier
}

private fun meetsOpeningQualityRules(board: WorkingBoard, row: Int, col: Int, config: Config): Boolean {
    if (config.distribution != Distribution.CLUSTERED) return true
    val simulated = board.deepCopy()
    revealSafeArea(simulated, row, col)
    return countOpeningFrontier(simulated) >= 3
}

private fun finalizeGeneratedBoard(board: WorkingBoard, puzzle: MinesweeperPuzzle, row: Int, col: Int): WorkingBoard {
    if (board.generated) return board

    val difficulty = Difficulty.fromKey(puzzle.difficulty)
    val config = buildConfig(difficulty, board.rows, board.cols)
    val mineCount = board.mines
    val protectedIndexes = getProtectedIndexes(board.rows, board.cols, row, col, config.protectNeighbors)

    repeat(config.retryLimit) {
        val mineIndexes = getRandomMineIndexes(board.rows, board.cols, mineCount, protectedIndexes)
        val candidate = createResolvedBoard(config, mineIndexes, mineCount)

        if (!meetsDistributionRules(candidate, config, mineCount)) return@repeat
        if (!meetsOpeningRules(candidate, row, col, config)) return@repeat
        if (!meetsOpeningQualityRules(candidate, row, col, config)) return@repeat

        return candidate
    }

    error("Unable to generate ${puzzle.difficulty} Minesweeper board within retry limit.")
}

fun revealMinesweeperCell(board: MinesweeperBoard, puzzle: MinesweeperPuzzle, row: Int, col: Int): MinesweeperBoard {
    if (!(row in 0 until board.rows && col in 0 until board.cols) || board.status != MinesweeperStatus.PLAYING) return board

    var working = board.toWorking()
    if (!working.generated) working = finalizeGeneratedBoard(working, puzzle, row, col)

    val currentCell = working.cells[row][col]
    if (currentCell.state == MinesweeperCellState.FLAGGED || currentCell.state == MinesweeperCellState.REVEALED) {
        return working.toImmutable()
    }

    val next = working.deepCopy()
    val cell = next.cells[row][col]

    if (cell.isMine) {
        cell.state = MinesweeperCellState.REVEALED
        revealAllMines(next)
        next.status = MinesweeperStatus.LOST
        return next.toImmutable()
    }

    revealSafeArea(next, row, col)
    next.status = computeStatus(next)
    return next.toImmutable()
}

fun toggleMinesweeperFlag(board: MinesweeperBoard, row: Int, col: Int): MinesweeperBoard {
    if (!(row in 0 until board.rows && col in 0 until board.cols) || board.status != MinesweeperStatus.PLAYING || !board.generated) return board

    val cell = board.cells[row][col]
    if (cell.state == MinesweeperCellState.REVEALED) return board

    val nextState = if (cell.state == MinesweeperCellState.FLAGGED) MinesweeperCellState.HIDDEN else MinesweeperCellState.FLAGGED
    val newCells = board.cells.mapIndexed { r, rowCells ->
        rowCells.mapIndexed { c, c0 -> if (r == row && c == col) c0.copy(state = nextState) else c0 }
    }
    return board.copy(cells = newCells)
}

fun countFlaggedMinesweeperCells(board: MinesweeperBoard): Int =
    board.cells.sumOf { row -> row.count { it.state == MinesweeperCellState.FLAGGED } }
