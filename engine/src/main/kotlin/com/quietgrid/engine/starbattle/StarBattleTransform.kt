package com.quietgrid.engine.starbattle

import com.quietgrid.engine.journey.GridTransform
import com.quietgrid.engine.journey.gridTransformsFor
import com.quietgrid.engine.journey.mapCell

fun starBattleReplayCandidates(entry: StarBattlePuzzleEntry): List<GridTransform> =
    gridTransformsFor(entry.size, entry.size) - GridTransform.IDENTITY

fun StarBattlePuzzleEntry.transformed(t: GridTransform): StarBattlePuzzleEntry {
    if (t == GridTransform.IDENTITY) return this
    val newRegions = List(size) { MutableList(size) { 0 } }
    for (row in 0 until size) {
        for (col in 0 until size) {
            val (r, c) = t.mapCell(row, col, size, size)
            newRegions[r][c] = regions[row][col]
        }
    }
    val newSolution = List(size) { mutableListOf<Int>() }
    solution.forEachIndexed { row, cols ->
        cols.forEach { col ->
            val (r, c) = t.mapCell(row, col, size, size)
            newSolution[r] += c
        }
    }
    return copy(regions = newRegions, solution = newSolution.map { it.sorted() })
}
