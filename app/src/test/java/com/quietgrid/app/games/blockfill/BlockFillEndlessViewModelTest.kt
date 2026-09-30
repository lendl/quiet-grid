package com.quietgrid.app.games.blockfill

import com.quietgrid.app.MainDispatcherRule
import com.quietgrid.app.core.GameId
import com.quietgrid.app.data.ActiveSessionEnvelope
import com.quietgrid.app.data.SESSION_MODE_ENDLESS
import com.quietgrid.app.testutil.FakeSessionStore
import com.quietgrid.app.testutil.FakeStatsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BlockFillEndlessViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun piece(id: String) = shapeDefToPiece(ALL_SHAPES.first { it.id == id })

    private fun savedRun(session: BlockFillEndlessSession) = ActiveSessionEnvelope(
        gameId = GameId.BLOCKFILL.key,
        elapsedSeconds = 0.0,
        payload = Json.encodeToString(session),
        mode = SESSION_MODE_ENDLESS,
    )

    private fun stuckNextRun(): BlockFillEndlessSession {
        val emptyCells = setOf(0 to 0, 0 to 2, 2 to 0, 1 to 1, 3 to 3, 4 to 4, 5 to 5, 6 to 6, 7 to 7)
        var board = createEmptyBoard()
        for (row in 0 until BLOCKFILL_BOARD_SIZE) {
            for (col in 0 until BLOCKFILL_BOARD_SIZE) {
                if (row to col !in emptyCells) board = placePieceAt(board, listOf(0 to 0), row, col, BlockFillShapeFamily.STONE)
            }
        }
        return BlockFillEndlessSession(
            board = board,
            tray = listOf(piece("single"), piece("domino-h"), piece("domino-h")),
            score = 640,
            comboStreak = 0,
            multiplier = 3,
            moves = 40,
            movesSinceClear = 2,
            linesCleared = 7,
            bestAtStart = 900,
            status = BlockFillStatus.PLAYING,
        )
    }

    private fun collectResults(viewModel: BlockFillEndlessViewModel, into: MutableList<BlockFillEndlessResult>) =
        CoroutineScope(mainDispatcherRule.dispatcher).launch { viewModel.result.collect { into.add(it) } }

    private fun settle() {
        mainDispatcherRule.dispatcher.scheduler.advanceTimeBy(1_000)
        mainDispatcherRule.dispatcher.scheduler.runCurrent()
    }

    @Test
    fun `fresh run uses the stored best`() {
        val stats = FakeStatsStore().apply { seedEndless(GameId.BLOCKFILL, bestScore = 900, bestLevel = 4) }
        val viewModel = BlockFillEndlessViewModel(FakeSessionStore(), stats, resume = false)

        assertEquals(900, viewModel.session?.bestAtStart)
        assertEquals(BlockFillStatus.PLAYING, viewModel.session?.status)
    }

    @Test
    fun `placement saves an endless envelope`() {
        val sessionStore = FakeSessionStore()
        val viewModel = BlockFillEndlessViewModel(sessionStore, FakeStatsStore(), resume = false)
        val session = viewModel.session!!
        val (pieceIndex, anchor) = session.tray.withIndex().firstNotNullOf { (index, piece) ->
            piece?.let { p -> findValidPlacements(session.board, p.cells).firstOrNull()?.let { index to it } }
        }

        assertTrue(viewModel.onPlacePiece(pieceIndex, anchor.first, anchor.second))

        val envelope = runBlocking { sessionStore.activeSession.first() }
        assertNotNull(envelope)
        assertEquals(SESSION_MODE_ENDLESS, envelope!!.mode)
        assertEquals(viewModel.session, Json.decodeFromString(BlockFillEndlessSession.serializer(), envelope.payload))
    }

    @Test
    fun `resume restores the saved run`() {
        val saved = stuckNextRun().copy(score = 1234)
        val sessionStore = FakeSessionStore().apply { preload(savedRun(saved)) }

        val viewModel = BlockFillEndlessViewModel(sessionStore, FakeStatsStore(), resume = true)

        assertEquals(saved, viewModel.session)
    }

    @Test
    fun `stuck run reports and clears`() {
        val sessionStore = FakeSessionStore().apply { preload(savedRun(stuckNextRun())) }
        val stats = FakeStatsStore()
        val viewModel = BlockFillEndlessViewModel(sessionStore, stats, resume = true)
        val results = mutableListOf<BlockFillEndlessResult>()
        val job = collectResults(viewModel, results)

        viewModel.onPlacePiece(0, 0, 0)
        settle()

        assertEquals("stuck", results.single().reason)
        assertEquals(640, results.single().score)
        assertEquals(3, results.single().levelReached)
        assertEquals(false, results.single().isNewBest)
        assertTrue(sessionStore.cleared)
        assertNull(runBlocking { sessionStore.activeSession.first() })
        assertEquals(640, runBlocking { stats.endlessStatsFor(GameId.BLOCKFILL).first() }.bestScore)
        job.cancel()
    }

    @Test
    fun `ending early reports abandoned`() {
        val sessionStore = FakeSessionStore()
        val viewModel = BlockFillEndlessViewModel(sessionStore, FakeStatsStore(), resume = false)
        val results = mutableListOf<BlockFillEndlessResult>()
        val job = collectResults(viewModel, results)

        viewModel.endRun()
        settle()

        assertEquals("abandoned", results.single().reason)
        assertTrue(sessionStore.cleared)
        job.cancel()
    }
}
