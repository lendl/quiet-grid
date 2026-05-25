import {
  analyzeNonogramBoard,
  lineCellsToRefs,
  type NonogramLineCheck,
} from '../rules/solver';
import type {
  NonogramBoard,
  NonogramCanonicalMoveKey,
  NonogramCellRef,
  NonogramDirectState,
  NonogramLineOrientation,
  NonogramPuzzle,
} from '../../types';
import {
  buildCompleteLineCopy,
  buildForcedEmptyCopy,
  buildInvalidBoardCopy,
  buildOverlapFillCopy,
} from './content';

export interface NonogramNextMoveTarget extends NonogramCellRef {
  value: NonogramDirectState;
}

export interface NonogramNextMoveHint {
  kind: 'invalid-board' | NonogramCanonicalMoveKey | 'forced-empty';
  ruleKey: 'invalid-board' | NonogramCanonicalMoveKey | 'forced-empty';
  title: string;
  body: string;
  evidenceCells: NonogramCellRef[];
  targetCells: NonogramNextMoveTarget[];
  lineOrientation: NonogramLineOrientation;
  lineIndex: number;
}

function buildTargetCells(
  orientation: NonogramLineOrientation,
  index: number,
  targetIndexes: readonly number[],
  value: NonogramDirectState,
): NonogramNextMoveTarget[] {
  return lineCellsToRefs(orientation, index, targetIndexes).map((cell) => ({
    ...cell,
    value,
  }));
}

function toEvidenceCells(
  orientation: NonogramLineOrientation,
  index: number,
  cells: readonly number[],
): NonogramCellRef[] {
  return lineCellsToRefs(orientation, index, cells);
}

function buildHintFromLine(
  line: NonogramLineCheck,
): NonogramNextMoveHint | null {
  if (!line.analysis) {
    return null;
  }

  if (line.analysis.overlapFillCells.length > 0) {
    const copy = buildOverlapFillCopy(line.orientation, line.index, line.analysis.overlapFillCells.length);
    return {
      kind: 'overlap-fill',
      ruleKey: 'overlap-fill',
      title: copy.title,
      body: copy.body,
      evidenceCells: toEvidenceCells(line.orientation, line.index, line.cells.map((_, cellIndex) => cellIndex)),
      targetCells: buildTargetCells(line.orientation, line.index, line.analysis.overlapFillCells, 1),
      lineOrientation: line.orientation,
      lineIndex: line.index,
    };
  }

  if (line.analysis.isComplete && line.analysis.forcedEmptyCells.length > 0) {
    const copy = buildCompleteLineCopy(line.orientation, line.index, line.analysis.forcedEmptyCells.length);
    return {
      kind: 'complete-line',
      ruleKey: 'complete-line',
      title: copy.title,
      body: copy.body,
      evidenceCells: toEvidenceCells(line.orientation, line.index, line.cells.map((_, cellIndex) => cellIndex)),
      targetCells: buildTargetCells(line.orientation, line.index, line.analysis.forcedEmptyCells, 0),
      lineOrientation: line.orientation,
      lineIndex: line.index,
    };
  }

  if (line.analysis.forcedEmptyCells.length > 0) {
    const copy = buildForcedEmptyCopy(line.orientation, line.index, line.analysis.forcedEmptyCells.length);
    return {
      kind: 'forced-empty',
      ruleKey: 'forced-empty',
      title: copy.title,
      body: copy.body,
      evidenceCells: toEvidenceCells(line.orientation, line.index, line.cells.map((_, cellIndex) => cellIndex)),
      targetCells: buildTargetCells(line.orientation, line.index, line.analysis.forcedEmptyCells, 0),
      lineOrientation: line.orientation,
      lineIndex: line.index,
    };
  }

  return null;
}

export function getNonogramNextMoveHint(
  puzzle: NonogramPuzzle,
  board: NonogramBoard,
): NonogramNextMoveHint | null {
  const analysis = analyzeNonogramBoard(puzzle, board);
  if (analysis.invalidLine) {
    const copy = buildInvalidBoardCopy(analysis.invalidLine.orientation, analysis.invalidLine.index);
    return {
      kind: 'invalid-board',
      ruleKey: 'invalid-board',
      title: copy.title,
      body: copy.body,
      evidenceCells: toEvidenceCells(
        analysis.invalidLine.orientation,
        analysis.invalidLine.index,
        analysis.invalidLine.cells.map((_, cellIndex) => cellIndex),
      ),
      targetCells: [],
      lineOrientation: analysis.invalidLine.orientation,
      lineIndex: analysis.invalidLine.index,
    };
  }

  for (const line of [...analysis.rows, ...analysis.cols]) {
    const hint = buildHintFromLine(line);
    if (hint) {
      return hint;
    }
  }

  return null;
}
