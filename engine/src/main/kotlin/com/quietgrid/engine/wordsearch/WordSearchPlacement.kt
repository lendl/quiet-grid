package com.quietgrid.engine.wordsearch

import kotlin.math.abs
import kotlin.random.Random

data class WordPlacement(val id: String, val word: String, val start: WSCellRef, val direction: WordSearchDirection, val positions: List<WSCellRef>)
data class PlacementResult(val grid: List<MutableList<String>>, val placements: List<WordPlacement>)

private data class CandidatePlacement(val word: String, val start: WSCellRef, val direction: WordSearchDirection, val positions: List<WSCellRef>, val score: Double)

private const val MAX_REPAIR_STEPS = 300
private const val TABU_TENURE = 15

private class CoverageBuilder(
    private val rows: Int,
    private val cols: Int,
    reservedCells: Set<Int>,
    private val allowedDirections: List<WordSearchDirection>,
    private val overlapFrequency: Double,
) {
    val grid: List<MutableList<String>> = List(rows) { MutableList(cols) { "" } }
    val placements = mutableListOf<WordPlacement>()
    private val uncovered = mutableSetOf<Int>()
    private val coveringIds = mutableMapOf<Int, MutableSet<String>>()
    private val placementsById = mutableMapOf<String, WordPlacement>()
    private var nextId = 1

    init {
        reservedCells.forEach { key -> grid[key / 1000][key % 1000] = "#" }
        for (row in 0 until rows) for (col in 0 until cols) {
            val key = row * 1000 + col
            if (key !in reservedCells) uncovered.add(key)
        }
    }

    fun build(wordPool: List<String>): Boolean {
        val pool = wordPool.distinct()
        spread(pool)
        return uncovered.isEmpty() || repair(pool)
    }

    private fun spread(pool: List<String>) {
        for (word in pool.sortedByDescending { it.length }) {
            if (uncovered.isEmpty()) return
            val candidate = bestPlacementAnywhere(word) ?: continue
            commitUnlessDuplicate(candidate)
        }
    }

    private fun repair(pool: List<String>): Boolean {
        val usedWords = placements.map { it.word }.toMutableSet()
        val tabuUntilStep = mutableMapOf<String, Int>()
        var step = 0
        while (uncovered.isNotEmpty()) {
            step += 1
            if (step > MAX_REPAIR_STEPS) return false

            var targetKey = -1
            var targetCandidates: List<CandidatePlacement>? = null
            for (key in uncovered) {
                val candidates = pool.mapNotNull { word ->
                    if (word in usedWords || (tabuUntilStep[word] ?: 0) >= step) null else bestPlacementThrough(word, key)
                }
                if (targetCandidates == null || candidates.size < targetCandidates.size) {
                    targetKey = key
                    targetCandidates = candidates
                }
                if (candidates.isEmpty()) break
            }

            val best = targetCandidates?.maxByOrNull { it.score }
            if (best != null) {
                usedWords.add(best.word)
                if (!commitUnlessDuplicate(best)) {
                    usedWords.remove(best.word)
                    tabuUntilStep[best.word] = step + TABU_TENURE
                }
                continue
            }

            val evicted = placements
                .filter { placement -> placement.positions.any { isOrthogonallyAdjacent(it, targetKey) } }
                .minByOrNull { it.positions.size } ?: return false
            evict(evicted)
            usedWords.remove(evicted.word)
            tabuUntilStep[evicted.word] = step + TABU_TENURE
        }
        return true
    }

    private fun bestPlacementAnywhere(word: String): CandidatePlacement? {
        val length = word.length
        var best: CandidatePlacement? = null
        for (direction in allowedDirections) {
            val (dRow, dCol) = directionToDelta.getValue(direction)
            val minRow = if (dRow < 0) length - 1 else 0
            val maxRow = if (dRow > 0) rows - length else rows - 1
            val minCol = if (dCol < 0) length - 1 else 0
            val maxCol = if (dCol > 0) cols - length else cols - 1
            for (row in minRow..maxRow) for (col in minCol..maxCol) {
                best = higherScoring(best, evaluate(word, row, col, direction))
            }
        }
        return best
    }

    private fun bestPlacementThrough(word: String, key: Int): CandidatePlacement? {
        val length = word.length
        val targetRow = key / 1000
        val targetCol = key % 1000
        var best: CandidatePlacement? = null
        for (direction in allowedDirections) {
            val (dRow, dCol) = directionToDelta.getValue(direction)
            for (offset in 0 until length) {
                val startRow = targetRow - dRow * offset
                val startCol = targetCol - dCol * offset
                val endRow = startRow + dRow * (length - 1)
                val endCol = startCol + dCol * (length - 1)
                if (startRow !in 0 until rows || startCol !in 0 until cols) continue
                if (endRow !in 0 until rows || endCol !in 0 until cols) continue
                best = higherScoring(best, evaluate(word, startRow, startCol, direction))
            }
        }
        return best
    }

    private fun higherScoring(current: CandidatePlacement?, next: CandidatePlacement?): CandidatePlacement? =
        if (next != null && (current == null || next.score > current.score)) next else current

    private fun evaluate(word: String, startRow: Int, startCol: Int, direction: WordSearchDirection): CandidatePlacement? {
        val (dRow, dCol) = directionToDelta.getValue(direction)
        val positions = word.indices.map { WSCellRef(startRow + dRow * it, startCol + dCol * it) }
        var freshCells = 0
        var overlapCells = 0
        positions.forEachIndexed { index, cell ->
            val existing = grid[cell.row][cell.col]
            when {
                existing.isEmpty() -> freshCells += 1
                existing[0] == word[index] -> overlapCells += 1
                else -> return null
            }
        }
        if (freshCells == 0 || takesLastOwnCell(positions)) return null
        val score = freshCells * 100.0 + overlapCells * overlapFrequency * 10.0 + Random.nextDouble()
        return CandidatePlacement(word, WSCellRef(startRow, startCol), direction, positions, score)
    }

    private fun takesLastOwnCell(positions: List<WSCellRef>): Boolean {
        val lostCells = mutableMapOf<String, Int>()
        for (cell in positions) {
            val owners = coveringIds[toGridKey(cell)] ?: continue
            if (owners.size == 1) {
                val owner = owners.first()
                lostCells[owner] = (lostCells[owner] ?: 0) + 1
            }
        }
        return lostCells.any { (id, lost) -> ownCellCount(placementsById.getValue(id)) <= lost }
    }

    private fun ownCellCount(placement: WordPlacement): Int = placement.positions.count { coveringIds[toGridKey(it)]?.size == 1 }

    private fun isOrthogonallyAdjacent(cell: WSCellRef, key: Int): Boolean = abs(cell.row - key / 1000) + abs(cell.col - key % 1000) == 1

    private fun commitUnlessDuplicate(candidate: CandidatePlacement): Boolean {
        val placement = commit(candidate)
        if (!hasDuplicateOccurrence(grid, placements.map { it.word to it.positions })) return true
        evict(placement)
        return false
    }

    private fun commit(candidate: CandidatePlacement): WordPlacement {
        val placement = WordPlacement("${nextId++}", candidate.word, candidate.start, candidate.direction, candidate.positions)
        placement.positions.forEachIndexed { index, cell ->
            val key = toGridKey(cell)
            grid[cell.row][cell.col] = placement.word[index].toString()
            coveringIds.getOrPut(key) { mutableSetOf() }.add(placement.id)
            uncovered.remove(key)
        }
        placements.add(placement)
        placementsById[placement.id] = placement
        return placement
    }

    private fun evict(placement: WordPlacement) {
        placements.remove(placement)
        placementsById.remove(placement.id)
        placement.positions.forEach { cell ->
            val key = toGridKey(cell)
            val owners = coveringIds.getValue(key)
            owners.remove(placement.id)
            if (owners.isEmpty()) {
                coveringIds.remove(key)
                uncovered.add(key)
                grid[cell.row][cell.col] = ""
            }
        }
    }
}

fun buildFullCoverageGrid(
    rows: Int, cols: Int, wordPool: List<String>, reservedCells: Set<Int>,
    allowedDirections: List<WordSearchDirection>, overlapFrequency: Double,
): PlacementResult? {
    val builder = CoverageBuilder(rows, cols, reservedCells, allowedDirections, overlapFrequency)
    return if (builder.build(wordPool)) PlacementResult(builder.grid, builder.placements) else null
}
