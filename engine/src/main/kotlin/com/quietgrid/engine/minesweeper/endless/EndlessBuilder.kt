package com.quietgrid.engine.minesweeper.endless

import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

const val ENDLESS_REROLL_ATTEMPTS = 40
const val ENDLESS_UNGRADED_ATTEMPTS = 20
const val ENDLESS_REBUILD_ATTEMPTS = 20
const val ENDLESS_LOOKAHEAD_SAMPLES = 24
const val ENDLESS_MAX_MINE_REDUCTION = 3

data class EndlessBuildStats(
    val builds: Int = 0,
    val candidates: Int = 0,
    val rebuilds: Int = 0,
    val minesDropped: Int = 0,
    val lastPathHits: Int = 0,
    val candidatesByLevel: Map<Int, Int> = emptyMap(),
    val buildsByLevel: Map<Int, Int> = emptyMap(),
    val lastPathByLevel: Map<Int, Int> = emptyMap(),
) {
    operator fun plus(other: EndlessBuildStats) = EndlessBuildStats(
        builds = builds + other.builds,
        candidates = candidates + other.candidates,
        rebuilds = rebuilds + other.rebuilds,
        minesDropped = minesDropped + other.minesDropped,
        lastPathHits = lastPathHits + other.lastPathHits,
        candidatesByLevel = mergeCounts(candidatesByLevel, other.candidatesByLevel),
        buildsByLevel = mergeCounts(buildsByLevel, other.buildsByLevel),
        lastPathByLevel = mergeCounts(lastPathByLevel, other.lastPathByLevel),
    )
}

private fun mergeCounts(a: Map<Int, Int>, b: Map<Int, Int>): Map<Int, Int> =
    if (b.isEmpty()) a else if (a.isEmpty()) b else (a.keys + b.keys).associateWith { (a[it] ?: 0) + (b[it] ?: 0) }

data class EndlessBuildResult(val world: EndlessWorld, val stats: EndlessBuildStats, val rebuilt: List<SectionPos>)

fun buildEndlessSection(
    world: EndlessWorld,
    pos: SectionPos,
    level: Int,
    random: Random,
    entry: EndlessEntry? = null,
    protectedCells: Set<WorldCell> = emptySet(),
): EndlessBuildResult {
    val targets = endlessTargetsFor(level)
    var current = world
    var candidates = 0
    var rebuilds = 0
    var lastCandidate: EndlessWorld? = null

    fun finish(result: EndlessWorld, reduction: Int, lastPath: Boolean, rebuilt: List<SectionPos>) = EndlessBuildResult(
        result,
        EndlessBuildStats(
            builds = 1,
            candidates = candidates,
            rebuilds = rebuilds,
            minesDropped = reduction,
            lastPathHits = if (lastPath) 1 else 0,
            candidatesByLevel = mapOf(level to candidates),
            buildsByLevel = mapOf(level to 1),
            lastPathByLevel = if (lastPath) mapOf(level to 1) else emptyMap(),
        ),
        rebuilt,
    )

    fun tryRolls(base: EndlessWorld, mineTotal: Int, attempts: Int, grade: Boolean, lookAhead: Boolean = true): EndlessWorld? {
        repeat(attempts) {
            val candidate = rollEndlessSection(base, pos, level, mineTotal, targets.density, random, protectedCells)
            candidates++
            lastCandidate = candidate
            val entryForCandidate = gradeEntryFor(candidate, pos, entry)
            if (passesEndlessChecks(candidate, pos, targets, entryForCandidate, random, lookAhead = lookAhead, grade = grade)) return candidate
        }
        return null
    }

    for (reduction in 0..ENDLESS_MAX_MINE_REDUCTION) {
        val mineTotal = max(0, targets.mines - reduction)
        tryRolls(current, mineTotal, ENDLESS_REROLL_ATTEMPTS, grade = true)?.let { return finish(it, reduction, false, emptyList()) }
        tryRolls(current, mineTotal, ENDLESS_UNGRADED_ATTEMPTS, grade = false)?.let { return finish(it, reduction, false, emptyList()) }
        for (neighbor in pos.sideNeighbors()) {
            if (current.stateOf(neighbor) != SectionState.BUILT || rebuildableCells(current, neighbor).isEmpty()) continue
            repeat(ENDLESS_REBUILD_ATTEMPTS) {
                val rebuiltWorld = rebuildEndlessSection(current, neighbor, random) ?: return@repeat
                rebuilds++
                tryRolls(rebuiltWorld, mineTotal, ENDLESS_REROLL_ATTEMPTS / 4, grade = false)?.let {
                    return finish(it, reduction, false, listOf(neighbor))
                }
            }
        }
    }
    val lowestMines = max(0, targets.mines - ENDLESS_MAX_MINE_REDUCTION)
    tryRolls(current, lowestMines, ENDLESS_REROLL_ATTEMPTS, grade = false, lookAhead = false)?.let {
        return finish(it, ENDLESS_MAX_MINE_REDUCTION, false, emptyList())
    }
    val fallback = lastCandidate
        ?: rollEndlessSection(current, pos, level, max(0, targets.mines - ENDLESS_MAX_MINE_REDUCTION), targets.density, random, protectedCells)
    return finish(fallback, ENDLESS_MAX_MINE_REDUCTION, true, emptyList())
}

