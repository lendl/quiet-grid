import type { PuzzleDifficulty } from '../shared/types';

export const SUDOKU_SIZE = 9;
export const SUDOKU_BOX_SIZE = 3;
export const sudokuDigits = [1, 2, 3, 4, 5, 6, 7, 8, 9] as const;
export const sudokuInputModes = ['digit', 'notes'] as const;

export type SudokuDigit = typeof sudokuDigits[number];
export type SudokuCellValue = SudokuDigit | null;
export type SudokuBoard = SudokuCellValue[][];
export type SudokuSolution = SudokuDigit[][];
export type SudokuNotes = boolean[][][];
export type SudokuInputMode = typeof sudokuInputModes[number];
export type SudokuUnitKey = `r${number}` | `c${number}` | `b${number}`;

export interface SudokuPuzzle {
  id: string;
  difficulty: PuzzleDifficulty;
  rows: number;
  cols: number;
  givens: SudokuBoard;
  solution: SudokuSolution;
}

export interface SudokuSession {
  puzzle: SudokuPuzzle;
  board: SudokuBoard;
  notes: SudokuNotes;
  inputMode: SudokuInputMode;
  selectedNoteDigit: SudokuDigit | null;
  accuracyDrops: number;
  finishedCells: boolean[][];
  validatedUnitKeys: SudokuUnitKey[];
  penalizedUnitKeys: SudokuUnitKey[];
}

export interface SudokuActiveSession extends SudokuSession {
  gameId: 'sudoku';
  elapsedSeconds: number;
}

export function cloneSudokuBoard(board: SudokuBoard): SudokuBoard {
  return board.map((row) => [...row]);
}

export function createEmptySudokuNotes(rows: number, cols: number): SudokuNotes {
  return Array.from(
    { length: rows },
    () => Array.from({ length: cols }, () => Array.from({ length: sudokuDigits.length }, () => false)),
  );
}

export function cloneSudokuNotes(notes: SudokuNotes): SudokuNotes {
  return notes.map((row) => row.map((cellNotes) => [...cellNotes]));
}

export function cloneSudokuSolution(solution: SudokuSolution): SudokuSolution {
  return solution.map((row) => [...row]);
}

export function cloneSudokuUnitKeys(unitKeys: readonly SudokuUnitKey[]): SudokuUnitKey[] {
  return [...unitKeys];
}

export function cloneSudokuBooleanGrid(grid: boolean[][]): boolean[][] {
  return grid.map((row) => [...row]);
}

export function countFilledSudokuCells(board: SudokuBoard): number {
  return board.reduce(
    (count, row) => count + row.filter((cell) => cell !== null).length,
    0,
  );
}

export function hasSudokuNotes(notes: SudokuNotes): boolean {
  return notes.some((row) => row.some((cellNotes) => cellNotes.some(Boolean)));
}

export function isSudokuGivenCell(givens: SudokuBoard, row: number, col: number): boolean {
  return givens[row]?.[col] !== null;
}

export function isSudokuSolved(board: SudokuBoard, solution: SudokuSolution): boolean {
  return board.length === solution.length
    && board.every((row, rowIndex) => row.length === solution[rowIndex]?.length
      && row.every((cell, colIndex) => cell === solution[rowIndex][colIndex]));
}
