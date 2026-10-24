package com.quietgrid.engine.minesweeper.endless

import kotlin.random.Random

const val ENDLESS_OPENING_CAP = 30
const val ENDLESS_REPAIR_STEPS = 40

private class EndlessOpening(val zeros: Set<WorldCell>, val cells: Set<WorldCell>)

fun largestEndlessOpeningThrough(world: EndlessWorld, pos: SectionPos, limit: Int = ENDLESS_OPENING_CAP): Int =
    largestOpening(world, pos, limit)?.cells?.size ?: 0

internal fun repairEndlessOpenings(world: EndlessWorld, pos: SectionPos, free: Set<Int>, random: Random, cap: Int): EndlessWorld {
    var current = world
    val cells = pos.cells()
    repeat(ENDLESS_REPAIR_STEPS) {
        val opening = largestOpening(current, pos, cap) ?: return current
        if (opening.cells.size <= cap) return current
        val section = current.sections.getValue(pos)
        val targets = opening.cells.filter { it.section == pos && it.localIndex in free && section.layout[it.localIndex] == LAYOUT_SAFE }
        val mines = free.filter { section.layout[it] == LAYOUT_MINE }
        if (targets.isEmpty() || mines.isEmpty()) return current
        val splitting = targets.associateWith { target -> target.neighbors().count { it in opening.zeros } }
        val widest = splitting.values.max()
        val target = targets.filter { splitting.getValue(it) == widest }.random(random)
        val uncovered = mines.associateWith { index -> soleCoverCount(current, cells[index]) }
        val fewest = uncovered.values.min()
        val moved = mines.filter { uncovered.getValue(it) == fewest }.random(random)
        val chars = section.layout.toCharArray()
        chars[moved] = LAYOUT_SAFE
        chars[target.localIndex] = LAYOUT_MINE
        current = current.withSection(section.copy(layout = String(chars)))
    }
    return current
}

private fun largestOpening(world: EndlessWorld, pos: SectionPos, limit: Int): EndlessOpening? {
    val visited = HashSet<WorldCell>()
    var largest: EndlessOpening? = null
    for (cell in pos.cells()) {
        if (cell in visited || !isKnownZero(world, cell)) continue
        val zeros = HashSet<WorldCell>()
        val opening = HashSet<WorldCell>()
        val stack = ArrayDeque<WorldCell>()
        stack.add(cell)
        visited.add(cell)
        while (stack.isNotEmpty()) {
            val zero = stack.removeLast()
            zeros.add(zero)
            opening.add(zero)
            for (neighbor in zero.neighbors()) {
                opening.add(neighbor)
                if (neighbor !in visited && isKnownZero(world, neighbor)) {
                    visited.add(neighbor)
                    stack.add(neighbor)
                }
            }
            if (opening.size > limit) return EndlessOpening(zeros, opening)
        }
        if (opening.size > (largest?.cells?.size ?: 0)) largest = EndlessOpening(zeros, opening)
    }
    return largest
}

private fun soleCoverCount(world: EndlessWorld, mine: WorldCell): Int = mine.neighbors().count { neighbor ->
    world.mineAt(neighbor) == false && neighbor.neighbors().count { world.mineAt(it) == true } == 1
}

private fun isKnownZero(world: EndlessWorld, cell: WorldCell): Boolean =
    world.mineAt(cell) == false && cell.neighbors().all { world.mineAt(it) == false }
