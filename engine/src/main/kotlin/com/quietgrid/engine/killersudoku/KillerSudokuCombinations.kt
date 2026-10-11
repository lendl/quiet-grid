package com.quietgrid.engine.killersudoku

import com.quietgrid.engine.sudoku.digitToBit
import com.quietgrid.engine.sudoku.popcount

const val MAX_CAGE_SUM = 45

val maskDigitSum: IntArray = IntArray(512) { mask -> (1..9).sumOf { digit -> if ((mask and digitToBit[digit]) != 0) digit else 0 } }

private const val SUM_SLOTS = MAX_CAGE_SUM + 1
private const val COUNT_SLOTS = 10

private fun comboSlot(used: Int, count: Int, sum: Int): Int = (used * COUNT_SLOTS + count) * SUM_SLOTS + sum

private val comboUnionTable: IntArray = IntArray(512 * COUNT_SLOTS * SUM_SLOTS).also { table ->
    for (mask in 1 until 512) {
        val count = popcount[mask]
        val sum = maskDigitSum[mask]
        for (used in 0 until 512) {
            if ((used and mask) != 0) continue
            val slot = comboSlot(used, count, sum)
            table[slot] = table[slot] or mask
        }
    }
}

fun cageComboUnion(usedMask: Int, remainingCells: Int, remainingSum: Int): Int {
    if (remainingCells == 0 || remainingCells >= COUNT_SLOTS || remainingSum <= 0 || remainingSum > MAX_CAGE_SUM) return 0
    return comboUnionTable[comboSlot(usedMask, remainingCells, remainingSum)]
}

fun isCageRemainderFeasible(usedMask: Int, remainingCells: Int, remainingSum: Int): Boolean =
    if (remainingCells == 0) remainingSum == 0 else cageComboUnion(usedMask, remainingCells, remainingSum) != 0

private val combinationsByShape: Array<IntArray> = Array(COUNT_SLOTS * SUM_SLOTS) { slot ->
    (1 until 512).filter { popcount[it] == slot / SUM_SLOTS && maskDigitSum[it] == slot % SUM_SLOTS }.toIntArray()
}

fun cageCombinations(cellCount: Int, sum: Int, excludedMask: Int = 0): List<Int> {
    if (cellCount !in 0 until COUNT_SLOTS || sum !in 0 until SUM_SLOTS) return emptyList()
    return combinationsByShape[cellCount * SUM_SLOTS + sum].filter { (it and excludedMask) == 0 }
}

class CageSupport(val support: IntArray, val requiredMask: Int)

fun computeCageSupport(candidates: IntArray, placedMask: Int, cageSum: Int, mustInclude: Int = 0): CageSupport? {
    val cells = candidates.size
    val levels = Array(cells + 1) { IntArray(0) }
    levels[0] = intArrayOf(placedMask)
    val seen = BooleanArray(512)
    for (position in 0 until cells) {
        val next = mutableListOf<Int>()
        seen.fill(false)
        for (used in levels[position]) {
            var allowed = candidates[position] and cageComboUnion(used, cells - position, cageSum - maskDigitSum[used])
            while (allowed != 0) {
                val bit = allowed and -allowed
                allowed = allowed xor bit
                val reached = used or bit
                if (!seen[reached]) {
                    seen[reached] = true
                    next += reached
                }
            }
        }
        if (next.isEmpty()) return null
        levels[position + 1] = next.toIntArray()
    }

    val alive = BooleanArray(512)
    var required = 0x1FF
    var feasible = false
    for (used in levels[cells]) {
        if (maskDigitSum[used] == cageSum && (used and mustInclude) == mustInclude) {
            alive[used] = true
            required = required and used and placedMask.inv()
            feasible = true
        }
    }
    if (!feasible) return null

    val support = IntArray(cells)
    var aliveNext = alive
    for (position in cells - 1 downTo 0) {
        val aliveHere = BooleanArray(512)
        for (used in levels[position]) {
            var allowed = candidates[position] and used.inv()
            while (allowed != 0) {
                val bit = allowed and -allowed
                allowed = allowed xor bit
                if (aliveNext[used or bit]) {
                    aliveHere[used] = true
                    support[position] = support[position] or bit
                }
            }
        }
        aliveNext = aliveHere
    }
    return CageSupport(support, required)
}
