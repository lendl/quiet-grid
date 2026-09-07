// cli/src/main/kotlin/com/quietgrid/cli/nonogram/NonogramGenerator.kt
package com.quietgrid.cli.nonogram

import com.quietgrid.cli.GenerationState
import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.nonogram.NonogramPuzzleEntry
import com.quietgrid.engine.nonogram.analyzeNonogramDifficulty
import com.quietgrid.engine.nonogram.buildNonogramClues
import com.quietgrid.engine.nonogram.classifyNonogramDifficulty
import com.quietgrid.engine.nonogram.isDegenerateNonogramPuzzle
import kotlin.random.Random

fun nonogramSizesForDifficulty(difficulty: Difficulty): List<Pair<Int, Int>> = when (difficulty) {
    Difficulty.EASY -> listOf(10 to 5, 10 to 10)
    Difficulty.MEDIUM -> listOf(10 to 5, 10 to 10)
    Difficulty.HARD -> listOf(10 to 5, 10 to 10)
    // 10x5 can only reach expert via the rare PROBING path - classifyNonogramDifficulty
    // requires shortSide >= 10 to reach expert via chain depth alone, and 10x5's short side
    // is 5. Since 10x10 already covers the PROBING path too, including 10x5 here would just
    // waste roughly half of every generation attempt chasing a target it almost never hits.
    Difficulty.EXPERT -> listOf(10 to 10)
}

private const val CA_FILL_PROBABILITY = 0.45
private const val CA_ITERATIONS = 1
private const val CA_BIRTH_THRESHOLD = 4

private fun cellularAutomatonSolution(rows: Int, cols: Int, random: Random): List<List<Boolean>> {
    var grid = Array(rows) { BooleanArray(cols) { random.nextDouble() < CA_FILL_PROBABILITY } }
    repeat(CA_ITERATIONS) {
        val next = Array(rows) { BooleanArray(cols) }
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                var filledNeighbors = 0
                for (dr in -1..1) {
                    for (dc in -1..1) {
                        if (dr == 0 && dc == 0) continue
                        val nr = r + dr
                        val nc = c + dc
                        if (nr in 0 until rows && nc in 0 until cols && grid[nr][nc]) filledNeighbors += 1
                    }
                }
                next[r][c] = filledNeighbors >= CA_BIRTH_THRESHOLD
            }
        }
        grid = next
    }
    return grid.map { it.toList() }
}

private const val MIN_FILL_RATIO = 0.35
private const val MAX_FILL_RATIO = 0.75

fun generateRandomNonogramPuzzle(
    rows: Int,
    cols: Int,
    targetDifficulty: Difficulty,
    idPrefix: String,
    state: GenerationState,
    maxAttempts: Int = 1000,
): NonogramPuzzleEntry? {
    val random = Random(System.nanoTime() xor (rows * 31L + cols))
    var fillRatioRejects = 0
    var unsolvableRejects = 0
    var wrongDifficultyRejects = 0
    var degenerateRejects = 0

    repeat(maxAttempts) {
        val solution = cellularAutomatonSolution(rows, cols, random)

        val fillRatio = solution.sumOf { row -> row.count { it } }.toDouble() / (rows * cols)
        if (fillRatio < MIN_FILL_RATIO || fillRatio > MAX_FILL_RATIO) {
            fillRatioRejects += 1
            return@repeat
        }

        val rowClues = solution.map { buildNonogramClues(it) }
        val colClues = (0 until cols).map { c -> buildNonogramClues(solution.map { it[c] }) }

        val metrics = analyzeNonogramDifficulty(rowClues, colClues, solution)
        if (metrics == null) {
            unsolvableRejects += 1
            return@repeat
        }
        val difficulty = classifyNonogramDifficulty(rows, cols, metrics)
        if (difficulty != targetDifficulty) {
            wrongDifficultyRejects += 1
            return@repeat
        }
        if (isDegenerateNonogramPuzzle(rows, cols, difficulty, metrics)) {
            degenerateRejects += 1
            return@repeat
        }

        return NonogramPuzzleEntry(
            id = state.nextPuzzleId(idPrefix),
            difficulty = difficulty.key,
            rows = rows,
            cols = cols,
            solution = solution,
        )
    }

    System.err.println(
        "nonogram generation exhausted ${maxAttempts} attempts for ${rows}x${cols} $targetDifficulty: " +
            "fillRatioRejects=$fillRatioRejects unsolvableRejects=$unsolvableRejects " +
            "wrongDifficultyRejects=$wrongDifficultyRejects degenerateRejects=$degenerateRejects",
    )
    return null
}
