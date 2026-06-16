import {
  cloneNonogramBoard,
  type NonogramBoard,
  type NonogramCellRef,
  type NonogramCellValue,
  type NonogramDirectState,
  type NonogramSession,
} from '../types';
import { isNonogramLineComplete } from './rules/solver';

export type NonogramAction =
  | { kind: 'tap'; row: number; col: number; mode: 'fill' | 'cross' }
  | { kind: 'swipe'; cells: readonly NonogramCellRef[]; value: NonogramDirectState };

export interface NonogramActionResult {
  changed: boolean;
  session: NonogramSession;
}

function cycleForMode(
  value: NonogramBoard[number][number],
  mode: 'fill' | 'cross',
): NonogramBoard[number][number] {
  if (mode === 'fill') return value === 1 ? null : 1;
  return value === 0 ? null : 0;
}

function applyDirectValue(
  board: NonogramBoard,
  row: number,
  col: number,
  value: NonogramDirectState,
): boolean {
  const rowCells = board[row];
  if (!rowCells || typeof rowCells[col] === 'undefined') {
    return false;
  }

  const cell = rowCells[col];
  if (cell === value) {
    return false;
  }

  rowCells[col] = value;
  return true;
}

function applyAutoCorrect(board: NonogramBoard, solution: boolean[][]): void {
  board.forEach((row, rowIndex) => {
    row.forEach((cell, colIndex) => {
      const shouldBeFilled = solution[rowIndex]?.[colIndex] ?? false;
      if (cell === 1 && !shouldBeFilled) {
        row[colIndex] = null;
      } else if (cell === 0 && shouldBeFilled) {
        row[colIndex] = null;
      }
    });
  });
}

function isLineCorrectlyComplete(
  cells: readonly NonogramCellValue[],
  clues: readonly number[],
  solutionLine: readonly boolean[],
): boolean {
  return isNonogramLineComplete(cells, clues)
    && cells.every((cell, index) => cell !== 1 || solutionLine[index] === true);
}

function applyAutoFillCompletedLines(
  board: NonogramBoard,
  puzzle: NonogramSession['puzzle'],
  solution: boolean[][],
): void {
  puzzle.rowClues.forEach((clues, rowIndex) => {
    const cells = board[rowIndex];
    if (!cells) return;
    const solutionRow = solution[rowIndex] ?? [];
    if (isLineCorrectlyComplete(cells, clues, solutionRow)) {
      cells.forEach((cell, colIndex) => {
        if (cell === null) cells[colIndex] = 0;
      });
    }
  });

  puzzle.colClues.forEach((clues, colIndex) => {
    const cells = board.map((row) => row[colIndex] ?? null);
    const solutionCol = solution.map((row) => row[colIndex] ?? false);
    if (isLineCorrectlyComplete(cells, clues, solutionCol)) {
      board.forEach((row) => {
        if (row[colIndex] === null) row[colIndex] = 0;
      });
    }
  });
}

export function runNonogramAction(
  session: NonogramSession,
  action: NonogramAction,
): NonogramActionResult {
  const nextSession: NonogramSession = {
    ...session,
    board: cloneNonogramBoard(session.board),
  };

  let changed = false;

  if (action.kind === 'tap') {
    const rowCells = nextSession.board[action.row];
    if (!rowCells || typeof rowCells[action.col] === 'undefined') {
      return {
        changed: false,
        session: nextSession,
      };
    }

    const currentValue = rowCells[action.col];
    const nextValue = cycleForMode(currentValue, action.mode);
    rowCells[action.col] = nextValue;
    changed = currentValue !== nextValue;
  } else {
    const visited = new Set<string>();

    action.cells.forEach(({ row, col }) => {
      const key = `${row}:${col}`;
      if (visited.has(key)) {
        return;
      }
      visited.add(key);
      if (applyDirectValue(nextSession.board, row, col, action.value)) {
        changed = true;
      }
    });
  }

  if (changed) {
    applyAutoFillCompletedLines(nextSession.board, nextSession.puzzle, nextSession.solution);
  }

  return {
    changed,
    session: nextSession,
  };
}
