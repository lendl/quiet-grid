import type { TakuzuPlaySession } from './playContract';
import type { CellValue, Grid, LineKey } from './types';
import {
  getCompletedLineStateForKey,
  getMismatchedCompletedLines,
} from './validation';

export type TakuzuAction =
  | { type: 'press-cell'; row: number; col: number }
  | { type: 'finalize-validation'; board: Grid; lineKeys: LineKey[] };

export interface TakuzuValidationEffect {
  type: 'validated-lines';
  correctRowIndexes: number[];
  correctColIndexes: number[];
  newPenaltyCount: number;
}

export interface TakuzuActionResult {
  session: TakuzuPlaySession;
  changed: boolean;
  effects: TakuzuValidationEffect[];
}

function mergeFinishedCellsForLineKeys(
  currentFinishedCells: boolean[][],
  board: Grid,
  solution: Grid,
  isGiven: boolean[][],
  lineKeys: Iterable<LineKey>,
): boolean[][] {
  const nextFinishedCells = currentFinishedCells.map((row) => [...row]);
  let changed = false;

  for (const lineKey of lineKeys) {
    if (getCompletedLineStateForKey(board, solution, lineKey) !== 'correct') continue;

    const lineIndex = Number(lineKey.slice(1));
    if (lineKey.startsWith('r')) {
      board[lineIndex].forEach((value, colIndex) => {
        if (!isGiven[lineIndex][colIndex] && value !== null && !nextFinishedCells[lineIndex][colIndex]) {
          nextFinishedCells[lineIndex][colIndex] = true;
          changed = true;
        }
      });
      continue;
    }

    board.forEach((row, rowIndex) => {
      if (!isGiven[rowIndex][lineIndex] && row[lineIndex] !== null && !nextFinishedCells[rowIndex][lineIndex]) {
        nextFinishedCells[rowIndex][lineIndex] = true;
        changed = true;
      }
    });
  }

  return changed ? nextFinishedCells : currentFinishedCells;
}

export function applyTakuzuAction(
  session: TakuzuPlaySession,
  action: TakuzuAction,
): TakuzuActionResult {
  if (action.type === 'press-cell') {
    if (session.isGiven[action.row][action.col] || session.finishedCells[action.row][action.col]) {
      return { session, changed: false, effects: [] };
    }

    const prev = session.board[action.row][action.col];
    const next: CellValue = prev === null ? 0 : prev === 0 ? 1 : null;
    const board = session.board.map((row) => [...row]) as Grid;
    board[action.row][action.col] = next;

    return {
      session: { ...session, board },
      changed: true,
      effects: [],
    };
  }

  const completedLineKeys = action.lineKeys.filter(
    (lineKey) => getCompletedLineStateForKey(action.board, session.solution, lineKey) !== 'incomplete',
  );
  const mismatchKeys = completedLineKeys.filter(
    (lineKey) => getCompletedLineStateForKey(action.board, session.solution, lineKey) === 'incorrect',
  );
  const newPenaltyCount = mismatchKeys.filter(
    (lineKey) => !session.penalizedLineKeys.includes(lineKey),
  ).length;
  const correctRowIndexes = completedLineKeys
    .filter((lineKey) => lineKey.startsWith('r'))
    .filter((lineKey) => getCompletedLineStateForKey(action.board, session.solution, lineKey) === 'correct')
    .map((lineKey) => Number(lineKey.slice(1)));
  const correctColIndexes = completedLineKeys
    .filter((lineKey) => lineKey.startsWith('c'))
    .filter((lineKey) => getCompletedLineStateForKey(action.board, session.solution, lineKey) === 'correct')
    .map((lineKey) => Number(lineKey.slice(1)));

  return {
    session: {
      ...session,
      board: action.board,
      finishedCells: mergeFinishedCellsForLineKeys(
        session.finishedCells,
        action.board,
        session.solution,
        session.isGiven,
        action.lineKeys,
      ),
      penalizedLineKeys: getMismatchedCompletedLines(action.board, session.solution),
      accuracyDrops: session.accuracyDrops + newPenaltyCount,
    },
    changed: true,
    effects: [{
      type: 'validated-lines',
      correctRowIndexes,
      correctColIndexes,
      newPenaltyCount,
    }],
  };
}
