import type { PuzzleDifficulty } from '../shared/types';

export interface ChimpTestPuzzle {
  id: string;
  difficulty: PuzzleDifficulty;
  gridSize: number;
  startCount: number;
  maxCount: number;
}

export interface ChimpTestCell {
  number: number;
  row: number;
  col: number;
  hidden: boolean;
}

export interface ChimpTestSession {
  puzzle: ChimpTestPuzzle;
  currentCount: number;
  cells: ChimpTestCell[];
  nextExpected: number;
  revealAll: boolean;
  wrongTapCell: number | null;
  roundTimes: number[];
  roundStartElapsed: number;
  status: 'playing' | 'won';
}

export interface ChimpTestActiveSession extends ChimpTestSession {
  gameId: 'chimptest';
  elapsedSeconds: number;
}
