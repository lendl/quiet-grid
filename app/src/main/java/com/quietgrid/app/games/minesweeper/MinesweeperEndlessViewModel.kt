package com.quietgrid.app.games.minesweeper

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quietgrid.app.core.GameId
import com.quietgrid.app.data.ActiveSessionEnvelope
import com.quietgrid.app.data.SESSION_MODE_ENDLESS
import com.quietgrid.app.data.SessionStore
import com.quietgrid.app.data.StatsStore
import com.quietgrid.app.session.AppForeground
import com.quietgrid.engine.minesweeper.endless.ENDLESS_START_SECTION
import com.quietgrid.engine.minesweeper.endless.EndlessCellState
import com.quietgrid.engine.minesweeper.endless.EndlessWorld
import com.quietgrid.engine.minesweeper.endless.WorldCell
import com.quietgrid.engine.minesweeper.endless.canTapEndlessCell
import com.quietgrid.engine.minesweeper.endless.endlessNeedsSettle
import com.quietgrid.engine.minesweeper.endless.revealEndlessCell
import com.quietgrid.engine.minesweeper.endless.section
import com.quietgrid.engine.minesweeper.endless.settleEndlessWorld
import com.quietgrid.engine.minesweeper.endless.startEndlessRun
import com.quietgrid.engine.minesweeper.endless.toggleEndlessFlag
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlin.random.Random

private val endlessJson = Json { ignoreUnknownKeys = true }
private const val TICK_INTERVAL_MS = 1000L
private const val CHECKPOINT_INTERVAL_TICKS = 15
const val MINESWEEPER_ENDLESS_FINISH_DELAY_MS = 1200L

