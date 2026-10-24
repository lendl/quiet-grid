package com.quietgrid.app.games.minesweeper

import com.quietgrid.app.MainDispatcherRule
import com.quietgrid.app.core.GameId
import com.quietgrid.app.data.ActiveSessionEnvelope
import com.quietgrid.app.data.SESSION_MODE_ENDLESS
import com.quietgrid.app.session.AppForeground
import com.quietgrid.app.testutil.FakeSessionStore
import com.quietgrid.app.testutil.FakeStatsStore
import com.quietgrid.engine.minesweeper.endless.ENDLESS_START_SECTION
import com.quietgrid.engine.minesweeper.endless.EndlessCellState
import com.quietgrid.engine.minesweeper.endless.EndlessWorld
import com.quietgrid.engine.minesweeper.endless.SectionPos
import com.quietgrid.engine.minesweeper.endless.SectionState
import com.quietgrid.engine.minesweeper.endless.WorldCell
import com.quietgrid.engine.minesweeper.endless.cells
import com.quietgrid.engine.minesweeper.endless.encodeEndlessSections
import com.quietgrid.engine.minesweeper.endless.endlessStalledSections
import com.quietgrid.engine.minesweeper.endless.neighbors
import com.quietgrid.engine.minesweeper.endless.revealEndlessCell
import com.quietgrid.engine.minesweeper.endless.section
import com.quietgrid.engine.minesweeper.endless.sideNeighbors
import com.quietgrid.engine.minesweeper.endless.startEndlessRun
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
class MinesweeperEndlessViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val json = Json { ignoreUnknownKeys = true }
    private var foreground = true
    private val start = ENDLESS_START_SECTION

    private fun viewModel(
        store: FakeSessionStore = FakeSessionStore(),
        stats: FakeStatsStore = FakeStatsStore(),
        resume: Boolean = false,
        buildDispatcher: CoroutineDispatcher = mainDispatcherRule.dispatcher,
        seed: Int = 7,
    ) = MinesweeperEndlessViewModel(store, stats, AppForeground { foreground }, resume, buildDispatcher, Random(seed))

    private fun envelopeFor(
        world: EndlessWorld,
        hearts: Int = MINESWEEPER_ENDLESS_HEARTS,
        bestAtStart: Int = 0,
        camera: MinesweeperEndlessCamera = MinesweeperEndlessCamera(),
        elapsedSeconds: Double = 0.0,
    ) = ActiveSessionEnvelope(
        gameId = GameId.MINESWEEPER.key,
        elapsedSeconds = elapsedSeconds,
        payload = json.encodeToString(
            MinesweeperEndlessPersistedSession.serializer(),
            MinesweeperEndlessPersistedSession(encodeEndlessSections(world), hearts, bestAtStart, camera),
        ),
        mode = SESSION_MODE_ENDLESS,
    )

    private fun storeWith(world: EndlessWorld, hearts: Int = MINESWEEPER_ENDLESS_HEARTS, bestAtStart: Int = 0) =
        FakeSessionStore().apply { preload(envelopeFor(world, hearts, bestAtStart)) }

    private fun savedPayload(store: FakeSessionStore): MinesweeperEndlessPersistedSession? {
        val envelope = runBlocking { store.activeSession.first() } ?: return null
        assertEquals(SESSION_MODE_ENDLESS, envelope.mode)
        assertEquals(GameId.MINESWEEPER.key, envelope.gameId)
        return json.decodeFromString(MinesweeperEndlessPersistedSession.serializer(), envelope.payload)
    }

    private fun worldWithOpenMine(): Pair<EndlessWorld, WorldCell> {
        var seed = 1
        while (true) {
            val world = startEndlessRun(WorldCell(4, 4), Random(seed)).world
            val mine = world.sections.values.filter { it.state == SectionState.OPEN }
                .flatMap { it.pos.cells() }
                .firstOrNull { world.mineAt(it) == true && world.cellStateAt(it) == EndlessCellState.HIDDEN }
            if (mine != null) return world to mine
            seed++
        }
    }

    private fun worldOneCellFromClearing(): Pair<EndlessWorld, WorldCell> {
        var seed = 1
        while (true) {
            var world = startEndlessRun(WorldCell(4, 4), Random(seed)).world
            seed++
            if (world.stateOf(start) != SectionState.OPEN) continue
            while (true) {
                val hidden = start.cells().filter { world.mineAt(it) == false && world.cellStateAt(it) == EndlessCellState.HIDDEN }
                if (hidden.size <= 1) {
                    if (hidden.size == 1) return world to hidden.single()
                    break
                }
                world = revealEndlessCell(world, hidden.first()).world
            }
        }
    }

    private fun worldWithStalledOpening(): EndlessWorld {
        for (seed in 1..50) {
            val world = startEndlessRun(WorldCell(4, 4), Random(seed)).world
            for (open in world.sections.values.filter { it.state == SectionState.OPEN }) {
                for (neighbor in open.pos.sideNeighbors()) {
                    val section = world.sections[neighbor] ?: continue
                    if (section.state == SectionState.UNBUILT) continue
                    val touches = open.pos.cells().any { cell ->
                        world.cellStateAt(cell) == EndlessCellState.REVEALED && world.numberAt(cell) == 0 &&
                            cell.neighbors().any { it.section == neighbor }
                    }
                    if (touches) return world.withSection(section.copy(state = SectionState.UNBUILT, cells = "-".repeat(81)))
                }
            }
        }
        error("no seed in 1..50 has an opening across a section border")
    }

    private fun everyOpenSectionHasBuiltNeighbours(world: EndlessWorld): Boolean =
        world.sections.values.filter { it.state == SectionState.OPEN }
            .all { section -> section.pos.sideNeighbors().all { world.stateOf(it) != SectionState.UNBUILT } }

    private fun collectResults(viewModel: MinesweeperEndlessViewModel, into: MutableList<MinesweeperEndlessResult>) =
        CoroutineScope(mainDispatcherRule.dispatcher).launch { viewModel.result.collect { into.add(it) } }

    private fun advance(millis: Long) {
        mainDispatcherRule.dispatcher.scheduler.advanceTimeBy(millis)
        mainDispatcherRule.dispatcher.scheduler.runCurrent()
    }

    @Test
    fun `fresh run waits for the first tap and saves nothing`() {
        val store = FakeSessionStore()
        val viewModel = viewModel(store)

        advance(20_000)

        val session = viewModel.session!!
        assertFalse(session.started)
        assertEquals(MINESWEEPER_ENDLESS_HEARTS, session.hearts)
        assertEquals(0, store.saveCount)
        assertEquals(0, viewModel.elapsedSeconds)
    }

    @Test
    fun `first tap builds the start section and saves an endless envelope`() {
        val store = FakeSessionStore()
        val viewModel = viewModel(store)

        viewModel.onReveal(WorldCell(4, 4))

        val world = viewModel.session!!.world
        assertTrue(viewModel.session!!.started)
        assertEquals(EndlessCellState.REVEALED, world.cellStateAt(WorldCell(4, 4)))
        assertEquals(encodeEndlessSections(world), savedPayload(store)!!.sections)
    }

    @Test
    fun `taps during a build are ignored`() {
        val buildDispatcher = StandardTestDispatcher(mainDispatcherRule.dispatcher.scheduler)
        val viewModel = viewModel(buildDispatcher = buildDispatcher)

        viewModel.onReveal(WorldCell(4, 4))
        assertTrue(viewModel.building)
        viewModel.onReveal(WorldCell(0, 8))
        viewModel.onToggleFlag(WorldCell(8, 0))
        mainDispatcherRule.dispatcher.scheduler.runCurrent()

        assertEquals(startEndlessRun(WorldCell(4, 4), Random(7)).world, viewModel.session!!.world)
        assertFalse(viewModel.building)
    }

    @Test
    fun `taps outside the start section before the first tap do nothing`() {
        val store = FakeSessionStore()
        val viewModel = viewModel(store)

        viewModel.onReveal(WorldCell(20, 20))

        assertFalse(viewModel.session!!.started)
        assertEquals(0, store.saveCount)
    }

    @Test
    fun `closed sections ignore taps and flags`() {
        val (world, _) = worldWithOpenMine()
        val closed = world.sections.values.first { it.state == SectionState.BUILT }.pos.cells()
            .first { world.cellStateAt(it) == EndlessCellState.HIDDEN }
        val viewModel = viewModel(storeWith(world), resume = true)

        viewModel.onReveal(closed)
        viewModel.onToggleFlag(closed)

        assertEquals(world, viewModel.session!!.world)
    }

    @Test
    fun `hitting a mine costs a heart and leaves a known mine`() {
        val (world, mine) = worldWithOpenMine()
        val store = storeWith(world)
        val viewModel = viewModel(store, resume = true)

        viewModel.onReveal(mine)

        val session = viewModel.session!!
        assertEquals(MINESWEEPER_ENDLESS_HEARTS - 1, session.hearts)
        assertEquals(1, session.heartShake)
        assertEquals(EndlessCellState.KNOWN_MINE, session.world.cellStateAt(mine))
        assertEquals(MINESWEEPER_ENDLESS_HEARTS - 1, savedPayload(store)!!.hearts)
    }

    @Test
    fun `third mine ends the run out of hearts`() {
        val (world, mine) = worldWithOpenMine()
        val store = storeWith(world, hearts = 1)
        val stats = FakeStatsStore()
        val viewModel = viewModel(store, stats, resume = true)
        val results = mutableListOf<MinesweeperEndlessResult>()
        val job = collectResults(viewModel, results)

        viewModel.onReveal(mine)
        advance(5_000)

        val result = results.single()
        assertEquals(MINESWEEPER_ENDLESS_REASON_OUT_OF_HEARTS, result.reason)
        assertEquals(world.clearedCount, result.score)
        assertTrue(viewModel.revealMines)
        assertTrue(store.cleared)
        assertEquals(result.score, runBlocking { stats.endlessStatsFor(GameId.MINESWEEPER).first() }.bestScore)
        job.cancel()
    }

    @Test
    fun `clearing a section flags its mines locks it and opens its neighbours`() {
        val (world, lastCell) = worldOneCellFromClearing()
        val viewModel = viewModel(storeWith(world), resume = true)

        viewModel.onReveal(lastCell)

        val after = viewModel.session!!.world
        assertEquals(SectionState.CLEARED, after.stateOf(start))
        assertTrue(viewModel.session!!.score >= 1)
        for (cell in start.cells()) {
            if (after.mineAt(cell) == true) assertEquals(EndlessCellState.FLAGGED, after.cellStateAt(cell))
        }
        for (neighbor in start.sideNeighbors()) {
            val state = after.stateOf(neighbor)
            assertTrue(state == SectionState.OPEN || state == SectionState.CLEARED)
        }
        assertTrue(everyOpenSectionHasBuiltNeighbours(after))
        assertTrue(endlessStalledSections(after).isEmpty())
    }

    @Test
    fun `restore finishes a stalled opening before taps`() {
        val stalled = worldWithStalledOpening()
        assertTrue(endlessStalledSections(stalled).isNotEmpty())
        val buildDispatcher = StandardTestDispatcher(mainDispatcherRule.dispatcher.scheduler)
        val viewModel = viewModel(storeWith(stalled), resume = true, buildDispatcher = buildDispatcher)

        assertTrue(viewModel.building)
        mainDispatcherRule.dispatcher.scheduler.runCurrent()

        assertFalse(viewModel.building)
        assertTrue(endlessStalledSections(viewModel.session!!.world).isEmpty())
    }

    @Test
    fun `end puzzle before the first tap reports a zero score`() {
        val store = FakeSessionStore()
        val viewModel = viewModel(store)
        val results = mutableListOf<MinesweeperEndlessResult>()
        val job = collectResults(viewModel, results)

        viewModel.endRun()
        advance(5_000)

        val result = results.single()
        assertEquals(0, result.score)
        assertEquals(MINESWEEPER_ENDLESS_REASON_ENDED, result.reason)
        assertFalse(viewModel.revealMines)
        assertNull(runBlocking { store.activeSession.first() })
        job.cancel()
    }

    @Test
    fun `ending early keeps the score`() {
        val (almost, lastCell) = worldOneCellFromClearing()
        val viewModel = viewModel(storeWith(almost, bestAtStart = 5), resume = true)
        viewModel.onReveal(lastCell)
        val cleared = viewModel.session!!.world
        val results = mutableListOf<MinesweeperEndlessResult>()
        val job = collectResults(viewModel, results)

        viewModel.endRun()
        advance(5_000)

        val result = results.single()
        assertEquals(cleared.clearedCount, result.score)
        assertEquals(cleared.furthestClearedRing, result.ring)
        assertEquals(5, result.previousBest)
        assertEquals(result.score > 5, result.isNewBest)
        job.cancel()
    }

    @Test
    fun `save then restore gives the same run`() {
        val (world, _) = worldWithOpenMine()
        val camera = MinesweeperEndlessCamera(12.5f, -3f, 0.8f)
        val store = FakeSessionStore().apply { preload(envelopeFor(world, hearts = 2, bestAtStart = 9, camera = camera, elapsedSeconds = 75.0)) }

        val viewModel = viewModel(store, resume = true)

        val session = viewModel.session!!
        assertEquals(world, session.world)
        assertEquals(2, session.hearts)
        assertEquals(9, session.bestAtStart)
        assertEquals(camera, session.camera)
        assertEquals(75, viewModel.elapsedSeconds)
    }

    @Test
    fun `a save that cannot be read starts a fresh run`() {
        val store = FakeSessionStore().apply {
            preload(ActiveSessionEnvelope(gameId = GameId.MINESWEEPER.key, elapsedSeconds = 9.0, payload = "{broken", mode = SESSION_MODE_ENDLESS))
        }

        val viewModel = viewModel(store, resume = true)

        assertFalse(viewModel.session!!.started)
        assertEquals(0, viewModel.elapsedSeconds)
    }

    @Test
    fun `restore finishes pending builds before taps`() {
        val (world, mine) = worldWithOpenMine()
        val closed = world.sections.values.first { it.state == SectionState.BUILT && it.pos.sideNeighbors().any { n -> world.stateOf(n) == SectionState.UNBUILT } }
        val pending = world.withSection(closed.copy(state = SectionState.OPEN))
        val buildDispatcher = StandardTestDispatcher(mainDispatcherRule.dispatcher.scheduler)
        val viewModel = viewModel(storeWith(pending), resume = true, buildDispatcher = buildDispatcher)

        assertTrue(viewModel.building)
        viewModel.onReveal(mine)
        mainDispatcherRule.dispatcher.scheduler.runCurrent()

        assertFalse(viewModel.building)
        assertEquals(MINESWEEPER_ENDLESS_HEARTS, viewModel.session!!.hearts)
        assertTrue(everyOpenSectionHasBuiltNeighbours(viewModel.session!!.world))
    }

    @Test
    fun `time counts only after the first tap and while in the foreground`() {
        val viewModel = viewModel()
        advance(3_000)
        assertEquals(0, viewModel.elapsedSeconds)

        viewModel.onReveal(WorldCell(4, 4))
        advance(3_000)
        assertEquals(3, viewModel.elapsedSeconds)

        foreground = false
        advance(3_000)
        assertEquals(3, viewModel.elapsedSeconds)
    }

    @Test
    fun `camera changes are saved with the run`() {
        val store = FakeSessionStore()
        val viewModel = viewModel(store)
        viewModel.onReveal(WorldCell(4, 4))
        val camera = MinesweeperEndlessCamera(20f, 6f, 0.5f)

        viewModel.onCameraChanged(camera)

        assertEquals(camera, viewModel.session!!.camera)
        assertNotNull(savedPayload(store))
        assertEquals(camera, savedPayload(store)!!.camera)
    }

    @Test
    fun `an unrelated save is left alone and a fresh run starts`() {
        val store = FakeSessionStore().apply {
            preload(ActiveSessionEnvelope(gameId = GameId.SUDOKU.key, elapsedSeconds = 1.0, payload = "{}"))
        }

        val viewModel = viewModel(store, resume = true)

        assertFalse(viewModel.session!!.started)
        assertEquals(GameId.SUDOKU.key, runBlocking { store.activeSession.first() }!!.gameId)
    }
}
