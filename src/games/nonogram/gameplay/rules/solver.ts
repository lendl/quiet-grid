import type {
  NonogramAxis,
  NonogramCellRef,
  NonogramCellState,
  NonogramLineRef,
  NonogramPuzzle,
} from '../../types';
import { puzzleMatchesClues } from './clues';
import { cellIndex, cellRefFromIndex } from './board';

export type NonogramDeductionKind = 'complete-line' | 'overlap-fill' | 'forced-empty';

export interface NonogramDeduction {
  kind: NonogramDeductionKind;
  line: NonogramLineRef;
  apply: 'filled' | 'marked';
  targetCells: NonogramCellRef[];
  evidenceCells: NonogramCellRef[];
}

export interface NonogramSolveStep extends NonogramDeduction {
  beforeCells: NonogramCellState[];
  afterCells: NonogramCellState[];
}

export interface NonogramSolveResult {
  solved: boolean;
  unique: boolean;
  contradiction: boolean;
  steps: NonogramSolveStep[];
  cells: NonogramCellState[];
}

type LineCellConstraint = 'unknown' | 'filled' | 'empty';

interface LineAnalysis {
  forcedFilled: number[];
  forcedEmpty: number[];
  placements: boolean[][];
}

function normalizeClues(clues: readonly number[]): number[] {
  return clues.length === 1 && clues[0] === 0 ? [] : [...clues];
}

function toConstraint(cell: NonogramCellState): LineCellConstraint {
  if (cell === 'filled') {
    return 'filled';
  }
  if (cell === 'marked') {
    return 'empty';
  }
  return 'unknown';
}

function getLineCells(
  cells: readonly NonogramCellState[],
  rows: number,
  cols: number,
  axis: NonogramAxis,
  index: number,
): NonogramCellState[] {
  if (axis === 'row') {
    const start = index * cols;
    return cells.slice(start, start + cols);
  }

  return Array.from({ length: rows }, (_, rowIndex) => cells[cellIndex(rowIndex, index, cols)] ?? 'empty');
}

function enumerateLinePlacements(
  length: number,
  clues: readonly number[],
  constraints: readonly LineCellConstraint[],
): boolean[][] {
  const normalized = normalizeClues(clues);
  if (normalized.length === 0) {
    const placement = Array.from({ length }, () => false);
    return placement.every((cell, index) => (
      constraints[index] === 'unknown'
      || (constraints[index] === 'filled' ? cell : !cell)
    )) ? [placement] : [];
  }

  const placements: boolean[][] = [];

  const minRemainingLength = (blockIndex: number): number => {
    let total = 0;
    for (let index = blockIndex; index < normalized.length; index += 1) {
      total += normalized[index] ?? 0;
    }
    total += normalized.length - blockIndex - 1;
    return total;
  };

  const recurse = (blockIndex: number, cursor: number, current: boolean[]) => {
    if (blockIndex >= normalized.length) {
      const candidate = [...current];
      for (let index = cursor; index < length; index += 1) {
        candidate[index] = false;
      }
      const isValid = candidate.every((cell, index) => {
        const constraint = constraints[index];
        if (constraint === 'filled') {
          return cell;
        }
        if (constraint === 'empty') {
          return !cell;
        }
        return true;
      });
      if (isValid) {
        placements.push(candidate);
      }
      return;
    }

    const blockLength = normalized[blockIndex] ?? 0;
    const remainingLength = minRemainingLength(blockIndex);
    const maxStart = length - remainingLength;

    for (let start = cursor; start <= maxStart; start += 1) {
      let blocked = false;
      const candidate = [...current];

      for (let index = cursor; index < start; index += 1) {
        candidate[index] = false;
        if (constraints[index] === 'filled') {
          blocked = true;
          break;
        }
      }
      if (blocked) {
        continue;
      }

      for (let offset = 0; offset < blockLength; offset += 1) {
        const cellIdx = start + offset;
        candidate[cellIdx] = true;
        if (constraints[cellIdx] === 'empty') {
          blocked = true;
          break;
        }
      }
      if (blocked) {
        continue;
      }

      const nextCursor = start + blockLength;
      if (blockIndex < normalized.length - 1) {
        if (nextCursor >= length) {
          continue;
        }
        candidate[nextCursor] = false;
        if (constraints[nextCursor] === 'filled') {
          continue;
        }
        recurse(blockIndex + 1, nextCursor + 1, candidate);
      } else {
        recurse(blockIndex + 1, nextCursor, candidate);
      }
    }
  };

  recurse(0, 0, Array.from({ length }, () => false));
  return placements;
}

