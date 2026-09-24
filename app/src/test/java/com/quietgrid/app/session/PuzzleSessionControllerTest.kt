package com.quietgrid.app.session

import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.data.ActiveSessionEnvelope
import com.quietgrid.app.data.DifficultyStats
import com.quietgrid.app.data.GameStats
import com.quietgrid.app.data.PlayHistoryStore
import com.quietgrid.app.data.PlayRecord
import com.quietgrid.app.data.SessionStore
import com.quietgrid.app.data.StatsStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertFalse
import java.time.LocalDate

private data class TestSession(val value: Int, val meaningful: Boolean = true)

private data class TestResult(
    val solved: Boolean,
    val score: Int,
    val lossReason: String?,
    val isFirstSolve: Boolean,
    val isNewHighScore: Boolean,
)

private class FakeSessionStore : SessionStore {
    private val state = MutableStateFlow<ActiveSessionEnvelope?>(null)
    override val activeSession: Flow<ActiveSessionEnvelope?> = state
    var saveCount = 0
        private set
    var cleared = false
        private set

    fun preload(envelope: ActiveSessionEnvelope) {
        state.value = envelope
    }

    override suspend fun save(envelope: ActiveSessionEnvelope) {
        saveCount++
        state.value = envelope
    }

    override suspend fun clear() {
        cleared = true
        state.value = null
    }
}

private class FakeStatsStore : StatsStore {
    private val stats = mutableMapOf<GameId, GameStats>()
    private val challengerStats = mutableMapOf<GameId, DifficultyStats>()

    fun seed(gameId: GameId, difficulty: Difficulty, solved: Int, bestScore: Int) {
        stats[gameId] = GameStats(
            byDifficulty = mapOf(difficulty.key to DifficultyStats(played = solved, solved = solved, bestScore = bestScore)),
        )
    }

    override fun statsFor(gameId: GameId): Flow<GameStats> = MutableStateFlow(stats[gameId] ?: GameStats())

    override suspend fun recordResult(gameId: GameId, difficulty: Difficulty, solved: Boolean, score: Int) {
        val current = stats[gameId] ?: GameStats()
        val existing = current.forDifficulty(difficulty)
        val updated = existing.copy(
            played = existing.played + 1,
            solved = existing.solved + if (solved) 1 else 0,
            bestScore = if (solved) maxOf(existing.bestScore, score) else existing.bestScore,
        )
        stats[gameId] = GameStats(byDifficulty = current.byDifficulty + (difficulty.key to updated))
    }

    override fun challengerStatsFor(gameId: GameId): Flow<DifficultyStats> =
        MutableStateFlow(challengerStats[gameId] ?: DifficultyStats())

    override suspend fun recordChallengerResult(gameId: GameId, puzzlesSolved: Int, score: Int) {
        val existing = challengerStats[gameId] ?: DifficultyStats()
        challengerStats[gameId] = existing.copy(
            played = existing.played + 1,
            solved = maxOf(existing.solved, puzzlesSolved),
            bestScore = maxOf(existing.bestScore, score),
        )
    }
}

private class FakeHistoryStore : PlayHistoryStore {
    private val records = mutableListOf<PlayRecord>()
    val appended: List<PlayRecord> get() = records

    override fun allRecords(): Flow<List<PlayRecord>> = MutableStateFlow(records.toList())

    override fun recordsFor(gameId: GameId): Flow<List<PlayRecord>> =
        MutableStateFlow(records.filter { it.gameId == gameId.key })

    override fun recordsForPuzzle(gameId: GameId, puzzleId: String, difficulty: Difficulty): Flow<List<PlayRecord>> =
        MutableStateFlow(records.filter { it.gameId == gameId.key && it.puzzleId == puzzleId && it.difficulty == difficulty.key })

    override suspend fun appendRecord(record: PlayRecord) {
        records.add(record)
    }

    override suspend fun clear() {
        records.clear()
    }
}

