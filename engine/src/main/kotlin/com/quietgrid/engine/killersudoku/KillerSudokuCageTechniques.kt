package com.quietgrid.engine.killersudoku

import com.quietgrid.engine.sudoku.SudokuHouseRef
import com.quietgrid.engine.sudoku.allHouseRefs
import com.quietgrid.engine.sudoku.arePeerIndexes
import com.quietgrid.engine.sudoku.bitToDigit
import com.quietgrid.engine.sudoku.boxCellIndexes
import com.quietgrid.engine.sudoku.cellBoxIndexes
import com.quietgrid.engine.sudoku.cellColIndexes
import com.quietgrid.engine.sudoku.cellRowIndexes
import com.quietgrid.engine.sudoku.columnCellIndexes
import com.quietgrid.engine.sudoku.digitToBit
import com.quietgrid.engine.sudoku.iterateMaskDigits
import com.quietgrid.engine.sudoku.rowCellIndexes

private const val MAX_VIRTUAL_CAGE_CELLS = 4

class KillerCageAnalysis(val emptyCells: List<Int>, val support: IntArray, val requiredMask: Int, val feasible: Boolean)

fun analyzeKillerCage(state: KillerSolverState, cage: Int, mustInclude: Int = 0): KillerCageAnalysis {
    val emptyCells = state.cageEmptyCells(cage)
    val result = computeCageSupport(
        IntArray(emptyCells.size) { state.grid.candidateMask[emptyCells[it]] },
        state.cagePlacedMask(cage),
        state.layout.cages[cage].sum,
        mustInclude,
    ) ?: return KillerCageAnalysis(emptyCells, IntArray(emptyCells.size), 0, false)
    return KillerCageAnalysis(emptyCells, result.support, result.requiredMask, true)
}

private fun cageMove(
    technique: KillerTechnique,
    state: KillerSolverState,
    cage: Int,
    eliminations: List<Pair<Int, Int>>,
    complexity: Int,
    extraEvidence: List<Int> = emptyList(),
    houses: List<SudokuHouseRef> = emptyList(),
): KillerMove? {
    if (eliminations.isEmpty()) return null
    return KillerMove(
        technique = technique,
        complexity = complexity,
        placementCell = null,
        placementDigit = null,
        eliminations = eliminations.distinct().sortedWith(compareBy({ it.first }, { it.second })),
        evidenceCells = (state.layout.cages[cage].cells + extraEvidence).distinct().sorted(),
        cages = listOf(cage),
        houses = houses,
    )
}

private fun unsupportedEliminations(state: KillerSolverState, cells: List<Int>, support: IntArray): List<Pair<Int, Int>> =
    cells.indices.flatMap { i -> iterateMaskDigits(state.grid.candidateMask[cells[i]] and support[i].inv()).map { cells[i] to it } }

private fun minByScore(moves: Sequence<KillerMove>): KillerMove? = moves.minByOrNull { it.complexity }

object CageLastCellTechnique : KillerTechniqueDispatcher {
    override val technique = KillerTechnique.CAGE_LAST_CELL

    override fun findMove(state: KillerSolverState): KillerMove? = minByScore(
        state.layout.cages.indices.asSequence().mapNotNull { cage ->
            val empty = state.cageEmptyCells(cage)
            if (empty.size != 1) return@mapNotNull null
            val digit = state.cageRemainingSum(cage)
            val cell = empty[0]
            if (digit !in 1..9 || (state.grid.candidateMask[cell] and digitToBit[digit]) == 0) return@mapNotNull null
            KillerMove(technique, state.layout.cages[cage].cells.size, cell, digit, emptyList(), state.layout.cages[cage].cells.sorted(), listOf(cage), emptyList())
        },
    )
}

object CageCombinationTechnique : KillerTechniqueDispatcher {
    override val technique = KillerTechnique.CAGE_COMBINATION

    override fun findMove(state: KillerSolverState): KillerMove? = minByScore(
        state.layout.cages.indices.asSequence().mapNotNull { cage ->
            val empty = state.cageEmptyCells(cage)
            if (empty.isEmpty()) return@mapNotNull null
            val placed = state.cagePlacedMask(cage)
            val remaining = state.cageRemainingSum(cage)
            val union = cageComboUnion(placed, empty.size, remaining)
            val eliminations = empty.flatMap { cell -> iterateMaskDigits(state.grid.candidateMask[cell] and union.inv()).map { cell to it } }
            cageMove(technique, state, cage, eliminations, cageCombinations(empty.size, remaining, placed).size)
        },
    )
}

