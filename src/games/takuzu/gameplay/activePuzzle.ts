import type { Grid, LineKey, Puzzle } from '../types';

export interface TakuzuActiveSession {
  gameId: 'takuzu';
  puzzle: Puzzle;
  board: Grid;
  elapsedSeconds: number;
  accuracyDrops: number;
  finishedCells: boolean[][];
  penalizedLineKeys: LineKey[];
}