private class FakePuzzleAdapter(
    private val freshValue: Int = 0,
    private val restoreValue: TestSession? = null,
    private val puzzleId: String? = null,
    override val gameId: GameId = GameId.TAKUZU,
    private val shareDetail: String? = null,
) : PuzzleAdapter<TestSession, TestResult> {
    var freshSessionCalls = 0
        private set

    override suspend fun freshSession(difficulty: Difficulty): TestSession {
        freshSessionCalls++
        return TestSession(freshValue)
    }

    override fun restoreSession(payload: String, elapsedSeconds: Double): TestSession? = restoreValue

    var dailySessionCalls = 0
        private set

    override suspend fun dailySession(difficulty: Difficulty, date: LocalDate): TestSession {
        dailySessionCalls++
        return TestSession(freshValue + 1000)
    }

    override fun dailyShareDetail(session: TestSession): String? = shareDetail

    override fun difficultyOf(session: TestSession): Difficulty = Difficulty.MEDIUM

    override fun hasMeaningfulProgress(session: TestSession): Boolean = session.meaningful

    override fun encode(session: TestSession): String = "encoded-${session.value}"

    override fun scoreOnWin(session: TestSession, difficulty: Difficulty, elapsedSeconds: Int): Int =
        100 - elapsedSeconds

    override fun puzzleIdOf(session: TestSession): String? = puzzleId

    override fun buildResult(session: TestSession?, outcome: PuzzleOutcome): TestResult = TestResult(
        solved = outcome.solved,
        score = outcome.score,
        lossReason = outcome.lossReason,
        isFirstSolve = outcome.isFirstSolve,
        isNewHighScore = outcome.isNewHighScore,
    )
}

class PuzzleSessionControllerTest {

    private val dailyDay = LocalDate.of(2026, 9, 24)

    @Test
    fun `daily start uses dailySession and tags the envelope`() = runTest {
        val sessionStore = FakeSessionStore()
        val adapter = FakePuzzleAdapter(freshValue = 1)
        val controller = PuzzleSessionController(
            backgroundScope, sessionStore, FakeStatsStore(), FakeHistoryStore(), adapter,
            isAppForeground = { true },
        )

        controller.start(Difficulty.HARD, resume = false, requestedDailyDate = dailyDay)
        advanceTimeBy(1_001)

        assertEquals(1, adapter.dailySessionCalls)
        assertEquals(0, adapter.freshSessionCalls)
        assertEquals(dailyDay, controller.dailyDate)
        val envelope = sessionStore.activeSession.first()
        assertEquals("2026-09-24", envelope?.dailyDate)
        assertEquals("hard", envelope?.dailyTier)
    }

    @Test
    fun `daily win record carries daily date and share detail`() = runTest {
        val history = FakeHistoryStore()
        val controller = PuzzleSessionController(
            backgroundScope, FakeSessionStore(), FakeStatsStore(), history, FakePuzzleAdapter(shareDetail = "grid"),
            isAppForeground = { true },
        )

        controller.start(Difficulty.HARD, resume = false, requestedDailyDate = dailyDay)
        runCurrent()
        controller.finishAsWin()
        runCurrent()

        val record = history.appended.single()
        assertEquals("2026-09-24", record.dailyDate)
        assertEquals("grid", record.shareDetail)
    }

    @Test
    fun `non-daily win record has no daily fields`() = runTest {
        val history = FakeHistoryStore()
        val controller = PuzzleSessionController(
            backgroundScope, FakeSessionStore(), FakeStatsStore(), history, FakePuzzleAdapter(shareDetail = "grid"),
            isAppForeground = { true },
        )

        controller.start(Difficulty.HARD, resume = false)
        runCurrent()
        controller.finishAsWin()
        runCurrent()

        assertNull(history.appended.single().dailyDate)
        assertNull(history.appended.single().shareDetail)
    }

