export type NonogramCellValue = 0 | 1 | null;

export interface NonogramCellRef {
  row: number;
  col: number;
}

export interface NonogramPuzzle {
  id: string;
  difficulty: 'easy' | 'medium' | 'hard' | 'expert';
  rows: number;
  cols: number;
  rowClues: readonly (readonly number[])[];
  colClues: readonly (readonly number[])[];
}

export type NonogramBoard = NonogramCellValue[][];

export interface NonogramSession {
  puzzle: NonogramPuzzle;
  board: NonogramBoard;
  solution: boolean[][];
}

export interface NonogramActiveSession extends NonogramSession {
  gameId: 'nonogram';
  elapsedSeconds: number;
}

export type NonogramLineOrientation = 'row' | 'col';

export type NonogramDirectState = 0 | 1;

export const nonogramCanonicalMoves = [
  'overlap-fill',
  'complete-line',
] as const;

export type NonogramCanonicalMoveKey = typeof nonogramCanonicalMoves[number];

export function createEmptyNonogramBoard(rows: number, cols: number): NonogramBoard {
  return Array.from({ length: rows }, () => Array.from({ length: cols }, () => null));
}

export function cloneNonogramBoard(board: NonogramBoard): NonogramBoard {
  return board.map((row) => [...row]);
}

export function cloneBooleanMatrix(matrix: boolean[][]): boolean[][] {
  return matrix.map((row) => [...row]);
}

export function isNonogramSolved(board: NonogramBoard, solution: boolean[][]): boolean {
  return board.length === solution.length
    && board.every((row, rowIndex) => row.length === solution[rowIndex]?.length
      && row.every((cell, colIndex) => (
        solution[rowIndex][colIndex]
          ? cell === 1
          : cell !== 1
      )));
}
