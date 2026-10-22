package com.quietgrid.engine.minesweeper.endless

import kotlinx.serialization.Serializable
import kotlin.math.abs

const val ENDLESS_SECTION_SIZE = 9
const val ENDLESS_SECTION_CELLS = ENDLESS_SECTION_SIZE * ENDLESS_SECTION_SIZE

@Serializable
data class SectionPos(val sx: Int, val sy: Int)

val ENDLESS_START_SECTION = SectionPos(0, 0)

val SectionPos.ring: Int get() = abs(sx) + abs(sy)

fun SectionPos.sideNeighbors(): List<SectionPos> = listOf(
    SectionPos(sx, sy - 1),
    SectionPos(sx + 1, sy),
    SectionPos(sx, sy + 1),
    SectionPos(sx - 1, sy),
)

fun SectionPos.cells(): List<WorldCell> = List(ENDLESS_SECTION_CELLS) { index ->
    WorldCell(sx * ENDLESS_SECTION_SIZE + index % ENDLESS_SECTION_SIZE, sy * ENDLESS_SECTION_SIZE + index / ENDLESS_SECTION_SIZE)
}

fun SectionPos.halo(): List<WorldCell> {
    val left = sx * ENDLESS_SECTION_SIZE - 1
    val top = sy * ENDLESS_SECTION_SIZE - 1
    val right = left + ENDLESS_SECTION_SIZE + 1
    val bottom = top + ENDLESS_SECTION_SIZE + 1
    val result = ArrayList<WorldCell>(40)
    for (y in top..bottom) for (x in left..right) {
        if (y == top || y == bottom || x == left || x == right) result.add(WorldCell(x, y))
    }
    return result
}

data class WorldCell(val x: Int, val y: Int)

val WorldCell.section: SectionPos
    get() = SectionPos(Math.floorDiv(x, ENDLESS_SECTION_SIZE), Math.floorDiv(y, ENDLESS_SECTION_SIZE))

val WorldCell.localIndex: Int
    get() = Math.floorMod(y, ENDLESS_SECTION_SIZE) * ENDLESS_SECTION_SIZE + Math.floorMod(x, ENDLESS_SECTION_SIZE)

fun WorldCell.neighbors(): List<WorldCell> {
    val result = ArrayList<WorldCell>(8)
    for (dy in -1..1) for (dx in -1..1) {
        if (dx != 0 || dy != 0) result.add(WorldCell(x + dx, y + dy))
    }
    return result
}
