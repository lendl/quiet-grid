import { formatElapsed } from '../../../app/utils/formatElapsed';
import { computeAccuracyPct, computeFinalScore } from '../../../app/utils/scoring';
import type { PuzzlePlayContract } from '../../../app/shell/playContract';
import { getRandomPuzzle } from '../platform/puzzleData';
import {
  createEmptyCells,
  hasMeaningfulProgress,
  isSolvedSession,
} from './rules/board';
import type {
  NonogramActivePuzzle,
  NonogramHudState,
  NonogramPlaySession,
} from './activePuzzle';

export type { NonogramAction, NonogramActionResult } from './actions';

export const nonogramPlayContract: PuzzlePlayContract<
  NonogramPlaySession,
  NonogramActivePuzzle,
  NonogramHudState
> = {
  createSession: ({ difficulty }) => {
    const puzzle = getRandomPuzzle(difficulty);
    if (!puzzle) {
      return null;
    }

    return {
      puzzle,
      cells: createEmptyCells(puzzle.rows, puzzle.cols),
    };
  },
  canResume: (activePuzzle): activePuzzle is NonogramActivePuzzle => activePuzzle?.puzzleTypeId === 'nonogram',
  restoreSession: (activePuzzle) => ({
    session: {
      puzzle: activePuzzle.puzzle,
      cells: [...activePuzzle.cells],
    },
    elapsedSeconds: activePuzzle.elapsedSeconds,
  }),
  serializeSession: ({ session, elapsedSeconds }) => ({
    puzzleTypeId: 'nonogram',
    puzzle: session.puzzle,
    cells: [...session.cells],
    elapsedSeconds,
  }),
  getPersistenceKey: ({ session, elapsedBucket }) => JSON.stringify({
    puzzleId: session.puzzle.id,
    cells: session.cells,
    elapsedBucket,
  }),
  getHudState: ({ elapsedSeconds }) => ({
    elapsedLabel: formatElapsed(elapsedSeconds),
  }),
  getSolvedState: ({ session, elapsedSeconds }) => {
    if (!isSolvedSession(session)) {
      return null;
    }

    return {
      puzzleTypeId: 'nonogram',
      difficulty: session.puzzle.difficulty,
      solved: true,
      score: computeFinalScore(session.puzzle.difficulty, elapsedSeconds, 0),
      accuracy: computeAccuracyPct(0),
      elapsedSeconds,
    };
  },
  isInProgress: (session) => !isSolvedSession(session),
  hasMeaningfulProgress: (session) => hasMeaningfulProgress(session.cells),
};
