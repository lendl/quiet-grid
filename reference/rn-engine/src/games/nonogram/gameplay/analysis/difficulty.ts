import {
  createEmptyNonogramBoard,
  isNonogramSolved,
  type NonogramCellRef,
  type NonogramLineOrientation,
  type NonogramPuzzle,
} from '../../types';
import { analyzeNonogramBoard, type NonogramBoardAnalysis, lineCellsToRefs } from '../rules/solver';
import { buildNonogramPuzzle } from '../../platform/puzzleData';
import type { NonogramCatalogEntry } from '../../platform/codecs/codec';

export interface NonogramDifficultyMetrics {
  steps: number;
  filledCells: number;
  clueSegments: number;
  /** Average number of valid placements that existed when each deduction was made. Higher = harder to spot. */
  avgPlacementsAtDeduction: number;
  /** Maximum number of valid placements for any single deduction. Captures the hardest individual step. */
  maxPlacementsAtDeduction: number;
  /** Count of overlap-fill steps that revealed only 1 cell. Each requires very precise incremental reasoning. */
  singleCellStepCount: number;
  /** Count of steps that unlocked at least one new deduction on a line of the opposite orientation. */
  crossAxisUnlocks: number;
}

function countClueSegments(clues: readonly (readonly number[])[]): number {
  return clues.reduce((sum, clueLine) => sum + clueLine.filter((value) => value > 0).length, 0);
}

interface CanonicalStep {
  kind: 'overlap-fill' | 'forced-empty' | 'complete-line';
  targetCells: Array<NonogramCellRef & { value: 0 | 1 }>;
  placementCount: number;
  orientation: NonogramLineOrientation;
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

function getCanonicalStepFromAnalysis(
  analysis: NonogramBoardAnalysis,
): CanonicalStep | null {
  if (analysis.invalidLine) {
    return null;
  }

  for (const line of [...analysis.rows, ...analysis.cols]) {
    if (!line.analysis) {
      return null;
    }

    const placementCount = line.analysis.placements.length;
    const { orientation } = line;

    if (line.analysis.overlapFillCells.length > 0) {
      return {
        kind: 'overlap-fill',
        targetCells: buildTargets(orientation, line.index, line.analysis.overlapFillCells, 1),
        placementCount,
        orientation,
      };
    }

    if (line.analysis.isComplete && line.analysis.forcedEmptyCells.length > 0) {
      return {
        kind: 'complete-line',
        targetCells: buildTargets(orientation, line.index, line.analysis.forcedEmptyCells, 0),
        placementCount,
        orientation,
      };
    }

    if (line.analysis.forcedEmptyCells.length > 0) {
      return {
        kind: 'forced-empty',
        targetCells: buildTargets(orientation, line.index, line.analysis.forcedEmptyCells, 0),
        placementCount,
        orientation,
      };
    }
  }

  return null;
}

function isLineActionable(lineAnalysis: NonogramBoardAnalysis['rows'][number]['analysis']): boolean {
  if (!lineAnalysis) {
    return false;
  }
  return lineAnalysis.overlapFillCells.length > 0 || lineAnalysis.forcedEmptyCells.length > 0;
}

function hasCrossAxisUnlock(
  before: NonogramBoardAnalysis,
  after: NonogramBoardAnalysis,
  stepOrientation: NonogramLineOrientation,
): boolean {
  const beforeOpposite = stepOrientation === 'row' ? before.cols : before.rows;
  const afterOpposite = stepOrientation === 'row' ? after.cols : after.rows;

  return afterOpposite.some((line, i) => !isLineActionable(beforeOpposite[i]?.analysis ?? null) && isLineActionable(line.analysis));
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
  let totalPlacements = 0;
  let maxPlacementsAtDeduction = 0;
  let singleCellStepCount = 0;
  let crossAxisUnlocks = 0;
  const safetyLimit = Math.max(8, puzzle.rows * puzzle.cols * 2);

  while (!isNonogramSolved(board, solution) && steps < safetyLimit) {
    const before = analyzeNonogramBoard(puzzle, board);
    const step = getCanonicalStepFromAnalysis(before);
    if (!step) {
      return null;
    }

    totalPlacements += step.placementCount;
    maxPlacementsAtDeduction = Math.max(maxPlacementsAtDeduction, step.placementCount);

    if (step.kind === 'overlap-fill' && step.targetCells.length <= 1) {
      singleCellStepCount += 1;
    }

    applyStep(board, step);
    steps += 1;

    const after = analyzeNonogramBoard(puzzle, board);
    if (hasCrossAxisUnlock(before, after, step.orientation)) {
      crossAxisUnlocks += 1;
    }
  }

  if (!isNonogramSolved(board, solution)) {
    return null;
  }

  return {
    steps,
    filledCells: solution.flat().filter(Boolean).length,
    clueSegments: countClueSegments(puzzle.rowClues) + countClueSegments(puzzle.colClues),
    avgPlacementsAtDeduction: steps > 0 ? totalPlacements / steps : 0,
    maxPlacementsAtDeduction,
    singleCellStepCount,
    crossAxisUnlocks,
  };
}

export function computeNonogramScore(metrics: NonogramDifficultyMetrics): number {
  // +1 per 5 placements in the hardest single deduction (captures the peak mental effort)
  const placementBonus = Math.floor(metrics.maxPlacementsAtDeduction / 5);
  // each 1-cell overlap-fill step adds +1 (most incremental possible reasoning)
  const incrementalBonus = metrics.singleCellStepCount;
  // +1 per 4 steps that unlock new deductions across the opposite axis (cross-line chaining)
  const chainBonus = Math.floor(metrics.crossAxisUnlocks / 4);
  return metrics.steps + placementBonus + incrementalBonus + chainBonus;
}

export function classifyNonogramDifficulty(
  rows: number,
  cols: number,
  metrics: NonogramDifficultyMetrics,
): NonogramCatalogEntry['difficulty'] {
  const score = computeNonogramScore(metrics);
  const shortSide = Math.min(rows, cols);
  const longSide = Math.max(rows, cols);

  // Score-based classification
  let difficulty: NonogramCatalogEntry['difficulty'];

  if (shortSide <= 5 && longSide <= 5) {
    if (score <= 3) difficulty = 'easy';
    else if (score <= 5) difficulty = 'medium';
    else difficulty = 'hard'; // 5x5 is too small to reach genuine expert complexity
  } else if (shortSide <= 5 && longSide <= 10) {
    if (score <= 4) difficulty = 'easy';
    else if (score <= 7) difficulty = 'medium';
    else if (score <= 10) difficulty = 'hard';
    else difficulty = 'expert';
  } else {
    // 10x10
    if (score <= 6) difficulty = 'medium';
    else if (score <= 9) difficulty = 'hard';
    else difficulty = 'expert';
  }

  // Size ceiling gate: expert requires at least 10x10
  if (difficulty === 'expert' && shortSide < 10) difficulty = 'hard';

  // Size floor gate: bigger than 5x5 is at least medium
  if (difficulty === 'easy' && longSide > 5) difficulty = 'medium';

  // Minimum steps gate (cascades downward so each level is independently enforced)
  if (difficulty === 'expert' && metrics.steps < 20) difficulty = 'hard';
  if (difficulty === 'hard' && metrics.steps < 15) difficulty = 'medium';
  if (difficulty === 'medium' && metrics.steps < 10) difficulty = 'easy';

  return difficulty;
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
