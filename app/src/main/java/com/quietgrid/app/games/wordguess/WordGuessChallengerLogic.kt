package com.quietgrid.app.games.wordguess

import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.session.advanceAfterSolve
import com.quietgrid.app.session.startChallengerSession
import com.quietgrid.engine.wordguess.WordGuessPuzzleEntry

private fun sessionFromEntry(entry: WordGuessPuzzleEntry): WordGuessSession = WordGuessSession(
    puzzleId = entry.id,
    locale = entry.locale,
    difficulty = entry.difficulty,
    targetWord = entry.word,
    wordLength = entry.word.length,
    guesses = emptyList(),
    status = WordGuessStatus.PLAYING,
)

fun advanceWordGuessChallengerAfterLoss(
    session: WordGuessChallengerSession,
    nextPuzzle: WordGuessPuzzleEntry,
): WordGuessChallengerSession = session.copy(
    puzzleSession = sessionFromEntry(nextPuzzle),
    secondsOnCurrentPuzzle = 0.0,
    servedPuzzleIds = session.servedPuzzleIds + nextPuzzle.id,
)

fun createInitialWordGuessChallengerSession(firstPuzzle: WordGuessPuzzleEntry): WordGuessChallengerSession =
    startChallengerSession(WORDGUESS_CHALLENGER_CONFIG, sessionFromEntry(firstPuzzle), firstPuzzle.id)

fun wordGuessChallengerPuzzleScore(session: WordGuessChallengerSession): Int =
    computeWordGuessScore(session.puzzleSession.difficulty, session.puzzleSession.guesses.size, session.secondsOnCurrentPuzzle.toInt())

fun advanceWordGuessChallengerAfterSolve(
    session: WordGuessChallengerSession,
    nextTier: Difficulty,
    nextSolvesInTier: Int,
    nextPuzzle: WordGuessPuzzleEntry,
): WordGuessChallengerSession = session.advanceAfterSolve(
    config = WORDGUESS_CHALLENGER_CONFIG,
    gainedScore = wordGuessChallengerPuzzleScore(session),
    nextTier = nextTier,
    nextSolvesInTier = nextSolvesInTier,
    nextPuzzleSession = sessionFromEntry(nextPuzzle),
    nextPuzzleId = nextPuzzle.id,
)
