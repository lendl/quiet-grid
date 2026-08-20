// cli/src/main/kotlin/com/quietgrid/cli/sudoku/SudokuGenerator.kt
package com.quietgrid.cli.sudoku

import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.sudoku.SUDOKU_DIFFICULTY_PROFILES
import com.quietgrid.engine.sudoku.SudokuPuzzleEntry
import com.quietgrid.engine.sudoku.SudokuTechnique
import com.quietgrid.engine.sudoku.classifyDifficulty
import com.quietgrid.engine.sudoku.collectDifficultyMetrics
import com.quietgrid.engine.sudoku.sudokuTechniqueDifficultyFloor
import com.quietgrid.engine.sudoku.traceHumanSolve

private val TARGET_GIVEN_RANGES: Map<Difficulty, IntRange> = mapOf(
    Difficulty.EASY to 36..45, Difficulty.MEDIUM to 30..36, Difficulty.HARD to 26..32, Difficulty.EXPERT to 22..28,
)

private const val REMOVAL_PATH_ATTEMPTS = 8
private const val GENERATION_ATTEMPTS = 160

private fun shuffledDigits(): List<Int> = (1..9).shuffled()

private fun createSolvedBoard(): List<List<Int>> {
    val digits = shuffledDigits()
    val rowBands = listOf(0, 1, 2).shuffled()
    val colStacks = listOf(0, 1, 2).shuffled()
    val rows = rowBands.flatMap { band -> listOf(0, 1, 2).shuffled().map { band * 3 + it } }
    val cols = colStacks.flatMap { stack -> listOf(0, 1, 2).shuffled().map { stack * 3 + it } }
    return rows.map { row -> cols.map { col -> digits[(row * 3 + row / 3 + col) % 9] } }
}

private fun getAllowedTechniquesForDifficulty(difficulty: Difficulty): List<SudokuTechnique> =
    SudokuTechnique.entries.filter { sudokuTechniqueDifficultyFloor.getValue(it).ordinal <= difficulty.ordinal }

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

private fun classifyProgress(givens: List<List<Int?>>, targetDifficulty: Difficulty): Pair<Difficulty, Int>? {
    val trace = traceHumanSolve(givens, getAllowedTechniquesForDifficulty(targetDifficulty))
    if (!trace.solved) return null
    val metrics = collectDifficultyMetrics(trace.moves)
    val classification = classifyDifficulty(metrics) ?: return null
    return classification.difficulty to classification.score
}

private data class SudokuGenerationCandidate(val givens: List<List<Int?>>, val score: Int, val filledCells: Int)

private fun getTargetScoreCenter(targetDifficulty: Difficulty): Int {
    val profile = SUDOKU_DIFFICULTY_PROFILES.getValue(targetDifficulty)
    return if (profile.scoreMax != Int.MAX_VALUE) (profile.scoreMin + profile.scoreMax) / 2 else profile.scoreMin + 160
}

private fun isBetterCandidate(
    candidate: SudokuGenerationCandidate,
    candidateScoreDistance: Int,
    best: SudokuGenerationCandidate?,
    bestScoreDistance: Int,
): Boolean {
    if (best == null) return true
    if (candidateScoreDistance != bestScoreDistance) return candidateScoreDistance < bestScoreDistance
    if (candidate.filledCells != best.filledCells) return candidate.filledCells < best.filledCells
    return candidate.score > best.score
}

private fun searchRemovalPath(solution: List<List<Int>>, targetDifficulty: Difficulty): SudokuGenerationCandidate? {
    val range = TARGET_GIVEN_RANGES.getValue(targetDifficulty)
    val givens: MutableList<MutableList<Int?>> = solution.map { row -> row.map { it as Int? }.toMutableList() }.toMutableList()
    val removalOrder = (0 until 81).map { it / 9 to it % 9 }.shuffled()
    val targetScoreCenter = getTargetScoreCenter(targetDifficulty)
    var best: SudokuGenerationCandidate? = null
    var bestScoreDistance = Int.MAX_VALUE

    for ((row, col) in removalOrder) {
        val previous = givens[row][col] ?: continue
        givens[row][col] = null
        val filledCells = givens.sumOf { r -> r.count { it != null } }
        if (filledCells < range.first) { givens[row][col] = previous; break }

        if (countSudokuSolutions(givens, limit = 2) != 1) { givens[row][col] = previous; continue }

        val progress = classifyProgress(givens, targetDifficulty)
        if (progress == null || progress.first.ordinal > targetDifficulty.ordinal) { givens[row][col] = previous; continue }

        if (progress.first == targetDifficulty && filledCells in range) {
            val candidate = SudokuGenerationCandidate(givens.map { it.toList() }, progress.second, filledCells)
            val distance = kotlin.math.abs(progress.second - targetScoreCenter)
            if (isBetterCandidate(candidate, distance, best, bestScoreDistance)) {
                best = candidate
                bestScoreDistance = distance
            }
        }
    }
    return best
}

fun generateSudokuPuzzle(targetDifficulty: Difficulty, idPrefix: String): SudokuPuzzleEntry? {
    val targetScoreCenter = getTargetScoreCenter(targetDifficulty)
    repeat(GENERATION_ATTEMPTS) {
        val solution = createSolvedBoard()
        var best: SudokuGenerationCandidate? = null
        var bestScoreDistance = Int.MAX_VALUE
        repeat(REMOVAL_PATH_ATTEMPTS) {
            val candidate = searchRemovalPath(solution, targetDifficulty) ?: return@repeat
            val distance = kotlin.math.abs(candidate.score - targetScoreCenter)
            if (isBetterCandidate(candidate, distance, best, bestScoreDistance)) {
                best = candidate
                bestScoreDistance = distance
            }
        }
        val chosen = best ?: return@repeat
        return SudokuPuzzleEntry(
            id = "$idPrefix-${solution.hashCode()}",
            difficulty = targetDifficulty.key,
            givens = chosen.givens,
            solution = solution,
        )
    }
    return null
}
