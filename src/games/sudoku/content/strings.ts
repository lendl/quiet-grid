import { getSudokuStrings as resolveSudokuStrings, type SudokuStrings } from './i18n';

export type { SudokuStrings };

export function getSudokuStrings(): SudokuStrings {
  return resolveSudokuStrings();
}
