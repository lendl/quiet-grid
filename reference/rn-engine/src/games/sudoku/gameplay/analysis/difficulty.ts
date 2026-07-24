import type { PuzzleDifficulty } from '../../../shared/types';
import type { SudokuCanonicalMove } from './moves';
import { countSudokuMoveTargets } from './moves';
import {
  compareSudokuTechniques,
  getHardestSudokuTechnique,
  sudokuTechniqueDifficultyFloor,
  type SudokuTechnique,
} from './techniques';

const difficultyOrder = ['easy', 'medium', 'hard', 'expert'] as const satisfies readonly PuzzleDifficulty[];

export const SUDOKU_ENGINE_DIFFICULTIES = difficultyOrder;

export interface SudokuDifficultyWeights {
  perStep: number;
  perPlacement: number;
  perCandidateElimination: number;
  perAdvancedStep: number;
  techniqueWeights: Record<SudokuTechnique, number>;
}

export interface SudokuDifficultyMetrics {
  stepCount: number;
  placementCount: number;
  candidateEliminationCount: number;
  advancedStepCount: number;
  branchingFactor: number;
  branchingScore: number;
  openingTechnique: SudokuTechnique | null;
  hardestTechnique: SudokuTechnique | null;
  techniqueCounts: Partial<Record<SudokuTechnique, number>>;
  maxComplexityPerTechnique: Partial<Record<SudokuTechnique, number>>;
}

export interface SudokuDifficultySafetyRails {
  maxOpeningTechnique: SudokuTechnique | null;
  maxOverallTechnique: SudokuTechnique | null;
  maxStepCount: number;
  maxAdvancedStepCount: number;
  maxComplexityPerTechnique: Partial<Record<SudokuTechnique, number>>;
}

export interface SudokuDifficultyProfile {
  scoreBand: { min: number; max: number };
  techniqueCeiling: SudokuTechnique;
  safetyRails: SudokuDifficultySafetyRails;
}

export interface SudokuDifficultyClassification {
  difficulty: PuzzleDifficulty;
  score: number;
  scoreBucket: PuzzleDifficulty;
  techniqueBucket: PuzzleDifficulty;
  usedSafetyRails: SudokuDifficultySafetyRails;
}

export const DEFAULT_SUDOKU_DIFFICULTY_WEIGHTS: SudokuDifficultyWeights = {
  perStep: 6,
  perPlacement: 4,
  perCandidateElimination: 3,
  perAdvancedStep: 5,
  techniqueWeights: {
    'naked-single': 2,
    'hidden-single': 3,
    'naked-pair': 6,
    'hidden-pair': 9,
    'pointing-pair-triple': 10,
    'box-line-reduction': 10,
    'x-wing': 17,
    'swordfish': 24,
    'xy-wing': 21,
    'xyz-wing': 28,
    coloring: 27,
    chains: 30,
  },
};

export const SUDOKU_DIFFICULTY_PROFILES: Record<PuzzleDifficulty, SudokuDifficultyProfile> = {
  easy: {
    scoreBand: { min: 0, max: 360 },
    techniqueCeiling: 'naked-pair',
    safetyRails: {
      maxOpeningTechnique: 'naked-pair',
      maxOverallTechnique: 'naked-pair',
      maxStepCount: 64,
      maxAdvancedStepCount: 0,
      maxComplexityPerTechnique: {
        'naked-single': 3,
        'hidden-single': 4,
        'naked-pair': 5,
      },
    },
  },
  medium: {
    scoreBand: { min: 361, max: 760 },
    techniqueCeiling: 'box-line-reduction',
    safetyRails: {
      maxOpeningTechnique: 'pointing-pair-triple',
      maxOverallTechnique: 'box-line-reduction',
      maxStepCount: 108,
      maxAdvancedStepCount: 24,
      maxComplexityPerTechnique: {
        'naked-single': 6,
        'hidden-single': 7,
        'naked-pair': 8,
        'hidden-pair': 7,
        'pointing-pair-triple': 5,
        'box-line-reduction': 5,
      },
    },
  },
  hard: {
    scoreBand: { min: 761, max: 1120 },
    techniqueCeiling: 'xy-wing',
    safetyRails: {
      maxOpeningTechnique: 'naked-pair',
      maxOverallTechnique: 'xy-wing',
      maxStepCount: 148,
      maxAdvancedStepCount: 56,
      maxComplexityPerTechnique: {
        'x-wing': 6,
        'swordfish': 7,
        'xy-wing': 12,
        coloring: 8,
      },
    },
  },
  expert: {
    scoreBand: { min: 1121, max: Number.POSITIVE_INFINITY },
    techniqueCeiling: 'chains',
    safetyRails: {
      maxOpeningTechnique: 'x-wing',
      maxOverallTechnique: 'chains',
      maxStepCount: 280,
      maxAdvancedStepCount: 128,
      maxComplexityPerTechnique: {},
    },
  },
};

function getDifficultyRank(difficulty: PuzzleDifficulty): number {
  return difficultyOrder.indexOf(difficulty);
}

function maxDifficulty(a: PuzzleDifficulty, b: PuzzleDifficulty): PuzzleDifficulty {
  return getDifficultyRank(a) >= getDifficultyRank(b) ? a : b;
}

function isTechniqueWithinCeiling(
  technique: SudokuTechnique | null,
  ceiling: SudokuTechnique | null,
): boolean {
  if (technique === null || ceiling === null) {
    return true;
  }

  return compareSudokuTechniques(technique, ceiling) <= 0;
}

