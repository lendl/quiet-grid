import type { Grid, LineKey, Puzzle } from './types';

export interface BinaryActivePuzzle {
  puzzleTypeId: 'binary';
  puzzle: Puzzle;
  board: Grid;
  elapsedSeconds: number;
  accuracyDrops: number;
  finishedCells: boolean[][];
  penalizedLineKeys: LineKey[];
}
