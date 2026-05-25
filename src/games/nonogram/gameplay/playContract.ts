import { formatElapsed } from '../../../app/utils/formatElapsed';
import type { PuzzlePlayContract } from '../../../app/shell/playContract';
import type { SessionResult } from '../../../app/shell/types';
import { createRandomNonogramSession } from '../platform/puzzleData';
import {
  cloneBooleanMatrix,
  cloneNonogramBoard,
  isNonogramSolved,
  type NonogramActiveSession,
  type NonogramSession,
} from '../types';

export type NonogramPlaySession = NonogramSession;

export interface NonogramHudState {
  elapsedLabel: string;
}

export function buildNonogramResult(
  session: NonogramPlaySession,
  elapsedSeconds = 0,
): SessionResult {
  const solved = isNonogramSolved(session.board, session.solution);

  return {
    gameId: 'nonogram',
    difficulty: session.puzzle.difficulty,
    status: solved ? 'solved' : 'failed',
    score: solved ? Math.max(0, 10_000 - (elapsedSeconds * 10)) : 0,
    accuracy: solved ? 100 : 0,
    elapsedSeconds,
    streak: 0,
  };
}

export const nonogramPlayContract: PuzzlePlayContract<
  NonogramPlaySession,
  NonogramActiveSession,
  NonogramHudState
> = {
  createSession: ({ difficulty }) => createRandomNonogramSession(difficulty),
  canResume: (activeSession): activeSession is NonogramActiveSession => activeSession?.gameId === 'nonogram',
  restoreSession: (activeSession) => ({
    session: {
      puzzle: activeSession.puzzle,
      board: cloneNonogramBoard(activeSession.board),
      solution: cloneBooleanMatrix(activeSession.solution),
    },
    elapsedSeconds: activeSession.elapsedSeconds,
  }),
  serializeSession: ({ session, elapsedSeconds }) => ({
    gameId: 'nonogram',
    puzzle: session.puzzle,
    board: cloneNonogramBoard(session.board),
    solution: cloneBooleanMatrix(session.solution),
    elapsedSeconds,
  }),
  getPersistenceKey: ({ session, elapsedBucket }) => JSON.stringify({
    puzzleId: session.puzzle.id,
    board: session.board,
    solution: session.solution,
    elapsedBucket,
  }),
  getHudState: ({ elapsedSeconds }) => ({
    elapsedLabel: formatElapsed(elapsedSeconds),
  }),
  getSolvedState: ({ session, elapsedSeconds }) => (isNonogramSolved(session.board, session.solution)
    ? {
        gameId: 'nonogram',
        difficulty: session.puzzle.difficulty,
        status: 'solved',
        score: Math.max(0, 10_000 - (elapsedSeconds * 10)),
        accuracy: 100,
        elapsedSeconds,
      }
    : null),
  isInProgress: (session) => !isNonogramSolved(session.board, session.solution),
  hasMeaningfulProgress: (session) => session.board.some((row) => row.some((cell) => cell !== null)),
};
