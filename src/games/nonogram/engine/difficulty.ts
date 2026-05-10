import type { NonogramDifficulty, NonogramPuzzle } from '../types';
import type { NonogramSolveResult } from '../gameplay/rules/solver';

function countSegments(clues: readonly number[][]): number {
  return clues.reduce((total, line) => (
    total + (line.length === 1 && line[0] === 0 ? 0 : line.length)
  ), 0);
}

function countFilledCells(puzzle: NonogramPuzzle): number {
  return puzzle.solution
    .split('')
    .flatMap((char) => {
      const value = parseInt(char, 16);
      return [(value >> 3) & 1, (value >> 2) & 1, (value >> 1) & 1, value & 1];
    })
    .slice(0, puzzle.rows * puzzle.cols)
    .reduce((total, bit) => total + bit, 0);
}

export function classifyNonogramDifficulty(
  puzzle: NonogramPuzzle,
  solveResult: NonogramSolveResult,
): NonogramDifficulty | null {
  if (!solveResult.solved || !solveResult.unique || solveResult.contradiction || solveResult.steps.length === 0) {
    return null;
  }

  const totalSegments = countSegments(puzzle.rowClues) + countSegments(puzzle.colClues);
  const averageSegments = totalSegments / (puzzle.rows + puzzle.cols);
  const firstStepSize = solveResult.steps[0]?.targetCells.length ?? 0;
  const usesOnlyBasicSteps = solveResult.steps.every((step) => step.kind !== 'forced-empty');
  const filledRatio = countFilledCells(puzzle) / (puzzle.rows * puzzle.cols);

  if (
    firstStepSize >= 2
    && averageSegments <= (puzzle.size === 5 ? 1.35 : 1.85)
    && solveResult.steps.length <= (puzzle.size === 5 ? 8 : 26)
    && usesOnlyBasicSteps
    && filledRatio >= 0.28
  ) {
    return 'easy';
  }

  if (
    averageSegments <= (puzzle.size === 5 ? 2.1 : 2.7)
    && solveResult.steps.length <= (puzzle.size === 5 ? 16 : 70)
    && filledRatio >= 0.2
  ) {
    return 'medium';
  }

  return null;
}
