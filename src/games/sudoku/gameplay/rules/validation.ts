import {
  SUDOKU_BOX_SIZE,
  SUDOKU_SIZE,
  type SudokuBoard,
  type SudokuCellValue,
  type SudokuSolution,
  type SudokuUnitKey,
} from '../../types';

export type CompletedSudokuUnitState = 'incomplete' | 'correct' | 'incorrect';

function buildSudokuRowKey(index: number): SudokuUnitKey {
  return `r${index}` as SudokuUnitKey;
}

function buildSudokuColumnKey(index: number): SudokuUnitKey {
  return `c${index}` as SudokuUnitKey;
}

function buildSudokuBoxKey(index: number): SudokuUnitKey {
  return `b${index}` as SudokuUnitKey;
}

export function getSudokuBoxIndex(row: number, col: number): number {
  return Math.floor(row / SUDOKU_BOX_SIZE) * SUDOKU_BOX_SIZE + Math.floor(col / SUDOKU_BOX_SIZE);
}

export function getSudokuBoxOrigin(boxIndex: number): { rowStart: number; colStart: number } {
  return {
    rowStart: Math.floor(boxIndex / SUDOKU_BOX_SIZE) * SUDOKU_BOX_SIZE,
    colStart: (boxIndex % SUDOKU_BOX_SIZE) * SUDOKU_BOX_SIZE,
  };
}

export function getSudokuTouchedUnitKeys(row: number, col: number): SudokuUnitKey[] {
  return [
    buildSudokuRowKey(row),
    buildSudokuColumnKey(col),
    buildSudokuBoxKey(getSudokuBoxIndex(row, col)),
  ];
}

function getSudokuUnitIndexes(unitKey: SudokuUnitKey): Array<{ row: number; col: number }> {
  const index = Number(unitKey.slice(1));
  const prefix = unitKey[0];

  if (prefix === 'r') {
    return Array.from({ length: SUDOKU_SIZE }, (_, col) => ({ row: index, col }));
  }

  if (prefix === 'c') {
    return Array.from({ length: SUDOKU_SIZE }, (_, row) => ({ row, col: index }));
  }

  const { rowStart, colStart } = getSudokuBoxOrigin(index);
  return Array.from({ length: SUDOKU_BOX_SIZE * SUDOKU_BOX_SIZE }, (_, offset) => ({
    row: rowStart + Math.floor(offset / SUDOKU_BOX_SIZE),
    col: colStart + (offset % SUDOKU_BOX_SIZE),
  }));
}

export function getSudokuUnitValues(board: SudokuBoard, unitKey: SudokuUnitKey): SudokuCellValue[] {
  return getSudokuUnitIndexes(unitKey).map(({ row, col }) => board[row][col]);
}

export function isSudokuUnitComplete(board: SudokuBoard, unitKey: SudokuUnitKey): boolean {
  return getSudokuUnitValues(board, unitKey).every((cell) => cell !== null);
}

export function getCompletedSudokuUnitStateForKey(
  board: SudokuBoard,
  solution: SudokuSolution,
  unitKey: SudokuUnitKey,
): CompletedSudokuUnitState {
  const indexes = getSudokuUnitIndexes(unitKey);
  if (indexes.some(({ row, col }) => board[row][col] === null)) {
    return 'incomplete';
  }

  return indexes.every(({ row, col }) => board[row][col] === solution[row][col])
    ? 'correct'
    : 'incorrect';
}

function collectCompletedSudokuUnitKeys(
  board: SudokuBoard,
  solution: SudokuSolution,
  targetState: Exclude<CompletedSudokuUnitState, 'incomplete'>,
): SudokuUnitKey[] {
  const unitKeys: SudokuUnitKey[] = [];

  for (let index = 0; index < SUDOKU_SIZE; index += 1) {
    const rowKey = buildSudokuRowKey(index);
    const colKey = buildSudokuColumnKey(index);
    const boxKey = buildSudokuBoxKey(index);

    if (getCompletedSudokuUnitStateForKey(board, solution, rowKey) === targetState) {
      unitKeys.push(rowKey);
    }
    if (getCompletedSudokuUnitStateForKey(board, solution, colKey) === targetState) {
      unitKeys.push(colKey);
    }
    if (getCompletedSudokuUnitStateForKey(board, solution, boxKey) === targetState) {
      unitKeys.push(boxKey);
    }
  }

  return unitKeys;
}

export function getCorrectCompletedSudokuUnitKeys(
  board: SudokuBoard,
  solution: SudokuSolution,
): SudokuUnitKey[] {
  return collectCompletedSudokuUnitKeys(board, solution, 'correct');
}

export function getMismatchedCompletedSudokuUnitKeys(
  board: SudokuBoard,
  solution: SudokuSolution,
): SudokuUnitKey[] {
  return collectCompletedSudokuUnitKeys(board, solution, 'incorrect');
}
