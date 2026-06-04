import type { MinesweeperActiveSession } from './activePuzzle';
import type { MinesweeperBoard, MinesweeperPuzzle } from '../types';
import { countFlaggedCells, createMinesweeperBoard, createMinesweeperPuzzle } from './rules';
import { formatElapsed } from '../../../app/utils/formatElapsed';
import { computeAccuracyPct, computeFinalScore } from '../../../app/utils/scoring';
import type { PuzzlePlayContract } from '../../../app/shell/playContract';
import type { SessionResult } from '../../../app/shell/types';

export interface MinesweeperPlaySession {
  puzzle: MinesweeperPuzzle;
  board: MinesweeperBoard;
}

export interface MinesweeperHudState {
  elapsedLabel: string;
  remainingMines: number;
}

export type { MinesweeperAction, MinesweeperActionEffect, MinesweeperActionResult } from './actions';

export function buildMinesweeperResult(
  session: MinesweeperPlaySession,
  elapsedSeconds = 0,
): SessionResult {
  const solved = session.board.status === 'won';

  return {
    gameId: 'minesweeper',
    difficulty: session.puzzle.difficulty,
    status: solved ? 'solved' : 'failed',
    score: solved ? computeFinalScore(session.puzzle.difficulty, elapsedSeconds, 0) : 0,
    accuracy: computeAccuracyPct(0),
    elapsedSeconds,
    streak: 0,
  };
}

function hasMinesweeperMeaningfulProgress(session: MinesweeperPlaySession): boolean {
  return session.board.generated
    || session.board.cells.some((row) => row.some((cell) => cell.state !== 'hidden'));
}

export const minesweeperPlayContract: PuzzlePlayContract<
  MinesweeperPlaySession,
  MinesweeperActiveSession,
  MinesweeperHudState
> = {
  createSession: ({ difficulty }) => {
    const puzzle = createMinesweeperPuzzle(difficulty);
    return {
      puzzle,
      board: createMinesweeperBoard(puzzle),
    };
  },
  canResume: (activeSession): activeSession is MinesweeperActiveSession => (
    activeSession?.gameId === 'minesweeper' && activeSession.board.status === 'playing'
  ),
  restoreSession: (activeSession) => ({
    session: {
      puzzle: activeSession.puzzle,
      board: activeSession.board,
    },
    elapsedSeconds: activeSession.elapsedSeconds,
  }),
  serializeSession: ({ session, elapsedSeconds }) => ({
    gameId: 'minesweeper',
    puzzle: session.puzzle,
    board: session.board,
    elapsedSeconds,
  }),
  getPersistenceKey: ({ session, elapsedBucket }) => JSON.stringify({
    puzzle: session.puzzle,
    board: session.board,
    elapsedBucket,
  }),
  getHudState: ({ session, elapsedSeconds }) => ({
    elapsedLabel: formatElapsed(elapsedSeconds),
    remainingMines: Math.max(0, session.board.mines - countFlaggedCells(session.board)),
  }),
  getSolvedState: ({ session, elapsedSeconds }) => {
    const result = buildMinesweeperResult(session, elapsedSeconds);
    if (result.status !== 'solved') {
      return null;
    }

    return {
      gameId: result.gameId,
      difficulty: result.difficulty,
      status: result.status,
      score: result.score,
      accuracy: result.accuracy,
      elapsedSeconds: result.elapsedSeconds,
    };
  },
  isInProgress: (session) => session.board.status === 'playing',
  hasMeaningfulProgress: hasMinesweeperMeaningfulProgress,
};
