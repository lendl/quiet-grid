package com.quietgrid.engine.minesweeper.endless

import com.quietgrid.engine.minesweeper.MinesweeperSolverState
import com.quietgrid.engine.minesweeper.nextMinesweeperDeductions
import kotlin.random.Random

data class EndlessSimulationResult(
    val sectionsCleared: Int,
    val stuck: Boolean,
    val hitMine: Boolean,
    val stats: EndlessBuildStats,
    val settleMillis: List<Double>,
    val buildsPerSettle: List<Int>,
    val world: EndlessWorld,
    val strandedSections: Int = countStrandedEndlessSections(world),
    val largestOpening: Int = largestEndlessOpening(world),
)

fun largestEndlessOpening(world: EndlessWorld): Int =
    world.sections.values.filter { it.state != SectionState.UNBUILT }
        .maxOfOrNull { largestEndlessOpeningThrough(world, it.pos, Int.MAX_VALUE) } ?: 0

fun countStrandedEndlessSections(world: EndlessWorld): Int = world.sections.values.count { section ->
    section.state == SectionState.BUILT && section.pos.sideNeighbors().none { neighbor ->
        val state = world.stateOf(neighbor)
        (state == SectionState.OPEN || state == SectionState.BUILT) &&
            solveEndlessSection(world, section.pos, EndlessEntry.FromSection(neighbor)).solved
    }
}

fun nextEndlessBotMove(world: EndlessWorld, random: Random): WorldCell? = nextEndlessBotMoves(world, random).firstOrNull()

fun nextEndlessBotMoves(world: EndlessWorld, random: Random): List<WorldCell> {
    val open = world.sections.values.filter { it.state == SectionState.OPEN }.map { it.pos }
    if (open.isEmpty()) return emptyList()
    for (pos in open.shuffled(random)) {
        val moves = safeOpenCellsIn(
            world,
            minX = (pos.sx - 1) * ENDLESS_SECTION_SIZE - 1,
            minY = (pos.sy - 1) * ENDLESS_SECTION_SIZE - 1,
            maxX = (pos.sx + 2) * ENDLESS_SECTION_SIZE,
            maxY = (pos.sy + 2) * ENDLESS_SECTION_SIZE,
        )
        if (moves.isNotEmpty()) return moves
    }
    val built = world.sections.values.filter { it.state != SectionState.UNBUILT }.map { it.pos }
    return safeOpenCellsIn(
        world,
        minX = built.minOf { it.sx } * ENDLESS_SECTION_SIZE - 1,
        minY = built.minOf { it.sy } * ENDLESS_SECTION_SIZE - 1,
        maxX = (built.maxOf { it.sx } + 1) * ENDLESS_SECTION_SIZE,
        maxY = (built.maxOf { it.sy } + 1) * ENDLESS_SECTION_SIZE,
    )
}

private fun safeOpenCellsIn(world: EndlessWorld, minX: Int, minY: Int, maxX: Int, maxY: Int): List<WorldCell> {
    val cols = maxX - minX + 1
    val rows = maxY - minY + 1
    val state = MinesweeperSolverState(rows, cols, 0)
    fun cellAt(index: Int) = WorldCell(minX + index % cols, minY + index / cols)
    for (index in 0 until rows * cols) {
        val cell = cellAt(index)
        val onEdge = cell.x == minX || cell.x == maxX || cell.y == minY || cell.y == maxY
        when (world.cellStateAt(cell)) {
            EndlessCellState.REVEALED -> if (!onEdge) state.open(index, world.numberAt(cell))
            EndlessCellState.KNOWN_MINE -> state.markMine(index)
            EndlessCellState.FLAGGED -> if (world.stateOf(cell.section) == SectionState.CLEARED) state.markMine(index)
            EndlessCellState.HIDDEN -> Unit
        }
    }
    val moves = LinkedHashSet<WorldCell>()
    while (true) {
        val deductions = nextMinesweeperDeductions(state, ENDLESS_TECHNIQUES)
        if (deductions.isEmpty()) break
        for (deduction in deductions) {
            deduction.mines.forEach(state::markMine)
            for (index in deduction.safe) {
                val cell = cellAt(index)
                state.markSafeLocked(index)
                if (world.stateOf(cell.section) == SectionState.OPEN && world.cellStateAt(cell) == EndlessCellState.HIDDEN) {
                    moves.add(cell)
                }
            }
        }
    }
    return moves.toList()
}

fun simulateEndlessRun(targetClears: Int, random: Random): EndlessSimulationResult {
    val first = WorldCell(random.nextInt(ENDLESS_SECTION_SIZE), random.nextInt(ENDLESS_SECTION_SIZE))
    val started = startEndlessRun(first, random)
    var world = started.world
    var stats = started.stats
    val settleMillis = ArrayList<Double>()
    val buildsPerSettle = ArrayList<Int>()

    fun result(stuck: Boolean, hitMine: Boolean) =
        EndlessSimulationResult(world.clearedCount, stuck, hitMine, stats, settleMillis, buildsPerSettle, world)

    while (world.clearedCount < targetClears) {
        val moves = nextEndlessBotMoves(world, random)
        if (moves.isEmpty()) return result(stuck = true, hitMine = false)
        for (cell in moves) {
            val reveal = revealEndlessCell(world, cell)
            if (reveal.hitMine) return result(stuck = false, hitMine = true)
            world = reveal.world
        }
        if (endlessNeedsSettle(world)) {
            val startedAt = System.nanoTime()
            val settled = settleEndlessWorld(world, random)
            val elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000.0
            world = settled.world
            stats += settled.stats
            settleMillis.add(elapsedMillis)
            buildsPerSettle.add(settled.stats.builds)
        }
    }
    return result(stuck = false, hitMine = false)
}
