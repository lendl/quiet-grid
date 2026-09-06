package com.quietgrid.app.games.wordguess

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quietgrid.app.core.ChallengerPuzzleSolve
import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.core.GameId
import com.quietgrid.app.data.SettingsRepository
import com.quietgrid.app.data.StatsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val CHALLENGER_TICK_INTERVAL_MS = 1000L
private const val CHALLENGER_FINISH_DELAY_MS = 450L
private const val CHALLENGER_SOLVE_ADVANCE_DELAY_MS = 700L
private const val CHALLENGER_LOSS_REVEAL_DELAY_MS = 1400L

@HiltViewModel
class WordGuessChallengerViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val settingsRepository: SettingsRepository,
    private val statsStore: StatsStore,
) : ViewModel() {

    var session by mutableStateOf<WordGuessChallengerSession?>(null)
        private set

    var wrongGuessTrigger by mutableStateOf(0)
        private set

    var puzzleWonTrigger by mutableStateOf(0)
        private set
    var puzzleLostTrigger by mutableStateOf(0)
        private set

    private var dictionary: Set<String> = emptySet()
    private var finalized = false
    private var locale: String = "en"

    private val _result = MutableSharedFlow<WordGuessChallengerResult>(extraBufferCapacity = 1)
    val result: SharedFlow<WordGuessChallengerResult> = _result

    init {
        viewModelScope.launch {
            locale = currentWordGuessLocale(settingsRepository.settings.first().puzzleLanguage)
            dictionary = WordGuessPuzzleBank.loadDictionary(appContext, locale)
            val firstPuzzle = WordGuessPuzzleBank.randomPuzzle(appContext, locale, Difficulty.EASY)
            if (firstPuzzle != null) {
                session = createInitialWordGuessChallengerSession(firstPuzzle)
                runTicker()
            }
        }
    }

    fun onSubmitGuess(rawGuess: String, onInvalid: () -> Unit) {
        if (finalized) return
        val current = session ?: return
        when (val outcome = submitWordGuess(current.puzzleSession, dictionary, rawGuess)) {
            WordGuessSubmitResult.InvalidWord -> onInvalid()
            is WordGuessSubmitResult.Updated -> when (outcome.session.status) {
                WordGuessStatus.WON -> onWon(current.copy(puzzleSession = outcome.session))
                WordGuessStatus.LOST -> onLost(current.copy(puzzleSession = outcome.session))
                WordGuessStatus.PLAYING -> {
                    wrongGuessTrigger++
                    session = current.copy(puzzleSession = outcome.session)
                }
            }
        }
    }

    private fun onWon(withGuess: WordGuessChallengerSession) {
        puzzleWonTrigger++
        session = withGuess
        val (nextTier, nextSolvesInTier) = wordGuessChallengerTierAfterSolve(withGuess.tier, withGuess.solvesInTier)
        viewModelScope.launch {
            delay(CHALLENGER_SOLVE_ADVANCE_DELAY_MS)
            val nextPuzzle = WordGuessPuzzleBank.randomPuzzle(appContext, locale, nextTier, withGuess.servedPuzzleIds)
            if (nextPuzzle == null) {
                val creditedScore = withGuess.score + computeWordGuessScore(withGuess.puzzleSession.difficulty, withGuess.puzzleSession.guesses.size, withGuess.secondsOnCurrentPuzzle.toInt())
                val credited = withGuess.copy(
                    puzzlesSolved = withGuess.puzzlesSolved + 1,
                    score = creditedScore,
                    fastestSolveSeconds = wordGuessChallengerFastestSolve(withGuess),
                    puzzleHistory = withGuess.puzzleHistory + ChallengerPuzzleSolve(withGuess.tier, withGuess.secondsOnCurrentPuzzle),
                )
                finalizeRun(credited, "bank_exhausted")
            } else {
                session = advanceWordGuessChallengerAfterSolve(withGuess, nextTier, nextSolvesInTier, nextPuzzle)
            }
        }
    }

    private fun onLost(withGuess: WordGuessChallengerSession) {
        puzzleLostTrigger++
        session = withGuess
        val remainingLives = withGuess.livesRemaining - 1
        val afterLoss = withGuess.copy(livesRemaining = remainingLives)
        if (remainingLives <= 0) {
            finalizeRun(afterLoss, "lives_exhausted")
            return
        }
        viewModelScope.launch {
            delay(CHALLENGER_LOSS_REVEAL_DELAY_MS)
            val nextPuzzle = WordGuessPuzzleBank.randomPuzzle(appContext, locale, afterLoss.tier, afterLoss.servedPuzzleIds)
            if (nextPuzzle == null) {
                finalizeRun(afterLoss, "bank_exhausted")
            } else {
                session = advanceWordGuessChallengerAfterLoss(afterLoss, nextPuzzle)
            }
        }
    }

    fun endRun() {
        val current = session ?: return
        finalizeRun(current, "abandoned")
    }

    private suspend fun runTicker() {
        while (true) {
            delay(CHALLENGER_TICK_INTERVAL_MS)
            if (finalized) continue
            val current = session ?: continue
            val ticked = tickWordGuessChallenger(current)
            session = ticked
            if (ticked.secondsRemaining <= 0) finalizeRun(ticked, "time_up")
        }
    }

    private fun finalizeRun(current: WordGuessChallengerSession, reason: String) {
        if (finalized) return
        finalized = true
        viewModelScope.launch {
            val previousBest = statsStore.challengerStatsFor(GameId.WORDGUESS).first()
            val isNewHighScore = current.score > previousBest.bestScore
            statsStore.recordChallengerResult(GameId.WORDGUESS, current.puzzlesSolved, current.score)
            delay(CHALLENGER_FINISH_DELAY_MS)
            _result.emit(
                WordGuessChallengerResult(
                    puzzlesSolved = current.puzzlesSolved,
                    tierReached = current.tier,
                    score = current.score,
                    isNewHighScore = isNewHighScore,
                    reason = reason,
                    previousBest = previousBest.bestScore,
                    fastestSolveSeconds = current.fastestSolveSeconds,
                    puzzleHistory = current.puzzleHistory,
                    solvesInTier = current.solvesInTier,
                ),
            )
        }
    }
}
