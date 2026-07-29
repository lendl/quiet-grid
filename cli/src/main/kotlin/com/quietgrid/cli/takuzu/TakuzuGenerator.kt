package com.quietgrid.cli.takuzu

import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.takuzu.TAKUZU_SUPPORTED_SIZES
import com.quietgrid.engine.takuzu.TakuzuGrid
import com.quietgrid.engine.takuzu.TakuzuPuzzleEntry
import com.quietgrid.engine.takuzu.analyzeTakuzuDifficulty
import com.quietgrid.engine.takuzu.classifyTakuzuDifficulty
import com.quietgrid.engine.takuzu.computeTakuzuDifficultyScore
import com.quietgrid.engine.takuzu.countSolutions
import com.quietgrid.engine.takuzu.generateSolvedGrid
import com.quietgrid.engine.takuzu.getTakuzuTargetRevealBounds
import com.quietgrid.engine.takuzu.gridToHex
import com.quietgrid.engine.takuzu.maskToHex
import com.quietgrid.engine.takuzu.passesTakuzuDifficultyRails
import com.quietgrid.engine.takuzu.TakuzuDifficultyStalledException
import kotlin.random.Random

private fun buildMaskFromRevealed(size: Int, revealed: Set<Int>): List<List<Boolean>> =
    List(size) { row -> List(size) { col -> revealed.contains(row * size + col) } }

private fun buildPuzzleFromRevealed(grid: TakuzuGrid, revealed: Set<Int>): TakuzuGrid {
    val size = grid.size
    return grid.mapIndexed { r, row -> row.mapIndexed { c, value -> if (revealed.contains(r * size + c)) value else null } }
}

private fun evaluateCandidate(grid: TakuzuGrid, revealed: Set<Int>, targetDifficulty: Difficulty): Pair<Difficulty, Int>? {
    val size = grid.size
    val puzzle = buildPuzzleFromRevealed(grid, revealed)
    if (countSolutions(puzzle) != 1) return null

    val metrics = try {
        analyzeTakuzuDifficulty(puzzle, grid)
    } catch (stalled: TakuzuDifficultyStalledException) {
        return null
    }

    val score = computeTakuzuDifficultyScore(size, metrics)
    val difficulty = classifyTakuzuDifficulty(size, metrics, score) ?: return null
    if (!passesTakuzuDifficultyRails(size, targetDifficulty, metrics)) return null
    if (difficulty > targetDifficulty) return null
    return difficulty to score
}

private fun generateSparseMask(grid: TakuzuGrid, minReveal: Int, maxReveal: Int, targetDifficulty: Difficulty): List<List<Boolean>>? {
    val size = grid.size
    val total = size * size
    val targetReveal = Random.nextInt(minReveal, maxReveal + 1)
    val revealed = (0 until total).toMutableSet()
    var progress = true

    while (progress && revealed.size > minReveal) {
        progress = false
        val indices = revealed.toList().shuffled()

        for (idx in indices) {
            if (revealed.size <= minReveal) break
            revealed.remove(idx)

            val evaluation = evaluateCandidate(grid, revealed, targetDifficulty)
            if (evaluation == null) {
                revealed.add(idx)
                continue
            }
            progress = true
            if (revealed.size <= targetReveal && evaluation.first == targetDifficulty) break
        }

        val current = evaluateCandidate(grid, revealed, targetDifficulty)
        if (revealed.size <= targetReveal && current?.first == targetDifficulty) break
    }

    if (revealed.size < minReveal || revealed.size > maxReveal) return null
    val final = evaluateCandidate(grid, revealed, targetDifficulty)
    if (final == null || final.first != targetDifficulty) return null
    return buildMaskFromRevealed(size, revealed)
}

/** Generates a single takuzu puzzle at [size]/[targetDifficulty], retrying internally on failed attempts. */
fun generateTakuzuPuzzle(size: Int, targetDifficulty: Difficulty, idPrefix: String, maxAttempts: Int = 40): TakuzuPuzzleEntry? {
    require(size in TAKUZU_SUPPORTED_SIZES) {
        "Unsupported takuzu puzzle size: $size. Supported sizes: $TAKUZU_SUPPORTED_SIZES"
    }

    val (minReveal, maxReveal) = getTakuzuTargetRevealBounds(size, targetDifficulty)

    repeat(maxAttempts) {
        val grid = generateSolvedGrid(size) ?: return@repeat
        val mask = generateSparseMask(grid, minReveal, maxReveal, targetDifficulty) ?: return@repeat
        return TakuzuPuzzleEntry(
            id = "$idPrefix-${gridToHex(grid).take(12)}",
            size = size,
            difficulty = targetDifficulty.key,
            solution = gridToHex(grid),
            mask = maskToHex(mask),
        )
    }
    return null
}
