package com.quietgrid.engine.nonogram

data class NonogramLinePlacement(val starts: List<Int>)

data class NonogramLineAnalysis(
    val placements: List<NonogramLinePlacement>,
    val overlapFillCells: List<Int>,
    val forcedEmptyCells: List<Int>,
    val isComplete: Boolean,
)

fun isNonogramLineComplete(line: List<NonogramCellValue>, clues: List<Int>): Boolean {
    val segments = mutableListOf<Int>()
    var run = 0
    for (cell in line) {
        if (cell == 1) {
            run += 1
        } else if (run > 0) {
            segments.add(run)
            run = 0
        }
    }
    if (run > 0) segments.add(run)
    return segments.size == clues.size && segments.indices.all { segments[it] == clues[it] }
}

private fun rangeHasFilledCell(line: List<NonogramCellValue>, start: Int, endExclusive: Int): Boolean {
    for (index in start until endExclusive) if (line[index] == 1) return true
    return false
}

private fun canPlaceBlockAt(line: List<NonogramCellValue>, start: Int, length: Int): Boolean {
    val end = start + length - 1
    if (end >= line.size) return false
    for (index in start..end) if (line[index] == 0) return false
    if (start > 0 && line[start - 1] == 1) return false
    if (end < line.size - 1 && line[end + 1] == 1) return false
    return true
}

private fun placementCoversAllFilledCells(line: List<NonogramCellValue>, starts: List<Int>, clues: List<Int>): Boolean {
    val intervals = starts.mapIndexed { clueIndex, start -> start to (start + clues[clueIndex] - 1) }
    return line.indices.all { cellIndex ->
        if (line[cellIndex] != 1) true else intervals.any { (start, end) -> cellIndex in start..end }
    }
}

private fun getMinimumRemainingLengths(clues: List<Int>): List<Int> {
    val result = IntArray(clues.size)
    var remaining = 0
    for (index in clues.indices.reversed()) {
        remaining += clues[index]
        result[index] = remaining + (clues.size - index - 1)
    }
    return result.toList()
}

private class StackFrame(val clueIndex: Int, var nextStart: Int)

fun enumerateLinePlacements(line: List<NonogramCellValue>, clues: List<Int>): List<NonogramLinePlacement> {
    if (clues.isEmpty()) {
        return if (line.any { it == 1 }) emptyList() else listOf(NonogramLinePlacement(emptyList()))
    }

    val minimumRemainingLengths = getMinimumRemainingLengths(clues)
    if (minimumRemainingLengths[0] > line.size) return emptyList()

    val placements = mutableListOf<NonogramLinePlacement>()
    val starts = IntArray(clues.size)
    val stack = mutableListOf(StackFrame(0, 0))

    while (stack.isNotEmpty()) {
        val frame = stack.last()
        val clueIndex = frame.clueIndex
        val clueLength = clues[clueIndex]
        val latestStart = line.size - minimumRemainingLengths[clueIndex]
        var candidateStart = frame.nextStart
        var advanced = false

        while (candidateStart <= latestStart) {
            if (!canPlaceBlockAt(line, candidateStart, clueLength)) {
                candidateStart += 1
                continue
            }

            if (clueIndex == 0) {
                if (rangeHasFilledCell(line, 0, candidateStart)) {
                    candidateStart += 1
                    continue
                }
            } else {
                val previousEnd = starts[clueIndex - 1] + clues[clueIndex - 1] - 1
                val gapStart = previousEnd + 1
                if (candidateStart < gapStart + 1) {
                    candidateStart += 1
                    continue
                }
                if (rangeHasFilledCell(line, gapStart, candidateStart)) {
                    candidateStart += 1
                    continue
                }
            }

            starts[clueIndex] = candidateStart
            frame.nextStart = candidateStart + 1
            advanced = true

            if (clueIndex == clues.size - 1) {
                if (placementCoversAllFilledCells(line, starts.toList(), clues)) {
                    placements.add(NonogramLinePlacement(starts.toList()))
                }
            } else {
                stack.add(StackFrame(clueIndex + 1, candidateStart + clueLength + 1))
            }
            break
        }

        if (!advanced) stack.removeAt(stack.size - 1)
    }

    return placements
}

fun analyzeLine(line: List<NonogramCellValue>, clues: List<Int>): NonogramLineAnalysis? {
    val placements = enumerateLinePlacements(line, clues)
    if (placements.isEmpty()) return null

    val coverage = IntArray(line.size)
    for (placement in placements) {
        placement.starts.forEachIndexed { clueIndex, start ->
            val end = start + clues[clueIndex] - 1
            for (index in start..end) coverage[index] += 1
        }
    }

    val overlapFillCells = mutableListOf<Int>()
    val forcedEmptyCells = mutableListOf<Int>()
    line.forEachIndexed { index, cell ->
        if (cell != null) return@forEachIndexed
        when (coverage[index]) {
            placements.size -> overlapFillCells.add(index)
            0 -> forcedEmptyCells.add(index)
        }
    }

    return NonogramLineAnalysis(placements, overlapFillCells, forcedEmptyCells, isNonogramLineComplete(line, clues))
}
