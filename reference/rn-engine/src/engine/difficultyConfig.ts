export type SupportedPuzzleSize = 6 | 8 | 10;
export type DifficultyLabel = 'easy' | 'medium' | 'hard' | 'expert';

export interface DifficultySafetyRail {
  maxOpeningTipLevel: number;
  maxOverallTipLevel: number;
  maxSparseMoveCount: number;
  allowImpossibleCombinations: boolean;
  maxImpossibleCombinationLineCompletions: number;
  minGivenCount?: number;
  maxGivenCount?: number;
}

export interface DifficultyBucketConfig {
  size: SupportedPuzzleSize;
  difficulty: DifficultyLabel;
  minScore: number;
  maxScore: number;
  rails: DifficultySafetyRail;
}

export const SUPPORTED_PUZZLE_SIZES: SupportedPuzzleSize[] = [6, 8, 10];

export const SUPPORTED_BUCKETS: DifficultyBucketConfig[] = [
  {
    size: 6,
    difficulty: 'easy',
    minScore: 0,
    maxScore: 999,
    rails: {
      maxOpeningTipLevel: 2,
      maxOverallTipLevel: 4,
      maxSparseMoveCount: 24,
      allowImpossibleCombinations: true,
      maxImpossibleCombinationLineCompletions: 2,
      minGivenCount: 10,
      maxGivenCount: 14,
    },
  },
  {
    size: 6,
    difficulty: 'medium',
    minScore: 1000,
    maxScore: Number.POSITIVE_INFINITY,
    rails: {
      maxOpeningTipLevel: 3,
      maxOverallTipLevel: 4,
      maxSparseMoveCount: 30,
      allowImpossibleCombinations: true,
      maxImpossibleCombinationLineCompletions: 4,
      minGivenCount: 6,
      maxGivenCount: 10,
    },
  },
  {
    size: 8,
    difficulty: 'easy',
    minScore: 0,
    maxScore: 1199,
    rails: {
      maxOpeningTipLevel: 2,
      maxOverallTipLevel: 4,
      maxSparseMoveCount: 36,
      allowImpossibleCombinations: true,
      maxImpossibleCombinationLineCompletions: 2,
    },
  },
  {
    size: 8,
    difficulty: 'medium',
    minScore: 1200,
    maxScore: 1700,
    rails: {
      maxOpeningTipLevel: 4,
      maxOverallTipLevel: 4,
      maxSparseMoveCount: 40,
      allowImpossibleCombinations: true,
      maxImpossibleCombinationLineCompletions: 4,
    },
  },
  {
    size: 8,
    difficulty: 'hard',
    minScore: 1450,
    maxScore: Number.POSITIVE_INFINITY,
    rails: {
      maxOpeningTipLevel: 4,
      maxOverallTipLevel: 4,
      maxSparseMoveCount: Number.POSITIVE_INFINITY,
      allowImpossibleCombinations: true,
      maxImpossibleCombinationLineCompletions: 8,
    },
  },
  {
    size: 10,
    difficulty: 'medium',
    minScore: 0,
    maxScore: 2350,
    rails: {
      maxOpeningTipLevel: 2,
      maxOverallTipLevel: 4,
      maxSparseMoveCount: 56,
      allowImpossibleCombinations: true,
      maxImpossibleCombinationLineCompletions: 4,
    },
  },
  {
    size: 10,
    difficulty: 'hard',
    minScore: 1900,
    maxScore: 2750,
    rails: {
      maxOpeningTipLevel: 4,
      maxOverallTipLevel: 4,
      maxSparseMoveCount: Number.POSITIVE_INFINITY,
      allowImpossibleCombinations: true,
      maxImpossibleCombinationLineCompletions: 8,
    },
  },
  {
    size: 10,
    difficulty: 'expert',
    minScore: 2500,
    maxScore: Number.POSITIVE_INFINITY,
    rails: {
      maxOpeningTipLevel: 4,
      maxOverallTipLevel: 4,
      maxSparseMoveCount: Number.POSITIVE_INFINITY,
      allowImpossibleCombinations: true,
      maxImpossibleCombinationLineCompletions: 15,
    },
  },
];

export const SCORE_WEIGHTS = {
  sizeBase: { 6: 120, 8: 340, 10: 620 } as const,
  revealGap: 18,
  sparseMove: 10,
  tipSequencePressure: 22,
  highestTipLevel: 40,
  openingHighestTipLevel: 56,
  impossibleCombinationPressure: 18,
  tipUsage: {
    findPairs: 4,
    avoidTrios: 6,
    completeLines: 9,
    eliminateFilledLines: 16,
    eliminateImpossibleCombinations: 24,
  },
};

export function getRevealBounds(size: SupportedPuzzleSize): { min: number; max: number } {
  const totalCells = size * size;
  return {
    min: Math.floor(totalCells * 0.15),
    max: Math.floor(totalCells * 0.25),
  };
}

export function getTargetRevealBounds(
  size: SupportedPuzzleSize,
  difficulty: DifficultyLabel,
): { min: number; max: number } {
  const bucket = SUPPORTED_BUCKETS.find((entry) => entry.size === size && entry.difficulty === difficulty);
  if (!bucket) {
    return getRevealBounds(size);
  }

  return {
    min: bucket.rails.minGivenCount ?? getRevealBounds(size).min,
    max: bucket.rails.maxGivenCount ?? getRevealBounds(size).max,
  };
}
