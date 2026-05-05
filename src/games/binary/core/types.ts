import type { PuzzleDifficulty } from '../../shared/types';

export type BinaryCellValue = 0 | 1 | null;
export type BinarySolvedCellValue = 0 | 1;
export type CellValue = BinaryCellValue;
export type BinaryGrid = BinaryCellValue[][];
export type BinarySolutionGrid = BinarySolvedCellValue[][];
export type Grid = BinaryGrid;
export type PuzzleSize = 6 | 8 | 10;
export type BinaryLineKey = `r${number}` | `c${number}`;
export type LineKey = BinaryLineKey;

export interface BinaryPuzzle {
  id: string;
  size: PuzzleSize;
  rows: number;
  cols: number;
  difficulty: PuzzleDifficulty;
  solution: string;
  mask: string;
}

export type Puzzle = BinaryPuzzle;

export interface BinaryPuzzleSession {
  puzzle: BinaryPuzzle;
  board: BinaryGrid;
  elapsedSeconds: number;
  accuracyDrops: number;
  finishedCells: boolean[][];
  penalizedLineKeys: BinaryLineKey[];
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

export type BinaryNextMoveRuleKey = TutorialLessonKey;
export type BinaryNextMoveHintKind = 'progress' | 'recovery' | 'paused';

export interface BinaryNextMoveCell {
  row: number;
  col: number;
}

export interface BinaryNextMoveTargetCell extends BinaryNextMoveCell {
  value?: 0 | 1;
}

export interface BinaryNextMoveHint {
  kind: BinaryNextMoveHintKind;
  title: string;
  body: string;
  ruleKey?: BinaryNextMoveRuleKey;
  evidenceCells: BinaryNextMoveCell[];
  targetCells: BinaryNextMoveTargetCell[];
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
