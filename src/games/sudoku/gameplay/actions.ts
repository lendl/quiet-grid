import {
  cloneSudokuBoard,
  cloneSudokuNotes,
  isSudokuGivenCell,
  type SudokuBoard,
  type SudokuDigit,
  type SudokuSession,
  type SudokuUnitKey,
} from '../types';
import {
  getCompletedSudokuUnitStateForKey,
  getCorrectCompletedSudokuUnitKeys,
  getMismatchedCompletedSudokuUnitKeys,
} from './rules/validation';

export type SudokuAction =
  | { type: 'set-cell-digit'; row: number; col: number; digit: SudokuDigit }
  | { type: 'clear-cell'; row: number; col: number }
  | { type: 'toggle-cell-note'; row: number; col: number; digit: SudokuDigit }
  | { type: 'finalize-validation'; board: SudokuBoard; unitKeys: SudokuUnitKey[] };

export interface SudokuValidationEffect {
  type: 'validated-units';
  correctRowIndexes: number[];
  correctColIndexes: number[];
  correctBoxIndexes: number[];
  incorrectRowIndexes: number[];
  incorrectColIndexes: number[];
  incorrectBoxIndexes: number[];
  newPenaltyCount: number;
}

export type SudokuActionEffect = SudokuValidationEffect;

export interface SudokuActionResult {
  session: SudokuSession;
  changed: boolean;
  effects: SudokuActionEffect[];
}

function clearCellNotes(notes: SudokuSession['notes'], row: number, col: number): void {
  const cellNotes = notes[row]?.[col];
  if (!cellNotes) {
    return;
  }

  for (let index = 0; index < cellNotes.length; index += 1) {
    cellNotes[index] = false;
  }
}

function groupSudokuUnitIndexes(unitKeys: readonly SudokuUnitKey[]) {
  return unitKeys.reduce<{
    rows: number[];
    cols: number[];
    boxes: number[];
  }>((acc, unitKey) => {
    const index = Number(unitKey.slice(1));
    switch (unitKey[0]) {
      case 'r':
        acc.rows.push(index);
        break;
      case 'c':
        acc.cols.push(index);
        break;
      default:
        acc.boxes.push(index);
        break;
    }
    return acc;
  }, {
    rows: [],
    cols: [],
    boxes: [],
  });
}

function mergeFinishedCellsForUnitKeys(
  currentFinishedCells: boolean[][],
  board: SudokuBoard,
  givens: SudokuBoard,
  unitKeys: readonly SudokuUnitKey[],
): boolean[][] {
  const nextFinishedCells = currentFinishedCells.map((row) => [...row]);
  let changed = false;

  unitKeys.forEach((unitKey) => {
    const index = Number(unitKey.slice(1));

    if (unitKey.startsWith('r')) {
      board[index].forEach((value, colIndex) => {
        if (givens[index][colIndex] === null && value !== null && !nextFinishedCells[index][colIndex]) {
          nextFinishedCells[index][colIndex] = true;
          changed = true;
        }
      });
      return;
    }

    if (unitKey.startsWith('c')) {
      board.forEach((row, rowIndex) => {
        if (givens[rowIndex][index] === null && row[index] !== null && !nextFinishedCells[rowIndex][index]) {
          nextFinishedCells[rowIndex][index] = true;
          changed = true;
        }
      });
      return;
    }

    const boxRow = Math.floor(index / 3) * 3;
    const boxCol = (index % 3) * 3;
    for (let row = boxRow; row < boxRow + 3; row += 1) {
      for (let col = boxCol; col < boxCol + 3; col += 1) {
        if (givens[row][col] === null && board[row][col] !== null && !nextFinishedCells[row][col]) {
          nextFinishedCells[row][col] = true;
          changed = true;
        }
      }
    }
  });

  return changed ? nextFinishedCells : currentFinishedCells;
}

