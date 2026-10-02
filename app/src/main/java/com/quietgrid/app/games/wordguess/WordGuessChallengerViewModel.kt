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
import com.quietgrid.app.data.PlayHistoryStore
import com.quietgrid.app.data.SettingsRepository
import com.quietgrid.app.data.StatsStore
import com.quietgrid.app.session.AppForeground
import com.quietgrid.app.session.ChallengerRunController
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val CHALLENGER_SOLVE_ADVANCE_DELAY_MS = 700L
private const val CHALLENGER_LOSS_REVEAL_DELAY_MS = 1400L

@HiltViewModel
class WordGuessChallengerViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val settingsRepository: SettingsRepository,
    statsStore: StatsStore,
    historyStore: PlayHistoryStore,
    appForeground: AppForeground,
) : ViewModel() {

    private val controller = ChallengerRunController(
        scope = viewModelScope,
        gameId = GameId.WORDGUESS,
        statsStore = statsStore,
        historyStore = historyStore,
        appForeground = appForeground,
        tick = ::tickWordGuessChallenger,
    )

    val session get() = controller.session
    val result = controller.result

    var wrongGuessTrigger by mutableStateOf(0)
        private set

    var puzzleWonTrigger by mutableStateOf(0)
        private set
    var puzzleLostTrigger by mutableStateOf(0)
        private set

    private var dictionary: Set<String> = emptySet()
    private var locale: String = "en"

    init {
        viewModelScope.launch {
            locale = currentWordGuessLocale(settingsRepository.settings.first().puzzleLanguage)
            dictionary = WordGuessPuzzleBank.loadDictionary(appContext, locale)
            val firstPuzzle = WordGuessPuzzleBank.randomPuzzle(appContext, locale, Difficulty.EASY)
            if (firstPuzzle != null) {
                controller.start(createInitialWordGuessChallengerSession(firstPuzzle))
            }
        }
    }

    fun onSubmitGuess(rawGuess: String, onInvalid: () -> Unit) {
        if (controller.isFinalized) return
        val current = session ?: return
        when (val outcome = submitWordGuess(current.puzzleSession, dictionary, rawGuess)) {
            WordGuessSubmitResult.InvalidWord -> onInvalid()
            is WordGuessSubmitResult.Updated -> when (outcome.session.status) {
                WordGuessStatus.WON -> onWon(current.copy(puzzleSession = outcome.session))
                WordGuessStatus.LOST -> onLost(current.copy(puzzleSession = outcome.session))
                WordGuessStatus.PLAYING -> {
                    wrongGuessTrigger++
                    controller.session = current.copy(puzzleSession = outcome.session)
                }
            }
        }
    }

    private fun onWon(withGuess: WordGuessChallengerSession) {
        puzzleWonTrigger++
        controller.session = withGuess
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
                controller.finalizeRun(credited, "bank_exhausted")
            } else {
                controller.session = advanceWordGuessChallengerAfterSolve(withGuess, nextTier, nextSolvesInTier, nextPuzzle)
            }
        }
    }

    private fun onLost(withGuess: WordGuessChallengerSession) {
        puzzleLostTrigger++
        controller.session = withGuess
        val remainingLives = withGuess.livesRemaining - 1
        val afterLoss = withGuess.copy(livesRemaining = remainingLives)
        if (remainingLives <= 0) {
            controller.finalizeRun(afterLoss, "lives_exhausted")
            return
        }
        viewModelScope.launch {
            delay(CHALLENGER_LOSS_REVEAL_DELAY_MS)
            val nextPuzzle = WordGuessPuzzleBank.randomPuzzle(appContext, locale, afterLoss.tier, afterLoss.servedPuzzleIds)
            if (nextPuzzle == null) {
                controller.finalizeRun(afterLoss, "bank_exhausted")
            } else {
                controller.session = advanceWordGuessChallengerAfterLoss(afterLoss, nextPuzzle)
            }
        }
    }

    fun endRun() = controller.endRun()
}
