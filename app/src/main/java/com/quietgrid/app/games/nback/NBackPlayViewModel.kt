package com.quietgrid.app.games.nback

import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.data.PlayHistoryStore
import com.quietgrid.app.data.SessionStore
import com.quietgrid.app.data.StatsStore
import com.quietgrid.app.session.PuzzleAdapter
import com.quietgrid.app.session.PuzzleOutcome
import com.quietgrid.app.session.PuzzleSessionController
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class NBackResult(
    val difficulty: Difficulty,
    val solved: Boolean,
    val score: Int,
    val elapsedSeconds: Int,
    val lossReason: String?,
    val isFirstSolve: Boolean = false,
    val isNewHighScore: Boolean = false,
    val accuracyPct: Int = 0,
    val hits: Int = 0,
    val misses: Int = 0,
    val falsePositives: Int = 0,
    val averageReactionTimeMs: Int = 0,
)

private class NBackPuzzleAdapter : PuzzleAdapter<NBackSession, NBackResult> {
    override val gameId: GameId = GameId.NBACK

    override suspend fun freshSession(difficulty: Difficulty): NBackSession =
        createNBackSession(difficulty)

    override fun restoreSession(payload: String, elapsedSeconds: Double): NBackSession? = null

    override fun difficultyOf(session: NBackSession): Difficulty = session.difficulty

    override fun hasMeaningfulProgress(session: NBackSession): Boolean =
        nbackHasMeaningfulProgress(session)

    override fun encode(session: NBackSession): String = "{}"

    override fun scoreOnWin(session: NBackSession, difficulty: Difficulty, elapsedSeconds: Int): Int =
        computeNBackScore(session.trials)

    override fun scoreOnLoss(session: NBackSession, difficulty: Difficulty, elapsedSeconds: Int): Int =
        computeNBackScore(session.trials)

    override fun buildResult(session: NBackSession?, outcome: PuzzleOutcome): NBackResult {
        val trials = session?.trials.orEmpty()
        return NBackResult(
            difficulty = outcome.difficulty,
            solved = outcome.solved,
            score = outcome.score,
            elapsedSeconds = outcome.elapsedSeconds,
            lossReason = outcome.lossReason,
            isFirstSolve = outcome.isFirstSolve,
            isNewHighScore = outcome.isNewHighScore,
            accuracyPct = nbackAccuracyPct(trials),
            hits = nbackHits(trials),
            misses = nbackMisses(trials),
            falsePositives = nbackFalsePositives(trials),
            averageReactionTimeMs = nbackAverageReactionTimeMs(trials),
        )
    }
}

@HiltViewModel(assistedFactory = NBackPlayViewModel.Factory::class)
class NBackPlayViewModel @AssistedInject constructor(
    sessionRepository: SessionStore,
    statsRepository: StatsStore,
    historyRepository: PlayHistoryStore,
    @Assisted requestedDifficulty: Difficulty,
    @Assisted resume: Boolean,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(requestedDifficulty: Difficulty, resume: Boolean): NBackPlayViewModel
    }

    private val controller = PuzzleSessionController(
        scope = viewModelScope,
        sessionStore = sessionRepository,
        statsStore = statsRepository,
        historyStore = historyRepository,
        adapter = NBackPuzzleAdapter(),
    )

    val session get() = controller.session
    val elapsedSeconds get() = controller.elapsedSeconds
    val result = controller.result

    private var trialStartMillis = 0L
    private var stimulusJob: Job? = null

    init {
        controller.start(requestedDifficulty, resume)
        viewModelScope.launch {
            val started = snapshotFlow { controller.session }.filterNotNull().first()
            beginStimulusLoop(started)
        }
    }

    private fun beginStimulusLoop(initial: NBackSession) {
        stimulusJob?.cancel()
        stimulusJob = viewModelScope.launch {
            delay(NBACK_START_DELAY_MS)
            for (index in initial.trials.indices) {
                val current = session ?: return@launch
                controller.updateSession(current.copy(currentIndex = index, showStimulus = true), persist = false)
                trialStartMillis = System.currentTimeMillis()
                delay(NBACK_STIMULUS_ON_MS)
                val lit = session ?: return@launch
                controller.updateSession(lit.copy(showStimulus = false), persist = false)
                delay(current.config.intervalMs - NBACK_STIMULUS_ON_MS)
            }
            val finished = session ?: return@launch
            controller.updateSession(finished.copy(currentIndex = finished.trials.size), persist = false)
            controller.finishAsWin()
        }
    }

    fun onMatchTap() {
        val current = session ?: return
        val index = current.currentIndex
        if (index !in current.trials.indices) return
        if (current.trials[index].responded) return
        val reactionTimeMs = System.currentTimeMillis() - trialStartMillis
        val updatedTrials = current.trials.toMutableList()
        updatedTrials[index] = updatedTrials[index].copy(responded = true, reactionTimeMs = reactionTimeMs)
        controller.updateSession(current.copy(trials = updatedTrials), persist = false)
    }

    fun endPuzzle() {
        stimulusJob?.cancel()
        controller.endPuzzle()
    }
}
