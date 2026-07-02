import {
  cloneWordSearchSession,
  type WordSearchCellRef,
  type WordSearchSelection,
  type WordSearchSession,
} from '../types';

export type WordSearchAction =
  | { type: 'begin-selection'; cell: WordSearchCellRef }
  | { type: 'update-selection'; cell: WordSearchCellRef }
  | { type: 'set-selection'; path: readonly WordSearchCellRef[] }
  | { type: 'commit-selection' }
  | { type: 'clear-selection' }
  | { type: 'toggle-hidden-word-mode' }
  | { type: 'clear-hidden-word-progress' }
  | { type: 'input-hidden-word-cell'; cell: WordSearchCellRef };

export interface WordSearchActionResult {
  changed: boolean;
  session: WordSearchSession;
}

function isInsideGrid(session: WordSearchSession, cell: WordSearchCellRef): boolean {
  return cell.row >= 0
    && cell.col >= 0
    && cell.row < session.puzzle.rows
    && cell.col < session.puzzle.cols;
}

function computeStepDelta(start: WordSearchCellRef, end: WordSearchCellRef): { row: number; col: number } | null {
  const rowDelta = end.row - start.row;
  const colDelta = end.col - start.col;

  const rowStep = Math.sign(rowDelta);
  const colStep = Math.sign(colDelta);

  if (rowDelta === 0 && colDelta === 0) {
    return { row: 0, col: 0 };
  }

  if (rowDelta === 0 || colDelta === 0 || Math.abs(rowDelta) === Math.abs(colDelta)) {
    return { row: rowStep, col: colStep };
  }

  return null;
}

function buildStraightPath(start: WordSearchCellRef, end: WordSearchCellRef): WordSearchCellRef[] | null {
  const step = computeStepDelta(start, end);
  if (!step) {
    return null;
  }

  const path: WordSearchCellRef[] = [];
  let row = start.row;
  let col = start.col;
  path.push({ row, col });

  while (row !== end.row || col !== end.col) {
    row += step.row;
    col += step.col;
    path.push({ row, col });
  }

  return path;
}

function cellsEqual(a: WordSearchCellRef, b: WordSearchCellRef): boolean {
  return a.row === b.row && a.col === b.col;
}

function getNextHiddenWordCell(session: WordSearchSession): WordSearchCellRef | null {
  if (session.hiddenWordSolved) {
    return null;
  }

  return session.puzzle.hiddenWord.positions[session.hiddenWordProgress.length] ?? null;
}

function pathMatchesWord(
  path: readonly WordSearchCellRef[],
  positions: readonly WordSearchCellRef[],
): boolean {
  if (path.length !== positions.length) {
    return false;
  }

  const forward = path.every((cell, index) => cellsEqual(cell, positions[index]));
  if (forward) {
    return true;
  }

  return path.every((cell, index) => cellsEqual(cell, positions[positions.length - 1 - index]));
}

function buildSelection(start: WordSearchCellRef, end: WordSearchCellRef): WordSearchSelection | null {
  const path = buildStraightPath(start, end);
  if (!path) {
    return null;
  }

  return { start: { ...start }, end: { ...end }, path };
}

export function runWordSearchAction(
  session: WordSearchSession,
  action: WordSearchAction,
): WordSearchActionResult {
  if (action.type === 'begin-selection') {
    if (!isInsideGrid(session, action.cell)) {
      return { changed: false, session };
    }

    const nextSelection = buildSelection(action.cell, action.cell);
    if (!nextSelection) {
      return { changed: false, session };
    }

    return {
      changed: true,
      session: {
        ...cloneWordSearchSession(session),
        tempSelection: nextSelection,
      },
    };
  }

  if (action.type === 'update-selection') {
    if (!session.tempSelection || !isInsideGrid(session, action.cell)) {
      return { changed: false, session };
    }

    const nextSelection = buildSelection(session.tempSelection.start, action.cell);
    if (!nextSelection) {
      return { changed: false, session };
    }

    const unchanged = session.tempSelection.path.length === nextSelection.path.length
      && session.tempSelection.path.every((cell, index) => cellsEqual(cell, nextSelection.path[index]));

    if (unchanged) {
      return { changed: false, session };
    }

    return {
      changed: true,
      session: {
        ...cloneWordSearchSession(session),
        tempSelection: nextSelection,
      },
    };
  }

  if (action.type === 'set-selection') {
    if (action.path.length < 2) {
      return { changed: false, session };
    }
    const start = action.path[0]!;
    const end = action.path[action.path.length - 1]!;
    return {
      changed: true,
      session: {
        ...cloneWordSearchSession(session),
        tempSelection: {
          start: { ...start },
          end: { ...end },
          path: action.path.map((c) => ({ ...c })),
        },
      },
    };
  }

  if (action.type === 'clear-selection') {
    if (!session.tempSelection) {
      return { changed: false, session };
    }

    return {
      changed: true,
      session: {
        ...cloneWordSearchSession(session),
        tempSelection: null,
      },
    };
  }

  if (action.type === 'toggle-hidden-word-mode') {
    if (session.hiddenWordSolved) {
      return { changed: false, session };
    }

    return {
      changed: true,
      session: {
        ...cloneWordSearchSession(session),
        hiddenWordMode: !session.hiddenWordMode,
        tempSelection: null,
      },
    };
  }

  if (action.type === 'clear-hidden-word-progress') {
    if (session.hiddenWordProgress.length === 0) {
      return { changed: false, session };
    }

    return {
      changed: true,
      session: {
        ...cloneWordSearchSession(session),
        hiddenWordProgress: [],
      },
    };
  }

  if (action.type === 'input-hidden-word-cell') {
    if (!session.hiddenWordMode || session.hiddenWordSolved || !isInsideGrid(session, action.cell)) {
      return { changed: false, session };
    }

    const expectedCell = getNextHiddenWordCell(session);
    if (!expectedCell) {
      return { changed: false, session };
    }

    if (!cellsEqual(expectedCell, action.cell)) {
      if (session.hiddenWordProgress.length === 0) {
        return { changed: false, session };
      }

      return {
        changed: true,
        session: {
          ...cloneWordSearchSession(session),
          hiddenWordProgress: [],
          tempSelection: null,
        },
      };
    }

    const hiddenWordProgress = [
      ...session.hiddenWordProgress.map((cell) => ({ ...cell })),
      { ...action.cell },
    ];
    const hiddenWordSolved = hiddenWordProgress.length >= session.puzzle.hiddenWord.positions.length;

    return {
      changed: true,
      session: {
        ...cloneWordSearchSession(session),
        hiddenWordProgress,
        hiddenWordSolved,
        hiddenWordMode: hiddenWordSolved ? false : session.hiddenWordMode,
        tempSelection: null,
      },
    };
  }

  const selection = session.tempSelection;
  if (!selection) {
    return { changed: false, session };
  }

  const pendingWord = session.puzzle.words.find((word) => (
    !session.foundWordIds.includes(word.id)
    && pathMatchesWord(selection.path, word.positions)
  ));

  if (!pendingWord) {
    return { changed: false, session };
  }

  const nextFoundWordIds = [...session.foundWordIds, pendingWord.id];
  const allWordsFound = nextFoundWordIds.length >= session.puzzle.words.length;

  return {
    changed: true,
    session: {
      ...cloneWordSearchSession(session),
      foundWordIds: nextFoundWordIds,
      tempSelection: null,
      hiddenWordMode: !session.hiddenWordSolved && allWordsFound,
    },
  };
}
