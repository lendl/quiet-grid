import type { MinesweeperBoard, MinesweeperPuzzle } from '../types';

export interface MinesweeperActiveSession {
  gameId: 'minesweeper';
  puzzle: MinesweeperPuzzle;
  board: MinesweeperBoard;
  elapsedSeconds: number;
}
