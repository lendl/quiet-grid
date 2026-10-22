package com.quietgrid.engine.minesweeper.endless

import kotlinx.serialization.Serializable

internal const val LAYOUT_UNFIXED = '?'
internal const val LAYOUT_SAFE = '.'
internal const val LAYOUT_MINE = '*'
internal const val CELL_HIDDEN = '-'
internal const val CELL_REVEALED = 'o'
internal const val CELL_FLAGGED = 'f'
internal const val CELL_KNOWN_MINE = 'x'

@Serializable
enum class SectionState { UNBUILT, BUILT, OPEN, CLEARED }

enum class EndlessCellState { HIDDEN, REVEALED, FLAGGED, KNOWN_MINE }

@Serializable
data class EndlessSection(
    val pos: SectionPos,
    val state: SectionState,
    val level: Int,
    val layout: String,
    val cells: String,
)

data class EndlessWorld(val sections: Map<SectionPos, EndlessSection>) {

    fun stateOf(pos: SectionPos): SectionState = sections[pos]?.state ?: SectionState.UNBUILT

    fun mineAt(cell: WorldCell): Boolean? = when (sections[cell.section]?.layout?.get(cell.localIndex)) {
        LAYOUT_MINE -> true
        LAYOUT_SAFE -> false
        else -> null
    }

    fun cellStateAt(cell: WorldCell): EndlessCellState = when (sections[cell.section]?.cells?.get(cell.localIndex)) {
        CELL_REVEALED -> EndlessCellState.REVEALED
        CELL_FLAGGED -> EndlessCellState.FLAGGED
        CELL_KNOWN_MINE -> EndlessCellState.KNOWN_MINE
        else -> EndlessCellState.HIDDEN
    }

    fun numberAt(cell: WorldCell): Int = cell.neighbors().count { neighbor ->
        requireNotNull(mineAt(neighbor)) { "Unfixed neighbour $neighbor of $cell" }
    }

    val clearedCount: Int get() = sections.values.count { it.state == SectionState.CLEARED }

    val furthestClearedRing: Int
        get() = sections.values.filter { it.state == SectionState.CLEARED }.maxOfOrNull { it.pos.ring } ?: 0

    fun isResolved(pos: SectionPos): Boolean {
        val section = sections[pos] ?: return false
        return (0 until ENDLESS_SECTION_CELLS).all { section.layout[it] == LAYOUT_MINE || section.cells[it] == CELL_REVEALED }
    }

    fun isSeen(pos: SectionPos): Boolean = (pos.cells() + pos.halo()).any {
        val state = cellStateAt(it)
        state == EndlessCellState.REVEALED || state == EndlessCellState.KNOWN_MINE
    }

    fun withSection(section: EndlessSection): EndlessWorld = EndlessWorld(sections + (section.pos to section))

    fun toSectionList(): List<EndlessSection> = sections.values.sortedWith(compareBy({ it.pos.sy }, { it.pos.sx }))

    companion object {
        val EMPTY = EndlessWorld(emptyMap())

        fun of(sections: List<EndlessSection>): EndlessWorld = EndlessWorld(sections.associateBy { it.pos })
    }
}

private const val LAYOUT_CODES = "?.*"
private const val CELL_CODES = "-ofx"
private const val PACKED_BASE = 'a'

fun encodeEndlessSections(world: EndlessWorld): List<String> = world.toSectionList().map { section ->
    val packed = CharArray(ENDLESS_SECTION_CELLS) { index ->
        PACKED_BASE + LAYOUT_CODES.indexOf(section.layout[index]) * CELL_CODES.length + CELL_CODES.indexOf(section.cells[index])
    }
    "${section.pos.sx},${section.pos.sy},${section.state.ordinal},${section.level},${String(packed)}"
}

fun decodeEndlessSections(encoded: List<String>): EndlessWorld? {
    val sections = encoded.map { decodeEndlessSection(it) ?: return null }
    return EndlessWorld.of(sections)
}

private fun decodeEndlessSection(text: String): EndlessSection? {
    val parts = text.split(',')
    if (parts.size != 5) return null
    val sx = parts[0].toIntOrNull() ?: return null
    val sy = parts[1].toIntOrNull() ?: return null
    val state = SectionState.entries.getOrNull(parts[2].toIntOrNull() ?: return null) ?: return null
    val level = parts[3].toIntOrNull() ?: return null
    val packed = parts[4]
    if (packed.length != ENDLESS_SECTION_CELLS) return null
    val layout = CharArray(ENDLESS_SECTION_CELLS)
    val cells = CharArray(ENDLESS_SECTION_CELLS)
    for (index in packed.indices) {
        val code = packed[index] - PACKED_BASE
        if (code !in 0 until LAYOUT_CODES.length * CELL_CODES.length) return null
        layout[index] = LAYOUT_CODES[code / CELL_CODES.length]
        cells[index] = CELL_CODES[code % CELL_CODES.length]
    }
    return EndlessSection(SectionPos(sx, sy), state, level, String(layout), String(cells))
}
