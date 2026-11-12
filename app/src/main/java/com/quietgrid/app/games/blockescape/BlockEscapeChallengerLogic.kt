package com.quietgrid.app.games.blockescape

import com.quietgrid.app.core.Difficulty
import com.quietgrid.app.session.advanceAfterSolve
import com.quietgrid.app.session.creditSolve
import com.quietgrid.app.session.startChallengerSession
import com.quietgrid.engine.blockescape.BlockEscapePuzzleEntry

fun creditBlockEscapeChallengerSolve(session: BlockEscapeChallengerSession): BlockEscapeChallengerSession =
    session.creditSolve(blockEscapeChallengerPuzzleScore(session))

fun createInitialBlockEscapeChallengerSession(firstPuzzle: BlockEscapePuzzleEntry): BlockEscapeChallengerSession =
    startChallengerSession(BLOCKESCAPE_CHALLENGER_CONFIG, createBlockEscapeSession(firstPuzzle), firstPuzzle.id)

fun blockEscapeChallengerPuzzleScore(session: BlockEscapeChallengerSession): Int =
    blockEscapeScore(session.puzzleSession, session.secondsOnCurrentPuzzle.toInt())

fun advanceBlockEscapeChallengerAfterSolve(
    session: BlockEscapeChallengerSession,
    nextTier: Difficulty,
    nextSolvesInTier: Int,
    nextPuzzle: BlockEscapePuzzleEntry,
): BlockEscapeChallengerSession = session.advanceAfterSolve(
    config = BLOCKESCAPE_CHALLENGER_CONFIG,
    gainedScore = blockEscapeChallengerPuzzleScore(session),
    nextTier = nextTier,
    nextSolvesInTier = nextSolvesInTier,
    nextPuzzleSession = createBlockEscapeSession(nextPuzzle),
    nextPuzzleId = nextPuzzle.id,
)
