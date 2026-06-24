import type { TakuzuActiveSession } from './activePuzzle';
import type { Grid, LineKey, Puzzle } from '../types';
import { formatElapsed } from '../../../app/utils/formatElapsed';
import { computeAccuracyPct, computeFinalScore } from '../../../app/utils/scoring';

const TIME_TO_ZERO_SECONDS = { easy: 300, medium: 450, hard: 600, expert: 900 };
import { decodeMask, decodePuzzle, decodeSolution, getRandomPuzzle } from '../platform/puzzleData';
import { isBoardSolved } from './rules/validation';
import { makeEmptyBooleanGrid } from '../../../app/utils/activeSessionStateStorage';
import type { PuzzlePlayContract } from '../../../app/shell/playContract';
import type { SessionResult } from '../../../app/shell/types';

export interface TakuzuPlaySession {
  puzzle: Puzzle;
  board: Grid;
  solution: Grid;
  isGiven: boolean[][];
  finishedCells: boolean[][];
  accuracyDrops: number;
  penalizedLineKeys: LineKey[];
}

export interface TakuzuHudState {
  elapsedLabel: string;
}

export type { TakuzuAction, TakuzuActionResult, TakuzuValidationEffect } from './actions';

function createTakuzuSession(puzzle: Puzzle): TakuzuPlaySession {
  return {
    puzzle,
    board: decodePuzzle(puzzle.solution, puzzle.mask, puzzle.size),
    solution: decodeSolution(puzzle.solution, puzzle.size),
    isGiven: decodeMask(puzzle.mask, puzzle.size),
    finishedCells: makeEmptyBooleanGrid(puzzle.size),
    accuracyDrops: 0,
    penalizedLineKeys: [],
  };
}

function hasBoardDifference(a: Grid, b: Grid): boolean {
  return a.some((row, rowIndex) => row.some((value, colIndex) => value !== b[rowIndex][colIndex]));
}

function hasFinishedCells(session: TakuzuPlaySession): boolean {
  return session.finishedCells.some((row) => row.some(Boolean));
}

function hasTakuzuMeaningfulProgress(session: TakuzuPlaySession): boolean {
  const initialBoard = decodePuzzle(session.puzzle.solution, session.puzzle.mask, session.puzzle.size);
  return hasBoardDifference(session.board, initialBoard)
    || hasFinishedCells(session)
    || session.accuracyDrops > 0
    || session.penalizedLineKeys.length > 0;
}

export function buildTakuzuResult(
  session: TakuzuPlaySession,
  elapsedSeconds = 0,
): SessionResult {
  const solved = isBoardSolved(session.board, session.solution);

  return {
    gameId: 'takuzu',
    difficulty: session.puzzle.difficulty,
    status: solved ? 'solved' : 'failed',
    score: solved
      ? computeFinalScore(session.puzzle.difficulty, elapsedSeconds, session.accuracyDrops, TIME_TO_ZERO_SECONDS)
      : 0,
    accuracy: computeAccuracyPct(session.accuracyDrops),
    elapsedSeconds,
    streak: 0,
  };
}

export const takuzuPlayContract: PuzzlePlayContract<
  TakuzuPlaySession,
  TakuzuActiveSession,
  TakuzuHudState
> = {
  createSession: ({ difficulty }) => {
    const puzzle = getRandomPuzzle(difficulty);
    return puzzle ? createTakuzuSession(puzzle) : null;
  },
  canResume: (activeSession): activeSession is TakuzuActiveSession => activeSession?.gameId === 'takuzu',
  restoreSession: (activeSession) => ({
    session: {
      puzzle: activeSession.puzzle,
      board: activeSession.board,
      solution: decodeSolution(activeSession.puzzle.solution, activeSession.puzzle.size),
      isGiven: decodeMask(activeSession.puzzle.mask, activeSession.puzzle.size),
      finishedCells: activeSession.finishedCells,
      accuracyDrops: activeSession.accuracyDrops,
      penalizedLineKeys: activeSession.penalizedLineKeys,
    },
    elapsedSeconds: activeSession.elapsedSeconds,
  }),
  serializeSession: ({ session, elapsedSeconds }) => ({
    gameId: 'takuzu',
    puzzle: session.puzzle,
    board: session.board,
    elapsedSeconds,
    accuracyDrops: session.accuracyDrops,
    finishedCells: session.finishedCells,
    penalizedLineKeys: session.penalizedLineKeys,
  }),
  getPersistenceKey: ({ session, elapsedBucket }) => JSON.stringify({
    puzzleId: session.puzzle.id,
    board: session.board,
    finishedCells: session.finishedCells,
    accuracyDrops: session.accuracyDrops,
    penalizedLineKeys: session.penalizedLineKeys,
    elapsedBucket,
  }),
  getHudState: ({ elapsedSeconds }) => ({
    elapsedLabel: formatElapsed(elapsedSeconds),
  }),
  getSolvedState: ({ session, elapsedSeconds }) => {
    if (!isBoardSolved(session.board, session.solution)) {
      return null;
    }

    return {
      gameId: 'takuzu',
      difficulty: session.puzzle.difficulty,
      status: 'solved',
      score: computeFinalScore(session.puzzle.difficulty, elapsedSeconds, session.accuracyDrops, TIME_TO_ZERO_SECONDS),
      accuracy: computeAccuracyPct(session.accuracyDrops),
      elapsedSeconds,
    };
  },
  isInProgress: (session) => !isBoardSolved(session.board, session.solution),
  hasMeaningfulProgress: hasTakuzuMeaningfulProgress,
};
