import { Dimensions } from 'react-native';
import type { MinesweeperActivePuzzle } from './activePuzzle';
import type { MinesweeperBoard, MinesweeperPuzzle } from '../types';
import { countFlaggedCells, createMinesweeperBoard, createMinesweeperPuzzle } from './rules';
import { estimateMinesweeperPlayWidth } from '../core/responsive';
import { formatElapsed } from '../../../app/utils/formatElapsed';
import { computeAccuracyPct, computeFinalScore } from '../../../app/utils/scoring';
import type { PuzzlePlayContract } from '../../../app/shell/playContract';

export interface MinesweeperPlaySession {
  puzzle: MinesweeperPuzzle;
  board: MinesweeperBoard;
}

export interface MinesweeperHudState {
  elapsedLabel: string;
  remainingMines: number;
}

export type { MinesweeperAction, MinesweeperActionEffect, MinesweeperActionResult } from './actions';

function hasMinesweeperMeaningfulProgress(session: MinesweeperPlaySession): boolean {
  return session.board.generated
    || session.board.cells.some((row) => row.some((cell) => cell.state !== 'hidden'));
}

export const minesweeperPlayContract: PuzzlePlayContract<
  MinesweeperPlaySession,
  MinesweeperActivePuzzle,
  MinesweeperHudState
> = {
  createSession: ({ difficulty }) => {
    const availableWidth = estimateMinesweeperPlayWidth(Dimensions.get('window').width);
    const puzzle = createMinesweeperPuzzle(difficulty, availableWidth);
    return {
      puzzle,
      board: createMinesweeperBoard(puzzle),
    };
  },
  canResume: (activePuzzle): activePuzzle is MinesweeperActivePuzzle => (
    activePuzzle?.puzzleTypeId === 'minesweeper' && activePuzzle.board.status === 'playing'
  ),
  restoreSession: (activePuzzle) => ({
    session: {
      puzzle: activePuzzle.puzzle,
      board: activePuzzle.board,
    },
    elapsedSeconds: activePuzzle.elapsedSeconds,
  }),
  serializeSession: ({ session, elapsedSeconds }) => ({
    puzzleTypeId: 'minesweeper',
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
    if (session.board.status !== 'won') {
      return null;
    }

    return {
      puzzleTypeId: 'minesweeper',
      difficulty: session.puzzle.difficulty,
      solved: true,
      score: computeFinalScore(session.puzzle.difficulty, elapsedSeconds, 0),
      accuracy: computeAccuracyPct(0),
      elapsedSeconds,
    };
  },
  isInProgress: (session) => session.board.status === 'playing',
  hasMeaningfulProgress: hasMinesweeperMeaningfulProgress,
};