internal fun gradeEntryFor(world: EndlessWorld, pos: SectionPos, entry: EndlessEntry?): EndlessEntry? =
    entry ?: pos.sideNeighbors().firstOrNull { world.stateOf(it) == SectionState.OPEN }?.let { EndlessEntry.FromSection(it) }

internal fun passesEndlessChecks(
    world: EndlessWorld,
    pos: SectionPos,
    targets: EndlessTargets,
    gradeEntry: EndlessEntry?,
    random: Random,
    lookAhead: Boolean = true,
    grade: Boolean = true,
    builtEntries: Boolean = true,
): Boolean {
    val otherEntries = pos.sideNeighbors()
        .filter { neighbor ->
            val state = world.stateOf(neighbor)
            state == SectionState.OPEN || (builtEntries && state == SectionState.BUILT)
        }
        .map { EndlessEntry.FromSection(it) }
        .filter { it != gradeEntry }
    val trace = (listOfNotNull(gradeEntry).asSequence() + otherEntries.asSequence())
        .map { solveEndlessSection(world, pos, it) }
        .firstOrNull { it.solved } ?: return false
    if (grade && gradeEntry != null && !withinBand(trace, targets)) return false
    if (lookAhead) {
        for (neighbor in pos.sideNeighbors()) {
            if (world.stateOf(neighbor) == SectionState.UNBUILT && !lookAheadPasses(world, neighbor, pos, targets, random)) return false
        }
    }
    return true
}

private fun withinBand(trace: EndlessSolveTrace, targets: EndlessTargets): Boolean =
    trace.cluePairSteps in targets.cluePairSteps &&
        trace.bottleneckSteps in targets.bottleneckSteps &&
        trace.largestCascade <= (targets.largestCascadeLimit ?: Int.MAX_VALUE)

private fun lookAheadPasses(world: EndlessWorld, pos: SectionPos, entry: SectionPos, targets: EndlessTargets, random: Random): Boolean {
    repeat(ENDLESS_LOOKAHEAD_SAMPLES) {
        val sample = rollEndlessSection(world, pos, targets.level, targets.mines, targets.density, random, emptySet())
        if (solveEndlessSection(sample, pos, EndlessEntry.FromSection(entry)).solved) return true
    }
    return false
}

