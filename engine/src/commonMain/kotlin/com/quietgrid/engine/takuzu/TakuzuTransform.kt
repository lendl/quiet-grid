package com.quietgrid.engine.takuzu

import com.quietgrid.engine.journey.GridTransform
import com.quietgrid.engine.journey.gridTransformsFor
import com.quietgrid.engine.journey.movedBy

fun takuzuReplayCandidates(entry: TakuzuPuzzleEntry): List<GridTransform> =
    gridTransformsFor(entry.size, entry.size) - GridTransform.IDENTITY

fun TakuzuPuzzleEntry.transformed(t: GridTransform): TakuzuPuzzleEntry {
    if (t == GridTransform.IDENTITY) return this
    return copy(
        solution = gridToHex(decodeSolution(solution, size).movedBy(t)),
        mask = maskToHex(decodeMask(mask, size).movedBy(t)),
    )
}

fun takuzuHasOneSolution(entry: TakuzuPuzzleEntry): Boolean =
    countSolutions(decodePuzzleBoard(entry.solution, entry.mask, entry.size)) == 1
