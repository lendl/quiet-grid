import { sudokuDigits, type SudokuBoard, type SudokuDigit, type SudokuNotes } from '../../types';
import { createSudokuBitmaskStateFromBoard, getCandidateMaskAtCell, iterateMaskDigits } from './bitmask';

export interface SudokuCandidateCell {
  row: number;
  col: number;
  value: SudokuBoard[number][number];
  given: boolean;
  logicalCandidates: SudokuDigit[];
  notedCandidates: SudokuDigit[];
  playerCandidates: SudokuDigit[];
}

export type SudokuCandidateGrid = SudokuCandidateCell[][];

export function getLogicalSudokuCandidates(
  board: SudokuBoard,
  row: number,
  col: number,
): SudokuDigit[] {
  if (board[row]?.[col] !== null) {
    return [];
  }

  const state = createSudokuBitmaskStateFromBoard(board);
  return iterateMaskDigits(getCandidateMaskAtCell(state, row, col));
}

export function buildSudokuCandidateGrid(input: {
  board: SudokuBoard;
  givens: SudokuBoard;
  notes?: SudokuNotes;
}): SudokuCandidateGrid {
  const state = createSudokuBitmaskStateFromBoard(input.board);
  return input.board.map((row, rowIndex) => row.map((value, colIndex) => {
    const logicalCandidates = value === null
      ? iterateMaskDigits(getCandidateMaskAtCell(state, rowIndex, colIndex))
      : [];
    const notedCandidates = input.notes?.[rowIndex]?.[colIndex]
      ?.flatMap((enabled, noteIndex) => (enabled ? [sudokuDigits[noteIndex]] : [])) ?? [];
    const playerCandidates = notedCandidates.length > 0
      ? logicalCandidates.filter((digit) => notedCandidates.includes(digit))
      : logicalCandidates;

    return {
      row: rowIndex,
      col: colIndex,
      value,
      given: input.givens[rowIndex]?.[colIndex] !== null,
      logicalCandidates,
      notedCandidates,
      playerCandidates,
    };
  }));
}
