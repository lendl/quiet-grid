import type { NonogramCellRef, NonogramCellState } from '../../types';
import { decodeSolutionBits } from '../../platform/puzzleData';
import type { NonogramAction } from '../actions';
import type { NonogramPlaySession } from '../activePuzzle';

export interface NonogramActionResult {
  changed: boolean;
  session: NonogramPlaySession;
  effects: readonly [];
}

export function cellIndex(row: number, col: number, cols: number): number {
  return row * cols + col;
}

export function cellRefFromIndex(index: number, cols: number): NonogramCellRef {
  return {
    row: Math.floor(index / cols),
    col: index % cols,
  };
}

export function nextCellState(state: NonogramCellState): NonogramCellState {
  if (state === 'empty') {
    return 'filled';
  }
  if (state === 'filled') {
    return 'marked';
  }
  return 'empty';
}

export function createEmptyCells(rows: number, cols: number): NonogramCellState[] {
  return Array.from({ length: rows * cols }, () => 'empty');
}

export function hasMeaningfulProgress(cells: readonly NonogramCellState[]): boolean {
  return cells.some((cell) => cell !== 'empty');
}

export function isSolvedSession(session: NonogramPlaySession): boolean {
  const { puzzle, cells } = session;
  const solutionBits = decodeSolutionBits(puzzle.solution, puzzle.rows, puzzle.cols);

  for (let index = 0; index < solutionBits.length; index += 1) {
    const shouldBeFilled = solutionBits[index] === true;
    const state = cells[index];
    if (shouldBeFilled && state !== 'filled') {
      return false;
    }
    if (!shouldBeFilled && state === 'filled') {
      return false;
    }
  }

  return true;
}

function paintIndices(
  session: NonogramPlaySession,
  indices: readonly number[],
  state: NonogramCellState,
): NonogramActionResult {
  const nextCells = [...session.cells];
  let changed = false;

  indices.forEach((index) => {
    if (index < 0 || index >= nextCells.length) {
      return;
    }
    if (nextCells[index] === state) {
      return;
    }
    nextCells[index] = state;
    changed = true;
  });

  return {
    changed,
    session: changed ? { ...session, cells: nextCells } : session,
    effects: [],
  };
}

export function applyNonogramAction(
  session: NonogramPlaySession,
  action: NonogramAction,
): NonogramActionResult {
  if (action.type === 'replace-session') {
    return {
      changed: true,
      session: action.session,
      effects: [],
    };
  }

  if (action.type === 'toggle-cell') {
    const current = session.cells[action.index];
    if (!current) {
      return { changed: false, session, effects: [] };
    }
    return paintIndices(session, [action.index], nextCellState(current));
  }

  if (action.type === 'paint-cell') {
    return paintIndices(session, [action.index], action.state);
  }

  return paintIndices(session, [...new Set(action.indices)], action.state);
}
