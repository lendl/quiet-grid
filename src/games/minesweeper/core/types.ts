import type { PuzzleDifficulty } from '../../shared/types';

export type MinesweeperCellState = 'hidden' | 'revealed' | 'flagged';

export interface MinesweeperCell {
  isMine: boolean;
  adjacentMines: number;
  state: MinesweeperCellState;
}

export interface MinesweeperBoard {
  rows: number;
  cols: number;
  mines: number;
  generated: boolean;
  cells: MinesweeperCell[][];
  status: 'playing' | 'won' | 'lost';
}

export interface MinesweeperPuzzle {
  difficulty: PuzzleDifficulty;
  rows: number;
  cols: number;
  mines: number;
}

export interface MinesweeperPuzzleSession {
  puzzle: MinesweeperPuzzle;
  board: MinesweeperBoard;
  elapsedSeconds: number;
}