function analyzeLine(
  cells: readonly NonogramCellState[],
  clues: readonly number[],
): LineAnalysis {
  const constraints = cells.map(toConstraint);
  const placements = enumerateLinePlacements(cells.length, clues, constraints);
  if (placements.length === 0) {
    return {
      forcedFilled: [],
      forcedEmpty: [],
      placements,
    };
  }

  const forcedFilled: number[] = [];
  const forcedEmpty: number[] = [];

  for (let index = 0; index < cells.length; index += 1) {
    const alwaysFilled = placements.every((placement) => placement[index] === true);
    const alwaysEmpty = placements.every((placement) => placement[index] === false);
    if (alwaysFilled && cells[index] !== 'filled') {
      forcedFilled.push(index);
    } else if (alwaysEmpty && cells[index] !== 'marked') {
      forcedEmpty.push(index);
    }
  }

  return { forcedFilled, forcedEmpty, placements };
}

function buildLineRef(axis: NonogramAxis, index: number, clues: readonly number[]): NonogramLineRef {
  return {
    axis,
    index,
    clues: [...clues],
  };
}

function projectLineCells(
  axis: NonogramAxis,
  lineIndex: number,
  cellIndexes: readonly number[],
): NonogramCellRef[] {
  return cellIndexes.map((cellIndexInLine) => (
    axis === 'row'
      ? { row: lineIndex, col: cellIndexInLine }
      : { row: cellIndexInLine, col: lineIndex }
  ));
}

function buildDeduction(
  axis: NonogramAxis,
  lineIndex: number,
  clues: readonly number[],
  analysis: LineAnalysis,
): NonogramDeduction | null {
  if (analysis.placements.length === 0) {
    return null;
  }

  if (analysis.forcedFilled.length > 0) {
    return {
      kind: analysis.placements.length === 1 ? 'complete-line' : 'overlap-fill',
      line: buildLineRef(axis, lineIndex, clues),
      apply: 'filled',
      targetCells: projectLineCells(axis, lineIndex, analysis.forcedFilled),
      evidenceCells: projectLineCells(axis, lineIndex, analysis.forcedFilled),
    };
  }

  if (analysis.forcedEmpty.length > 0) {
    return {
      kind: analysis.placements.length === 1 ? 'complete-line' : 'forced-empty',
      line: buildLineRef(axis, lineIndex, clues),
      apply: 'marked',
      targetCells: projectLineCells(axis, lineIndex, analysis.forcedEmpty),
      evidenceCells: projectLineCells(axis, lineIndex, analysis.forcedEmpty),
    };
  }

  return null;
}

export function findNextNonogramDeduction(
  puzzle: Pick<NonogramPuzzle, 'rows' | 'cols' | 'rowClues' | 'colClues'>,
  cells: readonly NonogramCellState[],
): NonogramDeduction | null {
  for (let rowIndex = 0; rowIndex < puzzle.rows; rowIndex += 1) {
    const deduction = buildDeduction(
      'row',
      rowIndex,
      puzzle.rowClues[rowIndex] ?? [0],
      analyzeLine(getLineCells(cells, puzzle.rows, puzzle.cols, 'row', rowIndex), puzzle.rowClues[rowIndex] ?? [0]),
    );
    if (deduction) {
      return deduction;
    }
  }

  for (let colIndex = 0; colIndex < puzzle.cols; colIndex += 1) {
    const deduction = buildDeduction(
      'col',
      colIndex,
      puzzle.colClues[colIndex] ?? [0],
      analyzeLine(getLineCells(cells, puzzle.rows, puzzle.cols, 'col', colIndex), puzzle.colClues[colIndex] ?? [0]),
    );
    if (deduction) {
      return deduction;
    }
  }

  return null;
}

export function applyDeduction(
  cells: readonly NonogramCellState[],
  cols: number,
  deduction: NonogramDeduction,
): NonogramCellState[] {
  const next = [...cells];
  deduction.targetCells.forEach(({ row, col }) => {
    const index = cellIndex(row, col, cols);
    next[index] = deduction.apply;
  });
  return next;
}

