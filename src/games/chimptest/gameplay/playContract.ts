import { formatElapsed } from '../../../app/utils/formatElapsed';
import type { PuzzlePlayContract } from '../../../app/shell/playContract';
import type { SessionResult } from '../../../app/shell/types';
import type { ChimpTestActiveSession, ChimpTestSession } from '../types';
import { createChimpTestSession, generateChimpTestCells } from '../platform/puzzleData';

export type ChimpTestPlaySession = ChimpTestSession;

export interface ChimpTestHudState {
  elapsedLabel: string;
}

export function buildChimpTestResult(
  session: ChimpTestPlaySession,
  elapsedSeconds = 0,
): SessionResult {
  const totalRoundTime = session.roundTimes.reduce((sum, t) => sum + t, 0);
  const solved = session.status === 'won';

  return {
    gameId: 'chimptest',
    difficulty: session.puzzle.difficulty,
    status: solved ? 'solved' : 'failed',
    score: solved ? Math.max(0, 50_000 - Math.round(totalRoundTime * 500)) : 0,
    accuracy: solved ? 100 : 0,
    elapsedSeconds,
    streak: 0,
  };
}

export const chimpTestPlayContract: PuzzlePlayContract<
  ChimpTestPlaySession,
  ChimpTestActiveSession,
  ChimpTestHudState
> = {
  createSession: ({ difficulty }) => createChimpTestSession(difficulty),

  canResume: (activeSession): activeSession is ChimpTestActiveSession =>
    activeSession?.gameId === 'chimptest',

  restoreSession: (activeSession) => ({
    session: {
      puzzle: { ...activeSession.puzzle },
      currentCount: activeSession.currentCount,
      cells: generateChimpTestCells(activeSession.currentCount, activeSession.puzzle.gridSize),
      nextExpected: 1,
      revealAll: false,
      wrongTapCell: null,
      roundTimes: [...activeSession.roundTimes],
      roundStartElapsed: activeSession.elapsedSeconds,
      status: activeSession.status,
    },
    elapsedSeconds: activeSession.elapsedSeconds,
  }),

  serializeSession: ({ session, elapsedSeconds }) => ({
    gameId: 'chimptest',
    puzzle: { ...session.puzzle },
    currentCount: session.currentCount,
    cells: session.cells.map((c) => ({ ...c })),
    nextExpected: session.nextExpected,
    revealAll: false,
    wrongTapCell: null,
    roundTimes: [...session.roundTimes],
    roundStartElapsed: session.roundStartElapsed,
    status: session.status,
    elapsedSeconds,
  }),

  getPersistenceKey: ({ session, elapsedBucket }) => JSON.stringify({
    puzzleId: session.puzzle.id,
    currentCount: session.currentCount,
    nextExpected: session.nextExpected,
    roundsCompleted: session.roundTimes.length,
    elapsedBucket,
  }),

  getHudState: ({ elapsedSeconds }) => ({
    elapsedLabel: formatElapsed(elapsedSeconds),
  }),

  getSolvedState: ({ session, elapsedSeconds }) => {
    if (session.status !== 'won') return null;
    const totalRoundTime = session.roundTimes.reduce((sum, t) => sum + t, 0);
    return {
      gameId: 'chimptest',
      difficulty: session.puzzle.difficulty,
      status: 'solved',
      score: Math.max(0, 50_000 - Math.round(totalRoundTime * 500)),
      accuracy: 100,
      elapsedSeconds,
    };
  },

  isInProgress: (session) => session.status === 'playing',

  hasMeaningfulProgress: (session) =>
    session.roundTimes.length > 0 || session.nextExpected > 1,
};
