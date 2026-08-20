package com.quietgrid.engine.wordsearch

data class WordPlacement(val id: String, val word: String, val start: WSCellRef, val direction: WordSearchDirection, val positions: List<WSCellRef>)
data class PlacementResult(val grid: List<MutableList<String>>, val placements: List<WordPlacement>)

private data class CandidatePlacement(val start: WSCellRef, val direction: WordSearchDirection, val positions: List<WSCellRef>)
private data class RepairCandidate(val word: String, val start: WSCellRef, val direction: WordSearchDirection, val positions: List<WSCellRef>, val score: Double)

private const val MAX_REPAIR_STEPS = 20_000
private const val TABU_TENURE = 15

private fun createGrid(rows: Int, cols: Int, reserved: Set<Int>): List<MutableList<String>> {
    val grid = List(rows) { MutableList(cols) { "" } }
    reserved.forEach { key -> grid[key / 1000][key % 1000] = "#" }
    return grid
}

private fun buildStraightPositions(startRow: Int, startCol: Int, dRow: Int, dCol: Int, wordLen: Int): List<WSCellRef> =
    (0 until wordLen).map { WSCellRef(startRow + dRow * it, startCol + dCol * it) }

private fun canPlace(grid: List<List<String>>, positions: List<WSCellRef>, word: String): Boolean =
    positions.withIndex().all { (index, cell) -> val existing = grid[cell.row][cell.col]; existing == "" || existing == word[index].toString() }

private fun placeWord(grid: List<MutableList<String>>, positions: List<WSCellRef>, word: String) {
    positions.forEachIndexed { index, cell -> grid[cell.row][cell.col] = word[index].toString() }
}

private fun scorePlacement(grid: List<List<String>>, positions: List<WSCellRef>, word: String, uncovered: Set<Int>, overlapFrequency: Double): Double {
    var overlapCount = 0
    var uncoveredCoverage = 0
    positions.forEachIndexed { index, cell ->
        val key = toGridKey(cell)
        if (key in uncovered) uncoveredCoverage += 1
        if (grid[cell.row][cell.col] == word[index].toString()) overlapCount += 1
    }
    return uncoveredCoverage * 100.0 + overlapCount * overlapFrequency * 10.0 + Math.random()
}

private fun findBestPlacement(
    grid: List<MutableList<String>>, rows: Int, cols: Int, word: String, directions: List<WordSearchDirection>,
    uncovered: Set<Int>, overlapFrequency: Double,
): CandidatePlacement? {
    val wordLen = word.length
    var best: CandidatePlacement? = null
    var bestScore = Double.NEGATIVE_INFINITY

    for (direction in directions) {
        val (dRow, dCol) = directionToDelta.getValue(direction)
        val minRow = if (dRow < 0) wordLen - 1 else 0
        val maxRow = if (dRow > 0) rows - wordLen else rows - 1
        val minCol = if (dCol < 0) wordLen - 1 else 0
        val maxCol = if (dCol > 0) cols - wordLen else cols - 1
        if (minRow > maxRow || minCol > maxCol) continue

        for (row in minRow..maxRow) {
            for (col in minCol..maxCol) {
                val positions = buildStraightPositions(row, col, dRow, dCol, wordLen)
                if (!canPlace(grid, positions, word)) continue
                val score = scorePlacement(grid, positions, word, uncovered, overlapFrequency)
                if (score > bestScore) { bestScore = score; best = CandidatePlacement(WSCellRef(row, col), direction, positions) }
            }
        }
    }
    return best
}

private fun findBestPlacementThroughCell(
    grid: List<MutableList<String>>, rows: Int, cols: Int, word: String, directions: List<WordSearchDirection>,
    mustCoverRow: Int, mustCoverCol: Int, uncovered: Set<Int>, overlapFrequency: Double,
): CandidatePlacement? {
    val wordLen = word.length
    var best: CandidatePlacement? = null
    var bestScore = Double.NEGATIVE_INFINITY

    for (direction in directions) {
        val (dRow, dCol) = directionToDelta.getValue(direction)
        for (i in 0 until wordLen) {
            val startRow = mustCoverRow - dRow * i
            val startCol = mustCoverCol - dCol * i
            val endRow = startRow + dRow * (wordLen - 1)
            val endCol = startCol + dCol * (wordLen - 1)
            if (startRow !in 0 until rows || startCol !in 0 until cols) continue
            if (endRow !in 0 until rows || endCol !in 0 until cols) continue

            val positions = buildStraightPositions(startRow, startCol, dRow, dCol, wordLen)
            if (!canPlace(grid, positions, word)) continue
            val score = scorePlacement(grid, positions, word, uncovered, overlapFrequency)
            if (score > bestScore) { bestScore = score; best = CandidatePlacement(WSCellRef(startRow, startCol), direction, positions) }
        }
    }
    return best
}

private fun isOrthogonallyAdjacent(a: WSCellRef, b: WSCellRef): Boolean = kotlin.math.abs(a.row - b.row) + kotlin.math.abs(a.col - b.col) == 1

