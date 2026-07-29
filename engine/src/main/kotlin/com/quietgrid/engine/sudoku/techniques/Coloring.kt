package com.quietgrid.engine.sudoku.techniques

import com.quietgrid.engine.core.Difficulty
import com.quietgrid.engine.sudoku.*
import java.util.ArrayDeque

object ColoringTechnique : SudokuTechniqueDispatcher {
    override val technique = SudokuTechnique.COLORING
    override val tier = Difficulty.EXPERT

    private fun addEdge(graph: MutableMap<Int, MutableSet<Int>>, left: Int, right: Int) {
        graph.getOrPut(left) { mutableSetOf() }.add(right)
        graph.getOrPut(right) { mutableSetOf() }.add(left)
    }

    override fun findMove(state: SudokuBitmaskState): SudokuCanonicalMove? {
        var best: SudokuCanonicalMove? = null
        var bestComplexity = Int.MAX_VALUE

        for (digit in 1..9) {
            val graph = mutableMapOf<Int, MutableSet<Int>>()
            (rowCellIndexes + columnCellIndexes + boxCellIndexes).forEach { houseCells ->
                val matches = getHouseDigitMatches(state, houseCells, digit)
                if (matches.size == 2) addEdge(graph, matches[0], matches[1])
            }

            val seen = mutableSetOf<Int>()
            for (start in graph.keys) {
                if (start in seen) continue

                val colors = mutableMapOf(start to 0)
                val queue = ArrayDeque<Int>()
                queue.add(start)
                val component = mutableListOf<Int>()

                while (queue.isNotEmpty()) {
                    val current = queue.poll()
                    if (current in seen) continue
                    seen.add(current)
                    component.add(current)
                    val currentColor = colors[current] ?: 0
                    (graph[current] ?: emptySet()).forEach { neighbor ->
                        if (neighbor !in colors) colors[neighbor] = if (currentColor == 0) 1 else 0
                        if (neighbor !in seen) queue.add(neighbor)
                    }
                }

                val complexity = component.size
                if (complexity >= bestComplexity) continue

                for (color in listOf(0, 1)) {
                    val colorCells = component.filter { colors[it] == color }
                    val hasConflict = colorCells.indices.any { leftIndex ->
                        colorCells.drop(leftIndex + 1).any { right -> cellPeers[colorCells[leftIndex]].contains(right) }
                    }
                    if (!hasConflict) continue

                    val move = buildCandidateEliminationMove(
                        SudokuTechnique.COLORING, colorCells.map { it to digit }, component, collectHousesFromIndexes(component), complexity,
                    )
                    if (move != null) { best = move; bestComplexity = complexity; break }
                }
                if (complexity >= bestComplexity) continue

                val colorZero = component.filter { colors[it] == 0 }
                val colorOne = component.filter { colors[it] == 1 }
                val eliminations = state.board.indices
                    .filter { state.board[it] == 0 && it !in component }
                    .filter { getHouseDigitMatches(state, listOf(it), digit).size == 1 }
                    .filter { index -> colorZero.any { cellPeers[index].contains(it) } && colorOne.any { cellPeers[index].contains(it) } }
                    .map { it to digit }
                val move = buildCandidateEliminationMove(SudokuTechnique.COLORING, eliminations, component, collectHousesFromIndexes(component), complexity)
                if (move != null) { best = move; bestComplexity = complexity }
            }
        }
        return best
    }
}
