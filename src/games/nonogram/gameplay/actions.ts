import {
  cloneNonogramBoard,
  type NonogramBoard,
  type NonogramCellRef,
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

function applyAutoFillCompletedLines(
  board: NonogramBoard,
  puzzle: NonogramSession['puzzle'],
): void {
  puzzle.rowClues.forEach((clues, rowIndex) => {
    const cells = board[rowIndex];
    if (!cells) return;
    if (isNonogramLineComplete(cells, clues)) {
      cells.forEach((cell, colIndex) => {
        if (cell === null) cells[colIndex] = 0;
      });
    }
  });

  puzzle.colClues.forEach((clues, colIndex) => {
    const cells = board.map((row) => row[colIndex] ?? null);
    if (isNonogramLineComplete(cells, clues)) {
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
    applyAutoCorrect(nextSession.board, nextSession.solution);
    applyAutoFillCompletedLines(nextSession.board, nextSession.puzzle);
  }

  return {
    changed,
    session: nextSession,
  };
}
