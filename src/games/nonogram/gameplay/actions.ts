import { cloneNonogramBoard, type NonogramBoard, type NonogramCellRef, type NonogramDirectState, type NonogramSession } from '../types';

export type NonogramAction =
  | { kind: 'tap'; row: number; col: number }
  | { kind: 'swipe'; cells: readonly NonogramCellRef[]; value: NonogramDirectState };

export interface NonogramActionResult {
  changed: boolean;
  session: NonogramSession;
}

function cycleDirectValue(value: NonogramBoard[number][number]): NonogramBoard[number][number] {
  if (value === null) {
    return 1;
  }

  if (value === 1) {
    return 0;
  }

  return null;
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

export function runNonogramAction(
  session: NonogramSession,
  action: NonogramAction,
): NonogramActionResult {
  const nextSession: NonogramSession = {
    ...session,
    board: cloneNonogramBoard(session.board),
  };

  if (action.kind === 'tap') {
    const rowCells = nextSession.board[action.row];
    if (!rowCells || typeof rowCells[action.col] === 'undefined') {
      return {
        changed: false,
        session: nextSession,
      };
    }

    const currentValue = rowCells[action.col];
    rowCells[action.col] = cycleDirectValue(currentValue);
    return {
      changed: currentValue !== rowCells[action.col],
      session: nextSession,
    };
  }

  const visited = new Set<string>();
  let changed = false;

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

  return {
    changed,
    session: nextSession,
  };
}
