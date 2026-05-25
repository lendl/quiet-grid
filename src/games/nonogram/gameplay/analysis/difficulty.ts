import {
  createEmptyNonogramBoard,
  isNonogramSolved,
  type NonogramCellRef,
  type NonogramLineOrientation,
  type NonogramPuzzle,
} from '../../types';
import { analyzeNonogramBoard, lineCellsToRefs } from '../rules/solver';
import { buildNonogramPuzzle } from '../../platform/puzzleData';
import type { NonogramCatalogEntry } from '../../platform/codecs/codec';

export interface NonogramDifficultyMetrics {
  steps: number;
  filledCells: number;
  clueSegments: number;
}

function countClueSegments(clues: readonly (readonly number[])[]): number {
  return clues.reduce((sum, clueLine) => sum + clueLine.filter((value) => value > 0).length, 0);
}

interface CanonicalStep {
  kind: 'overlap-fill' | 'forced-empty' | 'complete-line';
  targetCells: Array<NonogramCellRef & { value: 0 | 1 }>;
}

function buildTargets(
  orientation: NonogramLineOrientation,
  index: number,
  cellIndexes: readonly number[],
  value: 0 | 1,
): Array<NonogramCellRef & { value: 0 | 1 }> {
  return lineCellsToRefs(orientation, index, cellIndexes).map((cell) => ({
    ...cell,
    value,
  }));
}

function getCanonicalStep(
  puzzle: NonogramPuzzle,
  board: ReturnType<typeof createEmptyNonogramBoard>,
): CanonicalStep | null {
  const analysis = analyzeNonogramBoard(puzzle, board);
  if (analysis.invalidLine) {
    return null;
  }

  for (const line of [...analysis.rows, ...analysis.cols]) {
    if (!line.analysis) {
      return null;
    }

    if (line.analysis.overlapFillCells.length > 0) {
      return {
        kind: 'overlap-fill',
        targetCells: buildTargets(line.orientation, line.index, line.analysis.overlapFillCells, 1),
      };
    }

    if (line.analysis.isComplete && line.analysis.forcedEmptyCells.length > 0) {
      return {
        kind: 'complete-line',
        targetCells: buildTargets(line.orientation, line.index, line.analysis.forcedEmptyCells, 0),
      };
    }

    if (line.analysis.forcedEmptyCells.length > 0) {
      return {
        kind: 'forced-empty',
        targetCells: buildTargets(line.orientation, line.index, line.analysis.forcedEmptyCells, 0),
      };
    }
  }

  return null;
}

function applyStep(
  board: ReturnType<typeof createEmptyNonogramBoard>,
  step: CanonicalStep,
): void {
  step.targetCells.forEach(({ row, col, value }) => {
    board[row][col] = value;
  });
}

export function analyzeNonogramDifficulty(
  puzzle: NonogramPuzzle,
  solution: boolean[][],
): NonogramDifficultyMetrics | null {
  const board = createEmptyNonogramBoard(puzzle.rows, puzzle.cols);
  let steps = 0;
  const safetyLimit = Math.max(8, puzzle.rows * puzzle.cols * 2);

  while (!isNonogramSolved(board, solution) && steps < safetyLimit) {
    const step = getCanonicalStep(puzzle, board);
    if (!step) {
      return null;
    }

    applyStep(board, step);
    steps += 1;
  }

  if (!isNonogramSolved(board, solution)) {
    return null;
  }

  return {
    steps,
    filledCells: solution.flat().filter(Boolean).length,
    clueSegments: countClueSegments(puzzle.rowClues) + countClueSegments(puzzle.colClues),
  };
}

export function classifyNonogramDifficulty(
  rows: number,
  cols: number,
  metrics: NonogramDifficultyMetrics,
): NonogramCatalogEntry['difficulty'] {
  const score = metrics.steps;
  const shortSide = Math.min(rows, cols);
  const longSide = Math.max(rows, cols);

  if (shortSide <= 5 && longSide <= 5) {
    if (score <= 3) return 'easy';
    if (score <= 5) return 'medium';
    if (score <= 7) return 'hard';
    return 'expert';
  }

  if (shortSide <= 5 && longSide <= 10) {
    if (score <= 4) return 'easy';
    if (score <= 7) return 'medium';
    if (score <= 10) return 'hard';
    return 'expert';
  }

  if (shortSide <= 10 && longSide <= 10) {
    if (score <= 6) return 'medium';
    if (score <= 9) return 'hard';
    return 'expert';
  }

  if (shortSide <= 10 && longSide <= 15) {
    if (score <= 9) return 'medium';
    if (score <= 13) return 'hard';
    return 'expert';
  }

  if (score <= 18) return 'hard';
  return 'expert';
}

export function classifyNonogramEntry(
  entry: NonogramCatalogEntry,
): { difficulty: NonogramCatalogEntry['difficulty']; metrics: NonogramDifficultyMetrics } | null {
  const puzzle = buildNonogramPuzzle(entry);
  const metrics = analyzeNonogramDifficulty(puzzle, entry.solution);
  if (!metrics) {
    return null;
  }

  return {
    difficulty: classifyNonogramDifficulty(entry.rows, entry.cols, metrics),
    metrics,
  };
}
