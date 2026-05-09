import type { TakuzuActivePuzzle } from './activePuzzle';
import type { Grid, LineKey, Puzzle } from '../types';
import { formatElapsed } from '../../../app/utils/formatElapsed';
import { computeAccuracyPct, computeFinalScore } from '../../../app/utils/scoring';
import { decodeMask, decodePuzzle, decodeSolution, getRandomPuzzle } from '../platform/puzzleData';
import { isBoardSolved } from './rules/validation';
import { makeEmptyBooleanGrid } from '../../../app/utils/activePuzzleStateStorage';
import type { PuzzlePlayContract } from '../../../app/shell/playContract';

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

export const takuzuPlayContract: PuzzlePlayContract<
  TakuzuPlaySession,
  TakuzuActivePuzzle,
  TakuzuHudState
> = {
  createSession: ({ difficulty }) => {
    const puzzle = getRandomPuzzle(difficulty);
    return puzzle ? createTakuzuSession(puzzle) : null;
  },
  canResume: (activePuzzle): activePuzzle is TakuzuActivePuzzle => activePuzzle?.puzzleTypeId === 'takuzu',
  restoreSession: (activePuzzle) => ({
    session: {
      puzzle: activePuzzle.puzzle,
      board: activePuzzle.board,
      solution: decodeSolution(activePuzzle.puzzle.solution, activePuzzle.puzzle.size),
      isGiven: decodeMask(activePuzzle.puzzle.mask, activePuzzle.puzzle.size),
      finishedCells: activePuzzle.finishedCells,
      accuracyDrops: activePuzzle.accuracyDrops,
      penalizedLineKeys: activePuzzle.penalizedLineKeys,
    },
    elapsedSeconds: activePuzzle.elapsedSeconds,
  }),
  serializeSession: ({ session, elapsedSeconds }) => ({
    puzzleTypeId: 'takuzu',
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
      puzzleTypeId: 'takuzu',
      difficulty: session.puzzle.difficulty,
      solved: true,
      score: computeFinalScore(session.puzzle.difficulty, elapsedSeconds, session.accuracyDrops),
      accuracy: computeAccuracyPct(session.accuracyDrops),
      elapsedSeconds,
    };
  },
  isInProgress: (session) => !isBoardSolved(session.board, session.solution),
  hasMeaningfulProgress: hasTakuzuMeaningfulProgress,
};
