import type { PuzzleAnalysisPayload, PuzzleAnalysisSource } from '../../../../app/analysis/types';
import {
  cloneBooleanMatrix,
  cloneNonogramBoard,
  isNonogramSolved,
  type NonogramBoard,
  type NonogramPuzzle,
  type NonogramSession,
} from '../../types';
import {
  getNonogramNextMoveHint,
  type NonogramNextMoveHint,
} from './nextMove';
import type { NonogramCellValue } from '../../types';

function isBooleanGrid(value: unknown, rows: number, cols: number): boolean {
  return Array.isArray(value)
    && value.length === rows
    && value.every((row) => Array.isArray(row) && row.length === cols && row.every((cell) => typeof cell === 'boolean'));
}

function isDifficulty(value: unknown): value is NonogramPuzzle['difficulty'] {
  return value === 'easy' || value === 'medium' || value === 'hard' || value === 'expert';
}

function isNonogramCellValue(value: unknown): value is NonogramCellValue {
  return value === 0 || value === 1 || value === null;
}

function isTriStateBoard(value: unknown, rows: number, cols: number): value is NonogramBoard {
  return Array.isArray(value)
    && value.length === rows
    && value.every((row) => Array.isArray(row)
      && row.length === cols
      && row.every((cell) => isNonogramCellValue(cell)));
}

function isNonogramPuzzle(value: unknown): value is NonogramPuzzle {
  if (!value || typeof value !== 'object') {
    return false;
  }

  const puzzle = value as Record<string, unknown>;
  const isClueLine = (clues: unknown, lineLength: number): boolean => Array.isArray(clues)
    && clues.length === lineLength
    && clues.every((clue) => Array.isArray(clue)
      && clue.every((value) => Number.isInteger(value) && (value as number) > 0));

  return typeof puzzle.id === 'string'
    && isDifficulty(puzzle.difficulty)
    && typeof puzzle.rows === 'number'
    && Number.isInteger(puzzle.rows)
    && puzzle.rows > 0
    && typeof puzzle.cols === 'number'
    && Number.isInteger(puzzle.cols)
    && puzzle.cols > 0
    && isClueLine(puzzle.rowClues, puzzle.rows)
    && isClueLine(puzzle.colClues, puzzle.cols);
}

function isNonogramPlaySession(value: unknown): value is NonogramSession {
  if (!value || typeof value !== 'object') {
    return false;
  }

  const session = value as Partial<NonogramSession>;
  if (!session.puzzle || !isNonogramPuzzle(session.puzzle)) {
    return false;
  }

  return isTriStateBoard(session.board, session.puzzle.rows, session.puzzle.cols)
    && isBooleanGrid(session.solution, session.puzzle.rows, session.puzzle.cols);
}

interface NonogramAnalysisSource extends PuzzleAnalysisSource {
  payload: NonogramSession;
}

function isNonogramAnalysisSource(source: PuzzleAnalysisSource | undefined): source is NonogramAnalysisSource {
  if (!source || source.gameId !== 'nonogram') {
    return false;
  }

  const payload = source.payload as Partial<NonogramSession> | undefined;
  if (!payload?.puzzle || !isNonogramPuzzle(payload.puzzle)) {
    return false;
  }

  return isTriStateBoard(payload.board, payload.puzzle.rows, payload.puzzle.cols)
    && isBooleanGrid(payload.solution, payload.puzzle.rows, payload.puzzle.cols);
}

function applyHintToBoard(board: NonogramBoard, hint: NonogramNextMoveHint): boolean {
  let changed = false;

  hint.targetCells.forEach(({ row, col, value }) => {
    const rowCells = board[row];
    if (!rowCells || typeof rowCells[col] === 'undefined' || rowCells[col] === value) {
      return;
    }

    rowCells[col] = value;
    changed = true;
  });

  return changed;
}

function buildAnalysisStep(
  hint: NonogramNextMoveHint,
  beforeState: NonogramBoard,
  afterState: NonogramBoard,
  stepIndex: number,
) {
  return {
    key: `step-${stepIndex}`,
    title: hint.title,
    body: hint.body,
    ruleKey: hint.ruleKey === 'invalid-board' ? undefined : hint.ruleKey,
    evidenceCells: hint.evidenceCells,
    targetCells: hint.targetCells,
    highlightRows: hint.lineOrientation === 'row' ? [hint.lineIndex] : [],
    highlightCols: hint.lineOrientation === 'col' ? [hint.lineIndex] : [],
    beforeState,
    afterState,
  };
}

function buildNonogramAnalysisInternal(source: PuzzleAnalysisSource): PuzzleAnalysisPayload | null {
  if (!isNonogramAnalysisSource(source)) {
    return null;
  }

  const puzzle = source.payload.puzzle;
  const solution = source.payload.solution;
  const workingBoard = cloneNonogramBoard(source.payload.board);
  const steps: ReturnType<typeof buildAnalysisStep>[] = [];
  const seenBoards = new Set<string>();

  while (true) {
    const hint = getNonogramNextMoveHint(puzzle, workingBoard);
    if (!hint) {
      break;
    }

    const beforeState = cloneNonogramBoard(workingBoard);
    const boardKey = JSON.stringify(beforeState);
    if (seenBoards.has(boardKey)) {
      break;
    }
    seenBoards.add(boardKey);

    const changed = hint.kind === 'invalid-board' ? false : applyHintToBoard(workingBoard, hint);
    const afterState = cloneNonogramBoard(workingBoard);
    steps.push(buildAnalysisStep(hint, beforeState, afterState, steps.length + 1));

    if (hint.kind === 'invalid-board' || !changed || isNonogramSolved(workingBoard, solution)) {
      break;
    }
  }

  if (steps.length === 0) {
    return null;
  }

  return {
    gameId: 'nonogram',
    steps,
    payload: {
      puzzle,
    },
  };
}

export function buildNonogramAnalysisSource(session: unknown): PuzzleAnalysisSource | null {
  if (!isNonogramPlaySession(session)) {
    return null;
  }

  return {
    gameId: 'nonogram',
    payload: {
      puzzle: session.puzzle,
      board: cloneNonogramBoard(session.board),
      solution: cloneBooleanMatrix(session.solution),
    },
  };
}

export function supportsNonogramAnalysis(source: PuzzleAnalysisSource | undefined): boolean {
  return isNonogramAnalysisSource(source)
    && buildNonogramAnalysisInternal(source) !== null;
}

export function buildNonogramAnalysis(source: PuzzleAnalysisSource): PuzzleAnalysisPayload {
  const analysis = buildNonogramAnalysisInternal(source);
  if (!analysis) {
    throw new Error('Nonogram analysis is unavailable for this puzzle state.');
  }

  return analysis;
}
