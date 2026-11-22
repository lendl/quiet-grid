package com.quietgrid.engine.minesweeper.endless

data class EndlessReveal(val world: EndlessWorld, val hitMine: Boolean)

private val EDGE_INDICES = (0 until ENDLESS_SECTION_CELLS).filter { index ->
    val col = index % ENDLESS_SECTION_SIZE
    val row = index / ENDLESS_SECTION_SIZE
    col == 0 || row == 0 || col == ENDLESS_SECTION_SIZE - 1 || row == ENDLESS_SECTION_SIZE - 1
}

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
    if (isMine) {
        val edits = CellEdits(world)
        edits.set(cell, CELL_KNOWN_MINE)
        return EndlessReveal(edits.applyTo(world), hitMine = true)
    }
    return EndlessReveal(floodEndless(world, listOf(cell)), hitMine = false)
}

fun chordEndlessCell(world: EndlessWorld, cell: WorldCell): EndlessReveal {
    val unchanged = EndlessReveal(world, hitMine = false)
    if (world.stateOf(cell.section) != SectionState.OPEN || world.cellStateAt(cell) != EndlessCellState.REVEALED) return unchanged
    val number = world.numberAt(cell)
    if (number == 0) return unchanged
    val neighbors = cell.neighbors()
    val marked = neighbors.count {
        val state = world.cellStateAt(it)
        state == EndlessCellState.FLAGGED || state == EndlessCellState.KNOWN_MINE
    }
    val covered = neighbors.filter {
        world.cellStateAt(it) == EndlessCellState.HIDDEN && world.stateOf(it.section) != SectionState.UNBUILT && world.mineAt(it) != null
    }
    if (marked != number || covered.isEmpty()) return unchanged
    val (mines, safe) = covered.partition { world.mineAt(it) == true }
    val opened = floodEndless(world, safe)
    if (mines.isEmpty()) return EndlessReveal(opened, hitMine = false)
    val edits = CellEdits(opened)
    for (mine in mines) edits.set(mine, CELL_KNOWN_MINE)
    return EndlessReveal(edits.applyTo(opened), hitMine = true)
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

fun endlessNeedsSettle(world: EndlessWorld): Boolean =
    world.sections.values.any { section ->
        section.state == SectionState.OPEN &&
            (world.isResolved(section.pos) || section.pos.sideNeighbors().any { world.stateOf(it) == SectionState.UNBUILT })
    } || hiddenCellsBesideRevealedZeros(world).isNotEmpty()

fun endlessStalledSections(world: EndlessWorld): Set<SectionPos> =
    hiddenCellsBesideRevealedZeros(world).map { it.section }.filter { world.stateOf(it) == SectionState.UNBUILT }.toSet()

internal fun continueEndlessOpenings(world: EndlessWorld): EndlessWorld {
    val starts = hiddenCellsBesideRevealedZeros(world).filter { world.stateOf(it.section) != SectionState.UNBUILT }
    return if (starts.isEmpty()) world else floodEndless(world, starts)
}

private fun hiddenCellsBesideRevealedZeros(world: EndlessWorld): Set<WorldCell> {
    val result = HashSet<WorldCell>()
    for (section in world.sections.values) {
        if (section.state != SectionState.OPEN && section.state != SectionState.CLEARED) continue
        if (section.state == SectionState.CLEARED && isInsideClearedArea(world, section.pos)) continue
        for (index in EDGE_INDICES) {
            if (section.cells[index] != CELL_REVEALED) continue
            val cell = WorldCell(
                section.pos.sx * ENDLESS_SECTION_SIZE + index % ENDLESS_SECTION_SIZE,
                section.pos.sy * ENDLESS_SECTION_SIZE + index / ENDLESS_SECTION_SIZE,
            )
            if (world.numberAt(cell) != 0) continue
            for (neighbor in cell.neighbors()) {
                if (world.cellStateAt(neighbor) == EndlessCellState.HIDDEN) result.add(neighbor)
            }
        }
    }
    return result
}

private fun isInsideClearedArea(world: EndlessWorld, pos: SectionPos): Boolean {
    for (dy in -1..1) for (dx in -1..1) {
        if ((dx != 0 || dy != 0) && world.stateOf(SectionPos(pos.sx + dx, pos.sy + dy)) != SectionState.CLEARED) return false
    }
    return true
}

private fun floodEndless(world: EndlessWorld, starts: Collection<WorldCell>): EndlessWorld {
    val edits = CellEdits(world)
    val opened = HashSet<WorldCell>()
    val reached = HashSet<SectionPos>()
    val stack = ArrayDeque(starts)
    while (stack.isNotEmpty()) {
        val next = stack.removeLast()
        if (next in opened || world.stateOf(next.section) == SectionState.UNBUILT) continue
        if (world.cellStateAt(next) != EndlessCellState.HIDDEN || world.mineAt(next) != false) continue
        opened.add(next)
        reached.add(next.section)
        edits.set(next, CELL_REVEALED)
        if (world.numberAt(next) == 0) stack.addAll(next.neighbors())
    }
    var result = edits.applyTo(world)
    for (pos in reached) {
        val section = result.sections.getValue(pos)
        if (section.state == SectionState.BUILT) result = result.withSection(section.copy(state = SectionState.OPEN))
    }
    return result
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
