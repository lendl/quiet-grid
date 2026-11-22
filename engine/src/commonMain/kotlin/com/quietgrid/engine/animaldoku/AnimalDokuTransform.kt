package com.quietgrid.engine.animaldoku

import com.quietgrid.engine.journey.GridTransform
import com.quietgrid.engine.journey.gridTransformsFor
import com.quietgrid.engine.journey.mapCell

fun animalDokuReplayCandidates(entry: AnimalDokuPuzzleEntry): List<GridTransform> =
    gridTransformsFor(entry.size, entry.size) - GridTransform.IDENTITY

fun AnimalDokuPuzzleEntry.transformed(t: GridTransform): AnimalDokuPuzzleEntry {
    if (t == GridTransform.IDENTITY) return this
    val newRegions = List(size) { MutableList(size) { 0 } }
    for (row in 0 until size) {
        for (col in 0 until size) {
            val (r, c) = t.mapCell(row, col, size, size)
            newRegions[r][c] = regions[row][col]
        }
    }
    val newSolution = MutableList(size) { 0 }
    solution.forEachIndexed { row, col ->
        val (r, c) = t.mapCell(row, col, size, size)
        newSolution[r] = c
    }
    return copy(regions = newRegions, solution = newSolution)
}
