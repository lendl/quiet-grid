package com.quietgrid.app.games.sequencememory

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.data.PlayHistoryStore
import com.quietgrid.app.data.SessionStore
import com.quietgrid.app.data.StatsStore
import com.quietgrid.app.session.AppForeground
import com.quietgrid.app.session.PuzzleAdapter
import com.quietgrid.app.session.PuzzleOutcome
import com.quietgrid.app.session.PuzzleSessionController
import com.quietgrid.engine.sequencememory.sequenceMemoryScore
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }
internal const val SEQUENCE_MEMORY_START_DELAY_MS = 900L
internal const val SEQUENCE_MEMORY_ROUND_PAUSE_MS = 700L
private const val SEQUENCE_MEMORY_TAP_FLASH_MS = 180L
internal const val SEQUENCE_MEMORY_WRONG_TAP_REVEAL_MS = 900L
private const val SEQUENCE_MEMORY_FOREGROUND_POLL_MS = 250L

data class SequenceMemoryResult(
    val difficulty: Difficulty,
    val solved: Boolean,
    val score: Int,
    val elapsedSeconds: Int,
    val lossReason: String?,
    val isFirstSolve: Boolean = false,
    val isNewHighScore: Boolean = false,
)

private class SequenceMemoryPuzzleAdapter : PuzzleAdapter<SequenceMemorySession, SequenceMemoryResult> {
    override val gameId: GameId = GameId.SEQUENCEMEMORY

    override suspend fun freshSession(difficulty: Difficulty): SequenceMemorySession = createSequenceMemorySession(difficulty)

    override fun restoreSession(payload: String, elapsedSeconds: Double): SequenceMemorySession? =
        runCatching { json.decodeFromString<SequenceMemorySession>(payload) }
            .getOrNull()
            ?.takeIf(::isRestorableSequenceMemorySession)
            ?.let(::sequenceMemorySessionForReplay)

    override fun difficultyOf(session: SequenceMemorySession): Difficulty = Difficulty.fromKey(session.difficulty)

    override fun hasMeaningfulProgress(session: SequenceMemorySession): Boolean = session.completedRounds > 0

    override fun encode(session: SequenceMemorySession): String = json.encodeToString(sequenceMemorySessionForReplay(session))

    override fun scoreOnWin(session: SequenceMemorySession, difficulty: Difficulty, elapsedSeconds: Int): Int =
        sequenceMemoryScore(session.completedRounds)

    override fun scoreOnLoss(session: SequenceMemorySession, difficulty: Difficulty, elapsedSeconds: Int): Int =
        sequenceMemoryScore(session.completedRounds)

    override fun buildResult(session: SequenceMemorySession?, outcome: PuzzleOutcome): SequenceMemoryResult = SequenceMemoryResult(
        difficulty = outcome.difficulty,
        solved = outcome.solved,
        score = outcome.score,
        elapsedSeconds = outcome.elapsedSeconds,
        lossReason = outcome.lossReason,
        isFirstSolve = outcome.isFirstSolve,
        isNewHighScore = outcome.isNewHighScore,
    )
}

@HiltViewModel(assistedFactory = SequenceMemoryPlayViewModel.Factory::class)
class SequenceMemoryPlayViewModel @AssistedInject constructor(
    sessionRepository: SessionStore,
    statsRepository: StatsStore,
    historyRepository: PlayHistoryStore,
    private val appForeground: AppForeground,
    @Assisted requestedDifficulty: Difficulty,
    @Assisted resume: Boolean,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(requestedDifficulty: Difficulty, resume: Boolean): SequenceMemoryPlayViewModel
    }

    private val controller = PuzzleSessionController(
        scope = viewModelScope,
        sessionStore = sessionRepository,
        statsStore = statsRepository,
        historyStore = historyRepository,
        adapter = SequenceMemoryPuzzleAdapter(),
        isAppForeground = appForeground::isForeground,
    )

    val session get() = controller.session
    val elapsedSeconds get() = controller.elapsedSeconds
    val result = controller.result

    var litPad by mutableStateOf<Int?>(null)
        private set
    var roundCompleteTrigger by mutableIntStateOf(0)
        private set
    var wrongTapTrigger by mutableIntStateOf(0)
        private set

    private var playbackJob: Job? = null
    private var tapFlashJob: Job? = null

    init {
        controller.start(requestedDifficulty, resume)
        viewModelScope.launch {
            snapshotFlow { controller.session }.filterNotNull().first()
            playSequence(SEQUENCE_MEMORY_START_DELAY_MS)
        }
    }

    private fun playSequence(leadInMs: Long) {
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            delay(leadInMs)
            val showing = session ?: return@launch
            val config = sequenceMemoryConfig(showing)
            for (pad in showing.sequence) {
                while (!appForeground.isForeground()) delay(SEQUENCE_MEMORY_FOREGROUND_POLL_MS)
                litPad = pad
                delay(config.flashMs)
                litPad = null
                delay(config.gapMs)
            }
            val current = session ?: return@launch
            controller.updateSession(current.copy(phase = SequenceMemoryPhase.INPUT), persist = false)
        }
    }

    fun onPadTap(pad: Int) {
        val current = session ?: return
        if (current.phase != SequenceMemoryPhase.INPUT) return
        val next = applySequenceMemoryTap(current, pad)
        when (next.phase) {
            SequenceMemoryPhase.INPUT -> {
                flashPad(pad)
                controller.updateSession(next, persist = false)
            }
            SequenceMemoryPhase.SHOWING -> {
                flashPad(pad)
                roundCompleteTrigger++
                controller.updateSession(next)
                playSequence(SEQUENCE_MEMORY_ROUND_PAUSE_MS)
            }
            SequenceMemoryPhase.WON -> {
                flashPad(pad)
                controller.updateSession(next, persist = false)
                controller.finishAsWin()
            }
            SequenceMemoryPhase.FAILED -> {
                tapFlashJob?.cancel()
                litPad = sequenceMemoryExpectedPad(next)
                wrongTapTrigger++
                controller.updateSession(next, persist = false)
                viewModelScope.launch {
                    delay(SEQUENCE_MEMORY_WRONG_TAP_REVEAL_MS)
                    controller.finishAsLoss("rule-failure")
                }
            }
        }
    }

    private fun flashPad(pad: Int) {
        tapFlashJob?.cancel()
        litPad = pad
        tapFlashJob = viewModelScope.launch {
            delay(SEQUENCE_MEMORY_TAP_FLASH_MS)
            litPad = null
        }
    }

    fun endPuzzle() {
        playbackJob?.cancel()
        controller.endPuzzle()
    }
}
