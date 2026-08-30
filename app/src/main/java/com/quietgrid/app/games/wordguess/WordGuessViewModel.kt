package com.quietgrid.app.games.wordguess

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.data.PlayHistoryStore
import com.quietgrid.app.data.SessionStore
import com.quietgrid.app.data.SettingsRepository
import com.quietgrid.app.data.StatsStore
import com.quietgrid.app.data.recentlyPlayedPuzzleIds
import com.quietgrid.app.session.PuzzleAdapter
import com.quietgrid.app.session.PuzzleOutcome
import com.quietgrid.app.session.PuzzleSessionController
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { ignoreUnknownKeys = true }

data class WordGuessResult(
    val difficulty: Difficulty,
    val solved: Boolean,
    val score: Int,
    val elapsedSeconds: Int,
    val lossReason: String?,
    val isFirstSolve: Boolean = false,
    val isNewHighScore: Boolean = false,
    val targetWord: String = "",
)

private class WordGuessPuzzleAdapter(
    private val appContext: Context,
    private val settingsRepository: SettingsRepository,
    private val historyStore: PlayHistoryStore,
) : PuzzleAdapter<WordGuessSession, WordGuessResult> {
    override val gameId: GameId = GameId.WORDGUESS

    override suspend fun freshSession(difficulty: Difficulty): WordGuessSession? {
        val locale = currentWordGuessLocale(settingsRepository.settings.first().puzzleLanguage)
        val recentIds = historyStore.recentlyPlayedPuzzleIds(gameId, difficulty)
        val entry = WordGuessPuzzleBank.randomPuzzle(appContext, locale, difficulty, recentIds) ?: return null
        return WordGuessSession(
            puzzleId = entry.id,
            locale = locale,
            difficulty = entry.difficulty,
            targetWord = entry.word,
            wordLength = entry.word.length,
            guesses = emptyList(),
            status = WordGuessStatus.PLAYING,
        )
    }

    override fun restoreSession(payload: String, elapsedSeconds: Double): WordGuessSession? {
        val persisted = runCatching { json.decodeFromString<WordGuessPersistedSession>(payload) }.getOrNull() ?: return null
        if (persisted.status != WordGuessStatus.PLAYING) return null
        return WordGuessSession(
            puzzleId = persisted.puzzleId,
            locale = persisted.locale,
            difficulty = persisted.difficulty,
            targetWord = persisted.targetWord,
            wordLength = persisted.wordLength,
            guesses = persisted.guesses,
            status = persisted.status,
        )
    }

    override fun difficultyOf(session: WordGuessSession): Difficulty = Difficulty.fromKey(session.difficulty)

    override fun hasMeaningfulProgress(session: WordGuessSession): Boolean = wordGuessHasMeaningfulProgress(session)

    override fun encode(session: WordGuessSession): String = json.encodeToString(
        WordGuessPersistedSession(
            puzzleId = session.puzzleId,
            locale = session.locale,
            difficulty = session.difficulty,
            targetWord = session.targetWord,
            wordLength = session.wordLength,
            guesses = session.guesses,
            status = session.status,
        ),
    )

    override fun puzzleIdOf(session: WordGuessSession): String? = session.puzzleId

    override fun scoreOnWin(session: WordGuessSession, difficulty: Difficulty, elapsedSeconds: Int): Int =
        computeWordGuessScore(session.difficulty, session.guesses.size, elapsedSeconds)

    override fun buildResult(session: WordGuessSession?, outcome: PuzzleOutcome): WordGuessResult = WordGuessResult(
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

@HiltViewModel(assistedFactory = WordGuessPlayViewModel.Factory::class)
class WordGuessPlayViewModel @AssistedInject constructor(
    @ApplicationContext appContext: Context,
    sessionRepository: SessionStore,
    statsRepository: StatsStore,
    historyRepository: PlayHistoryStore,
    settingsRepository: SettingsRepository,
    @Assisted requestedDifficulty: Difficulty,
    @Assisted resume: Boolean,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(requestedDifficulty: Difficulty, resume: Boolean): WordGuessPlayViewModel
    }

    private val controller = PuzzleSessionController(
        scope = viewModelScope,
        sessionStore = sessionRepository,
        statsStore = statsRepository,
        historyStore = historyRepository,
        adapter = WordGuessPuzzleAdapter(appContext, settingsRepository, historyRepository),
    )

    val session get() = controller.session
    val elapsedSeconds get() = controller.elapsedSeconds
    val result = controller.result

    var wrongGuessTrigger by mutableStateOf(0)
        private set

    private val appCtx = appContext
    private var dictionary: Set<String> = emptySet()

    init {
        controller.start(requestedDifficulty, resume)
    }

    fun onSubmitGuess(rawGuess: String, onInvalid: () -> Unit) {
        val current = session ?: return
        viewModelScope.launch {
            if (dictionary.isEmpty()) {
                dictionary = WordGuessPuzzleBank.loadDictionary(appCtx, current.locale)
            }
            when (val outcome = submitWordGuess(current, dictionary, rawGuess)) {
                WordGuessSubmitResult.InvalidWord -> onInvalid()
                is WordGuessSubmitResult.Updated -> {
                    when (outcome.session.status) {
                        WordGuessStatus.WON -> {
                            controller.updateSession(outcome.session, persist = false)
                            controller.finishAsWin()
                        }
                        WordGuessStatus.LOST -> {
                            controller.updateSession(outcome.session, persist = false)
                            controller.finishAsLoss("rule-failure")
                        }
                        WordGuessStatus.PLAYING -> {
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
