import type { PuzzleAnalysisPayload, PuzzleLossAnalysisSource } from '../../../../../app/analysis/types';
import { getNonogramAnalysisContent } from '../../../content/i18n';
import type { NonogramPlaySession } from '../../../gameplay/activePuzzle';
import { describeNonogramDeduction } from '../../../gameplay/analysis/content';
import { findNextNonogramDeduction, applyDeduction } from '../../../gameplay/rules/solver';
import type {
  NonogramAnalysisPayload,
  NonogramAnalysisStep,
  NonogramLossAnalysisSource,
} from './types';

function isNonogramPlaySession(value: unknown): value is NonogramPlaySession {
  if (!value || typeof value !== 'object') {
    return false;
  }

  const candidate = value as Partial<NonogramPlaySession>;
  return Boolean(candidate.puzzle)
    && Array.isArray(candidate.cells)
    && typeof candidate.puzzle?.rows === 'number'
    && typeof candidate.puzzle?.cols === 'number'
    && Array.isArray(candidate.puzzle?.rowClues)
    && Array.isArray(candidate.puzzle?.colClues);
}

function isNonogramLossAnalysisSource(
  source: PuzzleLossAnalysisSource | undefined,
): source is NonogramLossAnalysisSource {
  if (!source || source.puzzleTypeId !== 'nonogram') {
    return false;
  }

  const payload = source.payload as Partial<NonogramLossAnalysisSource['payload']> | undefined;
  return Boolean(payload?.puzzle) && Array.isArray(payload?.cells);
}

function buildNonogramAnalysisInternal(source: NonogramLossAnalysisSource): NonogramAnalysisPayload | null {
  const content = getNonogramAnalysisContent();
  let currentCells = [...source.payload.cells];
  const steps: NonogramAnalysisStep[] = [];

  while (true) {
    const deduction = findNextNonogramDeduction(source.payload.puzzle, currentCells);
    if (!deduction) {
      break;
    }

    const beforeState = [...currentCells];
    const afterState = applyDeduction(currentCells, source.payload.puzzle.cols, deduction);
    const copy = describeNonogramDeduction(deduction);

    steps.push({
      key: `step-${steps.length + 1}`,
      title: copy.title,
      body: copy.body,
      evidenceCells: deduction.evidenceCells,
      targetCells: deduction.targetCells,
      highlightRows: deduction.line.axis === 'row' ? [deduction.line.index] : [],
      highlightCols: deduction.line.axis === 'col' ? [deduction.line.index] : [],
      beforeState,
      afterState,
    });

    currentCells = afterState;
  }

  if (steps.length === 0) {
    return null;
  }

  return {
    puzzleTypeId: 'nonogram',
    steps,
    payload: {
      puzzle: source.payload.puzzle,
      labels: {
        evidence: content.legendEvidence,
        target: content.legendTarget,
      },
    },
  };
}

export function buildNonogramLossAnalysisSource(session: unknown): PuzzleLossAnalysisSource | null {
  if (!isNonogramPlaySession(session)) {
    return null;
  }

  return {
    puzzleTypeId: 'nonogram',
    payload: {
      puzzle: session.puzzle,
      cells: [...session.cells],
    },
  };
}

export function supportsNonogramLossAnalysis(source: PuzzleLossAnalysisSource | undefined): boolean {
  if (!isNonogramLossAnalysisSource(source)) {
    return false;
  }

  return findNextNonogramDeduction(source.payload.puzzle, source.payload.cells) !== null;
}

export function buildNonogramAnalysis(source: PuzzleLossAnalysisSource): PuzzleAnalysisPayload {
  if (!isNonogramLossAnalysisSource(source)) {
    throw new Error('Nonogram analysis source is invalid.');
  }

  const analysis = buildNonogramAnalysisInternal(source);
  if (!analysis) {
    throw new Error('Nonogram analysis is unavailable for this loss state.');
  }

  return analysis;
}
