import type { DifficultyMetrics } from './difficultyAnalyzer';
import {
  getRevealBounds,
  SCORE_WEIGHTS,
  SUPPORTED_BUCKETS,
  type DifficultyLabel,
  type DifficultyBucketConfig,
  type SupportedPuzzleSize,
} from './difficultyConfig';

export type { DifficultyLabel };

export interface TechniqueEvaluation {
  highestTechnique: 1 | 2 | 3 | 4 | 5;
  difficulty: DifficultyLabel;
}

const DIFFICULTY_RANKS: Record<DifficultyLabel, number> = {
  easy: 0,
  medium: 1,
  hard: 2,
  expert: 3,
};

export function computeDifficultyScore(size: SupportedPuzzleSize, metrics: DifficultyMetrics): number {
  const revealBounds = getRevealBounds(size);
  const sizeContribution = SCORE_WEIGHTS.sizeBase[size];
  const tipContribution =
    metrics.tipUsageCounts.findPairs * SCORE_WEIGHTS.tipUsage.findPairs +
    metrics.tipUsageCounts.avoidTrios * SCORE_WEIGHTS.tipUsage.avoidTrios +
    metrics.tipUsageCounts.completeLines * SCORE_WEIGHTS.tipUsage.completeLines +
    metrics.tipUsageCounts.eliminateFilledLines * SCORE_WEIGHTS.tipUsage.eliminateFilledLines +
    metrics.tipUsageCounts.eliminateImpossibleCombinations * SCORE_WEIGHTS.tipUsage.eliminateImpossibleCombinations;
  const revealContribution = Math.max(0, revealBounds.max - metrics.givenCount) * SCORE_WEIGHTS.revealGap;

  return sizeContribution +
    revealContribution +
    tipContribution +
    metrics.sparseMoveCount * SCORE_WEIGHTS.sparseMove +
    metrics.tipSequencePressure * SCORE_WEIGHTS.tipSequencePressure +
    metrics.highestTipLevel * SCORE_WEIGHTS.highestTipLevel +
    metrics.openingHighestTipLevel * SCORE_WEIGHTS.openingHighestTipLevel +
    metrics.impossibleCombinationMaxLineCompletions * SCORE_WEIGHTS.impossibleCombinationPressure;
}

export function evaluateTechniques(metrics: DifficultyMetrics): TechniqueEvaluation {
  const highestTechnique =
    metrics.tipUsageCounts.eliminateImpossibleCombinations > 0 ? 5 :
    metrics.tipUsageCounts.eliminateFilledLines > 0 ? 4 :
    metrics.tipUsageCounts.completeLines > 0 ? 3 :
    metrics.tipUsageCounts.avoidTrios > 0 ? 2 :
    1;

  return {
    highestTechnique,
    difficulty: evaluateDifficultyFromTechniques(highestTechnique),
  };
}

export function evaluateDifficultyFromTechniques(highestTechnique: number): DifficultyLabel {
  if (highestTechnique >= 5) return 'expert';
  if (highestTechnique >= 4) return 'hard';
  if (highestTechnique >= 3) return 'medium';
  return 'easy';
}

export function evaluateDifficultyFromScore(
  size: SupportedPuzzleSize,
  score: number,
): DifficultyLabel | null {
  return SUPPORTED_BUCKETS.find((bucket) =>
    bucket.size === size &&
    score >= bucket.minScore &&
    score <= bucket.maxScore,
  )?.difficulty ?? null;
}

export function applyGridSizeConstraints(
  size: SupportedPuzzleSize,
  difficulty: DifficultyLabel,
  highestTechnique: number,
): DifficultyLabel {
  if (size === 6 && DIFFICULTY_RANKS[difficulty] > DIFFICULTY_RANKS.medium) {
    return 'medium';
  }

  if (size === 8 && difficulty === 'expert' && highestTechnique < 5) {
    return 'hard';
  }

  if (size === 10 && difficulty === 'easy') {
    return 'medium';
  }

  return difficulty;
}

export function maxDifficulty(left: DifficultyLabel, right: DifficultyLabel): DifficultyLabel {
  return DIFFICULTY_RANKS[left] >= DIFFICULTY_RANKS[right] ? left : right;
}

export function compareDifficulty(left: DifficultyLabel, right: DifficultyLabel): number {
  return DIFFICULTY_RANKS[left] - DIFFICULTY_RANKS[right];
}

export function getDifficultyBucket(
  size: SupportedPuzzleSize,
  difficulty: DifficultyLabel,
): DifficultyBucketConfig | null {
  return SUPPORTED_BUCKETS.find((bucket) => (
    bucket.size === size && bucket.difficulty === difficulty
  )) ?? null;
}

export function passesDifficultyRails(
  size: SupportedPuzzleSize,
  difficulty: DifficultyLabel,
  metrics: DifficultyMetrics,
): boolean {
  const bucket = getDifficultyBucket(size, difficulty);
  return bucket ? passesSafetyRails(metrics, bucket) : false;
}

export function classifyPuzzleDifficulty(
  size: SupportedPuzzleSize,
  metrics: DifficultyMetrics,
  difficultyScore: number,
): DifficultyLabel | null {
  const technique = evaluateTechniques(metrics);
  const scoreBucket = evaluateDifficultyFromScore(size, difficultyScore);
  if (!scoreBucket) {
    return null;
  }

  const combined = maxDifficulty(technique.difficulty, scoreBucket);
  const constrained = applyGridSizeConstraints(size, combined, technique.highestTechnique);
  const matchingBucket = getDifficultyBucket(size, constrained);
  if (!matchingBucket) {
    return null;
  }

  return passesSafetyRails(metrics, matchingBucket) ? matchingBucket.difficulty : null;
}

export function classifyDifficulty(
  size: SupportedPuzzleSize,
  metrics: DifficultyMetrics,
  difficultyScore: number,
): DifficultyLabel | null {
  return classifyPuzzleDifficulty(size, metrics, difficultyScore);
}

function passesSafetyRails(metrics: DifficultyMetrics, bucket: DifficultyBucketConfig): boolean {
  if (metrics.openingHighestTipLevel > bucket.rails.maxOpeningTipLevel) {
    return false;
  }

  if (metrics.highestTipLevel > bucket.rails.maxOverallTipLevel) {
    return false;
  }

  if (metrics.sparseMoveCount > bucket.rails.maxSparseMoveCount) {
    return false;
  }

  if (!bucket.rails.allowImpossibleCombinations && metrics.tipUsageCounts.eliminateImpossibleCombinations > 0) {
    return false;
  }

  if (metrics.impossibleCombinationMaxLineCompletions > bucket.rails.maxImpossibleCombinationLineCompletions) {
    return false;
  }

  return true;
}
