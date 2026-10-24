package com.quietgrid.engine.minesweeper.endless

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class EndlessBuilderTest {

    private val start = ENDLESS_START_SECTION

    private fun openWithoutBuilding(world: EndlessWorld, pos: SectionPos): EndlessWorld =
        world.withSection(world.sections.getValue(pos).copy(state = SectionState.OPEN))

    private fun seenCells(world: EndlessWorld): Set<WorldCell> {
        val result = HashSet<WorldCell>()
        for (section in world.sections.values) for (cell in section.pos.cells()) {
            val state = world.cellStateAt(cell)
            if (state == EndlessCellState.REVEALED || state == EndlessCellState.KNOWN_MINE) {
                result.add(cell)
                result.addAll(cell.neighbors())
            }
        }
        return result
    }

    private fun buildAroundNorth(seed: Int): Pair<EndlessWorld, List<Pair<SectionPos, EndlessBuildResult>>> {
        val random = Random(seed)
        var world = startEndlessRun(WorldCell(4, 4), random).world
        if (world.stateOf(NORTH) == SectionState.BUILT) world = openWithoutBuilding(world, NORTH)
        val before = world
        val builds = mutableListOf<Pair<SectionPos, EndlessBuildResult>>()
        for (neighbor in NORTH.sideNeighbors()) {
            if (world.stateOf(neighbor) != SectionState.UNBUILT) continue
            val result = buildEndlessSection(world, neighbor, 2, random)
            builds += neighbor to result
            world = result.world
        }
        return before to builds
    }

    @Test
    fun `a built section passes all its checks`() {
        var checked = 0
        for (seed in 1..20) {
            val (_, builds) = buildAroundNorth(seed)
            for ((pos, result) in builds) {
                if (result.stats.lastPathHits > 0) continue
                val gradeEntry = EndlessEntry.FromSection(pos.sideNeighbors().first { result.world.stateOf(it) == SectionState.OPEN })
                assertTrue(
                    "seed $seed section $pos",
                    passesEndlessChecks(result.world, pos, endlessTargetsFor(2), gradeEntry, Random(0), lookAhead = false),
                )
                checked++
            }
        }
        assertTrue(checked > 0)
    }

    @Test
    fun `the opening cap rejects an all-safe section`() {
        val world = allSafeWorld(3, mapOf(start to SectionState.OPEN))
        val targets = endlessTargetsFor(1)
        val entry = EndlessEntry.FromSection(start)

        assertFalse(passesEndlessChecks(world, EAST, targets, entry, Random(0), lookAhead = false, grade = false))
        assertTrue(passesEndlessChecks(world, EAST, targets, entry, Random(0), lookAhead = false, grade = false, openingCap = null))
    }

    @Test
    fun `built sections respect the opening cap`() {
        var checked = 0
        for (seed in 1..20) {
            val (_, builds) = buildAroundNorth(seed)
            for ((pos, result) in builds) {
                if (result.stats.lastPathHits > 0) continue
                val largest = largestEndlessOpeningThrough(result.world, pos)
                assertTrue("seed $seed section $pos opening $largest", largest <= ENDLESS_OPENING_CAP)
                checked++
            }
        }
        assertTrue(checked > 0)
    }

    @Test
    fun `halo mines never crowd an unbuilt section`() {
        for (seed in 1..200) {
            val random = Random(seed)
            val tap = WorldCell(random.nextInt(ENDLESS_SECTION_SIZE), random.nextInt(ENDLESS_SECTION_SIZE))
            val protectedCells = (tap.neighbors() + tap).toSet()
            val world = rollEndlessSection(EndlessWorld.EMPTY, start, 1, 9, 9.0 / 81, random, protectedCells)
            for (section in world.sections.values) {
                if (section.state != SectionState.UNBUILT) continue
                assertTrue("seed $seed ${section.pos}", section.layout.count { it == '*' } <= 3)
            }
        }
    }

    @Test
    fun `the first opening is at most thirty cells`() {
        for (seed in 1..10) {
            val started = startEndlessRun(WorldCell(4, 4), Random(seed))
            if (started.stats.lastPathHits > 0) continue
            val largest = largestEndlessOpeningThrough(started.world, ENDLESS_START_SECTION)
            assertTrue("seed $seed opening $largest", largest <= 30)
        }
    }

    @Test
    fun `a closed neighbour does not have to be enterable from the new section`() {
        val world = allSafeWorld(3, mapOf(start to SectionState.OPEN))
            .withSection(section(SectionPos(2, 0), SectionState.BUILT, List(ENDLESS_SECTION_SIZE) { row -> if (row == 8) "********." else "*********" }))

        assertTrue(
            passesEndlessChecks(world, EAST, endlessTargetsFor(1), EndlessEntry.FromSection(start), Random(0), lookAhead = false, openingCap = null),
        )
    }

    @Test
    fun `a section between two open neighbours needs only one way in`() {
        val northEast = SectionPos(1, -1)
        val wallFacingNorth = List(ENDLESS_SECTION_SIZE) { "*........" }
        val world = allSafeWorld(3, mapOf(start to SectionState.CLEARED, NORTH to SectionState.OPEN, EAST to SectionState.OPEN))
            .withSection(section(northEast, SectionState.BUILT, wallFacingNorth))

        assertFalse(solveEndlessSection(world, northEast, EndlessEntry.FromSection(NORTH)).solved)
        assertTrue(
            passesEndlessChecks(world, northEast, endlessTargetsFor(1), EndlessEntry.FromSection(NORTH), Random(0), lookAhead = false, grade = false, openingCap = null),
        )
    }

    @Test
    fun `a closed built neighbour can be the way in`() {
        val northEast = SectionPos(1, -1)
        val wallFacingSouth = List(ENDLESS_SECTION_SIZE) { row -> if (row == 8) "*********" else "........." }
        val world = allSafeWorld(3, mapOf(start to SectionState.CLEARED, NORTH to SectionState.BUILT, EAST to SectionState.OPEN))
            .withSection(section(northEast, SectionState.BUILT, wallFacingSouth))

        assertFalse(solveEndlessSection(world, northEast, EndlessEntry.FromSection(EAST)).solved)
        assertTrue(
            passesEndlessChecks(world, northEast, endlessTargetsFor(1), EndlessEntry.FromSection(EAST), Random(0), lookAhead = false, grade = false, openingCap = null),
        )
    }

    @Test
    fun `a failing look-ahead never forces the last path`() {
        val beyond = SectionPos(2, 0)
        val walled = List(ENDLESS_SECTION_SIZE) { row -> if (row == 8) "********." else "*********" }
        val base = allSafeWorld(3, mapOf(start to SectionState.OPEN))
        val world = EndlessWorld(base.sections - EAST)
            .withSection(section(beyond, SectionState.UNBUILT, walled))

        val result = buildEndlessSection(world, EAST, 1, Random(0), EndlessEntry.FromSection(start), openingCap = null)

        assertEquals(0, result.stats.lastPathHits)
        assertTrue(solveEndlessSection(result.world, EAST, EndlessEntry.FromSection(start)).solved)
    }

    @Test
    fun `a built section and its halo are fully fixed`() {
        for (seed in 1..10) {
            val (_, builds) = buildAroundNorth(seed)
            val world = builds.lastOrNull()?.second?.world ?: continue
            for (section in world.sections.values) {
                if (section.state == SectionState.UNBUILT) continue
                assertTrue((section.pos.cells() + section.pos.halo()).all { world.mineAt(it) != null })
            }
        }
    }

    @Test
    fun `seen cells never change`() {
        for (seed in 1..10) {
            val (before, builds) = buildAroundNorth(seed)
            val after = builds.lastOrNull()?.second?.world ?: continue
            for (cell in seenCells(before)) assertEquals("seed $seed $cell", before.mineAt(cell), after.mineAt(cell))
        }
    }

    @Test
    fun `only rebuildable cells change when a section is rebuilt`() {
        var rebuilt = 0
        for (seed in 1..20) {
            val (_, builds) = buildAroundNorth(seed)
            val world = builds.lastOrNull()?.second?.world ?: continue
            val candidate = world.sections.values.firstOrNull { it.state == SectionState.BUILT && !world.isSeen(it.pos) } ?: continue
            val allowed = rebuildableCells(world, candidate.pos)
            val result = rebuildEndlessSection(world, candidate.pos, Random(seed)) ?: continue
            rebuilt++
            for (section in world.sections.values) for (cell in section.pos.cells()) {
                val old = world.mineAt(cell) ?: continue
                if (result.mineAt(cell) != old) assertTrue("seed $seed $cell", cell in allowed)
            }
        }
        assertTrue(rebuilt > 0)
    }

    @Test
    fun `seen cells are never rebuildable`() {
        val world = startEndlessRun(WorldCell(4, 4), Random(3)).world
        val seen = seenCells(world)

        for (section in world.sections.values) {
            if (section.state == SectionState.UNBUILT) continue
            assertTrue(rebuildableCells(world, section.pos).none { it in seen })
        }
    }

    @Test
    fun `a partly seen section keeps its unseen cells rebuildable`() {
        val base = allSafeWorld(2, mapOf(start to SectionState.OPEN))
        val east = base.sections.getValue(EAST)
        val revealedCell = WorldCell(13, 4)
        val cells = StringBuilder(east.cells).apply { set(revealedCell.localIndex, 'o') }.toString()
        val world = base.withSection(east.copy(cells = cells))

        val interior = EAST.cells().filter { (it.x - 9) in 1..7 && it.y in 1..7 }.toSet()
        val seen = (revealedCell.neighbors() + revealedCell).toSet()

        assertEquals(interior - seen, rebuildableCells(world, EAST))
    }

    @Test
    fun `rebuildable cells skip other sections and their halos`() {
        val northEast = SectionPos(1, -1)
        val world = EndlessWorld(allSafeWorld(2, mapOf(start to SectionState.OPEN)).sections - northEast)

        val expected = EAST.cells().filter { (it.x - 9) in 1..7 && it.y in 0..7 }.toSet() +
            (11..15).map { WorldCell(it, -1) }

        assertEquals(expected, rebuildableCells(world, EAST))
    }

    @Test
    fun `a rebuild without an open neighbour is refused`() {
        val beyond = SectionPos(2, 0)
        val world = allSafeWorld(3, mapOf(start to SectionState.CLEARED, EAST to SectionState.CLEARED))

        assertTrue(rebuildableCells(world, beyond).isNotEmpty())
        assertEquals(null, rebuildEndlessSection(world, beyond, Random(1)))
    }

    @Test
    fun `protected first-tap cells stay safe`() {
        for (seed in 1..10) {
            val tap = WorldCell(8, 0)
            val world = startEndlessRun(tap, Random(seed)).world
            for (cell in tap.neighbors() + tap) assertFalse("seed $seed $cell", world.mineAt(cell)!!)
            assertEquals(EndlessCellState.REVEALED, world.cellStateAt(tap))
        }
    }
}