function hasContradiction(
  puzzle: Pick<NonogramPuzzle, 'rows' | 'cols' | 'rowClues' | 'colClues'>,
  cells: readonly NonogramCellState[],
): boolean {
  for (let rowIndex = 0; rowIndex < puzzle.rows; rowIndex += 1) {
    const line = getLineCells(cells, puzzle.rows, puzzle.cols, 'row', rowIndex);
    if (analyzeLine(line, puzzle.rowClues[rowIndex] ?? [0]).placements.length === 0) {
      return true;
    }
  }

  for (let colIndex = 0; colIndex < puzzle.cols; colIndex += 1) {
    const line = getLineCells(cells, puzzle.rows, puzzle.cols, 'col', colIndex);
    if (analyzeLine(line, puzzle.colClues[colIndex] ?? [0]).placements.length === 0) {
      return true;
    }
  }

  return false;
}

function isCompletedBoard(cells: readonly NonogramCellState[]): boolean {
  return cells.every((cell) => cell !== 'empty');
}

function toFilledGrid(
  cells: readonly NonogramCellState[],
  rows: number,
  cols: number,
): boolean[][] {
  return Array.from({ length: rows }, (_, rowIndex) => (
    Array.from({ length: cols }, (_, colIndex) => cells[cellIndex(rowIndex, colIndex, cols)] === 'filled')
  ));
}

function countSolutionsInternal(
  puzzle: NonogramPuzzle,
  cells: NonogramCellState[],
  limit: number,
): number {
  let current = [...cells];
  while (true) {
    if (hasContradiction(puzzle, current)) {
      return 0;
    }

    const deduction = findNextNonogramDeduction(puzzle, current);
    if (!deduction) {
      break;
    }

    current = applyDeduction(current, puzzle.cols, deduction);
  }

  if (isCompletedBoard(current)) {
    return puzzleMatchesClues(toFilledGrid(current, puzzle.rows, puzzle.cols), puzzle) ? 1 : 0;
  }

  const branchIndex = current.findIndex((cell) => cell === 'empty');
  if (branchIndex < 0) {
    return 0;
  }

  let total = 0;
  for (const branchState of ['filled', 'marked'] as const) {
    const next = [...current];
    next[branchIndex] = branchState;
    total += countSolutionsInternal(puzzle, next, limit - total);
    if (total >= limit) {
      return total;
    }
  }

  return total;
}

export function solveNonogramFromState(
  puzzle: NonogramPuzzle,
  cells: readonly NonogramCellState[],
): NonogramSolveResult {
  let current = [...cells];
  const steps: NonogramSolveStep[] = [];

  while (true) {
    if (hasContradiction(puzzle, current)) {
      return {
        solved: false,
        unique: false,
        contradiction: true,
        steps,
        cells: current,
      };
    }

    const deduction = findNextNonogramDeduction(puzzle, current);
    if (!deduction) {
      break;
    }

    const beforeCells = [...current];
    current = applyDeduction(current, puzzle.cols, deduction);
    steps.push({
      ...deduction,
      beforeCells,
      afterCells: [...current],
    });
  }

  const solutionCount = countSolutionsInternal(puzzle, current, 2);
  const solved = isCompletedBoard(current) && solutionCount === 1;

  return {
    solved,
    unique: solutionCount === 1,
    contradiction: false,
    steps,
    cells: current,
  };
}

export function countNonogramSolutions(
  puzzle: NonogramPuzzle,
  cells?: readonly NonogramCellState[],
  limit = 2,
): number {
  return countSolutionsInternal(
    puzzle,
    cells ? [...cells] : Array.from({ length: puzzle.rows * puzzle.cols }, () => 'empty'),
    limit,
  );
}

export function getLineConstraintCells(
  puzzle: Pick<NonogramPuzzle, 'rows' | 'cols'>,
  cells: readonly NonogramCellState[],
  line: NonogramLineRef,
): NonogramCellState[] {
  return getLineCells(cells, puzzle.rows, puzzle.cols, line.axis, line.index);
}

export function deductionCellsToIndexes(
  deduction: NonogramDeduction,
  cols: number,
): number[] {
  return deduction.targetCells.map(({ row, col }) => cellIndex(row, col, cols));
}

export function getCellRefsFromIndexes(indexes: readonly number[], cols: number): NonogramCellRef[] {
  return indexes.map((index) => cellRefFromIndex(index, cols));
}
