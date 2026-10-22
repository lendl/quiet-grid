package com.quietgrid.engine.minesweeper.endless

data class EndlessReveal(val world: EndlessWorld, val hitMine: Boolean)

fun canTapEndlessCell(world: EndlessWorld, cell: WorldCell): Boolean {
    if (world.stateOf(cell.section) != SectionState.OPEN) return false
    val state = world.cellStateAt(cell)
    return state == EndlessCellState.HIDDEN || state == EndlessCellState.FLAGGED
}

fun revealEndlessCell(world: EndlessWorld, cell: WorldCell): EndlessReveal {
    if (world.stateOf(cell.section) != SectionState.OPEN || world.cellStateAt(cell) != EndlessCellState.HIDDEN) {
        return EndlessReveal(world, hitMine = false)
    }
    val isMine = world.mineAt(cell) ?: return EndlessReveal(world, hitMine = false)
    val edits = CellEdits(world)
    if (isMine) {
        edits.set(cell, CELL_KNOWN_MINE)
        return EndlessReveal(edits.applyTo(world), hitMine = true)
    }
    val allowed = (cell.section.sideNeighbors() + cell.section).filter { world.stateOf(it) != SectionState.UNBUILT }.toSet()
    val opened = HashSet<WorldCell>()
    val stack = ArrayDeque<WorldCell>()
    stack.add(cell)
    while (stack.isNotEmpty()) {
        val next = stack.removeLast()
        if (next in opened || next.section !in allowed) continue
        if (world.cellStateAt(next) != EndlessCellState.HIDDEN || world.mineAt(next) != false) continue
        opened.add(next)
        edits.set(next, CELL_REVEALED)
        if (world.numberAt(next) == 0) stack.addAll(next.neighbors())
    }
    return EndlessReveal(edits.applyTo(world), hitMine = false)
}

fun toggleEndlessFlag(world: EndlessWorld, cell: WorldCell): EndlessWorld {
    if (world.stateOf(cell.section) != SectionState.OPEN) return world
    val next = when (world.cellStateAt(cell)) {
        EndlessCellState.HIDDEN -> CELL_FLAGGED
        EndlessCellState.FLAGGED -> CELL_HIDDEN
        else -> return world
    }
    return CellEdits(world).apply { set(cell, next) }.applyTo(world)
}

fun endlessNeedsSettle(world: EndlessWorld): Boolean = world.sections.values.any { section ->
    section.state == SectionState.OPEN &&
        (world.isResolved(section.pos) || section.pos.sideNeighbors().any { world.stateOf(it) == SectionState.UNBUILT })
}

internal class CellEdits(private val world: EndlessWorld) {
    private val builders = HashMap<SectionPos, StringBuilder>()

    fun set(cell: WorldCell, value: Char) {
        builders.getOrPut(cell.section) { StringBuilder(world.sections.getValue(cell.section).cells) }[cell.localIndex] = value
    }

    fun applyTo(target: EndlessWorld): EndlessWorld {
        if (builders.isEmpty()) return target
        val updated = HashMap(target.sections)
        for ((pos, builder) in builders) updated[pos] = updated.getValue(pos).copy(cells = builder.toString())
        return EndlessWorld(updated)
    }
}
