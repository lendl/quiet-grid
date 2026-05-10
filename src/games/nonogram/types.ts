import type { PuzzleDifficulty } from '../shared/types';

export type NonogramDifficulty = Extract<PuzzleDifficulty, 'easy' | 'medium'>;
export type NonogramSize = 5 | 10;
export type NonogramCellState = 'empty' | 'filled' | 'marked';
export type NonogramAxis = 'row' | 'col';
export type NonogramTutorialAction = 'filled' | 'marked';

export interface NonogramPuzzle {
  id: string;
  size: NonogramSize;
  rows: number;
  cols: number;
  difficulty: NonogramDifficulty;
  solution: string;
  rowClues: number[][];
  colClues: number[][];
}

export interface NonogramLineRef {
  axis: NonogramAxis;
  index: number;
  clues: number[];
}

export interface NonogramCellRef {
  row: number;
  col: number;
}

export interface NonogramExplanationCopy {
  title: string;
  body: string;
}

export interface NonogramTutorialLesson {
  key: string;
  title: string;
  body: string;
  prompt: string;
  retry: string;
  success: string;
  puzzle: {
    rows: number;
    cols: number;
    rowClues: number[][];
    colClues: number[][];
  };
  initialCells: NonogramCellState[];
  targetCells: NonogramCellRef[];
  action: NonogramTutorialAction;
}
