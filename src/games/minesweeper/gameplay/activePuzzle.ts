import type { MinesweeperBoard, MinesweeperPuzzle } from '../types';

export interface MinesweeperActivePuzzle {
  puzzleTypeId: 'minesweeper';
  puzzle: MinesweeperPuzzle;
  board: MinesweeperBoard;
  elapsedSeconds: number;
}
