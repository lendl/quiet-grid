// cli/src/main/kotlin/com/quietgrid/cli/sudoku/SudokuGenerator.kt
package com.quietgrid.cli.sudoku

import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.sudoku.SudokuPuzzleEntry
import com.quietgrid.engine.sudoku.SudokuSkillStep
import com.quietgrid.engine.sudoku.gradeSudoku
import kotlin.random.Random

private val TARGET_GIVEN_RANGES: Map<Difficulty, IntRange> = mapOf(
    Difficulty.EASY to 36..45, Difficulty.MEDIUM to 30..36, Difficulty.HARD to 26..32, Difficulty.EXPERT to 22..28,
)

private val MAX_STEP_FOR_TARGET: Map<Difficulty, SudokuSkillStep> = mapOf(
    Difficulty.EASY to SudokuSkillStep.NAKED_SINGLES,
    Difficulty.MEDIUM to SudokuSkillStep.HIDDEN_SINGLES,
    Difficulty.HARD to SudokuSkillStep.SUBSETS_AND_LOCKED_CANDIDATES,
    Difficulty.EXPERT to SudokuSkillStep.CHAINS_AND_COLORING,
)

private const val REMOVAL_PATH_ATTEMPTS = 8
private const val GENERATION_ATTEMPTS = 160

private fun shuffledDigits(random: Random): List<Int> = (1..9).shuffled(random)

private fun createSolvedBoard(random: Random): List<List<Int>> {
    val digits = shuffledDigits(random)
    val rowBands = listOf(0, 1, 2).shuffled(random)
    val colStacks = listOf(0, 1, 2).shuffled(random)
    val rows = rowBands.flatMap { band -> listOf(0, 1, 2).shuffled(random).map { band * 3 + it } }
    val cols = colStacks.flatMap { stack -> listOf(0, 1, 2).shuffled(random).map { stack * 3 + it } }
    return rows.map { row -> cols.map { col -> digits[(row * 3 + row / 3 + col) % 9] } }
}

fun countSudokuSolutions(givens: List<List<Int?>>, limit: Int): Int {
    val grid = givens.map { it.toMutableList() }.toMutableList()
    var count = 0

    fun isLegal(row: Int, col: Int, value: Int): Boolean {
        for (i in 0 until 9) if (grid[row][i] == value || grid[i][col] == value) return false
        val boxRow = row / 3 * 3
        val boxCol = col / 3 * 3
        for (r in boxRow until boxRow + 3) for (c in boxCol until boxCol + 3) if (grid[r][c] == value) return false
        return true
    }

    fun solve(pos: Int) {
        if (count >= limit) return
        if (pos == 81) { count += 1; return }
        val row = pos / 9
        val col = pos % 9
        if (grid[row][col] != null) { solve(pos + 1); return }
        for (value in 1..9) {
            if (isLegal(row, col, value)) {
                grid[row][col] = value
                solve(pos + 1)
                grid[row][col] = null
                if (count >= limit) return
            }
        }
    }

    solve(0)
    return count
}

private fun searchRemovalPath(solution: List<List<Int>>, targetDifficulty: Difficulty, random: Random): List<List<Int?>>? {
    val range = TARGET_GIVEN_RANGES.getValue(targetDifficulty)
    val maxStep = MAX_STEP_FOR_TARGET.getValue(targetDifficulty)
    val givens: MutableList<MutableList<Int?>> = solution.map { row -> row.map { it as Int? }.toMutableList() }.toMutableList()
    val removalOrder = (0 until 81).map { it / 9 to it % 9 }.shuffled(random)
    val candidates = mutableListOf<List<List<Int?>>>()

    for ((row, col) in removalOrder) {
        val previous = givens[row][col] ?: continue
        givens[row][col] = null
        val filledCells = givens.sumOf { r -> r.count { it != null } }
        if (filledCells < range.first) { givens[row][col] = previous; break }

        if (countSudokuSolutions(givens, limit = 2) != 1) { givens[row][col] = previous; continue }

        val grade = gradeSudoku(givens, maxStep)
        if (grade == null) { givens[row][col] = previous; continue }

        if (grade.difficulty == targetDifficulty && filledCells in range) candidates += givens.map { it.toList() }
    }
    return candidates.randomOrNull(random)
}

fun generateSudokuPuzzle(targetDifficulty: Difficulty, idPrefix: String, random: Random = Random.Default): SudokuPuzzleEntry? {
    repeat(GENERATION_ATTEMPTS) {
        val solution = createSolvedBoard(random)
        repeat(REMOVAL_PATH_ATTEMPTS) {
            val givens = searchRemovalPath(solution, targetDifficulty, random) ?: return@repeat
            return SudokuPuzzleEntry(
                id = "$idPrefix-${solution.hashCode()}",
                difficulty = targetDifficulty.key,
                givens = givens,
                solution = solution,
            )
        }
    }
    return null
}