    @Test
    fun `fresh start over an in-progress daily forfeits it as a loss`() = runTest {
        val sessionStore = FakeSessionStore()
        sessionStore.preload(ActiveSessionEnvelope("sudoku", 33.0, "p", "2026-09-23", "expert"))
        val history = FakeHistoryStore()
        val controller = PuzzleSessionController(
            backgroundScope, sessionStore, FakeStatsStore(), history, FakePuzzleAdapter(),
        )

        controller.start(Difficulty.EASY, resume = false)
        runCurrent()

        val forfeit = history.appended.single()
        assertEquals("sudoku", forfeit.gameId)
        assertEquals("expert", forfeit.difficulty)
        assertEquals("2026-09-23", forfeit.dailyDate)
        assertFalse(forfeit.solved)
        assertEquals("abandoned", forfeit.lossReason)
        assertEquals(33, forfeit.elapsedSeconds)
    }

    @Test
    fun `fresh start over a regular session records nothing`() = runTest {
        val sessionStore = FakeSessionStore()
        sessionStore.preload(ActiveSessionEnvelope("sudoku", 33.0, "p"))
        val history = FakeHistoryStore()
        val controller = PuzzleSessionController(
            backgroundScope, sessionStore, FakeStatsStore(), history, FakePuzzleAdapter(),
        )

        controller.start(Difficulty.EASY, resume = false)
        runCurrent()

        assertTrue(history.appended.isEmpty())
    }

    @Test
    fun `restarting the same daily run does not forfeit it`() = runTest {
        val sessionStore = FakeSessionStore()
        sessionStore.preload(ActiveSessionEnvelope(GameId.TAKUZU.key, 10.0, "p", "2026-09-24", "hard"))
        val history = FakeHistoryStore()
        val controller = PuzzleSessionController(
            backgroundScope, sessionStore, FakeStatsStore(), history, FakePuzzleAdapter(),
        )

        controller.start(Difficulty.HARD, resume = false, requestedDailyDate = dailyDay)
        runCurrent()

        assertTrue(history.appended.isEmpty())
    }

    @Test
    fun `resume restores daily date from the envelope`() = runTest {
        val sessionStore = FakeSessionStore()
        sessionStore.preload(ActiveSessionEnvelope(GameId.TAKUZU.key, 12.0, "p", "2026-09-24", "medium"))
        val controller = PuzzleSessionController(
            backgroundScope, sessionStore, FakeStatsStore(), FakeHistoryStore(), FakePuzzleAdapter(restoreValue = TestSession(7)),
        )

        controller.start(Difficulty.EASY, resume = true)
        runCurrent()

        assertEquals(dailyDay, controller.dailyDate)
    }

    @Test
    fun `fresh start ticks elapsed seconds and persists meaningful progress`() = runTest {
        val sessionStore = FakeSessionStore()
        val controller = PuzzleSessionController(
            backgroundScope, sessionStore, FakeStatsStore(), FakeHistoryStore(), FakePuzzleAdapter(),
            isAppForeground = { true },
        )

        controller.start(Difficulty.EASY, resume = false)
        advanceTimeBy(1_001)

        assertEquals(1.0, controller.elapsedSeconds, 0.0)
        assertTrue(sessionStore.saveCount > 0)
    }

    @Test
    fun `ticker does not advance elapsed seconds while the app is backgrounded`() = runTest {
        val sessionStore = FakeSessionStore()
        val controller = PuzzleSessionController(
            backgroundScope, sessionStore, FakeStatsStore(), FakeHistoryStore(), FakePuzzleAdapter(),
            isAppForeground = { false },
        )

        controller.start(Difficulty.EASY, resume = false)
        advanceTimeBy(3_001)

        assertEquals(0.0, controller.elapsedSeconds, 0.0)
        assertEquals(0, sessionStore.saveCount)
    }

