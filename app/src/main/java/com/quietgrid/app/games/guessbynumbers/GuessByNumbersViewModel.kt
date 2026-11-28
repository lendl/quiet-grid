package com.quietgrid.app.games.guessbynumbers

import com.quietgrid.app.platform.BundledFiles
import com.quietgrid.app.platform.MotionSettings
import com.quietgrid.app.session.AppForeground
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.core.badges.BadgeFacts
import com.quietgrid.app.data.BadgeStore
import com.quietgrid.app.data.PlayHistoryStore
import com.quietgrid.app.data.SessionStore
import com.quietgrid.app.data.SettingsRepository
import com.quietgrid.app.data.StatsStore
import com.quietgrid.app.data.recentlyPlayedPuzzleIds
import com.quietgrid.app.games.wordguess.WordGuessPuzzleBank
import com.quietgrid.app.games.wordguess.currentWordGuessLocale
import com.quietgrid.app.session.LossReason
import com.quietgrid.app.session.PuzzleAdapter
import com.quietgrid.app.session.PuzzleOutcome
import com.quietgrid.app.session.PuzzleSessionController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

data class GuessByNumbersResult(
    val difficulty: Difficulty,
    val solved: Boolean,
    val score: Int,
    val elapsedSeconds: Int,
    val lossReason: LossReason?,
    val isFirstSolve: Boolean = false,
    val isNewHighScore: Boolean = false,
    val targetWord: String = "",
)

private class GuessByNumbersPuzzleAdapter(
    private val files: BundledFiles,
    private val settingsRepository: SettingsRepository,
    private val historyStore: PlayHistoryStore,
) : PuzzleAdapter<GuessByNumbersSession, GuessByNumbersResult> {
    override val gameId: GameId = GameId.GUESSBYNUMBERS

    override suspend fun freshSession(difficulty: Difficulty): GuessByNumbersSession? {
        val locale = currentWordGuessLocale(settingsRepository.settings.first().puzzleLanguage)
        val recentIds = historyStore.recentlyPlayedPuzzleIds(gameId, difficulty)
        val entry = WordGuessPuzzleBank.randomPuzzle(files, locale, difficulty, recentIds) ?: return null
        return GuessByNumbersSession(
            puzzleId = entry.id,
            locale = locale,
            difficulty = entry.difficulty,
            targetWord = entry.word,
            wordLength = entry.word.length,
            guesses = emptyList(),
            status = GuessByNumbersStatus.PLAYING,
        )
    }

    override suspend fun restoreSession(payload: String, elapsedSeconds: Double): GuessByNumbersSession? {
        val persisted = runCatching { json.decodeFromString<GuessByNumbersPersistedSession>(payload) }.getOrNull() ?: return null
        if (persisted.status != GuessByNumbersStatus.PLAYING) return null
        return GuessByNumbersSession(
            puzzleId = persisted.puzzleId,
            locale = persisted.locale,
            difficulty = persisted.difficulty,
            targetWord = persisted.targetWord,
            wordLength = persisted.wordLength,
            guesses = persisted.guesses,
            status = persisted.status,
        )
    }

    override fun difficultyOf(session: GuessByNumbersSession): Difficulty = Difficulty.fromKey(session.difficulty)

    override fun hasMeaningfulProgress(session: GuessByNumbersSession): Boolean = guessByNumbersHasMeaningfulProgress(session)

    override fun encode(session: GuessByNumbersSession): String = json.encodeToString(
        GuessByNumbersPersistedSession(
            puzzleId = session.puzzleId,
            locale = session.locale,
            difficulty = session.difficulty,
            targetWord = session.targetWord,
            wordLength = session.wordLength,
            guesses = session.guesses,
            status = session.status,
        ),
    )

    override fun puzzleIdOf(session: GuessByNumbersSession): String? = session.puzzleId

    override fun badgeFacts(session: GuessByNumbersSession): BadgeFacts = BadgeFacts(guesses = session.guesses.size)

    override fun scoreOnWin(session: GuessByNumbersSession, difficulty: Difficulty, elapsedSeconds: Int): Int =
        computeGuessByNumbersScore(session.difficulty, session.guesses.size, elapsedSeconds)

    override fun buildResult(session: GuessByNumbersSession?, outcome: PuzzleOutcome): GuessByNumbersResult = GuessByNumbersResult(
        difficulty = outcome.difficulty,
        solved = outcome.solved,
        score = outcome.score,
        elapsedSeconds = outcome.elapsedSeconds,
        lossReason = outcome.lossReason,
        isFirstSolve = outcome.isFirstSolve,
        isNewHighScore = outcome.isNewHighScore,
        targetWord = session?.targetWord ?: "",
    )
}

class GuessByNumbersPlayViewModel(
    private val files: BundledFiles,
    sessionRepository: SessionStore,
    statsRepository: StatsStore,
    historyRepository: PlayHistoryStore,
    settingsRepository: SettingsRepository,
    badgeStore: BadgeStore,
    appForeground: AppForeground,
    motion: MotionSettings,
    requestedDifficulty: Difficulty,
    resume: Boolean,
) : ViewModel() {

    private val controller = PuzzleSessionController(
        scope = viewModelScope,
        sessionStore = sessionRepository,
        statsStore = statsRepository,
        historyStore = historyRepository,
        adapter = GuessByNumbersPuzzleAdapter(files, settingsRepository, historyRepository),
        isAppForeground = appForeground::isForeground,
        animationsEnabled = motion::animationsEnabled,
        badgeStore = badgeStore,
    )

    val session get() = controller.session
    val elapsedSeconds get() = controller.elapsedSeconds
    val result = controller.result
    val finishing = controller.finishing

    var wrongGuessTrigger by mutableStateOf(0)
        private set

    private var dictionary: Set<String> = emptySet()

    init {
        controller.start(requestedDifficulty, resume)
    }

    fun onSubmitGuess(rawGuess: String, onInvalid: () -> Unit) {
        val current = session ?: return
        viewModelScope.launch {
            if (dictionary.isEmpty()) {
                dictionary = WordGuessPuzzleBank.loadDictionary(files, current.locale)
            }
            when (val outcome = submitGuessByNumbersGuess(current, dictionary, rawGuess)) {
                GuessByNumbersSubmitResult.InvalidWord -> onInvalid()
                is GuessByNumbersSubmitResult.Updated -> {
                    when (outcome.session.status) {
                        GuessByNumbersStatus.WON -> {
                            controller.updateSession(outcome.session, persist = false)
                            controller.finishAsWin()
                        }
                        GuessByNumbersStatus.LOST -> {
                            controller.updateSession(outcome.session, persist = false)
                            controller.finishAsLoss(LossReason.RULE_FAILURE)
                        }
                        GuessByNumbersStatus.PLAYING -> {
                            wrongGuessTrigger++
                            controller.updateSession(outcome.session)
                        }
                    }
                }
            }
        }
    }

    fun endPuzzle() = controller.endPuzzle()
}
