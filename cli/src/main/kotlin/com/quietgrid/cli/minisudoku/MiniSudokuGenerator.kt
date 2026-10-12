package com.quietgrid.cli.minisudoku

import com.quietgrid.cli.sudoku.countSudokuSolutions
import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.sudoku.MINI_SUDOKU_SIZE_BY_DIFFICULTY
import com.quietgrid.engine.sudoku.SudokuLayout
import com.quietgrid.engine.sudoku.SudokuPuzzleEntry
import com.quietgrid.engine.sudoku.SudokuSkillStep
import com.quietgrid.engine.sudoku.gradeMiniSudoku
import kotlin.random.Random

private const val GENERATION_ATTEMPTS = 400
private const val REMOVAL_PATH_ATTEMPTS = 6

private val TARGET_GIVEN_RANGES: Map<Difficulty, IntRange> = mapOf(
    Difficulty.EASY to 5..7,
    Difficulty.MEDIUM to 14..17,
    Difficulty.HARD to 8..13,
    Difficulty.EXPERT to 8..13,
)

private val MAX_STEP_FOR_TARGET: Map<Difficulty, SudokuSkillStep> = mapOf(
    Difficulty.EASY to SudokuSkillStep.NAKED_SINGLES,
    Difficulty.MEDIUM to SudokuSkillStep.NAKED_SINGLES,
    Difficulty.HARD to SudokuSkillStep.HIDDEN_SINGLES,
    Difficulty.EXPERT to SudokuSkillStep.CHAINS_AND_COLORING,
)

fun createMiniSudokuSolution(size: Int, random: Random = Random.Default): List<List<Int>> {
    val layout = SudokuLayout.forSize(size)
    val grid = Array(size) { IntArray(size) }

    fun isLegal(row: Int, col: Int, value: Int): Boolean {
        val index = layout.index(row, col)
        return layout.cellPeers[index].none { grid[layout.rowOf(it)][layout.colOf(it)] == value }
    }

    fun fill(pos: Int): Boolean {
        if (pos == layout.cellCount) return true
        val row = pos / size
        val col = pos % size
        for (value in layout.digits.shuffled(random)) {
            if (!isLegal(row, col, value)) continue
            grid[row][col] = value
            if (fill(pos + 1)) return true
            grid[row][col] = 0
        }
        return false
    }

    check(fill(0)) { "Could not fill a $size x $size sudoku." }
    return grid.map { it.toList() }
}

private fun searchRemovalPath(solution: List<List<Int>>, targetDifficulty: Difficulty, random: Random): List<List<Int?>>? {
    val size = solution.size
    val range = TARGET_GIVEN_RANGES.getValue(targetDifficulty)
    val maxStep = MAX_STEP_FOR_TARGET.getValue(targetDifficulty)
    val givens: MutableList<MutableList<Int?>> = solution.map { row -> row.map { it as Int? }.toMutableList() }.toMutableList()
    val candidates = mutableListOf<List<List<Int?>>>()

    for (index in (0 until size * size).shuffled(random)) {
        val row = index / size
        val col = index % size
        val previous = givens[row][col] ?: continue
        givens[row][col] = null
        val filledCells = givens.sumOf { r -> r.count { it != null } }
        if (filledCells < range.first) { givens[row][col] = previous; break }

        if (countSudokuSolutions(givens, limit = 2) != 1) { givens[row][col] = previous; continue }

        val grade = gradeMiniSudoku(givens, maxStep)
        if (grade == null) { givens[row][col] = previous; continue }

        if (grade.difficulty == targetDifficulty && filledCells in range) candidates += givens.map { it.toList() }
    }
    return candidates.randomOrNull(random)
}

fun generateMiniSudokuPuzzle(targetDifficulty: Difficulty, random: Random = Random.Default): SudokuPuzzleEntry? {
    val size = MINI_SUDOKU_SIZE_BY_DIFFICULTY.getValue(targetDifficulty)
    repeat(GENERATION_ATTEMPTS) {
        val solution = createMiniSudokuSolution(size, random)
        repeat(REMOVAL_PATH_ATTEMPTS) {
            val givens = searchRemovalPath(solution, targetDifficulty, random) ?: return@repeat
            return SudokuPuzzleEntry(
                id = "m$size-${givens.hashCode().toUInt().toString(16)}",
                difficulty = targetDifficulty.key,
                givens = givens,
                solution = solution,
            )
        }
    }
    return null
}
