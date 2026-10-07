package com.quietgrid.app.games.whodidit

import android.content.Context
import com.quietgrid.app.MainDispatcherRule
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.data.ActiveSessionEnvelope
import com.quietgrid.app.testutil.FakeHistoryStore
import com.quietgrid.app.testutil.FakeSessionStore
import com.quietgrid.app.testutil.FakeStatsStore
import com.quietgrid.engine.whodidit.WhoDidItPuzzleEntry
import com.quietgrid.engine.whodidit.WhoDidItRef
import com.quietgrid.engine.whodidit.paired
import com.quietgrid.engine.whodidit.whoDidItCategoryPairs
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class WhoDidItPlayViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val easyPuzzle = loadWhoDidItBankForTest().first { it.difficulty == "easy" }

    @Before
    fun setUp() {
        mockkObject(WhoDidItPuzzleBank)
        coEvery { WhoDidItPuzzleBank.randomPuzzle(any(), any(), any()) } returns easyPuzzle
    }

    @After
    fun tearDown() {
        unmockkObject(WhoDidItPuzzleBank)
    }

    private fun newViewModel(
        difficulty: Difficulty = Difficulty.EASY,
        resume: Boolean = false,
        sessionStore: FakeSessionStore = FakeSessionStore(),
    ) = WhoDidItPlayViewModel(
        mockk<Context>(relaxed = true),
        sessionStore,
        FakeStatsStore(),
        FakeHistoryStore(),
        difficulty,
        resume,
        null,
    ).also { it.hintDispatcher = mainDispatcherRule.dispatcher }

    private fun WhoDidItPlayViewModel.showHint() {
        toggleHint()
        mainDispatcherRule.dispatcher.scheduler.runCurrent()
    }

    private fun pairedColumn(puzzle: WhoDidItPuzzleEntry, block: Int, row: Int): Int {
        val (rowCategory, colCategory) = whoDidItCategoryPairs(puzzle.categories.size)[block]
        return (0 until puzzle.size).first { puzzle.paired(WhoDidItRef(rowCategory, row), WhoDidItRef(colCategory, it)) }
    }

    private fun completeWrongSession(puzzle: WhoDidItPuzzleEntry): WhoDidItSession {
        val fresh = createWhoDidItSession(puzzle)
        val blocks = fresh.blocks.mapIndexed { b, block ->
            val columns = block.indices.map { pairedColumn(puzzle, b, it) }
            val shown = if (b == 0) listOf(columns[1], columns[0]) + columns.drop(2) else columns
            block.indices.map { r ->
                block.indices.map { c -> if (shown[r] == c) WhoDidItCellMark.O else WhoDidItCellMark.AUTO_X }
            }
        }
        return fresh.copy(blocks = blocks)
    }

    @Test
    fun `no-op and rejected taps keep the hint while a real change clears it`() {
        val viewModel = newViewModel()
        val puzzle = easyPuzzle
        val match = pairedColumn(puzzle, 0, 0)
        val autoCrossed = (0 until puzzle.size).first { it != match }
        val wrongForRowOne = (0 until puzzle.size).first { it != match && it != pairedColumn(puzzle, 0, 1) }
        viewModel.onCellTap(0, 0, match)
        viewModel.onCellTap(0, 0, match)
        viewModel.onCellTap(0, 1, wrongForRowOne)
        assertEquals(WhoDidItCellMark.AUTO_X, viewModel.session!!.blocks[0][0][autoCrossed])
        assertEquals(WhoDidItCellMark.X, viewModel.session!!.blocks[0][1][wrongForRowOne])

        viewModel.showHint()
        assertNotNull(viewModel.hint)

        viewModel.onCellTap(0, 0, autoCrossed)
        assertNotNull(viewModel.hint)

        viewModel.onCellTap(0, 1, wrongForRowOne)
        assertNotNull(viewModel.wrongPickEvent)
        assertNotNull(viewModel.hint)
        assertEquals(WhoDidItCellMark.X, viewModel.session!!.blocks[0][1][wrongForRowOne])

        val empty = (0 until puzzle.size).first { viewModel.session!!.blocks[1][0][it] == WhoDidItCellMark.EMPTY }
        viewModel.onCellTap(1, 0, empty)
        assertNull(viewModel.hint)
    }

    @Test
    fun `not quite is recomputed after restore and after undo`() {
        val puzzle = loadWhoDidItBankForTest().first { it.difficulty == "hard" }
        val wrong = completeWrongSession(puzzle)
        assertTrue(isWhoDidItBoardComplete(wrong))
        assertFalse(isWhoDidItSolved(wrong))
        val payload = Json.encodeToString(
            WhoDidItPersistedSession.serializer(),
            WhoDidItPersistedSession(puzzle, encodeWhoDidItMarks(wrong.blocks), WhoDidItStatus.PLAYING.name),
        )
        val sessionStore = FakeSessionStore().apply {
            preload(ActiveSessionEnvelope(gameId = GameId.WHODIDIT.key, elapsedSeconds = 12.0, payload = payload))
        }

        val viewModel = newViewModel(difficulty = Difficulty.HARD, resume = true, sessionStore = sessionStore)
        assertEquals(puzzle.id, viewModel.session?.puzzle?.id)
        assertTrue(viewModel.showNotQuite)

        val oColumn = viewModel.session!!.blocks[0][0].indexOf(WhoDidItCellMark.O)
        viewModel.onCellTap(0, 0, oColumn)
        assertFalse(viewModel.showNotQuite)

        viewModel.onUndo()
        assertTrue(viewModel.showNotQuite)
    }
}
