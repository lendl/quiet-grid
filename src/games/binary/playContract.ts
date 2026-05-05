import type { BinaryActivePuzzle } from './activePuzzle';
import type { Grid, LineKey, Puzzle } from './types';
import { formatElapsed } from '../../app/utils/format';
import { computeAccuracyPct, computeFinalScore } from '../../app/utils/scoring';
import { decodeMask, decodePuzzle, decodeSolution, getRandomPuzzle } from './puzzleData';
import { isBoardSolved } from './validation';
import { makeEmptyBooleanGrid } from '../../app/utils/activePuzzleStateStorage';
import type { PuzzlePlayContract } from '../../app/shell/playContract';

export interface BinaryPlaySession {
  puzzle: Puzzle;
  board: Grid;
  solution: Grid;
  isGiven: boolean[][];
  finishedCells: boolean[][];
  accuracyDrops: number;
  penalizedLineKeys: LineKey[];
}

export interface BinaryHudState {
  elapsedLabel: string;
}

export type { BinaryAction, BinaryActionResult, BinaryValidationEffect } from './actions';

function createBinarySession(puzzle: Puzzle): BinaryPlaySession {
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

export const binaryPlayContract: PuzzlePlayContract<
  BinaryPlaySession,
  BinaryActivePuzzle,
  BinaryHudState
> = {
  createSession: ({ difficulty }) => {
    const puzzle = getRandomPuzzle(difficulty);
    return puzzle ? createBinarySession(puzzle) : null;
  },
  canResume: (activePuzzle): activePuzzle is BinaryActivePuzzle => activePuzzle?.puzzleTypeId === 'binary',
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
    puzzleTypeId: 'binary',
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
      puzzleTypeId: 'binary',
      difficulty: session.puzzle.difficulty,
      solved: true,
      score: computeFinalScore(session.puzzle.difficulty, elapsedSeconds, session.accuracyDrops),
      accuracy: computeAccuracyPct(session.accuracyDrops),
      elapsedSeconds,
    };
  },
  isInProgress: (session) => !isBoardSolved(session.board, session.solution),
};