export function collectSudokuDifficultyMetrics(
  moves: readonly SudokuCanonicalMove[],
): SudokuDifficultyMetrics {
  const techniqueCounts = moves.reduce<Partial<Record<SudokuTechnique, number>>>((acc, move) => {
    acc[move.technique] = (acc[move.technique] ?? 0) + 1;
    return acc;
  }, {});
  const maxComplexityPerTechnique = moves.reduce<Partial<Record<SudokuTechnique, number>>>((acc, move) => {
    const current = acc[move.technique] ?? 0;
    acc[move.technique] = Math.max(current, move.complexity);
    return acc;
  }, {});
  const usedTechniques = moves.map((move) => move.technique);
  const hardestTechnique = getHardestSudokuTechnique(usedTechniques);
  const openingTechnique = moves[0]?.technique ?? null;
  const candidateEliminationCount = moves
    .filter((move) => move.kind === 'candidate-elimination')
    .reduce((count, move) => count + countSudokuMoveTargets(move), 0);
  const advancedStepCount = moves.filter((move) => (
    sudokuTechniqueDifficultyFloor[move.technique] === 'hard'
    || sudokuTechniqueDifficultyFloor[move.technique] === 'expert'
  )).length;
  const branchingFactor = moves.length === 0 ? 0 : candidateEliminationCount / moves.length;
  const branchingScore = moves.reduce((count, move) => (
    count + (move.kind === 'candidate-elimination' ? Math.max(1, countSudokuMoveTargets(move)) : 0)
  ), 0);

  return {
    stepCount: moves.length,
    placementCount: moves.filter((move) => move.kind === 'placement').length,
    candidateEliminationCount,
    advancedStepCount,
    branchingFactor,
    branchingScore,
    openingTechnique,
    hardestTechnique,
    techniqueCounts,
    maxComplexityPerTechnique,
  };
}

export function computeSudokuDifficultyScore(
  metrics: SudokuDifficultyMetrics,
  weights: SudokuDifficultyWeights = DEFAULT_SUDOKU_DIFFICULTY_WEIGHTS,
): number {
  const techniqueScore = Object.entries(metrics.techniqueCounts).reduce((total, [technique, count]) => (
    total + ((count ?? 0) * weights.techniqueWeights[technique as SudokuTechnique])
  ), 0);

  return (
    (metrics.stepCount * weights.perStep)
    + (metrics.placementCount * weights.perPlacement)
    + (metrics.candidateEliminationCount * weights.perCandidateElimination)
    + (metrics.advancedStepCount * weights.perAdvancedStep)
    + Math.round(metrics.branchingFactor * 10)
    + metrics.branchingScore
    + techniqueScore
  );
}

export function getSudokuDifficultyScoreBucket(score: number): PuzzleDifficulty {
  if (score <= SUDOKU_DIFFICULTY_PROFILES.easy.scoreBand.max) {
    return 'easy';
  }
  if (score <= SUDOKU_DIFFICULTY_PROFILES.medium.scoreBand.max) {
    return 'medium';
  }
  if (score <= SUDOKU_DIFFICULTY_PROFILES.hard.scoreBand.max) {
    return 'hard';
  }
  return 'expert';
}

export function getSudokuDifficultyTechniqueBucket(
  metrics: SudokuDifficultyMetrics,
): PuzzleDifficulty {
  return metrics.hardestTechnique
    ? sudokuTechniqueDifficultyFloor[metrics.hardestTechnique]
    : 'easy';
}

export function passesSudokuDifficultySafetyRails(
  difficulty: PuzzleDifficulty,
  metrics: SudokuDifficultyMetrics,
): boolean {
  const profile = SUDOKU_DIFFICULTY_PROFILES[difficulty];
  const rails = profile.safetyRails;

  if (!isTechniqueWithinCeiling(metrics.openingTechnique, rails.maxOpeningTechnique)) {
    return false;
  }
  if (!isTechniqueWithinCeiling(metrics.hardestTechnique, rails.maxOverallTechnique)) {
    return false;
  }
  if (metrics.stepCount > rails.maxStepCount) {
    return false;
  }
  if (metrics.advancedStepCount > rails.maxAdvancedStepCount) {
    return false;
  }

  for (const [technique, maxComplexity] of Object.entries(rails.maxComplexityPerTechnique)) {
    const observed = metrics.maxComplexityPerTechnique[technique as SudokuTechnique];
    if (observed !== undefined && observed > (maxComplexity ?? Infinity)) {
      return false;
    }
  }

  return true;
}

export function classifySudokuDifficulty(
  metrics: SudokuDifficultyMetrics,
  score = computeSudokuDifficultyScore(metrics),
): SudokuDifficultyClassification | null {
  const scoreBucket = getSudokuDifficultyScoreBucket(score);
  const techniqueBucket = getSudokuDifficultyTechniqueBucket(metrics);
  let difficulty = metrics.hardestTechnique === null ? 'easy' : techniqueBucket;

  while (!passesSudokuDifficultySafetyRails(difficulty, metrics)) {
    const nextDifficulty = difficultyOrder[getDifficultyRank(difficulty) + 1];
    if (!nextDifficulty) {
      return null;
    }
    difficulty = nextDifficulty;
  }

  return {
    difficulty: maxDifficulty(difficulty, techniqueBucket),
    score,
    scoreBucket,
    techniqueBucket,
    usedSafetyRails: SUDOKU_DIFFICULTY_PROFILES[difficulty].safetyRails,
  };
}
