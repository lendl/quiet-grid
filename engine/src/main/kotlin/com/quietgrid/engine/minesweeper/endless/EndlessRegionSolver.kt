package com.quietgrid.engine.minesweeper.endless

import com.quietgrid.engine.minesweeper.MinesweeperSolverState
import com.quietgrid.engine.minesweeper.MinesweeperTechnique
import com.quietgrid.engine.minesweeper.nextMinesweeperDeductions
import kotlin.math.max
import kotlin.math.min

val ENDLESS_TECHNIQUES: Set<MinesweeperTechnique> = setOf(MinesweeperTechnique.CLUE_COUNT, MinesweeperTechnique.CLUE_PAIR)

sealed interface EndlessEntry {
    data class FirstTap(val cell: WorldCell) : EndlessEntry
    data class FromSection(val pos: SectionPos) : EndlessEntry
}

data class EndlessSolveTrace(
    val solved: Boolean,
    val steps: Int,
    val cluePairSteps: Int,
    val bottleneckSteps: Int,
    val largestCascade: Int,
    val openedCount: Int,
)

fun solveEndlessSection(
    world: EndlessWorld,
    target: SectionPos,
    entry: EndlessEntry,
    techniques: Set<MinesweeperTechnique> = ENDLESS_TECHNIQUES,
): EndlessSolveTrace = RegionSolve(world, target, entry, techniques).run()

private class RegionSolve(
    private val world: EndlessWorld,
    private val target: SectionPos,
    private val entry: EndlessEntry,
    private val techniques: Set<MinesweeperTechnique>,
) {
    private val entrySection = (entry as? EndlessEntry.FromSection)?.pos
    private val minX = min(target.sx, entrySection?.sx ?: target.sx) * ENDLESS_SECTION_SIZE - 1
    private val minY = min(target.sy, entrySection?.sy ?: target.sy) * ENDLESS_SECTION_SIZE - 1
    private val maxX = (max(target.sx, entrySection?.sx ?: target.sx) + 1) * ENDLESS_SECTION_SIZE
    private val maxY = (max(target.sy, entrySection?.sy ?: target.sy) + 1) * ENDLESS_SECTION_SIZE
    private val cols = maxX - minX + 1
    private val rows = maxY - minY + 1
    private val state = MinesweeperSolverState(rows, cols, 0)
    private val mines = BooleanArray(rows * cols) { index ->
        val cell = cellAt(index)
        requireNotNull(world.mineAt(cell)) { "Unfixed cell $cell in the window of $target" }
    }
    private val numbers = IntArray(rows * cols) { index -> state.neighbors[index].count { mines[it] } }
    private val inTarget = BooleanArray(rows * cols) { cellAt(it).section == target }
    private var openedInTarget = 0
    private var largestCascade = 0

    private fun cellAt(index: Int) = WorldCell(minX + index % cols, minY + index / cols)

    private fun indexOf(cell: WorldCell) = (cell.y - minY) * cols + (cell.x - minX)

    private fun reveal(start: Int) {
        var opened = 0
        val stack = ArrayDeque<Int>()
        stack.add(start)
        while (stack.isNotEmpty()) {
            val next = stack.removeLast()
            if (!state.isUnknown(next) || mines[next] || !inTarget[next]) continue
            state.open(next, numbers[next])
            opened++
            if (numbers[next] != 0) continue
            for (neighbor in state.neighbors[next]) {
                if (inTarget[neighbor]) stack.add(neighbor) else state.markSafeLocked(neighbor)
            }
        }
        openedInTarget += opened
        largestCascade = max(largestCascade, opened)
    }

    private fun applyRevealedKnowledge() {
        for (index in 0 until rows * cols) {
            val cell = cellAt(index)
            when (world.cellStateAt(cell)) {
                EndlessCellState.REVEALED -> {
                    val onEdge = cell.x == minX || cell.x == maxX || cell.y == minY || cell.y == maxY
                    if (!onEdge && state.isUnknown(index)) {
                        state.open(index, numbers[index])
                        if (inTarget[index]) openedInTarget++
                    }
                }
                EndlessCellState.KNOWN_MINE -> state.markMine(index)
                EndlessCellState.FLAGGED -> if (world.stateOf(cell.section) == SectionState.CLEARED) state.markMine(index)
                EndlessCellState.HIDDEN -> Unit
            }
        }
    }

    fun run(): EndlessSolveTrace {
        applyRevealedKnowledge()
        when (entry) {
            is EndlessEntry.FirstTap -> {
                val index = indexOf(entry.cell)
                require(inTarget[index] && !mines[index]) { "First tap ${entry.cell} must be a safe cell of $target" }
                reveal(index)
            }
            is EndlessEntry.FromSection -> for (cell in entry.pos.cells()) {
                val index = indexOf(cell)
                if (mines[index]) state.markMine(index) else state.open(index, numbers[index])
            }
        }
        val safeInTarget = inTarget.indices.count { inTarget[it] && !mines[it] }
        var steps = 0
        var cluePairSteps = 0
        var bottleneckSteps = 0
        while (openedInTarget < safeInTarget) {
            val deductions = nextMinesweeperDeductions(state, techniques)
            if (deductions.isEmpty()) break
            steps++
            if (deductions.first().technique == MinesweeperTechnique.CLUE_PAIR) cluePairSteps++
            if (deductions.size == 1) bottleneckSteps++
            for (deduction in deductions) {
                if (deduction.safe.any { mines[it] } || deduction.mines.any { !mines[it] }) {
                    return trace(false, steps, cluePairSteps, bottleneckSteps)
                }
                deduction.mines.forEach(state::markMine)
                for (cell in deduction.safe) if (inTarget[cell]) reveal(cell) else state.markSafeLocked(cell)
            }
        }
        return trace(openedInTarget == safeInTarget, steps, cluePairSteps, bottleneckSteps)
    }

    private fun trace(solved: Boolean, steps: Int, cluePairSteps: Int, bottleneckSteps: Int) =
        EndlessSolveTrace(solved, steps, cluePairSteps, bottleneckSteps, largestCascade, openedInTarget)
}
