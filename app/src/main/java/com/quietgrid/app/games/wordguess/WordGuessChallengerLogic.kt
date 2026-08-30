package com.quietgrid.app.games.wordguess

import com.quietgrid.app.core.Difficulty
import com.quietgrid.engine.wordguess.WordGuessPuzzleEntry

private val WORDGUESS_CHALLENGER_TIER_ORDER = listOf(Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD, Difficulty.EXPERT)

private fun sessionFromEntry(entry: WordGuessPuzzleEntry): WordGuessSession = WordGuessSession(
    puzzleId = entry.id,
    locale = entry.locale,
    difficulty = entry.difficulty,
    targetWord = entry.word,
    wordLength = entry.word.length,
    guesses = emptyList(),
    status = WordGuessStatus.PLAYING,
)

fun createInitialWordGuessChallengerSession(firstPuzzle: WordGuessPuzzleEntry): WordGuessChallengerSession =
    WordGuessChallengerSession(
        puzzleSession = sessionFromEntry(firstPuzzle),
        livesRemaining = WORDGUESS_CHALLENGER_STARTING_LIVES,
        tier = Difficulty.EASY,
        solvesInTier = 0,
        puzzlesSolved = 0,
        score = 0,
        secondsRemaining = WORDGUESS_CHALLENGER_STARTING_SECONDS,
        secondsOnCurrentPuzzle = 0.0,
        servedPuzzleIds = setOf(firstPuzzle.id),
    )

fun wordGuessChallengerTierAfterSolve(currentTier: Difficulty, solvesInTier: Int): Pair<Difficulty, Int> {
    val nextSolves = solvesInTier + 1
    val currentIndex = WORDGUESS_CHALLENGER_TIER_ORDER.indexOf(currentTier)
    return if (nextSolves >= WORDGUESS_CHALLENGER_SOLVES_PER_TIER && currentIndex < WORDGUESS_CHALLENGER_TIER_ORDER.lastIndex) {
        WORDGUESS_CHALLENGER_TIER_ORDER[currentIndex + 1] to 0
    } else {
        currentTier to nextSolves
    }
}

fun advanceWordGuessChallengerAfterSolve(
    session: WordGuessChallengerSession,
    nextTier: Difficulty,
    nextSolvesInTier: Int,
    nextPuzzle: WordGuessPuzzleEntry,
): WordGuessChallengerSession {
    val gainedScore = computeWordGuessScore(session.puzzleSession.difficulty, session.puzzleSession.guesses.size, session.secondsOnCurrentPuzzle.toInt())
    return session.copy(
        puzzleSession = sessionFromEntry(nextPuzzle),
        tier = nextTier,
        solvesInTier = nextSolvesInTier,
        puzzlesSolved = session.puzzlesSolved + 1,
        score = session.score + gainedScore,
        secondsRemaining = session.secondsRemaining + WORDGUESS_CHALLENGER_BONUS_SECONDS,
        secondsOnCurrentPuzzle = 0.0,
        servedPuzzleIds = session.servedPuzzleIds + nextPuzzle.id,
    )
}

fun advanceWordGuessChallengerAfterLoss(
    session: WordGuessChallengerSession,
    nextPuzzle: WordGuessPuzzleEntry,
): WordGuessChallengerSession = session.copy(
    puzzleSession = sessionFromEntry(nextPuzzle),
    secondsOnCurrentPuzzle = 0.0,
    servedPuzzleIds = session.servedPuzzleIds + nextPuzzle.id,
)

fun tickWordGuessChallenger(session: WordGuessChallengerSession): WordGuessChallengerSession = session.copy(
    secondsRemaining = session.secondsRemaining - 1.0,
    secondsOnCurrentPuzzle = session.secondsOnCurrentPuzzle + 1.0,
)