export function applySudokuAction(
  session: SudokuSession,
  action: SudokuAction,
): SudokuActionResult {
  if (action.type === 'set-cell-digit') {
    if (!session.board[action.row] || typeof session.board[action.row][action.col] === 'undefined') {
      return { session, changed: false, effects: [] };
    }

    if (
      isSudokuGivenCell(session.puzzle.givens, action.row, action.col)
      || session.finishedCells[action.row][action.col]
    ) {
      return { session, changed: false, effects: [] };
    }

    const board = cloneSudokuBoard(session.board);
    const notes = cloneSudokuNotes(session.notes);
    if (board[action.row][action.col] === action.digit) {
      return { session, changed: false, effects: [] };
    }
    board[action.row][action.col] = action.digit;
    clearCellNotes(notes, action.row, action.col);

    return {
      session: {
        ...session,
        board,
        notes,
      },
      changed: true,
      effects: [],
    };
  }

  if (action.type === 'clear-cell') {
    if (!session.board[action.row] || typeof session.board[action.row][action.col] === 'undefined') {
      return { session, changed: false, effects: [] };
    }

    if (
      isSudokuGivenCell(session.puzzle.givens, action.row, action.col)
      || session.finishedCells[action.row][action.col]
    ) {
      return { session, changed: false, effects: [] };
    }

    const cellHasNotes = session.notes[action.row]?.[action.col]?.some(Boolean) ?? false;
    if (session.board[action.row][action.col] === null && !cellHasNotes) {
      return { session, changed: false, effects: [] };
    }

    const board = cloneSudokuBoard(session.board);
    const notes = cloneSudokuNotes(session.notes);
    board[action.row][action.col] = null;
    clearCellNotes(notes, action.row, action.col);

    return {
      session: {
        ...session,
        board,
        notes,
      },
      changed: true,
      effects: [],
    };
  }

  if (action.type === 'toggle-cell-note') {
    if (!session.board[action.row] || typeof session.board[action.row][action.col] === 'undefined') {
      return { session, changed: false, effects: [] };
    }

    if (
      isSudokuGivenCell(session.puzzle.givens, action.row, action.col)
      || session.finishedCells[action.row][action.col]
      || session.board[action.row][action.col] !== null
    ) {
      return { session, changed: false, effects: [] };
    }

    const notes = cloneSudokuNotes(session.notes);
    const digitIndex = action.digit - 1;
    notes[action.row][action.col][digitIndex] = !notes[action.row][action.col][digitIndex];

    return {
      session: {
        ...session,
        notes,
      },
      changed: true,
      effects: [],
    };
  }

  const uniqueUnitKeys = Array.from(new Set(action.unitKeys));
  const completedUnitKeys = uniqueUnitKeys.filter(
    (unitKey) => getCompletedSudokuUnitStateForKey(action.board, session.puzzle.solution, unitKey) !== 'incomplete',
  );
  const incorrectUnitKeys = completedUnitKeys.filter(
    (unitKey) => getCompletedSudokuUnitStateForKey(action.board, session.puzzle.solution, unitKey) === 'incorrect',
  );
  const correctUnitKeys = completedUnitKeys.filter(
    (unitKey) => getCompletedSudokuUnitStateForKey(action.board, session.puzzle.solution, unitKey) === 'correct',
  );
  const newPenaltyCount = incorrectUnitKeys.filter(
    (unitKey) => !session.penalizedUnitKeys.includes(unitKey),
  ).length;
  const groupedCorrect = groupSudokuUnitIndexes(correctUnitKeys);
  const groupedIncorrect = groupSudokuUnitIndexes(incorrectUnitKeys);

  return {
    session: {
      ...session,
      board: cloneSudokuBoard(action.board),
      accuracyDrops: session.accuracyDrops + newPenaltyCount,
      finishedCells: mergeFinishedCellsForUnitKeys(
        session.finishedCells,
        action.board,
        session.puzzle.givens,
        correctUnitKeys,
      ),
      validatedUnitKeys: getCorrectCompletedSudokuUnitKeys(action.board, session.puzzle.solution),
      penalizedUnitKeys: getMismatchedCompletedSudokuUnitKeys(action.board, session.puzzle.solution),
    },
    changed: true,
    effects: [{
      type: 'validated-units',
      correctRowIndexes: groupedCorrect.rows,
      correctColIndexes: groupedCorrect.cols,
      correctBoxIndexes: groupedCorrect.boxes,
      incorrectRowIndexes: groupedIncorrect.rows,
      incorrectColIndexes: groupedIncorrect.cols,
      incorrectBoxIndexes: groupedIncorrect.boxes,
      newPenaltyCount,
    }],
  };
}
