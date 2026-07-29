// cli/src/main/kotlin/com/quietgrid/cli/nonogram/NonogramGenerator.kt
package com.quietgrid.cli.nonogram

import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.nonogram.NonogramPuzzleEntry
import com.quietgrid.engine.nonogram.analyzeNonogramDifficulty
import com.quietgrid.engine.nonogram.buildNonogramClues
import com.quietgrid.engine.nonogram.classifyNonogramDifficulty

private fun transpose(solution: List<List<Boolean>>): List<List<Boolean>> {
    val rows = solution.size
    val cols = solution.firstOrNull()?.size ?: 0
    return (0 until cols).map { c -> (0 until rows).map { r -> solution[r][c] } }
}

private fun mirrorHorizontally(solution: List<List<Boolean>>): List<List<Boolean>> = solution.map { it.reversed() }
private fun mirrorVertically(solution: List<List<Boolean>>): List<List<Boolean>> = solution.reversed()
private fun rotate180(solution: List<List<Boolean>>): List<List<Boolean>> = mirrorVertically(mirrorHorizontally(solution))

private fun applySymmetryVariant(solution: List<List<Boolean>>, variantIndex: Int): List<List<Boolean>> =
    when (((variantIndex % 4) + 4) % 4) {
        0 -> solution
        1 -> mirrorHorizontally(solution)
        2 -> mirrorVertically(solution)
        else -> rotate180(solution)
    }

private class SeededRng(seed: Long) {
    private var state = (seed xor 0x9e3779b9L) and 0xFFFFFFFFL
    fun next(): Double {
        state = (state * 1664525L + 1013904223L) and 0xFFFFFFFFL
        return state.toDouble() / 0x100000000L
    }
}

private fun shuffleIndexes(length: Int, seed: Long): List<Int> {
    val values = (0 until length).toMutableList()
    val rng = SeededRng(seed)
    for (index in values.indices.reversed()) {
        if (index == 0) break
        val swapIndex = (rng.next() * (index + 1)).toInt()
        val tmp = values[index]
        values[index] = values[swapIndex]
        values[swapIndex] = tmp
    }
    return values
}

private fun applySeededPermutation(solution: List<List<Boolean>>, seed: Long): List<List<Boolean>> {
    val rowOrder = shuffleIndexes(solution.size, seed * 31 + 7)
    val colOrder = shuffleIndexes(solution.firstOrNull()?.size ?: 0, seed * 17 + 13)
    return rowOrder.map { r -> colOrder.map { c -> solution[r][c] } }
}

/** Builds one difficulty-classified nonogram variant from [seedSolution] via symmetry + seeded permutation. */
fun generateNonogramVariant(
    seedSolution: List<List<Boolean>>,
    targetDifficulty: Difficulty,
    variantSeed: Long,
    idPrefix: String,
    maxAttempts: Int = 16,
): NonogramPuzzleEntry? {
    repeat(maxAttempts) { attempt ->
        val symmetrized = applySymmetryVariant(seedSolution, (variantSeed + attempt).toInt())
        val permutationSeed = (variantSeed + attempt) + symmetrized.size * 31L + (symmetrized.firstOrNull()?.size ?: 0) * 17L
        val solution = applySeededPermutation(symmetrized, permutationSeed)
        val rowClues = solution.map { buildNonogramClues(it) }
        val cols = solution.firstOrNull()?.size ?: 0
        val colClues = (0 until cols).map { c -> buildNonogramClues(solution.map { it[c] }) }

        val metrics = analyzeNonogramDifficulty(rowClues, colClues, solution) ?: return@repeat
        val difficulty = classifyNonogramDifficulty(solution.size, cols, metrics)
        if (difficulty != targetDifficulty) return@repeat

        return NonogramPuzzleEntry(
            id = "$idPrefix-${variantSeed + attempt}",
            difficulty = difficulty.key,
            rows = solution.size,
            cols = cols,
            solution = solution,
        )
    }
    return null
}
