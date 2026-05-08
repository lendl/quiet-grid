import type { PuzzleAnalysisPayload, PuzzleLossAnalysisSource } from '../../../../app/analysis/types';
import { cloneGrid, decodeMask, decodeSolution } from '../../core/puzzleData';
import { isBoardSolved } from '../../core/validation';
import type { TakuzuPlaySession } from '../../playContract';
import { getTakuzuNextMoveHint } from '../nextMove';
import type {
  TakuzuAnalysisPayload,
  TakuzuAnalysisStep,
  TakuzuLossAnalysisSource,
} from './types';

function isGrid(value: unknown): value is TakuzuPlaySession['board'] {
  return Array.isArray(value)
    && value.every((row) => Array.isArray(row));
}

function isTakuzuPlaySession(value: unknown): value is TakuzuPlaySession {
  if (!value || typeof value !== 'object') {
    return false;
  }
  const candidate = value as Partial<TakuzuPlaySession>;
  return Boolean(candidate.puzzle)
    && isGrid(candidate.board)
    && typeof candidate.puzzle?.solution === 'string'
    && typeof candidate.puzzle?.mask === 'string'
    && typeof candidate.puzzle?.size === 'number';
}

function isTakuzuLossAnalysisSource(source: PuzzleLossAnalysisSource | undefined): source is TakuzuLossAnalysisSource {
  if (!source || source.puzzleTypeId !== 'takuzu') {
    return false;
  }
  const payload = source.payload as Partial<TakuzuLossAnalysisSource['payload']> | undefined;
  return Boolean(payload?.puzzle)
    && isGrid(payload?.board)
    && typeof payload?.puzzle?.solution === 'string'
    && typeof payload?.puzzle?.mask === 'string'
    && typeof payload?.puzzle?.size === 'number';
}

function buildTakuzuAnalysisInternal(source: TakuzuLossAnalysisSource): TakuzuAnalysisPayload | null {
  const puzzle = source.payload.puzzle;
  const isGiven = decodeMask(puzzle.mask, puzzle.size);
  const solution = decodeSolution(puzzle.solution, puzzle.size);
  const workingBoard = cloneGrid(source.payload.board);
  const seenBoards = new Set<string>([JSON.stringify(workingBoard)]);
  const steps: TakuzuAnalysisStep[] = [];

  if (isBoardSolved(workingBoard, solution)) {
    return null;
  }

  while (!isBoardSolved(workingBoard, solution)) {
    const hint = getTakuzuNextMoveHint(workingBoard);
    if (hint.kind !== 'progress' || hint.targetCells.length === 0) {
      return null;
    }

    const beforeState = cloneGrid(workingBoard);
    let changed = false;

    for (const targetCell of hint.targetCells) {
      const currentRow = workingBoard[targetCell.row];
      if (!currentRow || typeof targetCell.value === 'undefined') {
        return null;
      }

      if (currentRow[targetCell.col] === targetCell.value) {
        continue;
      }

      currentRow[targetCell.col] = targetCell.value;
      changed = true;
    }

    if (!changed) {
      return null;
    }

    const afterState = cloneGrid(workingBoard);
    const boardKey = JSON.stringify(afterState);
    if (seenBoards.has(boardKey)) {
      return null;
    }

    seenBoards.add(boardKey);
    steps.push({
      key: `step-${steps.length + 1}`,
      title: hint.title,
      body: hint.body,
      ruleKey: hint.ruleKey,
      evidenceCells: hint.evidenceCells,
      targetCells: hint.targetCells,
      highlightRows: hint.highlightRows,
      highlightCols: hint.highlightCols,
      beforeState,
      afterState,
    });
  }

  return {
    puzzleTypeId: 'takuzu',
    steps,
    payload: {
      size: puzzle.size,
      isGiven,
    },
  };
}

export function buildTakuzuLossAnalysisSource(session: unknown): PuzzleLossAnalysisSource | null {
  if (!isTakuzuPlaySession(session)) {
    return null;
  }

  return {
    puzzleTypeId: 'takuzu',
    payload: {
      puzzle: session.puzzle,
      board: cloneGrid(session.board),
    },
  };
}

export function supportsTakuzuLossAnalysis(source: PuzzleLossAnalysisSource | undefined): boolean {
  return isTakuzuLossAnalysisSource(source) && buildTakuzuAnalysisInternal(source) !== null;
}

export function buildTakuzuAnalysis(source: PuzzleLossAnalysisSource): PuzzleAnalysisPayload {
  if (!isTakuzuLossAnalysisSource(source)) {
    throw new Error('Takuzu analysis source is invalid.');
  }

  const analysis = buildTakuzuAnalysisInternal(source);
  if (!analysis) {
    throw new Error('Takuzu analysis is unavailable for this loss state.');
  }

  return analysis;
}
