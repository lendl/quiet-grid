import {
  applySeededPermutation,
  applySymmetryVariant,
  cloneSolutionGrid,
  solutionToKey,
} from '../engine/solver';
import { classifyNonogramEntry } from '../gameplay/analysis/difficulty';
import { getBuiltInNonogramEntries } from '../platform/puzzleData';
import type { NonogramCatalogEntry } from '../platform/codecs/codec';
import type { PuzzleDifficulty } from '../../shared/types';

export interface NonogramEngineGenerateResult {
  dedupeKey: string;
  entry: Omit<NonogramCatalogEntry, 'id'>;
  label: string;
  score?: number;
}

function pickCandidates(size: number, difficulty: PuzzleDifficulty): NonogramCatalogEntry[] {
  const preferredCols = size <= 5
    ? 5
    : size <= 10
      ? (difficulty === 'easy' ? 5 : 10)
      : (difficulty === 'expert' ? 15 : 10);

  return getBuiltInNonogramEntries()
    .filter((entry) => entry.rows === size)
    .sort((left, right) => {
      const leftExact = Number(left.difficulty === difficulty);
      const rightExact = Number(right.difficulty === difficulty);
      if (leftExact !== rightExact) {
        return rightExact - leftExact;
      }

      const leftDistance = Math.abs(left.cols - preferredCols);
      const rightDistance = Math.abs(right.cols - preferredCols);
      if (leftDistance !== rightDistance) {
        return leftDistance - rightDistance;
      }

      return left.cols - right.cols;
    });
}

function buildCandidateEntry(
  template: NonogramCatalogEntry,
  difficulty: PuzzleDifficulty,
  variantIndex: number,
): Omit<NonogramCatalogEntry, 'id'> {
  const symmetrized = applySymmetryVariant(template.solution, variantIndex);
  const solution = cloneSolutionGrid(applySeededPermutation(symmetrized, variantIndex + template.rows * 31 + template.cols * 17));
  return {
    difficulty,
    rows: solution.length,
    cols: solution[0]?.length ?? 0,
    solution,
  };
}

export function generateNonogramPuzzle(
  size: number,
  targetDifficulty: PuzzleDifficulty,
): NonogramEngineGenerateResult | null {
  const candidates = pickCandidates(size, targetDifficulty);
  if (candidates.length === 0) {
    return null;
  }

  const attempts = Math.max(8, candidates.length * 4);
  const startOffset = Math.floor(Math.random() * candidates.length);
  const variantSeed = Math.floor(Math.random() * 1_000_000);
  let fallback: NonogramEngineGenerateResult | null = null;
  for (let attempt = 0; attempt < attempts; attempt += 1) {
    const template = candidates[(startOffset + attempt) % candidates.length];
    const candidate = buildCandidateEntry(template, targetDifficulty, variantSeed + attempt);
    const classification = classifyNonogramEntry({
      id: 'candidate',
      difficulty: targetDifficulty,
      rows: candidate.rows,
      cols: candidate.cols,
      solution: candidate.solution,
    });

    const generated = {
      dedupeKey: solutionToKey(candidate.solution),
      entry: candidate,
      label: `${candidate.rows}x${candidate.cols} ${targetDifficulty}`,
      score: classification?.metrics.steps,
    };

    if (!fallback) {
      fallback = generated;
    }

    if (classification?.difficulty === targetDifficulty) {
      return generated;
    }
  }

  return fallback;
}
