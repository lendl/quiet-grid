package com.quietgrid.engine.minesweeper.endless

import kotlin.random.Random

internal val NORTH = SectionPos(0, -1)
internal val EAST = SectionPos(1, 0)
internal val SOUTH = SectionPos(0, 1)
internal val WEST = SectionPos(-1, 0)

internal fun section(
    pos: SectionPos,
    state: SectionState,
    rows: List<String>? = null,
    cells: String = "-".repeat(ENDLESS_SECTION_CELLS),
    level: Int = 1,
): EndlessSection {
    val layout = rows?.joinToString("") ?: ".".repeat(ENDLESS_SECTION_CELLS)
    require(layout.length == ENDLESS_SECTION_CELLS && cells.length == ENDLESS_SECTION_CELLS)
    return EndlessSection(pos, state, if (state == SectionState.UNBUILT) 0 else level, layout, cells)
}

internal fun allSafeWorld(radius: Int, states: Map<SectionPos, SectionState>): EndlessWorld {
    val sections = mutableListOf<EndlessSection>()
    for (sy in -radius..radius) for (sx in -radius..radius) {
        val pos = SectionPos(sx, sy)
        sections += section(pos, states[pos] ?: SectionState.BUILT)
    }
    return EndlessWorld.of(sections)
}

internal val WALLED_ROWS: List<String> = List(ENDLESS_SECTION_SIZE) { row -> if (row == 4) "****.****" else "*********" }

internal fun islandWorld(radius: Int, safe: Set<SectionPos>, states: Map<SectionPos, SectionState> = emptyMap()): EndlessWorld {
    val sections = mutableListOf<EndlessSection>()
    for (sy in -radius..radius) for (sx in -radius..radius) {
        val pos = SectionPos(sx, sy)
        sections += section(pos, states[pos] ?: SectionState.BUILT, if (pos in safe) null else WALLED_ROWS)
    }
    return EndlessWorld.of(sections)
}

internal fun localCell(pos: SectionPos, col: Int, row: Int) =
    WorldCell(pos.sx * ENDLESS_SECTION_SIZE + col, pos.sy * ENDLESS_SECTION_SIZE + row)

internal fun rowsWithMines(vararg mines: Pair<Int, Int>): List<String> =
    List(ENDLESS_SECTION_SIZE) { row ->
        String(CharArray(ENDLESS_SECTION_SIZE) { col -> if (col to row in mines) '*' else '.' })
    }

internal fun fixedPair(target: SectionPos, entry: SectionPos, targetMines: Int, entryMines: Int, random: Random): EndlessWorld {
    val layouts = HashMap<SectionPos, CharArray>()
    fun setCell(cell: WorldCell, mine: Boolean) {
        layouts.getOrPut(cell.section) { CharArray(ENDLESS_SECTION_CELLS) { '?' } }[cell.localIndex] = if (mine) '*' else '.'
    }
    fun fillSection(pos: SectionPos, mines: Int) {
        val chosen = (0 until ENDLESS_SECTION_CELLS).shuffled(random).take(mines).toSet()
        pos.cells().forEachIndexed { index, cell -> setCell(cell, index in chosen) }
    }
    fillSection(target, targetMines)
    fillSection(entry, entryMines)
    for (cell in target.halo() + entry.halo()) {
        if (cell.section != target && cell.section != entry) setCell(cell, random.nextDouble() < 0.1)
    }
    return EndlessWorld.of(
        layouts.map { (pos, chars) ->
            val layout = String(chars)
            when (pos) {
                target -> EndlessSection(pos, SectionState.BUILT, 1, layout, "-".repeat(ENDLESS_SECTION_CELLS))
                entry -> EndlessSection(pos, SectionState.CLEARED, 1, layout, layout.map { if (it == '*') 'f' else 'o' }.joinToString(""))
                else -> EndlessSection(pos, SectionState.UNBUILT, 0, layout, "-".repeat(ENDLESS_SECTION_CELLS))
            }
        },
    )
}

internal fun revealEverySafeCell(world: EndlessWorld, pos: SectionPos): EndlessWorld {
    var current = world
    for (cell in pos.cells()) {
        if (current.mineAt(cell) == false && current.cellStateAt(cell) == EndlessCellState.HIDDEN) {
            current = revealEndlessCell(current, cell).world
        }
    }
    return current
}