    @Test
    fun `resume restores session from a matching envelope`() = runTest {
        val sessionStore = FakeSessionStore()
        sessionStore.preload(ActiveSessionEnvelope(gameId = GameId.TAKUZU.key, elapsedSeconds = 42.0, payload = "p"))
        val adapter = FakePuzzleAdapter(restoreValue = TestSession(7))
        val controller = PuzzleSessionController(backgroundScope, sessionStore, FakeStatsStore(), FakeHistoryStore(), adapter)

        controller.start(Difficulty.EASY, resume = true)
        runCurrent()

        assertEquals(TestSession(7), controller.session)
        assertEquals(42.0, controller.elapsedSeconds, 0.0)
    }

    @Test
    fun `resume falls back to a fresh session when restore returns null`() = runTest {
        val sessionStore = FakeSessionStore()
        sessionStore.preload(ActiveSessionEnvelope(gameId = GameId.TAKUZU.key, elapsedSeconds = 42.0, payload = "bad"))
        val adapter = FakePuzzleAdapter(freshValue = 3, restoreValue = null)
        val controller = PuzzleSessionController(backgroundScope, sessionStore, FakeStatsStore(), FakeHistoryStore(), adapter)

        controller.start(Difficulty.EASY, resume = true)
        runCurrent()

        assertEquals(TestSession(3), controller.session)
        assertEquals(1, adapter.freshSessionCalls)
    }

    @Test
    fun `updateSession persists only when adapter reports meaningful progress`() = runTest {
        val sessionStore = FakeSessionStore()
        val controller = PuzzleSessionController(backgroundScope, sessionStore, FakeStatsStore(), FakeHistoryStore(), FakePuzzleAdapter())
        controller.start(Difficulty.EASY, resume = false)
        runCurrent()

        controller.updateSession(TestSession(1, meaningful = false))
        runCurrent()
        assertEquals(0, sessionStore.saveCount)

        controller.updateSession(TestSession(2, meaningful = true))
        runCurrent()
        assertEquals(1, sessionStore.saveCount)
    }

    @Test
    fun `updateSession with persist false never saves`() = runTest {
        val sessionStore = FakeSessionStore()
        val controller = PuzzleSessionController(backgroundScope, sessionStore, FakeStatsStore(), FakeHistoryStore(), FakePuzzleAdapter())
        controller.start(Difficulty.EASY, resume = false)
        runCurrent()

        controller.updateSession(TestSession(1, meaningful = true), persist = false)
        runCurrent()

        assertEquals(0, sessionStore.saveCount)
        assertEquals(TestSession(1), controller.session)
    }

    @Test
    fun `finishAsWin records stats, clears the session store, and emits isFirstSolve`() = runTest {
        val sessionStore = FakeSessionStore()
        val statsStore = FakeStatsStore()
        val controller = PuzzleSessionController(backgroundScope, sessionStore, statsStore, FakeHistoryStore(), FakePuzzleAdapter())
        controller.start(Difficulty.MEDIUM, resume = false)
        runCurrent()

        var emitted: TestResult? = null
        val collectJob = launch { controller.result.collect { emitted = it } }

        controller.finishAsWin()
        advanceTimeBy(500)
        runCurrent()

        assertTrue(sessionStore.cleared)
        assertEquals(true, emitted?.solved)
        assertEquals(true, emitted?.isFirstSolve)
        collectJob.cancel()
    }

    @Test
    fun `finishAsWin reports isNewHighScore against prior best`() = runTest {
        val statsStore = FakeStatsStore().apply { seed(GameId.TAKUZU, Difficulty.MEDIUM, solved = 3, bestScore = 50) }
        val controller = PuzzleSessionController(backgroundScope, FakeSessionStore(), statsStore, FakeHistoryStore(), FakePuzzleAdapter())
        controller.start(Difficulty.MEDIUM, resume = false)
        runCurrent()

        var emitted: TestResult? = null
        val collectJob = launch { controller.result.collect { emitted = it } }

        controller.finishAsWin()
        advanceTimeBy(500)
        runCurrent()

        assertEquals(false, emitted?.isFirstSolve)
        assertEquals(true, emitted?.isNewHighScore)
        collectJob.cancel()
    }

