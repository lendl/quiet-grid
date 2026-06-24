import type { Difficulty } from '../types';

export const ACCURACY_DROP_STEP = 10;
export const ACCURACY_SCORE_COST = 500;
export const MAX_SCORE = 10000;
export const MIN_SCORE = 1000;

export function computeFinalScore(
  difficulty: Difficulty,
  timeSeconds: number,
  accuracyDrops: number,
  timeCaps: Record<Difficulty, number>,
): number {
  const timePenalty = (Math.max(0, timeSeconds) * MAX_SCORE) / timeCaps[difficulty];
  const accuracyPenalty = accuracyDrops * ACCURACY_SCORE_COST;
  return Math.max(MIN_SCORE, Math.round(MAX_SCORE - timePenalty - accuracyPenalty));
}

export function computeAccuracyPct(accuracyDrops: number): number {
  return Math.max(0, 100 - accuracyDrops * ACCURACY_DROP_STEP);
}
