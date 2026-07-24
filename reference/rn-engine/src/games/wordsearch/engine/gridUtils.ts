import type { WordSearchCellRef, WordSearchDirection } from '../types';

export const directionToDelta: Record<WordSearchDirection, { row: number; col: number }> = {
  right: { row: 0, col: 1 },
  left: { row: 0, col: -1 },
  down: { row: 1, col: 0 },
  up: { row: -1, col: 0 },
  'down-right': { row: 1, col: 1 },
  'down-left': { row: 1, col: -1 },
  'up-right': { row: -1, col: 1 },
  'up-left': { row: -1, col: -1 },
};

export function toGridKey(cell: WordSearchCellRef): number {
  return (cell.row * 1000) + cell.col;
}

export function collectEmptyCells(grid: string[][]): WordSearchCellRef[] {
  const cells: WordSearchCellRef[] = [];
  grid.forEach((row, rowIndex) => {
    row.forEach((cell, colIndex) => {
      if (cell === '') {
        cells.push({ row: rowIndex, col: colIndex });
      }
    });
  });
  return cells;
}