    @Test
    fun `endPuzzle emits a loss result with the abandoned reason`() = runTest {
        val controller = PuzzleSessionController(backgroundScope, FakeSessionStore(), FakeStatsStore(), FakeHistoryStore(), FakePuzzleAdapter())
        controller.start(Difficulty.HARD, resume = false)
        runCurrent()

        var emitted: TestResult? = null
        val collectJob = launch { controller.result.collect { emitted = it } }

        controller.endPuzzle()
        advanceTimeBy(500)
        runCurrent()

        assertEquals(false, emitted?.solved)
        assertEquals("abandoned", emitted?.lossReason)
        collectJob.cancel()
    }

    @Test
    fun `finalized guard prevents a second result emission`() = runTest {
        val controller = PuzzleSessionController(backgroundScope, FakeSessionStore(), FakeStatsStore(), FakeHistoryStore(), FakePuzzleAdapter())
        controller.start(Difficulty.EASY, resume = false)
        runCurrent()

        var emitCount = 0
        val collectJob = launch { controller.result.collect { emitCount++ } }

        controller.finishAsWin()
        controller.finishAsWin()
        advanceTimeBy(500)
        runCurrent()

        assertEquals(1, emitCount)
        collectJob.cancel()
    }

    @Test
    fun `finishAsWin appends a solved play record carrying the difficulty and puzzle id`() = runTest {
        val historyStore = FakeHistoryStore()
        val controller = PuzzleSessionController(
            backgroundScope, FakeSessionStore(), FakeStatsStore(), historyStore, FakePuzzleAdapter(puzzleId = "t6-abc"),
        )
        controller.start(Difficulty.HARD, resume = false)
        runCurrent()

        controller.finishAsWin()
        advanceTimeBy(500)
        runCurrent()

        val record = historyStore.appended.single()
        assertEquals(GameId.TAKUZU.key, record.gameId)
        assertEquals(Difficulty.HARD.key, record.difficulty)
        assertEquals("t6-abc", record.puzzleId)
        assertTrue(record.solved)
        assertNull(record.lossReason)
    }

    @Test
    fun `endPuzzle appends an unsolved play record with the abandoned reason`() = runTest {
        val historyStore = FakeHistoryStore()
        val controller = PuzzleSessionController(
            backgroundScope, FakeSessionStore(), FakeStatsStore(), historyStore, FakePuzzleAdapter(),
        )
        controller.start(Difficulty.EASY, resume = false)
        runCurrent()

        controller.endPuzzle()
        advanceTimeBy(500)
        runCurrent()

        val record = historyStore.appended.single()
        assertEquals(false, record.solved)
        assertEquals("abandoned", record.lossReason)
        assertNull(record.puzzleId)
    }

    @Test
    fun `finishAsWin does not append a play record for a beta game`() = runTest {
        val historyStore = FakeHistoryStore()
        val statsStore = FakeStatsStore()
        val controller = PuzzleSessionController(
            backgroundScope, FakeSessionStore(), statsStore, historyStore, FakePuzzleAdapter(gameId = GameId.BLOCKFILL),
        )
        controller.start(Difficulty.EASY, resume = false)
        runCurrent()

        controller.finishAsWin()
        advanceTimeBy(500)
        runCurrent()

        assertTrue(historyStore.appended.isEmpty())
    }

    @Test
    fun `endPuzzle does not append a play record for a beta game`() = runTest {
        val historyStore = FakeHistoryStore()
        val controller = PuzzleSessionController(
            backgroundScope, FakeSessionStore(), FakeStatsStore(), historyStore, FakePuzzleAdapter(gameId = GameId.BLOCKFILL),
        )
        controller.start(Difficulty.EASY, resume = false)
        runCurrent()

        controller.endPuzzle()
        advanceTimeBy(500)
        runCurrent()

        assertTrue(historyStore.appended.isEmpty())
    }
}
