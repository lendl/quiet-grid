package com.quietgrid.app.games.wordguess

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.data.SessionStore
import com.quietgrid.app.data.StatsStore
import com.quietgrid.app.session.PuzzleAdapter
import com.quietgrid.app.session.PuzzleOutcome
import com.quietgrid.app.session.PuzzleSessionController
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

private class WordGuessPuzzleAdapter(private val appContext: Context) : PuzzleAdapter<WordGuessSession, WordGuessResult> {
    override val gameId: GameId = GameId.WORDGUESS

    override suspend fun freshSession(difficulty: Difficulty): WordGuessSession? {
        val locale = currentWordGuessLocale()
        val entry = WordGuessPuzzleBank.randomPuzzle(appContext, locale, difficulty) ?: return null
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

class WordGuessPlayViewModel(
    appContext: Context,
    sessionRepository: SessionStore,
    statsRepository: StatsStore,
    requestedDifficulty: Difficulty,
    resume: Boolean,
) : ViewModel() {

    private val controller = PuzzleSessionController(
        scope = viewModelScope,
        sessionStore = sessionRepository,
        statsStore = statsRepository,
        adapter = WordGuessPuzzleAdapter(appContext),
    )

    val session get() = controller.session
    val elapsedSeconds get() = controller.elapsedSeconds
    val result = controller.result

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
                        WordGuessStatus.PLAYING -> controller.updateSession(outcome.session)
                    }
                }
            }
        }
    }

    fun endPuzzle() = controller.endPuzzle()
}
