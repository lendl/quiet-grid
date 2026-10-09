package com.quietgrid.app.games.sequencememory

import com.quietgrid.app.MainDispatcherRule
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.data.ActiveSessionEnvelope
import com.quietgrid.app.session.AppForeground
import com.quietgrid.app.testutil.FakeHistoryStore
import com.quietgrid.app.testutil.FakeSessionStore
import com.quietgrid.app.testutil.FakeStatsStore
import com.quietgrid.engine.sequencememory.SEQUENCE_MEMORY_POINTS_PER_ROUND
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SequenceMemoryPlayViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun newViewModel(
        sessionStore: FakeSessionStore = FakeSessionStore(),
        foreground: () -> Boolean = { true },
        difficulty: Difficulty = Difficulty.EASY,
        resume: Boolean = false,
    ) = SequenceMemoryPlayViewModel(sessionStore, FakeStatsStore(), FakeHistoryStore(), AppForeground { foreground() }, difficulty, resume)

    private fun advance(ms: Long) {
        mainDispatcherRule.dispatcher.scheduler.advanceTimeBy(ms)
        mainDispatcherRule.dispatcher.scheduler.runCurrent()
    }

    private fun playbackMs(session: SequenceMemorySession): Long {
        val config = sequenceMemoryConfig(session)
        return session.sequence.size * (config.flashMs + config.gapMs)
    }

    private fun advanceUntilInput(viewModel: SequenceMemoryPlayViewModel) {
        repeat(400) {
            if (viewModel.session?.phase == SequenceMemoryPhase.INPUT) return
            advance(50)
        }
    }

    private fun wrongPad(viewModel: SequenceMemoryPlayViewModel): Int {
        val session = viewModel.session!!
        return (session.sequence[session.inputIndex] + 1) % sequenceMemoryConfig(session).padCount
    }

    @Test
    fun `starting fresh plays a one-step sequence then hands the turn to the player`() {
        val viewModel = newViewModel()
        advance(SEQUENCE_MEMORY_START_DELAY_MS)

        val session = viewModel.session!!
        assertEquals(1, session.sequence.size)
        assertEquals(SequenceMemoryPhase.SHOWING, session.phase)
        assertEquals(session.sequence.first(), viewModel.litPad)

        advance(playbackMs(session))

        assertEquals(SequenceMemoryPhase.INPUT, viewModel.session!!.phase)
        assertNull(viewModel.litPad)
    }

    @Test
    fun `taps are ignored while the sequence is playing`() {
        val viewModel = newViewModel()
        advance(SEQUENCE_MEMORY_START_DELAY_MS)
        val before = viewModel.session!!

        viewModel.onPadTap(before.sequence.first())

        assertEquals(before, viewModel.session)
    }

    @Test
    fun `repeating the round grows the sequence, saves it and replays it`() {
        val sessionStore = FakeSessionStore()
        val viewModel = newViewModel(sessionStore = sessionStore)
        advanceUntilInput(viewModel)
        val firstPad = viewModel.session!!.sequence.first()
        val savesBefore = sessionStore.saveCount

        viewModel.onPadTap(firstPad)

        val next = viewModel.session!!
        assertEquals(SequenceMemoryPhase.SHOWING, next.phase)
        assertEquals(2, next.sequence.size)
        assertEquals(firstPad, next.sequence.first())
        assertEquals(1, next.completedRounds)
        assertEquals(1, viewModel.roundCompleteTrigger)
        assertTrue(sessionStore.saveCount > savesBefore)

        advance(SEQUENCE_MEMORY_ROUND_PAUSE_MS + playbackMs(next))

        assertEquals(SequenceMemoryPhase.INPUT, viewModel.session!!.phase)
        assertEquals(0, viewModel.session!!.inputIndex)
    }

    @Test
    fun `a wrong tap reveals the expected pad before finishing as a lost puzzle`() {
        val viewModel = newViewModel()
        val results = mutableListOf<SequenceMemoryResult>()
        val collectJob = CoroutineScope(mainDispatcherRule.dispatcher).launch { viewModel.result.collect { results.add(it) } }
        advanceUntilInput(viewModel)
        val expected = viewModel.session!!.sequence.first()
        val wrong = wrongPad(viewModel)

        viewModel.onPadTap(wrong)

        assertEquals(SequenceMemoryPhase.FAILED, viewModel.session!!.phase)
        assertEquals(wrong, viewModel.session!!.wrongPad)
        assertEquals(expected, viewModel.litPad)
        assertEquals(1, viewModel.wrongTapTrigger)
        assertTrue(results.isEmpty())

        advance(SEQUENCE_MEMORY_WRONG_TAP_REVEAL_MS + 500)

        val result = results.single()
        assertFalse(result.solved)
        assertEquals("rule-failure", result.lossReason)
        collectJob.cancel()
    }

    @Test
    fun `repeating the full target length wins with a score per round`() {
        val viewModel = newViewModel()
        val results = mutableListOf<SequenceMemoryResult>()
        val collectJob = CoroutineScope(mainDispatcherRule.dispatcher).launch { viewModel.result.collect { results.add(it) } }
        val targetLength = sequenceMemoryConfig(Difficulty.EASY).targetLength

        repeat(targetLength) {
            advanceUntilInput(viewModel)
            viewModel.session!!.sequence.forEach(viewModel::onPadTap)
        }
        advance(500)

        val result = results.single()
        assertTrue(result.solved)
        assertEquals(targetLength * SEQUENCE_MEMORY_POINTS_PER_ROUND, result.score)
        collectJob.cancel()
    }

    @Test
    fun `endPuzzle finalizes the session as an abandoned loss`() {
        val sessionStore = FakeSessionStore()
        val viewModel = newViewModel(sessionStore = sessionStore)
        val results = mutableListOf<SequenceMemoryResult>()
        val collectJob = CoroutineScope(mainDispatcherRule.dispatcher).launch { viewModel.result.collect { results.add(it) } }

        viewModel.endPuzzle()
        advance(500)

        assertTrue(sessionStore.cleared)
        assertEquals("abandoned", results.single().lossReason)
        collectJob.cancel()
    }

    @Test
    fun `playback holds while the app is backgrounded and resumes on return`() {
        var foreground = false
        val viewModel = newViewModel(foreground = { foreground })

        advance(SEQUENCE_MEMORY_START_DELAY_MS + 10_000)

        assertEquals(SequenceMemoryPhase.SHOWING, viewModel.session!!.phase)
        assertNull(viewModel.litPad)

        foreground = true
        advance(playbackMs(viewModel.session!!) + 500)

        assertEquals(SequenceMemoryPhase.INPUT, viewModel.session!!.phase)
    }

    @Test
    fun `resuming a saved mid-round session replays the round from the start`() {
        val saved = SequenceMemorySession(
            difficulty = Difficulty.EASY.key,
            sequence = listOf(1, 2, 3),
            completedRounds = 2,
            inputIndex = 1,
            phase = SequenceMemoryPhase.INPUT,
        )
        val payload = Json.encodeToString(SequenceMemorySession.serializer(), saved)
        val sessionStore = FakeSessionStore().apply {
            preload(ActiveSessionEnvelope(gameId = GameId.SEQUENCEMEMORY.key, elapsedSeconds = 30.0, payload = payload))
        }

        val viewModel = newViewModel(sessionStore = sessionStore, resume = true)
        advance(SEQUENCE_MEMORY_START_DELAY_MS)

        val session = viewModel.session!!
        assertEquals(listOf(1, 2, 3), session.sequence)
        assertEquals(2, session.completedRounds)
        assertEquals(0, session.inputIndex)
        assertEquals(SequenceMemoryPhase.SHOWING, session.phase)
        assertEquals(1, viewModel.litPad)
    }
}
