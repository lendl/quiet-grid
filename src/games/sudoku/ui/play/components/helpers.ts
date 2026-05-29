import type { BoardFeedbackCell } from '../../../../../app/shell/boardFeedback';
import { SUDOKU_BOX_SIZE, SUDOKU_SIZE, type SudokuUnitKey } from '../../../types';
import { getSudokuBoxOrigin } from '../../../gameplay/rules/validation';

export type SudokuUnitStatus = 'correct' | 'incorrect';

export function buildSudokuCellKey(row: number, col: number): string {
  return `${row}:${col}`;
}

function appendUniqueCell(
  cells: BoardFeedbackCell[],
  keys: Set<string>,
  row: number,
  col: number,
): void {
  const key = buildSudokuCellKey(row, col);
  if (keys.has(key)) {
    return;
  }

  keys.add(key);
  cells.push({ row, col });
}

export function buildSudokuCellsForUnitGroups({
  rows = [],
  cols = [],
  boxes = [],
}: {
  rows?: readonly number[];
  cols?: readonly number[];
  boxes?: readonly number[];
}): BoardFeedbackCell[] {
  const keys = new Set<string>();
  const cells: BoardFeedbackCell[] = [];

  rows.forEach((row) => {
    for (let col = 0; col < SUDOKU_SIZE; col += 1) {
      appendUniqueCell(cells, keys, row, col);
    }
  });

  cols.forEach((col) => {
    for (let row = 0; row < SUDOKU_SIZE; row += 1) {
      appendUniqueCell(cells, keys, row, col);
    }
  });

  boxes.forEach((boxIndex) => {
    const { rowStart, colStart } = getSudokuBoxOrigin(boxIndex);

    for (let rowOffset = 0; rowOffset < SUDOKU_BOX_SIZE; rowOffset += 1) {
      for (let colOffset = 0; colOffset < SUDOKU_BOX_SIZE; colOffset += 1) {
        appendUniqueCell(
          cells,
          keys,
          rowStart + rowOffset,
          colStart + colOffset,
        );
      }
    }
  });

  return cells;
}

export function buildSudokuCellsForUnitKeys(unitKeys: readonly SudokuUnitKey[]): BoardFeedbackCell[] {
  const rows: number[] = [];
  const cols: number[] = [];
  const boxes: number[] = [];

  unitKeys.forEach((unitKey) => {
    const index = Number(unitKey.slice(1));
    switch (unitKey[0]) {
      case 'r':
        rows.push(index);
        break;
      case 'c':
        cols.push(index);
        break;
      default:
        boxes.push(index);
        break;
    }
  });

  return buildSudokuCellsForUnitGroups({ rows, cols, boxes });
}

export function buildSudokuUnitStatusLookup(
  validatedUnitKeys: readonly SudokuUnitKey[],
  penalizedUnitKeys: readonly SudokuUnitKey[],
): Map<string, SudokuUnitStatus> {
  const lookup = new Map<string, SudokuUnitStatus>();

  buildSudokuCellsForUnitKeys(validatedUnitKeys).forEach(({ row, col }) => {
    lookup.set(buildSudokuCellKey(row, col), 'correct');
  });
  buildSudokuCellsForUnitKeys(penalizedUnitKeys).forEach(({ row, col }) => {
    lookup.set(buildSudokuCellKey(row, col), 'incorrect');
  });

  return lookup;
}
