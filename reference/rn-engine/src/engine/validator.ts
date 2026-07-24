import type { CellValue } from '../games/takuzu/core';
import {
  equalHalvesFeasible,
  hasUniqueLines,
  isLegalCell,
  noThreeConsec,
} from '../games/takuzu/core';

export type Cell = CellValue;

export { equalHalvesFeasible, hasUniqueLines, isLegalCell, noThreeConsec };

export function isValidSolution(grid: (0 | 1)[][]): boolean {
  const size = grid.length;
  for (let rowIndex = 0; rowIndex < size; rowIndex += 1) {
    if (!noThreeConsec(grid[rowIndex])) return false;
    if (!equalHalvesFeasible(grid[rowIndex], size)) return false;
  }

  for (let colIndex = 0; colIndex < size; colIndex += 1) {
    const column = grid.map((row) => row[colIndex]);
    if (!noThreeConsec(column)) return false;
    if (!equalHalvesFeasible(column, size)) return false;
  }

  return hasUniqueLines(grid);
}