object CagePermutationTechnique : KillerTechniqueDispatcher {
    override val technique = KillerTechnique.CAGE_PERMUTATION

    override fun findMove(state: KillerSolverState): KillerMove? = minByScore(
        state.layout.cages.indices.asSequence().mapNotNull { cage ->
            if (state.cageEmptyCells(cage).size < 2) return@mapNotNull null
            val analysis = analyzeKillerCage(state, cage)
            if (!analysis.feasible) return@mapNotNull null
            val combos = cageCombinations(analysis.emptyCells.size, state.cageRemainingSum(cage), state.cagePlacedMask(cage)).size
            cageMove(technique, state, cage, unsupportedEliminations(state, analysis.emptyCells, analysis.support), analysis.emptyCells.size + combos)
        },
    )
}

private fun commonHouses(cells: List<Int>): List<Pair<SudokuHouseRef, List<Int>>> {
    if (cells.isEmpty()) return emptyList()
    val result = mutableListOf<Pair<SudokuHouseRef, List<Int>>>()
    if (cells.map { cellRowIndexes[it] }.distinct().size == 1) {
        val row = cellRowIndexes[cells[0]]
        result += SudokuHouseRef("row", row) to rowCellIndexes[row]
    }
    if (cells.map { cellColIndexes[it] }.distinct().size == 1) {
        val col = cellColIndexes[cells[0]]
        result += SudokuHouseRef("column", col) to columnCellIndexes[col]
    }
    if (cells.map { cellBoxIndexes[it] }.distinct().size == 1) {
        val box = cellBoxIndexes[cells[0]]
        result += SudokuHouseRef("box", box) to boxCellIndexes[box]
    }
    return result
}

object CageHouseInteractionTechnique : KillerTechniqueDispatcher {
    override val technique = KillerTechnique.CAGE_HOUSE_INTERACTION

    private fun cageToHouse(state: KillerSolverState): Sequence<KillerMove> = state.layout.cages.indices.asSequence().flatMap { cage ->
        val empty = state.cageEmptyCells(cage)
        if (empty.size < 2) return@flatMap emptySequence()
        val analysis = analyzeKillerCage(state, cage)
        if (!analysis.feasible) return@flatMap emptySequence()
        iterateMaskDigits(analysis.requiredMask).asSequence().flatMap { digit ->
            val holders = empty.filter { (state.grid.candidateMask[it] and digitToBit[digit]) != 0 }
            commonHouses(holders).asSequence().mapNotNull { (house, houseCells) ->
                val eliminations = houseCells
                    .filter { state.layout.cageOfCell[it] != cage && (state.grid.candidateMask[it] and digitToBit[digit]) != 0 }
                    .map { it to digit }
                cageMove(technique, state, cage, eliminations, empty.size + 1, houses = listOf(house))
            }
        }
    }

    private fun houseToCage(state: KillerSolverState): Sequence<KillerMove> = allHouseRefs.asSequence().flatMap { ref ->
        (1..9).asSequence().mapNotNull { digit ->
            if (ref.cells.any { state.grid.board[it] == digit }) return@mapNotNull null
            val holders = ref.cells.filter { (state.grid.candidateMask[it] and digitToBit[digit]) != 0 }
            if (holders.isEmpty()) return@mapNotNull null
            val cage = state.layout.cageOfCell[holders[0]]
            if (holders.any { state.layout.cageOfCell[it] != cage }) return@mapNotNull null
            val analysis = analyzeKillerCage(state, cage, mustInclude = digitToBit[digit])
            if (!analysis.feasible) return@mapNotNull null
            cageMove(
                technique, state, cage, unsupportedEliminations(state, analysis.emptyCells, analysis.support),
                analysis.emptyCells.size + 2, extraEvidence = holders, houses = listOf(ref.house),
            )
        }
    }

    override fun findMove(state: KillerSolverState): KillerMove? = minByScore(cageToHouse(state) + houseToCage(state))
}

class KillerRegion(val cells: List<Int>, val houses: List<SudokuHouseRef>) {
    val contains: BooleanArray = BooleanArray(81).also { flags -> cells.forEach { flags[it] = true } }
}

val killerRegions: List<KillerRegion> = buildList {
    for (span in 1..3) {
        for (start in 0..9 - span) {
            val rows = start until start + span
            add(KillerRegion(rows.flatMap { rowCellIndexes[it] }, rows.map { SudokuHouseRef("row", it) }))
            add(KillerRegion(rows.flatMap { columnCellIndexes[it] }, rows.map { SudokuHouseRef("column", it) }))
        }
    }
    boxCellIndexes.forEachIndexed { box, cells -> add(KillerRegion(cells, listOf(SudokuHouseRef("box", box)))) }
}