@HiltViewModel(assistedFactory = MinesweeperEndlessViewModel.Factory::class)
class MinesweeperEndlessViewModel internal constructor(
    private val sessionStore: SessionStore,
    private val statsStore: StatsStore,
    private val appForeground: AppForeground,
    resume: Boolean,
    private val buildDispatcher: CoroutineDispatcher,
    private val random: Random,
) : ViewModel() {

    @AssistedInject
    constructor(
        sessionStore: SessionStore,
        statsStore: StatsStore,
        appForeground: AppForeground,
        @Assisted resume: Boolean,
    ) : this(sessionStore, statsStore, appForeground, resume, Dispatchers.Default, Random.Default)

    @AssistedFactory
    interface Factory {
        fun create(resume: Boolean): MinesweeperEndlessViewModel
    }

    var session by mutableStateOf<MinesweeperEndlessSession?>(null)
        private set
    var elapsedSeconds by mutableIntStateOf(0)
        private set
    var building by mutableStateOf(true)
        private set
    var revealMines by mutableStateOf(false)
        private set

    private val _result = MutableSharedFlow<MinesweeperEndlessResult>(extraBufferCapacity = 1)
    val result: SharedFlow<MinesweeperEndlessResult> = _result

    private var finalized = false

    init {
        viewModelScope.launch {
            val restored = if (resume) restoreRun() else null
            if (restored != null) {
                session = restored.first
                elapsedSeconds = restored.second
            } else {
                val best = statsStore.endlessStatsFor(GameId.MINESWEEPER).first().bestScore
                session = MinesweeperEndlessSession(EndlessWorld.EMPTY, MINESWEEPER_ENDLESS_HEARTS, best, MinesweeperEndlessCamera())
            }
            building = false
            session?.takeIf { it.started && endlessNeedsSettle(it.world) }?.let(::settle)
        }
        viewModelScope.launch { runTicker() }
    }

    fun onReveal(cell: WorldCell) {
        if (finalized || building) return
        val current = session ?: return
        if (!current.started) {
            if (cell.section == ENDLESS_START_SECTION) startRun(current, cell)
            return
        }
        if (!canTapEndlessCell(current.world, cell) || current.world.cellStateAt(cell) != EndlessCellState.HIDDEN) return
        val reveal = revealEndlessCell(current.world, cell)
        if (reveal.world == current.world) return
        val next = if (reveal.hitMine) {
            current.copy(world = reveal.world, hearts = current.hearts - 1, heartShake = current.heartShake + 1)
        } else {
            current.copy(world = reveal.world)
        }
        session = next
        when {
            next.hearts <= 0 -> finish(next, MINESWEEPER_ENDLESS_REASON_OUT_OF_HEARTS)
            endlessNeedsSettle(next.world) -> settle(next)
            else -> save(next)
        }
    }

    fun onToggleFlag(cell: WorldCell) {
        if (finalized || building) return
        val current = session ?: return
        if (!current.started) return
        val world = toggleEndlessFlag(current.world, cell)
        if (world == current.world) return
        val next = current.copy(world = world)
        session = next
        save(next)
    }

    fun onCameraChanged(camera: MinesweeperEndlessCamera) {
        val current = session ?: return
        if (finalized || current.camera == camera) return
        val next = current.copy(camera = camera)
        session = next
        save(next)
    }

    fun endRun() {
        val current = session ?: return
        finish(current, MINESWEEPER_ENDLESS_REASON_ENDED)
    }

    private fun startRun(current: MinesweeperEndlessSession, cell: WorldCell) {
        building = true
        viewModelScope.launch {
            val world = withContext(buildDispatcher) { startEndlessRun(cell, random).world }
            if (finalized) return@launch
            val next = (session ?: current).copy(world = world)
            session = next
            building = false
            save(next)
        }
    }

    private fun settle(base: MinesweeperEndlessSession) {
        building = true
        viewModelScope.launch {
            val world = withContext(buildDispatcher) { settleEndlessWorld(base.world, random).world }
            if (finalized) return@launch
            val next = (session ?: base).copy(world = world)
            session = next
            building = false
            save(next)
        }
    }

    private suspend fun restoreRun(): Pair<MinesweeperEndlessSession, Int>? {
        val envelope = sessionStore.activeSession.first() ?: return null
        if (envelope.gameId != GameId.MINESWEEPER.key || envelope.mode != SESSION_MODE_ENDLESS) return null
        val persisted = runCatching {
            endlessJson.decodeFromString(MinesweeperEndlessPersistedSession.serializer(), envelope.payload)
        }.getOrNull() ?: return null
        val restored = persisted.toSession() ?: return null
        return restored to envelope.elapsedSeconds.toInt()
    }

    private fun envelopeFor(run: MinesweeperEndlessSession) = ActiveSessionEnvelope(
        gameId = GameId.MINESWEEPER.key,
        elapsedSeconds = elapsedSeconds.toDouble(),
        payload = endlessJson.encodeToString(MinesweeperEndlessPersistedSession.serializer(), run.toPersisted()),
        mode = SESSION_MODE_ENDLESS,
    )

    private fun save(run: MinesweeperEndlessSession) {
        if (finalized || !run.started) return
        val envelope = envelopeFor(run)
        viewModelScope.launch { sessionStore.save(envelope) }
    }

    private suspend fun runTicker() {
        var ticksSinceCheckpoint = 0
        var wasForeground = appForeground.isForeground()
        try {
            while (true) {
                delay(TICK_INTERVAL_MS)
                val current = session
                if (finalized || current == null || !current.started) continue
                if (!appForeground.isForeground()) {
                    if (wasForeground) save(current)
                    wasForeground = false
                    continue
                }
                wasForeground = true
                elapsedSeconds++
                ticksSinceCheckpoint++
                if (ticksSinceCheckpoint >= CHECKPOINT_INTERVAL_TICKS) {
                    ticksSinceCheckpoint = 0
                    save(current)
                }
            }
        } finally {
            withContext(NonCancellable) {
                session?.takeIf { it.started && !finalized }?.let { sessionStore.save(envelopeFor(it)) }
            }
        }
    }

    private fun finish(run: MinesweeperEndlessSession, reason: String) {
        if (finalized) return
        finalized = true
        building = false
        if (reason == MINESWEEPER_ENDLESS_REASON_OUT_OF_HEARTS) revealMines = true
        viewModelScope.launch {
            statsStore.recordEndlessResult(GameId.MINESWEEPER, run.score, run.world.furthestClearedRing)
            sessionStore.clear()
            delay(MINESWEEPER_ENDLESS_FINISH_DELAY_MS)
            _result.emit(
                MinesweeperEndlessResult(
                    score = run.score,
                    ring = run.world.furthestClearedRing,
                    elapsedSeconds = elapsedSeconds,
                    isNewBest = run.score > run.bestAtStart,
                    previousBest = run.bestAtStart,
                    reason = reason,
                ),
            )
        }
    }
}
