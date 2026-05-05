import { countSolutions } from './solver';
import type { Cell } from './validator';
import { shuffle } from './encoding';
import { gridToHex, maskToHex } from './encoding';
import { analyzeDifficulty } from './difficultyAnalyzer';
import {
  compareDifficulty,
  computeDifficultyScore,
  classifyPuzzleDifficulty,
  passesDifficultyRails,
} from './difficultyScore';
import {
  getTargetRevealBounds,
  type DifficultyLabel,
  type SupportedPuzzleSize,
} from './difficultyConfig';

function getRandomIntInclusive(min: number, max: number): number {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

function buildMaskFromRevealed(size: number, revealed: Set<number>): boolean[][] {
  return Array.from({ length: size }, (_, row) =>
    Array.from({ length: size }, (_, col) => revealed.has(row * size + col)),
  );
}

function buildPuzzleFromRevealed(grid: (0 | 1)[][], revealed: Set<number>): Cell[][] {
  const size = grid.length;
  return grid.map((row, r) =>
    row.map((value, c) => (revealed.has(r * size + c) ? value : null)),
  );
}

function evaluateCandidatePuzzle(
  grid: (0 | 1)[][],
  revealed: Set<number>,
  targetDifficulty: DifficultyLabel,
): {
  difficulty: DifficultyLabel;
  score: number;
} | null {
  const size = grid.length as SupportedPuzzleSize;
  const puzzle = buildPuzzleFromRevealed(grid, revealed);
  const solutionCount = countSolutions(puzzle);
  if (solutionCount !== 1) {
    return null;
  }

  const maskGrid = buildMaskFromRevealed(size, revealed);
  const solutionHex = gridToHex(grid);
  const maskHex = maskToHex(maskGrid);
  const metrics = analyzeDifficulty(solutionHex, maskHex, size);
  const score = computeDifficultyScore(size, metrics);
  const difficulty = classifyPuzzleDifficulty(size, metrics, score);
  if (!difficulty) {
    return null;
  }

  if (!passesDifficultyRails(size, targetDifficulty, metrics)) {
    return null;
  }

  if (compareDifficulty(difficulty, targetDifficulty) > 0) {
    return null;
  }

  return { difficulty, score };
}

function generateSparseMask(
  grid: (0 | 1)[][],
  minReveal: number,
  maxReveal: number,
  targetDifficulty: DifficultyLabel,
): boolean[][] | null {
  const size = grid.length;
  const total = size * size;
  const targetReveal = getRandomIntInclusive(minReveal, maxReveal);

  const revealed = new Set(Array.from({ length: total }, (_, index) => index));
  let progress = true;

  while (progress && revealed.size > minReveal) {
    progress = false;
    const indices = Array.from(revealed);
    shuffle(indices);

    for (const idx of indices) {
      if (revealed.size <= minReveal) {
        break;
      }

      revealed.delete(idx);

      const evaluation = evaluateCandidatePuzzle(grid, revealed, targetDifficulty);
      if (!evaluation) {
        revealed.add(idx);
        continue;
      }

      progress = true;

      if (revealed.size <= targetReveal && evaluation.difficulty === targetDifficulty) {
        break;
      }
    }

    const currentEvaluation = evaluateCandidatePuzzle(grid, revealed, targetDifficulty);
    if (revealed.size <= targetReveal && currentEvaluation?.difficulty === targetDifficulty) {
      break;
    }
  }

  if (revealed.size < minReveal || revealed.size > maxReveal) {
    return null;
  }

  const finalEvaluation = evaluateCandidatePuzzle(grid, revealed, targetDifficulty);
  if (!finalEvaluation || finalEvaluation.difficulty !== targetDifficulty) {
    return null;
  }

  return buildMaskFromRevealed(size, revealed);
}

/**
 * Generates a boolean mask for the given solution grid.
 * Removes cells from a fully revealed board while preserving exactly one solution
 * and keeping the puzzle within the requested difficulty rails.
 */
export function generateMask(
  grid: (0 | 1)[][],
  targetDifficulty: DifficultyLabel,
): boolean[][] | null {
  const size = grid.length;
  if (size !== 6 && size !== 8 && size !== 10) {
    throw new Error(`Unsupported puzzle size: ${size}. Engine mask generation supports 6x6, 8x8, and 10x10 puzzles only.`);
  }

  const revealRange = getTargetRevealBounds(size, targetDifficulty);
  return generateSparseMask(grid, revealRange.min, revealRange.max, targetDifficulty);
}
