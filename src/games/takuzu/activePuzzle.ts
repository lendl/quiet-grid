import type { Grid, LineKey, Puzzle } from './types';

export interface TakuzuActivePuzzle {
  puzzleTypeId: 'takuzu';
  puzzle: Puzzle;
  board: Grid;
  elapsedSeconds: number;
  accuracyDrops: number;
  finishedCells: boolean[][];
  penalizedLineKeys: LineKey[];
}
