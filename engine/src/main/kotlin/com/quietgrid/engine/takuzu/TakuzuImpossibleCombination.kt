package com.quietgrid.engine.takuzu

private val validLinesBySize = mutableMapOf<Int, List<List<Int>>>()

private fun isValidCompletedLine(line: List<Int>): Boolean {
    val half = line.size / 2
    if (countValue(line, 0) != half || countValue(line, 1) != half) return false
    return noThreeConsec(line)
}

private fun getValidCompletedLines(size: Int): List<List<Int>> {
    validLinesBySize[size]?.let { return it }

    val lines = mutableListOf<List<Int>>()
    val totalMasks = 1 shl size
    for (mask in 0 until totalMasks) {
        val line = (0 until size).map { index -> (mask shr (size - index - 1)) and 1 }
        if (isValidCompletedLine(line)) lines.add(line)
    }

    validLinesBySize[size] = lines
    return lines
}

fun countValidLineCompletions(line: List<TakuzuCellValue>): Int =
    getValidCompletedLines(line.size).count { candidate ->
        line.indices.all { line[it] == null || candidate[it] == line[it] }
    }

/**
 * For each empty cell, tries both candidate values and runs the human-branch-proof on each. If
 * exactly one of the two branches contradicts, the other value is forced -- this is takuzu's
 * hardest/expert-tier canonical move, since it requires a hypothetical branch-and-prove step
 * rather than a direct pattern match.
 */
fun findImpossibleCombinationMove(board: TakuzuGrid): TakuzuMove? {
    val size = board.size
    for (row in 0 until size) {
        for (col in 0 until size) {
            if (board[row][col] != null) continue

            val zeroBoard = board.map { it.toMutableList() }
            zeroBoard[row][col] = 0
            val zeroProof = runHumanBranchProof(zeroBoard.map { it.toList() })

            val oneBoard = board.map { it.toMutableList() }
            oneBoard[row][col] = 1
            val oneProof = runHumanBranchProof(oneBoard.map { it.toList() })

            val zeroDead = zeroProof is TakuzuHumanProofResult.Contradiction
            val oneDead = oneProof is TakuzuHumanProofResult.Contradiction
            if (zeroDead == oneDead) continue

            val forcedValue = if (zeroDead) 1 else 0
            val blockedProof = if (zeroDead) zeroProof else oneProof
            if (blockedProof !is TakuzuHumanProofResult.Contradiction || blockedProof.steps.isEmpty()) continue

            return TakuzuMove(row, col, forcedValue, TakuzuTechnique.ELIMINATE_IMPOSSIBLE_COMBINATIONS)
        }
    }
    return null
}
