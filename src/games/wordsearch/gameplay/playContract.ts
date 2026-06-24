import { formatElapsed } from '../../../app/utils/formatElapsed';
import { MIN_SCORE } from '../../../app/utils/scoring';
import type { PuzzlePlayContract } from '../../../app/shell/playContract';
import type { SessionResult } from '../../../app/shell/types';
import {
  cloneWordSearchPuzzle,
  cloneWordSearchSelection,
  isWordSearchSolved,
  type WordSearchActiveSession,
  type WordSearchSession,
} from '../types';
import { createRandomWordSearchSession } from '../platform/puzzleData';

export interface WordSearchHudState {
  elapsedLabel: string;
  foundWords: number;
  totalWords: number;
}

export type WordSearchPlaySession = WordSearchSession;

const SKIPPED_WORD_BONUS = 300;

function computeWordSearchScore(session: WordSearchPlaySession, elapsedSeconds: number): number {
  const base = Math.max(MIN_SCORE, 15_000 - (elapsedSeconds * 8));
  const skipped = session.puzzle.words.length - session.foundWordIds.length;
  return base + skipped * SKIPPED_WORD_BONUS;
}

export function buildWordSearchResult(session: WordSearchPlaySession, elapsedSeconds = 0): SessionResult {
  const solved = isWordSearchSolved(session);

  return {
    gameId: 'wordsearch',
    difficulty: session.puzzle.difficulty,
    status: solved ? 'solved' : 'failed',
    score: solved ? computeWordSearchScore(session, elapsedSeconds) : 0,
    accuracy: 100,
    elapsedSeconds,
    streak: 0,
  };
}

export const wordSearchPlayContract: PuzzlePlayContract<
  WordSearchPlaySession,
  WordSearchActiveSession,
  WordSearchHudState
> = {
  createSession: ({ difficulty }) => createRandomWordSearchSession(difficulty),
  canResume: (activeSession): activeSession is WordSearchActiveSession => activeSession?.gameId === 'wordsearch',
  restoreSession: (activeSession) => ({
    session: {
      puzzle: cloneWordSearchPuzzle(activeSession.puzzle),
      foundWordIds: [...activeSession.foundWordIds],
      tempSelection: cloneWordSearchSelection(activeSession.tempSelection),
      hiddenWordMode: activeSession.hiddenWordMode ?? false,
      hiddenWordProgress: activeSession.hiddenWordProgress?.map((cell) => ({ ...cell })) ?? [],
      hiddenWordSolved: activeSession.hiddenWordSolved ?? false,
    },
    elapsedSeconds: activeSession.elapsedSeconds,
  }),
  serializeSession: ({ session, elapsedSeconds }) => ({
    gameId: 'wordsearch',
    puzzle: cloneWordSearchPuzzle(session.puzzle),
    foundWordIds: [...session.foundWordIds],
    tempSelection: cloneWordSearchSelection(session.tempSelection),
    hiddenWordMode: session.hiddenWordMode,
    hiddenWordProgress: session.hiddenWordProgress.map((cell) => ({ ...cell })),
    hiddenWordSolved: session.hiddenWordSolved,
    elapsedSeconds,
  }),
  getPersistenceKey: ({ session, elapsedBucket }) => JSON.stringify({
    puzzleId: session.puzzle.id,
    foundWordIds: session.foundWordIds,
    hiddenWordMode: session.hiddenWordMode,
    hiddenWordProgress: session.hiddenWordProgress,
    hiddenWordSolved: session.hiddenWordSolved,
    elapsedBucket,
  }),
  getHudState: ({ session, elapsedSeconds }) => ({
    elapsedLabel: formatElapsed(elapsedSeconds),
    foundWords: session.foundWordIds.length,
    totalWords: session.puzzle.words.length,
  }),
  getSolvedState: ({ session, elapsedSeconds }) => (isWordSearchSolved(session)
    ? {
        gameId: 'wordsearch',
        difficulty: session.puzzle.difficulty,
        status: 'solved',
        score: computeWordSearchScore(session, elapsedSeconds),
        accuracy: 100,
        elapsedSeconds,
      }
    : null),
  isInProgress: (session) => !isWordSearchSolved(session),
  hasMeaningfulProgress: (session) => (
    session.foundWordIds.length > 0
    || session.hiddenWordProgress.length > 0
    || session.hiddenWordSolved
  ),
};