private fun incrementCover(coverCounts: MutableMap<Int, Int>, key: Int) { coverCounts[key] = (coverCounts[key] ?: 0) + 1 }

private fun decrementCover(coverCounts: MutableMap<Int, Int>, key: Int): Int {
    val next = (coverCounts[key] ?: 1) - 1
    if (next <= 0) coverCounts.remove(key) else coverCounts[key] = next
    return next
}

private fun repairCoverage(
    grid: List<MutableList<String>>, rows: Int, cols: Int, wordPool: List<String>, uncovered: MutableSet<Int>,
    usedWords: MutableSet<String>, placements: MutableList<WordPlacement>, coverCounts: MutableMap<Int, Int>,
    nextId: IntArray, allowedDirections: List<WordSearchDirection>, overlapFrequency: Double,
): Boolean {
    val tabuUntilStep = mutableMapOf<String, Int>()
    var step = 0

    while (uncovered.isNotEmpty()) {
        step += 1
        if (step > MAX_REPAIR_STEPS) return false

        var targetKey: Int? = null
        var targetCandidates: List<RepairCandidate>? = null
        for (key in uncovered) {
            val row = key / 1000
            val col = key % 1000
            val candidates = mutableListOf<RepairCandidate>()
            for (word in wordPool) {
                if (word in usedWords) continue
                if ((tabuUntilStep[word] ?: 0) >= step) continue
                val candidate = findBestPlacementThroughCell(grid, rows, cols, word, allowedDirections, row, col, uncovered, overlapFrequency) ?: continue
                candidates.add(RepairCandidate(word, candidate.start, candidate.direction, candidate.positions, scorePlacement(grid, candidate.positions, word, uncovered, overlapFrequency)))
            }
            if (targetCandidates == null || candidates.size < targetCandidates.size) { targetKey = key; targetCandidates = candidates }
            if (targetCandidates.isEmpty()) break
        }

        if (targetKey == null || targetCandidates == null) return false

        if (targetCandidates.isNotEmpty()) {
            val best = targetCandidates.maxByOrNull { it.score }!!
            val touchedKeys = best.positions.map { toGridKey(it) }
            placeWord(grid, best.positions, best.word)
            usedWords.add(best.word)
            touchedKeys.forEach { incrementCover(coverCounts, it); uncovered.remove(it) }
            placements.add(WordPlacement("${nextId[0]}", best.word, best.start, best.direction, best.positions))
            nextId[0] += 1
            continue
        }

        val targetRow = targetKey / 1000
        val targetCol = targetKey % 1000
        val nearbyPlacements = placements.filter { placement -> placement.positions.any { isOrthogonallyAdjacent(it, WSCellRef(targetRow, targetCol)) } }
        if (nearbyPlacements.isEmpty()) return false

        val evicted = nearbyPlacements.minByOrNull { it.positions.size }!!
        placements.remove(evicted)
        usedWords.remove(evicted.word)
        evicted.positions.forEach { cell ->
            val key = toGridKey(cell)
            if (decrementCover(coverCounts, key) == 0) { uncovered.add(key); grid[cell.row][cell.col] = "" }
        }
        tabuUntilStep[evicted.word] = step + TABU_TENURE
    }
    return true
}

fun buildFullCoverageGrid(
    rows: Int, cols: Int, wordPool: List<String>, reservedCells: Set<Int>,
    allowedDirections: List<WordSearchDirection>, overlapFrequency: Double,
): PlacementResult? {
    val grid = createGrid(rows, cols, reservedCells)
    val placements = mutableListOf<WordPlacement>()
    val coverCounts = mutableMapOf<Int, Int>()

    val uncovered = mutableSetOf<Int>()
    for (row in 0 until rows) for (col in 0 until cols) {
        val key = row * 1000 + col
        if (key !in reservedCells) uncovered.add(key)
    }

    val dedupedPool = wordPool.distinct()

    val spreadOrder = dedupedPool.sortedByDescending { it.length }
    var nextIdCounter = 1
    for (word in spreadOrder) {
        if (uncovered.isEmpty()) break
        val placement = findBestPlacement(grid, rows, cols, word, allowedDirections, uncovered, overlapFrequency) ?: continue
        placeWord(grid, placement.positions, word)
        placement.positions.forEach { cell -> val key = toGridKey(cell); incrementCover(coverCounts, key); uncovered.remove(key) }
        placements.add(WordPlacement("$nextIdCounter", word, placement.start, placement.direction, placement.positions))
        nextIdCounter += 1
    }

    if (uncovered.isEmpty()) return PlacementResult(grid, placements)

    val usedWords = placements.map { it.word }.toMutableSet()
    val nextId = intArrayOf(nextIdCounter)
    val repaired = repairCoverage(grid, rows, cols, dedupedPool, uncovered, usedWords, placements, coverCounts, nextId, allowedDirections, overlapFrequency)
    if (!repaired) return null

    return PlacementResult(grid, placements)
}
