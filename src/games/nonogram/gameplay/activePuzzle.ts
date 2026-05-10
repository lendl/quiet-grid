import type { NonogramCellState, NonogramPuzzle } from '../types';

export interface NonogramActivePuzzle {
  puzzleTypeId: 'nonogram';
  puzzle: NonogramPuzzle;
  cells: NonogramCellState[];
  elapsedSeconds: number;
}

export interface NonogramPlaySession {
  puzzle: NonogramPuzzle;
  cells: NonogramCellState[];
}

export interface NonogramHudState {
  elapsedLabel: string;
}