internal fun rollEndlessSection(
    world: EndlessWorld,
    pos: SectionPos,
    level: Int,
    mineTotal: Int,
    density: Double,
    random: Random,
    protectedCells: Set<WorldCell>,
): EndlessWorld {
    val layouts = HashMap<SectionPos, CharArray>()
    fun charsOf(section: SectionPos) = layouts.getOrPut(section) {
        (world.sections[section]?.layout ?: UNFIXED_LAYOUT).toCharArray()
    }
    val cells = pos.cells()
    val own = charsOf(pos)
    val free = cells.indices.filter { own[it] == LAYOUT_UNFIXED && cells[it] !in protectedCells }
    val fixedMines = own.count { it == LAYOUT_MINE }
    val chosen = free.shuffled(random).take(min(max(0, mineTotal - fixedMines), free.size)).toHashSet()
    for (index in cells.indices) {
        if (own[index] == LAYOUT_UNFIXED) own[index] = if (index in chosen) LAYOUT_MINE else LAYOUT_SAFE
    }
    for (cell in pos.halo()) {
        val chars = charsOf(cell.section)
        if (chars[cell.localIndex] != LAYOUT_UNFIXED) continue
        val mine = cell !in protectedCells && random.nextDouble() < density
        chars[cell.localIndex] = if (mine) LAYOUT_MINE else LAYOUT_SAFE
    }
    val updated = HashMap(world.sections)
    for ((section, chars) in layouts) {
        val layout = String(chars)
        val existing = world.sections[section]
        updated[section] = when {
            section == pos -> EndlessSection(
                pos,
                if (existing == null || existing.state == SectionState.UNBUILT) SectionState.BUILT else existing.state,
                if (existing == null || existing.state == SectionState.UNBUILT) level else existing.level,
                layout,
                existing?.cells ?: HIDDEN_CELLS,
            )
            existing != null -> existing.copy(layout = layout)
            else -> EndlessSection(section, SectionState.UNBUILT, 0, layout, HIDDEN_CELLS)
        }
    }
    return EndlessWorld(updated)
}

internal fun rebuildableCells(world: EndlessWorld, pos: SectionPos): Set<WorldCell> {
    val others = ArrayList<SectionPos>()
    for (sy in pos.sy - 2..pos.sy + 2) for (sx in pos.sx - 2..pos.sx + 2) {
        val other = SectionPos(sx, sy)
        if (other != pos && world.stateOf(other) != SectionState.UNBUILT) others.add(other)
    }
    return (pos.cells() + pos.halo()).filter { cell ->
        val reach = if (cell.section == pos) 1 else 2
        !isSeenCell(world, cell) && others.none { other -> cell.section == other || isNearSection(cell, other, reach) }
    }.toSet()
}

private fun isSeenCell(world: EndlessWorld, cell: WorldCell): Boolean = (cell.neighbors() + cell).any {
    val state = world.cellStateAt(it)
    state == EndlessCellState.REVEALED || state == EndlessCellState.KNOWN_MINE
}

private fun isNearSection(cell: WorldCell, pos: SectionPos, reach: Int): Boolean {
    val left = pos.sx * ENDLESS_SECTION_SIZE - reach
    val top = pos.sy * ENDLESS_SECTION_SIZE - reach
    val span = ENDLESS_SECTION_SIZE - 1 + reach * 2
    return cell.section != pos && cell.x in left..left + span && cell.y in top..top + span
}

internal fun rebuildEndlessSection(world: EndlessWorld, pos: SectionPos, random: Random): EndlessWorld? {
    val cells = rebuildableCells(world, pos)
    if (cells.isEmpty()) return null
    val section = world.sections[pos] ?: return null
    val unfixed = HashMap<SectionPos, CharArray>()
    for (cell in cells) {
        unfixed.getOrPut(cell.section) { (world.sections[cell.section]?.layout ?: UNFIXED_LAYOUT).toCharArray() }[cell.localIndex] = LAYOUT_UNFIXED
    }
    val sections = HashMap(world.sections)
    for ((key, chars) in unfixed) {
        sections[key] = sections[key]?.copy(layout = String(chars))
            ?: EndlessSection(key, SectionState.UNBUILT, 0, String(chars), HIDDEN_CELLS)
    }
    val entry = pos.sideNeighbors().firstOrNull { world.stateOf(it) == SectionState.OPEN } ?: return null
    val targets = endlessTargetsFor(section.level)
    val candidate = rollEndlessSection(EndlessWorld(sections), pos, section.level, targets.mines, targets.density, random, emptySet())
    return candidate.takeIf {
        passesEndlessChecks(it, pos, targets, EndlessEntry.FromSection(entry), random, grade = false, builtEntries = false)
    }
}

private val UNFIXED_LAYOUT = LAYOUT_UNFIXED.toString().repeat(ENDLESS_SECTION_CELLS)
internal val HIDDEN_CELLS = CELL_HIDDEN.toString().repeat(ENDLESS_SECTION_CELLS)
