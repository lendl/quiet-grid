package com.quietgrid.engine.minesweeper.endless

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class EndlessSimulationTest {

    private val runs = 1..8
    private val clearsPerRun = 40

    @Test
    fun `seeded runs never need a guess`() {
        for (seed in runs) {
            val result = simulateEndlessRun(clearsPerRun, Random(seed))
            val world = result.world

            assertFalse("seed $seed got stuck", result.stuck)
            assertFalse("seed $seed hit a mine", result.hitMine)
            assertEquals("seed $seed hit the last path", 0, result.stats.lastPathHits)
            assertEquals("seed $seed stranded a section", 0, result.strandedSections)
            assertTrue("seed $seed cleared ${result.sectionsCleared}", result.sectionsCleared >= clearsPerRun)
            assertTrue("seed $seed stalled", endlessStalledSections(world).isEmpty())
            assertTrue("seed $seed largest opening ${result.largestOpening}", result.largestOpening <= ENDLESS_OPENING_CAP)
            for (section in world.sections.values) {
                if (section.state == SectionState.UNBUILT) {
                    assertTrue(section.pos.cells().none { world.cellStateAt(it) == EndlessCellState.REVEALED })
                }
                if (section.state == SectionState.OPEN) {
                    assertTrue(section.pos.sideNeighbors().all { world.stateOf(it) != SectionState.UNBUILT })
                }
                if (section.state == SectionState.BUILT) {
                    val wayIn = section.pos.sideNeighbors().any { neighbor ->
                        world.stateOf(neighbor).let { it == SectionState.OPEN || it == SectionState.BUILT } &&
                            solveEndlessSection(world, section.pos, EndlessEntry.FromSection(neighbor)).solved
                    }
                    assertTrue("seed $seed ${section.pos} has no way in", wayIn)
                }
            }
        }
    }

    @Test
    fun `a long run saves in about a hundred characters per section`() {
        val world = simulateEndlessRun(clearsPerRun, Random(11)).world

        val json = Json.encodeToString(ListSerializer(String.serializer()), encodeEndlessSections(world))

        assertTrue("${json.length / world.sections.size} chars per section", json.length / world.sections.size <= 120)
    }

    @Test
    fun `encoded sections decode to the same world`() {
        val world = simulateEndlessRun(10, Random(4)).world

        assertEquals(world, decodeEndlessSections(encodeEndlessSections(world)))
    }

    @Test
    fun `a broken section string does not decode`() {
        assertNull(decodeEndlessSections(listOf("0,0,2,1,abc")))
        assertNull(decodeEndlessSections(listOf("nonsense")))
    }

    @Test
    fun `the bot never offers a mine or a closed cell`() {
        val random = Random(3)
        var world = startEndlessRun(WorldCell(4, 4), random).world
        repeat(30) {
            val moves = nextEndlessBotMoves(world, random)
            for (cell in moves) {
                assertEquals(false, world.mineAt(cell))
                assertEquals(SectionState.OPEN, world.stateOf(cell.section))
                world = revealEndlessCell(world, cell).world
            }
            if (endlessNeedsSettle(world)) world = settleEndlessWorld(world, random).world
        }
    }

    @Test
    fun `no move exists when nothing is open`() {
        assertNull(nextEndlessBotMove(allSafeWorld(1, emptyMap()), Random(1)))
    }
}
