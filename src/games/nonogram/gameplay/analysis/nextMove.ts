import type { NonogramPlaySession } from '../activePuzzle';
import type { NonogramCellRef, NonogramExplanationCopy } from '../../types';
import {
  findNextNonogramDeduction,
  type NonogramDeduction,
} from '../rules/solver';
import { describeNonogramDeduction } from './content';

export interface NonogramNextMoveHint extends NonogramExplanationCopy {
  deduction: NonogramDeduction;
  evidenceCells: NonogramCellRef[];
  targetCells: NonogramCellRef[];
  highlightRows: number[];
  highlightCols: number[];
}

function toHighlightRows(deduction: NonogramDeduction): number[] {
  return deduction.line.axis === 'row' ? [deduction.line.index] : deduction.targetCells.map(({ row }) => row);
}

function toHighlightCols(deduction: NonogramDeduction): number[] {
  return deduction.line.axis === 'col' ? [deduction.line.index] : deduction.targetCells.map(({ col }) => col);
}

export function getNonogramNextMoveHint(session: NonogramPlaySession): NonogramNextMoveHint | null {
  const deduction = findNextNonogramDeduction(session.puzzle, session.cells);
  if (!deduction) {
    return null;
  }

  const copy = describeNonogramDeduction(deduction);
  return {
    ...copy,
    deduction,
    evidenceCells: deduction.evidenceCells,
    targetCells: deduction.targetCells,
    highlightRows: [...new Set(toHighlightRows(deduction))],
    highlightCols: [...new Set(toHighlightCols(deduction))],
  };
}
