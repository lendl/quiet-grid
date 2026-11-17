package com.quietgrid.app.games.circuit

import android.content.Context
import com.quietgrid.app.MainDispatcherRule
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.data.ActiveSessionEnvelope
import com.quietgrid.app.testutil.FakeBadgeStore
import com.quietgrid.app.testutil.FakeHistoryStore
import com.quietgrid.app.testutil.FakeJourneyStore
import com.quietgrid.app.testutil.FakeSessionStore
import com.quietgrid.app.testutil.FakeStatsStore
import com.quietgrid.engine.circuit.CIRCUIT_DOWN
import com.quietgrid.engine.circuit.CIRCUIT_LEFT
import com.quietgrid.engine.circuit.CIRCUIT_RIGHT
import com.quietgrid.engine.circuit.CIRCUIT_UP
import com.quietgrid.engine.circuit.CircuitPuzzleEntry
import com.quietgrid.engine.circuit.rotateCircuitMask
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

private val solution = listOf(
    CIRCUIT_RIGHT or CIRCUIT_DOWN, CIRCUIT_LEFT or CIRCUIT_RIGHT or CIRCUIT_DOWN, CIRCUIT_LEFT or CIRCUIT_DOWN,
    CIRCUIT_UP or CIRCUIT_DOWN, CIRCUIT_UP or CIRCUIT_DOWN, CIRCUIT_UP or CIRCUIT_DOWN,
    CIRCUIT_UP, CIRCUIT_UP, CIRCUIT_UP,
)

private val scrambled = CircuitPuzzleEntry(
    id = "comb",
    rows = 3,
    cols = 3,
    difficulty = "easy",
    source = 4,
    solution = solution,
    start = solution.map { rotateCircuitMask(it, 3) },
)

private val oneOff = scrambled.copy(
    id = "one-off",
    start = solution.mapIndexed { cell, mask -> if (cell == 6) rotateCircuitMask(mask) else mask },
)

@OptIn(ExperimentalCoroutinesApi::class)
class CircuitPlayViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Before
    fun setUp() {
        mockkObject(CircuitPuzzleBank)
        coEvery { CircuitPuzzleBank.randomPuzzle(any(), any(), any()) } returns scrambled
    }

    @After
    fun tearDown() {
        unmockkObject(CircuitPuzzleBank)
    }

    private fun newViewModel(sessionStore: FakeSessionStore = FakeSessionStore(), resume: Boolean = false): CircuitPlayViewModel {
        val viewModel = CircuitPlayViewModel(
            mockk<Context>(relaxed = true),
            sessionStore,
            FakeStatsStore(),
            FakeHistoryStore(),
            FakeJourneyStore(),
            FakeBadgeStore(),
            Difficulty.EASY,
            resume = resume,
            journey = null,
        )
        viewModel.hintDispatcher = StandardTestDispatcher(mainDispatcherRule.dispatcher.scheduler)
        return viewModel
    }

    private fun runHint() = mainDispatcherRule.dispatcher.scheduler.runCurrent()

    @Test
    fun `a hint applies one tile, charges once and shows`() {
        val vm = newViewModel()
        vm.toggleHint()
        runHint()
        val hint = vm.hint!!
        val session = vm.session!!
        assertEquals(1, session.hintsUsed)
        assertTrue(session.locked[hint.cell])
        assertEquals(solution[hint.cell], session.masks[hint.cell])
    }

    @Test
    fun `pressing hint again turns it off without charging`() {
        val vm = newViewModel()
        vm.toggleHint()
        runHint()
        vm.toggleHint()
        assertNull(vm.hint)
        assertEquals(1, vm.session!!.hintsUsed)
    }

    @Test
    fun `the next hint after turning it off charges again`() {
        val vm = newViewModel()
        vm.toggleHint()
        runHint()
        val first = vm.hint!!.cell
        vm.toggleHint()
        vm.toggleHint()
        runHint()
        assertEquals(2, vm.session!!.hintsUsed)
        assertNotEquals(first, vm.hint!!.cell)
    }

    @Test
    fun `a second hint press while computing is ignored`() {
        val vm = newViewModel()
        vm.toggleHint()
        vm.toggleHint()
        runHint()
        assertNotNull(vm.hint)
        assertEquals(1, vm.session!!.hintsUsed)
        assertEquals(1, vm.session!!.locked.count { it })
    }

    @Test
    fun `turning a tile or toggling a lock clears the hint`() {
        val vm = newViewModel()
        vm.toggleHint()
        runHint()
        val other = (0 until 9).first { !vm.session!!.locked[it] }
        vm.onTileTap(other)
        assertNull(vm.hint)

        vm.toggleHint()
        runHint()
        assertNotNull(vm.hint)
        val unlocked = (0 until 9).first { !vm.session!!.locked[it] }
        vm.onTileLongPress(unlocked)
        assertNull(vm.hint)
    }

    @Test
    fun `turning a tile while a hint is computing cancels it without charging`() {
        val vm = newViewModel()
        vm.toggleHint()
        vm.onTileTap(0)
        runHint()
        assertNull(vm.hint)
        assertEquals(0, vm.session!!.hintsUsed)
        assertEquals(1, vm.session!!.moves)
        assertEquals(0, vm.session!!.locked.count { it })
    }

    @Test
    fun `a blocked tap keeps the hint`() {
        val vm = newViewModel()
        vm.toggleHint()
        runHint()
        val hint = vm.hint!!
        vm.onTileTap(hint.cell)
        assertEquals(hint, vm.hint)
        assertTrue(vm.lastTapEvent!!.blocked)
    }

    @Test
    fun `a hint that fixes the last tile wins the puzzle`() {
        coEvery { CircuitPuzzleBank.randomPuzzle(any(), any(), any()) } returns oneOff
        val vm = newViewModel()
        vm.toggleHint()
        runHint()
        assertEquals(6, vm.hint!!.cell)
        assertEquals(CircuitStatus.WON, vm.session!!.status)
        assertEquals(1, vm.session!!.hintsUsed)
    }

    @Test
    fun `a resumed session has no hint`() {
        val store = FakeSessionStore().apply {
            preload(
                ActiveSessionEnvelope(
                    gameId = GameId.CIRCUIT.key,
                    elapsedSeconds = 12.0,
                    payload = Json.encodeToString(
                        CircuitPersistedSession.serializer(),
                        CircuitPersistedSession(
                            puzzle = scrambled,
                            turns = List(9) { 0 },
                            locked = List(9) { it == 0 },
                            moves = 3,
                            hintsUsed = 1,
                            status = CircuitStatus.PLAYING.name,
                        ),
                    ),
                ),
            )
        }
        val vm = newViewModel(store, resume = true)
        runHint()
        assertEquals(3, vm.session!!.moves)
        assertNull(vm.hint)
    }
}