private fun analyzeVirtualCage(state: KillerSolverState, cells: List<Int>, sum: Int): IntArray? {
    val size = cells.size
    val masks = IntArray(size) { state.grid.candidateMask[cells[it]] }
    val conflicts = Array(size) { i ->
        BooleanArray(size) { j ->
            j < i && (state.layout.cageOfCell[cells[i]] == state.layout.cageOfCell[cells[j]] || arePeerIndexes(cells[i], cells[j]))
        }
    }
    val suffixMin = IntArray(size + 1)
    val suffixMax = IntArray(size + 1)
    for (i in size - 1 downTo 0) {
        if (masks[i] == 0) return null
        suffixMin[i] = suffixMin[i + 1] + bitToDigit[masks[i] and -masks[i]]
        suffixMax[i] = suffixMax[i + 1] + bitToDigit[Integer.highestOneBit(masks[i])]
    }
    val support = IntArray(size)
    val assigned = IntArray(size)
    var feasible = false

    fun search(position: Int, remaining: Int) {
        if (remaining < suffixMin[position] || remaining > suffixMax[position]) return
        if (position == size) {
            feasible = true
            for (i in 0 until size) support[i] = support[i] or assigned[i]
            return
        }
        var blocked = 0
        for (i in 0 until position) if (conflicts[position][i]) blocked = blocked or assigned[i]
        var allowed = masks[position] and blocked.inv()
        while (allowed != 0) {
            val bit = allowed and -allowed
            allowed = allowed xor bit
            assigned[position] = bit
            search(position + 1, remaining - bitToDigit[bit])
        }
    }

    search(0, sum)
    return if (feasible) support else null
}

private fun regionMoves(state: KillerSolverState, region: KillerRegion): List<KillerMove> {
    val board = state.grid.board
    val insideCount = IntArray(state.layout.cages.size)
    region.cells.forEach { insideCount[state.layout.cageOfCell[it]] += 1 }
    var insideSum = 0
    var partialSum = 0
    val partialCages = mutableListOf<Int>()
    insideCount.forEachIndexed { cage, count ->
        if (count == 0) return@forEachIndexed
        if (count == state.layout.cages[cage].cells.size) {
            insideSum += state.layout.cages[cage].sum
        } else {
            partialSum += state.layout.cages[cage].sum
            partialCages += cage
        }
    }
    if (partialCages.isEmpty()) return emptyList()

    val innieTotal = 45 * region.houses.size - insideSum
    val outieTotal = partialSum - innieTotal
    val partialCells = partialCages.flatMap { state.layout.cages[it].cells }
    val innies = partialCells.filter { region.contains[it] }
    val outies = partialCells.filter { !region.contains[it] }

    return listOf(innies to innieTotal, outies to outieTotal).mapNotNull { (cells, total) ->
        val open = cells.filter { board[it] == 0 }
        val target = total - cells.sumOf { board[it] }
        if (open.isEmpty() || open.size > MAX_VIRTUAL_CAGE_CELLS) return@mapNotNull null
        val technique = when {
            region.houses.size > 1 -> KillerTechnique.RULE_OF_45_MULTI_HOUSE
            open.size == 1 -> KillerTechnique.INNIE_OUTIE
            else -> KillerTechnique.RULE_OF_45
        }
        val complexity = open.size * 2 + region.houses.size + partialCages.size
        val evidence = (region.cells + partialCells).distinct().sorted()
        if (open.size == 1) {
            val cell = open[0]
            if (target !in 1..9 || (state.grid.candidateMask[cell] and digitToBit[target]) == 0) return@mapNotNull null
            return@mapNotNull KillerMove(technique, complexity, cell, target, emptyList(), evidence, partialCages, region.houses)
        }
        val support = analyzeVirtualCage(state, open, target) ?: return@mapNotNull null
        val eliminations = unsupportedEliminations(state, open, support)
        if (eliminations.isEmpty()) return@mapNotNull null
        KillerMove(technique, complexity, null, null, eliminations, evidence, partialCages, region.houses)
    }
}

class RuleOf45Technique(override val technique: KillerTechnique) : KillerTechniqueDispatcher {
    override fun findMove(state: KillerSolverState): KillerMove? {
        val moves = state.regionMoves ?: killerRegions.flatMap { regionMoves(state, it) }.also { state.regionMoves = it }
        return minByScore(moves.asSequence().filter { it.technique == technique })
    }
}
