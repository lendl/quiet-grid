import type { PuzzleDifficulty } from '../../shared/types';

export type TakuzuCellValue = 0 | 1 | null;
export type TakuzuSolvedCellValue = 0 | 1;
export type CellValue = TakuzuCellValue;
export type TakuzuGrid = TakuzuCellValue[][];
export type TakuzuSolutionGrid = TakuzuSolvedCellValue[][];
export type Grid = TakuzuGrid;
export type PuzzleSize = 6 | 8 | 10;
export type TakuzuLineKey = `r${number}` | `c${number}`;
export type LineKey = TakuzuLineKey;

export interface TakuzuPuzzle {
  id: string;
  size: PuzzleSize;
  rows: number;
  cols: number;
  difficulty: PuzzleDifficulty;
  solution: string;
  mask: string;
}

export type Puzzle = TakuzuPuzzle;

export interface TakuzuPuzzleSession {
  puzzle: TakuzuPuzzle;
  board: TakuzuGrid;
  elapsedSeconds: number;
  accuracyDrops: number;
  finishedCells: boolean[][];
  penalizedLineKeys: TakuzuLineKey[];
}

export interface TutorialMove {
  row: number;
  col: number;
  value: 0 | 1;
}

export type TutorialLessonKey =
  | 'find-pairs'
  | 'avoid-trios'
  | 'complete-lines'
  | 'eliminate-filled-lines'
  | 'eliminate-impossible-combinations';

export type TakuzuNextMoveRuleKey = TutorialLessonKey;
export type TakuzuNextMoveHintKind = 'progress' | 'recovery' | 'paused';

export interface TakuzuNextMoveCell {
  row: number;
  col: number;
}

export interface TakuzuNextMoveTargetCell extends TakuzuNextMoveCell {
  value?: 0 | 1;
}

export interface TakuzuNextMoveHint {
  kind: TakuzuNextMoveHintKind;
  title: string;
  body: string;
  ruleKey?: TakuzuNextMoveRuleKey;
  evidenceCells: TakuzuNextMoveCell[];
  targetCells: TakuzuNextMoveTargetCell[];
  highlightRows: number[];
  highlightCols: number[];
}

export interface TutorialLesson {
  key: TutorialLessonKey;
  title: string;
  body: string;
  prompt: string;
  retry: string;
  success: string;
  grid: Grid;
  moves: TutorialMove[];
}
