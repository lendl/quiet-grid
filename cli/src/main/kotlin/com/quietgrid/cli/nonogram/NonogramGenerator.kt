// cli/src/main/kotlin/com/quietgrid/cli/nonogram/NonogramGenerator.kt
package com.quietgrid.cli.nonogram

import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.nonogram.NonogramPuzzleEntry
import com.quietgrid.engine.nonogram.analyzeNonogramDifficulty
import com.quietgrid.engine.nonogram.buildNonogramClues
import com.quietgrid.engine.nonogram.classifyNonogramDifficulty
import kotlin.random.Random

fun nonogramSizesForDifficulty(difficulty: Difficulty): List<Pair<Int, Int>> = when (difficulty) {
    Difficulty.EASY -> listOf(5 to 5, 10 to 5)
    Difficulty.MEDIUM -> listOf(5 to 5, 10 to 5)
    Difficulty.HARD -> listOf(5 to 5, 10 to 5)
    Difficulty.EXPERT -> listOf(10 to 5, 10 to 10)
}

private fun stripeSolution(rows: Int, cols: Int, random: Random): List<List<Boolean>> {
    var rowStates: BooleanArray
    do {
        rowStates = BooleanArray(rows) { random.nextBoolean() }
    } while (rowStates.all { it } || rowStates.none { it })
    return (0 until rows).map { r -> List(cols) { rowStates[r] } }
}

private fun blockStampSolution(rows: Int, cols: Int, random: Random): List<List<Boolean>> {
    val grid = Array(rows) { BooleanArray(cols) }
    val blockCount = 1 + random.nextInt(12)
    repeat(blockCount) {
        val maxDim = maxOf(2, minOf(rows, cols) / 2)
        val blockRows = 1 + random.nextInt(minOf(rows, maxDim))
        val blockCols = 1 + random.nextInt(minOf(cols, maxDim))
        val startRow = random.nextInt(rows - blockRows + 1)
        val startCol = random.nextInt(cols - blockCols + 1)
        for (r in startRow until startRow + blockRows) {
            for (c in startCol until startCol + blockCols) {
                grid[r][c] = true
            }
        }
    }
    return grid.map { it.toList() }
}

fun generateRandomNonogramPuzzle(
    rows: Int,
    cols: Int,
    targetDifficulty: Difficulty,
    idPrefix: String,
    maxAttempts: Int = 300,
): NonogramPuzzleEntry? {
    val random = Random(System.nanoTime() xor (rows * 31L + cols))
    repeat(maxAttempts) {
        val solution = if (targetDifficulty == Difficulty.EASY) {
            stripeSolution(rows, cols, random)
        } else {
            blockStampSolution(rows, cols, random)
        }
        val rowClues = solution.map { buildNonogramClues(it) }
        val colClues = (0 until cols).map { c -> buildNonogramClues(solution.map { it[c] }) }

        val metrics = analyzeNonogramDifficulty(rowClues, colClues, solution) ?: return@repeat
        val difficulty = classifyNonogramDifficulty(rows, cols, metrics)
        if (difficulty != targetDifficulty) return@repeat

        return NonogramPuzzleEntry(
            id = "$idPrefix-${System.nanoTime()}",
            difficulty = difficulty.key,
            rows = rows,
            cols = cols,
            solution = solution,
        )
    }
    return null
}
