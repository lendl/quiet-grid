package com.quietgrid.engine.minesweeper.endless

import kotlin.random.Random

data class EndlessSettleResult(val world: EndlessWorld, val newlyCleared: Int, val stats: EndlessBuildStats)

fun startEndlessRun(cell: WorldCell, random: Random): EndlessSettleResult {
    require(cell.section == ENDLESS_START_SECTION) { "The first tap $cell must be in the start section" }
    val protectedCells = (cell.neighbors() + cell).toSet()
    val start = buildEndlessSection(
        EndlessWorld.EMPTY,
        ENDLESS_START_SECTION,
        level = 1,
        random = random,
        entry = EndlessEntry.FirstTap(cell),
        protectedCells = protectedCells,
    )
    var world = start.world.withSection(start.world.sections.getValue(ENDLESS_START_SECTION).copy(state = SectionState.OPEN))
    var stats = start.stats
    for (neighbor in ENDLESS_START_SECTION.sideNeighbors()) {
        if (world.stateOf(neighbor) != SectionState.UNBUILT) continue
        val built = buildEndlessSection(world, neighbor, level = 1, random = random)
        world = built.world
        stats += built.stats
    }
    world = revealEndlessCell(world, cell).world
    val settled = settleEndlessWorld(world, random)
    return EndlessSettleResult(settled.world, settled.newlyCleared, stats + settled.stats)
}

fun settleEndlessWorld(world: EndlessWorld, random: Random): EndlessSettleResult {
    var current = world
    var cleared = 0
    var stats = EndlessBuildStats()
    while (true) {
        var changed = false
        for (pos in openSectionsInOrder(current)) {
            if (current.stateOf(pos) != SectionState.OPEN || !current.isResolved(pos)) continue
            current = clearSection(current, pos)
            cleared++
            changed = true
        }
        for (pos in openSectionsInOrder(current)) {
            for (neighbor in pos.sideNeighbors()) {
                if (current.stateOf(neighbor) != SectionState.UNBUILT) continue
                val built = buildEndlessSection(current, neighbor, current.clearedCount + 1, random, EndlessEntry.FromSection(pos))
                current = built.world
                stats += built.stats
                changed = true
            }
        }
        if (!changed) break
    }
    return EndlessSettleResult(current, cleared, stats)
}

private fun openSectionsInOrder(world: EndlessWorld): List<SectionPos> =
    world.sections.values.filter { it.state == SectionState.OPEN }.map { it.pos }
        .sortedWith(compareBy({ it.ring }, { it.sy }, { it.sx }))

private fun clearSection(world: EndlessWorld, pos: SectionPos): EndlessWorld {
    val section = world.sections.getValue(pos)
    val cells = CharArray(ENDLESS_SECTION_CELLS) { index ->
        val current = section.cells[index]
        if (section.layout[index] == LAYOUT_MINE && (current == CELL_HIDDEN || current == CELL_FLAGGED)) CELL_FLAGGED else current
    }
    var result = world.withSection(section.copy(state = SectionState.CLEARED, cells = String(cells)))
    val cleared = result
    for (neighbor in pos.sideNeighbors()) {
        val other = result.sections[neighbor] ?: continue
        if (other.state != SectionState.BUILT) continue
        if (solveEndlessSection(cleared, neighbor, EndlessEntry.FromSection(pos)).solved) {
            result = result.withSection(other.copy(state = SectionState.OPEN))
        }
    }
    return result
}
